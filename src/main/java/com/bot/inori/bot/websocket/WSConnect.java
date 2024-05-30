package com.bot.inori.bot.websocket;

import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.MessageHandler;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Component
public class WSConnect {

    @Getter
    private static Session session;

    public WSConnect() {
        connect();
    }

    public static Boolean connect() {
        try {
            if (!StringUtils.hasLength(BaseConfig.WSClient)) return false;
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(WebsocketClient.class, URI.create(BaseConfig.WSClient));
            return true;
        } catch (Exception e) {
            MessageHandler.getLogger().error("初始ws配置失败 {}", e.getMessage());
            return false;
        }
    }
}
