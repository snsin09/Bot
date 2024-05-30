package com.bot.inori.bot.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ServerEndpoint(value = "/bot/v1")
public class WebsocketServer {

    Logger logger = LoggerFactory.getLogger(WebsocketServer.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("access_token") String access_token) {
        logger.info("ws客户端连接：被动连接 {} {}", session.getId(), access_token);
        //缓存Bot信息 群列表
        SimpleMessageUtils.getLoginInfo(session);
        SimpleMessageUtils.getGroupList(session);
        BotHandler.setSession(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JSONObject json = JSON.parseObject(message);
        logger.debug("ws服务器收到消息" + message);
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
        logger.warn("ws断开连接：{}", reason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("ws出现错误：{}", throwable.getMessage());
    }
}
