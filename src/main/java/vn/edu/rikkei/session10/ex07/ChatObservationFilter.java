package vn.edu.rikkei.session10.ex07;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.content.Content;
import org.springframework.ai.observation.ObservabilityHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Adds {@code gen_ai.prompt} and {@code gen_ai.completion} as high-cardinality
 * OpenTelemetry span attributes on every Spring AI chat model observation.
 *
 * <p>Without this filter, even with {@code log-prompt: true} and
 * {@code log-completion: true} in YAML, the content never reaches the OTel spans
 * and therefore never appears in Langfuse.
 *
 * <p>Required by the official Langfuse Spring AI integration guide.
 */
@Component
public class ChatObservationFilter implements ObservationFilter {

    @Override
    public Observation.Context map(Observation.Context context) {
        if (!(context instanceof ChatModelObservationContext chatCtx)) {
            return context;
        }

        List<String> prompts     = extractPrompts(chatCtx);
        List<String> completions = extractCompletions(chatCtx);

        chatCtx.addHighCardinalityKeyValue(keyValue("gen_ai.prompt",     ObservabilityHelper.concatenateStrings(prompts)));
        chatCtx.addHighCardinalityKeyValue(keyValue("gen_ai.completion", ObservabilityHelper.concatenateStrings(completions)));

        return chatCtx;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static KeyValue keyValue(String key, String value) {
        return new KeyValue() {
            @Override public String getKey()   { return key; }
            @Override public String getValue() { return value; }
        };
    }

    private static List<String> extractPrompts(ChatModelObservationContext ctx) {
        var instructions = ctx.getRequest().getInstructions();
        if (CollectionUtils.isEmpty(instructions)) return List.of();
        return instructions.stream().map(Content::getText).toList();
    }

    private static List<String> extractCompletions(ChatModelObservationContext ctx) {
        var response = ctx.getResponse();
        if (response == null || CollectionUtils.isEmpty(response.getResults())) return List.of();
        return response.getResults().stream()
                .filter(g -> g.getOutput() != null && StringUtils.hasText(g.getOutput().getText()))
                .map(g -> g.getOutput().getText())
                .toList();
    }
}
