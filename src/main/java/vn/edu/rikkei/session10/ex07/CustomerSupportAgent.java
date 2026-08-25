package vn.edu.rikkei.session10.ex07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CustomerSupportAgent {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(CustomerSupportAgent.class, args);
        ChatService chatService = ctx.getBean(ChatService.class);
        chatService.startChatLoop();
    }
}
