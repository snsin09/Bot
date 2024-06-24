package com.bot.inori.bot.handler;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.action.func.poke.Poke;
import com.bot.inori.bot.action.schedule.DaySchedule;
import com.bot.inori.bot.model.req.GroupLeaveMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.model.res.Sender;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoticeHandler {

    private static final Logger logger = LoggerFactory.getLogger(NoticeHandler.class);

    public static void handleNoticeMessage(Session session, JSONObject json) {
        try {
            switch (json.getString("notice_type")) {
                case "group_upload" -> //群文件上传
                        logger.info("{} 文件上传\n上传者 QQ:{}\n文件名：{}\n文件地址：{}\n文件大小(字节数)：{}", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("user_id"), json.getJSONObject("file").getString("name"), json.getJSONObject("file").getString("url"), json.getJSONObject("file").getLong("size"));
                case "private_upload" -> //私聊文件上传
                        logger.info("{} 文件上传\n上传者 QQ:{}\n文件名：{}\n文件地址：{}\n文件大小(字节数)：{}", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("user_id"), json.getJSONObject("private_file").getString("name"), json.getJSONObject("private_file").getString("url"), json.getJSONObject("private_file").getLong("size"));
                case "group_admin" -> //群管理员变动
                        logger.info("{} 管理员变动\n变动成员 QQ:{}\n{}", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("target_id"), "set".equalsIgnoreCase(json.getString("sub_type")) ? "设置管理员" : "取消管理员");
                case "group_decrease" -> {
                    //群成员减少
                    String sub_type = json.getString("sub_type");
                    sub_type = "leave".equalsIgnoreCase(sub_type) ? "主动退群" : "kick".equalsIgnoreCase(sub_type) ? "被踢了" : "我被踢了";
                    if ("kick".equalsIgnoreCase(json.getString("sub_type")) && BotHandler.isMaster(json.getLong("user_id"))) {
                        SimpleMessageUtils.sendGroupMessage(session, json.getLong("group_id"), new TextMessage("再见，退群"));
                        Thread.sleep(100);
                        SimpleMessageUtils.setGroupLeave(session, new GroupLeaveMessage(json.getLong("group_id")));
                    }
                    logger.info("{} 成员减少\n减少成员 QQ:{}\n操作者 QQ:{}\n减少类型 {}", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("user_id"), json.getLong("operator_id"), sub_type);
                }
                case "group_increase" -> {
                    //群成员增加
                    logger.info("{} 新人加入\n新增成员 QQ:{}\n操作者 QQ:{}\n加入类型 {}", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("user_id"), json.getLong("operator_id"), "invite".equalsIgnoreCase(json.getString("sub_type")) ? "邀请" : "申请");
                }
                case "group_ban" -> {
                    //群禁言
                    String data = String.format("%s %s事件\n被禁言成员 QQ：%s\n操作者 QQ：%s\n禁言时长(秒)：%s", BotHandler.getGroupText(json.getLong("group_id")), "ban".equalsIgnoreCase(json.getString("sub_type")) ? "禁言" : "解除禁言", json.getLong("user_id"), json.getLong("operator_id"), json.getLong("duration"));
                    logger.info(data);
                    if (json.getLong("user_id").equals(BotHandler.getBOT())) {
                        SimpleMessageUtils.sendMaster(new TextMessage(data));
                        Integer num = DaySchedule.getBAN_GROUPS().get(json.getLong("group_id"));
                        if (num == null) num = 1;
                        else num++;
                        DaySchedule.getBAN_GROUPS().put(json.getLong("group_id"), num);
                        if (num >= 5) {
                            SimpleMessageUtils.setGroupLeave(session, new GroupLeaveMessage(json.getLong("group_id")), "禁言退群");
                            SimpleMessageUtils.sendMaster(new TextMessage("本日禁言5次，主动退群！"));
                            return;
                        }
                        if (json.getLong("duration") >= 3 * 86400) {
                            SimpleMessageUtils.setGroupLeave(session, new GroupLeaveMessage(json.getLong("group_id")), "禁言退群");
                            SimpleMessageUtils.sendMaster(new TextMessage("禁言超过三天，主动退群！"));
                        }
                    }
                }
                case "group_recall" -> //群消息撤回
                        logger.info("{} 消息撤回事件\n撤回人 QQ：{}\n被撤回人 QQ：{}\n消息 ID：{}", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("operator_id"), json.getLong("user_id"), json.getLong("message_id"));
                case "group_card" -> //群成员名片变动
                        logger.info("{} 成员名片变动\n变动成员 QQ：{}\n新名片：{}\n旧名片：{}", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("user_id"), json.getString("card_new"), json.getString("card_old"));
                case "friend_add" -> //好友添加
                        logger.info("{} 添加你为好友", json.getLong("user_id"));
                case "friend_recall" -> //好友撤回
                        logger.info("好友{}撤回了一条消息 消息 ID：{}", json.getLong("user_id"), json.getLong("message_id"));
                case "offline_file" -> {
                    //接收到离线文件包 未实现
                }
                case "client_status" -> {
                    //客户端状态 未实现
                }
                case "essence" -> //精华消息
                        logger.info("{} {}精华消息\n发送者 QQ：{}\n操作者 QQ：{}\n消息 ID：{}", BotHandler.getGroupText(json.getLong("group_id")), "add".equalsIgnoreCase(json.getString("sub_type")) ? "添加" : "删除", json.getLong("sender_id"), json.getLong("operator_id"), json.getLong("message_id"));
                case "notify" -> {
                    //系统通知
                    switch (json.getString("sub_type")) {
                        case "honor" -> {
                            //群荣誉变更 未实现
                        }
                        case "poke" -> {
                            //戳一戳
                            logger.info(json.getLong("user_id") + " 戳一戳 " + json.getLong("target_id"));
                            if (json.getLong("target_id").equals(BotHandler.getBOT())) {
                                Sender sender = new Sender();
                                sender.setUser_id(json.getLong("user_id"));
                                MetadataChain chain = new MetadataChain();
                                chain.setSession(session);
                                chain.setGroup_id(json.getLong("group_id"));
                                chain.setSender(sender);
                                Poke.poke(chain);
                            }
                        }
                        case "lucky_king" -> {
                            //运气王 未实现
                        }
                        case "title" -> //群头衔变更
                                logger.info("{} 群头衔变更：成员 {} 获得头衔 【{}】", BotHandler.getGroupText(json.getLong("group_id")), json.getLong("user_id"), json.getString("title"));
                        default -> logger.info("不支持的系统消息通知 {}", json.toJSONString());
                    }
                }
                default -> logger.info("不支持的通知消息 {}", json.toJSONString());
            }
        } catch (Exception e) {
            logger.error("处理通知消息报错 {}", e.getMessage());
        }
    }
}
