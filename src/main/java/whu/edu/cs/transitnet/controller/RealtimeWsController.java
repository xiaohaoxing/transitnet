package whu.edu.cs.transitnet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import whu.edu.cs.transitnet.realtime.RealtimeSocketHandler;

@Configuration
@EnableWebSocket
public class RealtimeWsController implements WebSocketConfigurer {
    @Autowired
    private RealtimeSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "realtime")
                .setAllowedOrigins("*");
    }
}
