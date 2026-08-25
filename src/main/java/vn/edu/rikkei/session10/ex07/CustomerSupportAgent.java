package vn.edu.rikkei.session10.ex07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry point for the RikkeiMart Customer Support AI Agent.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Bootstraps the Spring Boot application context.</li>
 *   <li>Delegates the interactive chat loop to {@link ChatService}.</li>
 * </ul>
 *
 * <p>Package: {@code vn.edu.rikkei.session10.ex07}
 */
@SpringBootApplication
public class CustomerSupportAgent {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(CustomerSupportAgent.class, args);
        ChatService chatService = ctx.getBean(ChatService.class);
        chatService.startChatLoop();
    }
}
