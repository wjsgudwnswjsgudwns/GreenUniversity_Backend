package com.green.university.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 처음 연결할 엔드포인트
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // 개발 단계: 모두 허용 (나중에 도메인 제한)
                .withSockJS();                 // SockJS fallback 사용 (옵션)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 → 서버로 보낼 prefix
        registry.setApplicationDestinationPrefixes("/pub");

        // 서버 → 클라이언트로 브로드캐스트할 prefix
        registry.enableSimpleBroker("/sub", "/queue");
        // /sub/**, /queue/** 경로는 메시지 브로커가 처리 (in-memory)
    }
}