package whu.edu.cs.transitnet.realtime;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class RealtimeSocketHandler extends TextWebSocketHandler {

    @Autowired
    private RealtimeService realtimeService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // TODO: 校验 token 避免 DDOS 攻击
        String token = session.getId();
        WsSocketManager.add(token, session);
        // 连接后即将实时数据发送给客户端。
        String msg = JSON.toJSONString(realtimeService.getAllVehicles());
        session.sendMessage(new TextMessage(msg));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 暂时不处理前端传入的消息
        log.info("[socket]message from session {}: {}", session.getRemoteAddress(), message.toString());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 用户退出
        String token = session.getId();
        boolean result = WsSocketManager.remove(token);
        if (!result) {
            log.warn("[socket]Error while user connection closed, remove session from pool failed.");
        }
    }

}
