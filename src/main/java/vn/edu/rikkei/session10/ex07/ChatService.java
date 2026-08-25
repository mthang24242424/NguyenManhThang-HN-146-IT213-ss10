package vn.edu.rikkei.session10.ex07;

import com.langfuse.client.LangfuseClient;
import com.langfuse.client.resources.prompts.types.Prompt;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Service
public class ChatService {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String PROMPT_NAME     = "rikkei-support-prompt";
    private static final String PROMPT_VARIABLE = "{{user_question}}";
    private static final String EXIT_COMMAND    = "exit";

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final ChatModel          chatModel;
    private final LangfuseProperties langfuseProps;
    private final Tracer             tracer;

    // ── State ─────────────────────────────────────────────────────────────────
    /** Raw system prompt text fetched from Langfuse at start-up. */
    private String systemPromptTemplate;

    public ChatService(ChatModel chatModel,
                       LangfuseProperties langfuseProps,
                       Tracer tracer) {
        this.chatModel     = chatModel;
        this.langfuseProps = langfuseProps;
        this.tracer        = tracer;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the Langfuse client, downloads the remote prompt, then enters
     * the interactive chat loop until the user types {@code exit}.
     */
    public void startChatLoop() {
        LangfuseClient langfuse = buildLangfuseClient();

        if (!fetchPromptTemplate(langfuse)) {
            System.err.println("[ERROR] Không thể tải prompt. Vui lòng kiểm tra cấu hình Langfuse.");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== RIKKEI MART AI SUPPORT ===");
        System.out.print("Vui lòng nhập Mã Khách Hàng (User ID): ");
        String userId    = scanner.nextLine().trim();
        String sessionId = UUID.randomUUID().toString();

        while (true) {
            System.out.println("\nNhập câu hỏi của bạn (Gõ '" + EXIT_COMMAND + "' để thoát):");
            System.out.print("> ");
            String userQuestion = scanner.nextLine().trim();

            if (EXIT_COMMAND.equalsIgnoreCase(userQuestion)) {
                System.out.println("Cảm ơn bạn đã sử dụng dịch vụ. Tạm biệt!");
                break;
            }

            if (userQuestion.isEmpty()) continue;

            processQuestion(userQuestion, userId, sessionId);
            System.out.println("==============================");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Instantiates a {@link LangfuseClient} from the YAML-bound properties.
     */
    private LangfuseClient buildLangfuseClient() {
        return LangfuseClient.builder()
                .url(langfuseProps.getHost())
                .credentials(langfuseProps.getPublicKey(), langfuseProps.getSecretKey())
                .build();
    }

    /**
     * Fetches the {@value #PROMPT_NAME} text prompt from Langfuse and stores it
     * in {@link #systemPromptTemplate}.
     *
     * @return {@code true} on success, {@code false} on any error.
     */
    private boolean fetchPromptTemplate(LangfuseClient langfuse) {
        System.out.printf("[Langfuse] Đang tải prompt '%s' từ server... ", PROMPT_NAME);
        try {
            // prompts().get() returns Prompt – a union type with isText()/getText()
            Prompt response = langfuse.prompts().get(PROMPT_NAME);

            if (response.isText() && response.getText().isPresent()) {
                systemPromptTemplate = response.getText().get().getPrompt();
                System.out.println("Thành công!");
                return true;
            }

            System.out.println("LỖI – Prompt không phải kiểu Text.");
            return false;

        } catch (Exception e) {
            System.out.println("LỖI – " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends one user question to the LLM inside a parent OTel span that carries
     * the Langfuse user/session attributes, then prints the answer and usage stats.
     */
    private void processQuestion(String userQuestion, String userId, String sessionId) {
        System.out.println("[AI Processing] Đang phân tích yêu cầu...");

        // Build the final system prompt by substituting the variable
        String systemPrompt = systemPromptTemplate.replace(PROMPT_VARIABLE, userQuestion);

        // Open a parent span – Spring AI will create a child span for the LLM call.
        // The langfuse.* attributes are read by Langfuse's OTel ingestion to set
        // the user ID, session ID, input, and output on the root trace.
        Span parentSpan = tracer.spanBuilder("chat-interaction")
                .setAttribute("langfuse.user.id",            userId)
                .setAttribute("langfuse.session.id",         sessionId)
                .setAttribute("langfuse.observation.input",  userQuestion)
                .startSpan();

        String  aiAnswer;
        int     inputTokens  = 0;
        int     outputTokens = 0;
        String  traceId      = parentSpan.getSpanContext().getTraceId();

        try (Scope ignored = parentSpan.makeCurrent()) {
            // Build the Spring AI prompt with a system message and user message
            org.springframework.ai.chat.prompt.Prompt springPrompt =
                    new org.springframework.ai.chat.prompt.Prompt(List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(userQuestion)
                    ));

            ChatResponse chatResponse = chatModel.call(springPrompt);
            aiAnswer = chatResponse.getResult().getOutput().getText();

            // Extract token usage when available
            var usage = chatResponse.getMetadata().getUsage();
            if (usage != null) {
                inputTokens  = usage.getPromptTokens()     != null ? usage.getPromptTokens().intValue()     : 0;
                outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().intValue() : 0;
            }

            parentSpan.setAttribute("langfuse.observation.output", aiAnswer);

        } finally {
            parentSpan.end();
        }

        // ── Console output ────────────────────────────────────────────────────
        System.out.println("\nAI: " + aiAnswer);
        System.out.println();
        System.out.printf("[System] Lượt chat đã được lưu Trace ID: %s (User: %s).%n", traceId, userId);
        System.out.printf("[System] Token sử dụng: Input (%d), Output (%d). Tổng cộng: %d tokens.%n",
                inputTokens, outputTokens, inputTokens + outputTokens);
    }
}
