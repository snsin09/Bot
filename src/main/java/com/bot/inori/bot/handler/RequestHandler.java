package com.bot.inori.bot.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.model.req.GroupLeaveMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.List;

public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    public static void handleRequestMessage(Session session, JSONObject json) {
        try (Jedis jedis = new Jedis()) {
            List<Object> messages = new ArrayList<>();
            switch (json.getString("request_type")) {
                case "friend" -> {
                    if (BaseConfig.friendInfo) {
                        messages.add(new TextMessage(String.format("用户%s请求加好友，验证信息：%s",
                                json.getLong("user_id"), json.getString("comment"))));
                    }
                    SimpleMessageUtils.sendMaster(messages);
                }
                case "group" -> {
                    logger.info(json.toJSONString());
                    if (BaseConfig.groupInfo || "invite".equalsIgnoreCase(json.getString("sub_type"))) {
                        messages.add(new TextMessage(String.format("收到加群%s\n群号：%s\n请求人：%s\n验证信息：%s",
                                "add".equalsIgnoreCase(json.getString("sub_type")) ? "请求" : "邀请",
                                json.getLong("group_id"),
                                json.getLong("user_id"),
                                json.getString("comment"))));
                        SimpleMessageUtils.sendMaster(messages);
                    }
                    if (!BotHandler.isMaster(json.getLong("user_id"))
                            && "invite".equals(json.getString("sub_type"))) {
                        Boolean flag = checkGroupNumber(session, json.getLong("group_id"));
                        if (flag) {
                            String result = jedis.get("bot_invite_list");
                            List<JSONObject> list = new ArrayList<>();
                            if (result != null) list = JSON.parseArray(result, JSONObject.class);
                            if (list.stream().noneMatch(item -> item.getLong("group_id").equals(json.getLong("group_id")))) {
                                list.add(json);
                                jedis.set("bot_invite_list", JSON.toJSONString(list), new SetParams().ex(86400 * 3));
                            }
                        }
                    }
                }
                default -> logger.info("不支持的请求消息 {} {}", session.getId(), json.toJSONString());
            }
        } catch (Exception e) {
            logger.error("处理请求消息报错 {}", e.getMessage());
        }
    }

    protected static Boolean checkGroupNumber(Session session, Long group_id) {
        try {
            JSONObject obj = SimpleMessageUtils.getGroupInfoByGet(group_id, "获取群消息");
            logger.info("收到邀请，获取群消息： {}", obj.toJSONString());
            if (!obj.isEmpty() && "ok".equalsIgnoreCase(obj.getString("status"))) {
                obj = obj.getJSONObject("data");
                if ((obj.containsKey("member_count") && obj.getIntValue("member_count") < 50)
                        || (obj.containsKey("member_num") && obj.getIntValue("member_num") < 50)) {
                    SimpleMessageUtils.sendGroupMessage(session, group_id, new TextMessage("群人数小于50，请联系主人，已退出"));
                    Thread.sleep(1000);
                    SimpleMessageUtils.setGroupLeave(session, new GroupLeaveMessage(group_id));
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("获取群信息处理报错 {}", e.getMessage());
        }
        return true;
    }
}
