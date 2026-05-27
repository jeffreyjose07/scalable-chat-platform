package com.chatplatform.service;

import com.chatplatform.dto.MessageDistributionEvent;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MessageDistributionService {

    private static final Logger logger = LoggerFactory.getLogger(MessageDistributionService.class);

    private final ChatWebSocketHandler webSocketHandler;

    public MessageDistributionService(ChatWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @EventListener
    @Async
    public void handleMessageDistribution(MessageDistributionEvent event) {
        try {
            logger.info("📢 Received MessageDistributionEvent for message: {}", event.message().getId());
            webSocketHandler.broadcastMessage(event.message());
        } catch (Exception e) {
            logger.error("❌ Error distributing message: {}", e.getMessage());
        }
    }
}