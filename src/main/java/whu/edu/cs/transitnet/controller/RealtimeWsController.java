package whu.edu.cs.transitnet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${transitnet.realtime.baseuri}")
    private String baseuri = "";

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, baseuri + "/realtime")
                .setAllowedOrigins("*");
    }
}
