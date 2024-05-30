package com.bot.inori.bot.config;

import com.bot.inori.bot.handler.MessageHandler;
import jakarta.websocket.ClientEndpointConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.List;
import java.util.Map;

@Configuration
public class WSClientConfig extends ClientEndpointConfig.Configurator {

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Upgrade", List.of("websocket"));
        headers.put("Connection", List.of("upgrade"));
        headers.put("Authorization", List.of("Bearer " + BaseConfig.AccessToken));
        super.beforeRequest(headers);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        MessageHandler.getLogger().info("加载缓存大小配置");
        container.setMaxTextMessageBufferSize(5_000_000);
        container.setMaxBinaryMessageBufferSize(5_000_000);
        container.setMaxSessionIdleTimeout(15 * 60000L);
        return container;
    }
}
