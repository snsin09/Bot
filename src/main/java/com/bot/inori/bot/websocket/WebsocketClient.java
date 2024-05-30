package com.bot.inori.bot.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.config.WSClientConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ClientEndpoint(configurator = WSClientConfig.class)
public class WebsocketClient {

    Logger logger = LoggerFactory.getLogger(WebsocketClient.class);

    @OnOpen
    public void onOpen(Session session) {
        try {
            logger.info("ws打开连接：主动连接 {}", session.getId());
            if (WSConnect.getSession() != null && !Objects.equals(WSConnect.getSession().getId(), session.getId()))
                WSConnect.getSession().close();
            SimpleMessageUtils.getLoginInfo(session);
            SimpleMessageUtils.getGroupList(session);
            BotHandler.setSession(session);
        } catch (Exception e) {
            logger.error("ws打开连接报错！{}", e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        JSONObject json = JSON.parseObject(message);
        logger.info("ws客户端收到消息" + message);
        String post_type = json.getString("post_type");
        if (post_type != null) {
            switch (post_type) {
                case "message_sent", "message" -> MessageHandler.handleReceiveMessage(session, json);
                case "notice" -> MessageHandler.handleNoticeMessage(session, json);
                case "request" -> MessageHandler.handleRequestMessage(session, json);
                case "meta_event" -> {
                    //跳过心跳消息
                }
                default -> logger.info("不支持的消息类型 {}", json.getString("post_type"));
            }
        } else MessageHandler.handleResponseMessage(session, json);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        try {
            logger.warn("ws关闭连接：{} {}", session.getId(), reason.toString());
            //断开连接后执行重连
            int i = 1;
            while (true) {
                logger.info("{} 开始执行第 {} 次重连", session.getId(), i++);
                boolean flag = WSConnect.connect();
                if (flag) break;
                Thread.sleep(30_000);
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("断线重连报错！ {}", e.getMessage());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("ws出现错误：{} {}", session.getId(), throwable.getMessage());
    }
}
