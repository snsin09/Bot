package com.bot.inori.bot.config;

import com.bot.inori.bot.handler.MessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WSServerConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        MessageHandler.getLogger().info(String.format("ws服务器连接地址：ws://127.0.0.1:%s/bot/v1", BaseConfig.serverPort));
        return new ServerEndpointExporter();
    }
}
