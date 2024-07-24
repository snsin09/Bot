package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.util.DateUtils;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.req.*;
import com.bot.inori.bot.model.res.AtMsg;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.model.res.ReplyMsg;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.utils.annotation.BotCommand;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

/**
 * 群管功能
 */
@SuppressWarnings("unused")
@Component
public class GroupManageAction {

    @BotCommand(cmd = "群邀请", isMaster = true, description = "查询群邀请")
    public void getGroupSystemMsg(MetadataChain chain) {
        try (Jedis jedis = new Jedis()) {
            String val = jedis.get("bot_invite_list");
            if (val != null) {
                List<JSONObject> list = JSON.parseArray(val, JSONObject.class);
                if (!list.isEmpty()) {
                    List<Object> data = new ArrayList<>();
                    data.add(new TextMessage(String.format("现有未处理的群邀请如下，总共%s条", list.size())));
                    for (JSONObject json : list) {
                        String sb = "群号：" + json.getString("group_id") + "\n" +
                                "请求人：" + json.getString("user_id") + "\n" +
                                "验证信息：" + json.getString("comment") + "\n" +
                                "flag：" + json.getString("flag") + "\n" +
                                "时间：" + DateUtils.format(json.getLong("time"), DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
                        data.add(new TextMessage(sb));
                    }
                    chain.sendListForwardMsg(data);
                } else chain.sendReplyMsg(new TextMessage("暂无群邀请"));
            }
        }
    }

    @BotCommand(cmd = "禁言", alias = "解禁", isMaster = true, description = "解禁、禁言用户")
    public void setGroupBan(MetadataChain chain) {
        if (chain.getGroup_id() == null) return;
        String cmd = chain.getBasicCommand();
        Long duration = null, user_id = null;
        if (cmd.startsWith("解禁")) duration = 0L;
        cmd = cmd.substring(2).trim();
        AtMsg at = chain.getFirstAt();
        if (at != null) user_id = at.getQq();
        if (!StringUtil.isBlank(cmd)) {
            if (!StringUtil.isNumeric(cmd.replaceAll(" ", ""))) return;
            String[] arr = cmd.split(" ");
            if (arr.length >= 2) {
                if (user_id == null) {
                    user_id = Long.parseLong(arr[0]);
                    if (duration == null) duration = Long.parseLong(arr[1]);
                } else if (duration == null) duration = Long.parseLong(arr[0]);
            } else if (arr.length == 1) {
                if (user_id == null) user_id = Long.parseLong(arr[0]);
                else if (duration == null) duration = Long.parseLong(arr[0]);
            }
        }
        if (user_id == null || duration == null || duration < 0) return;
        SimpleMessageUtils.setGroupBan(chain.getSession(), new GroupBanMessage(chain.getGroup_id(), user_id, duration));
    }

    @BotCommand(cmd = "宵禁", alias = "解宵禁", isMaster = true)
    public void setGroupWholeBan(MetadataChain chain) {
        if (!BotHandler.isGroupAdmin(chain.getGroup_id())) return;
        SimpleMessageUtils.setGroupWholeBan(chain.getSession(), GroupAdmin.groupWholeBan(chain.getGroup_id(),
                chain.getBasicCommand().startsWith("宵禁")));
    }

    @BotCommand(cmd = "设置管理员", alias = "取消管理员", isMaster = true)
    public void setGroupAdmin(MetadataChain chain) {
        if (!BotHandler.isGroupOwner(chain.getGroup_id())) {
            chain.sendMsg(new TextMessage("我都没篡位成功，没办法的啦"));
            return;
        }
        String cmd = chain.getBasicCommand();
        boolean flag = !cmd.startsWith("取消");
        Long user_id;
        AtMsg at = chain.getFirstAt();
        if (at != null) user_id = at.getQq();
        else {
            cmd = cmd.substring(5).trim();
            if (StringUtil.isBlank(cmd) || !StringUtil.isNumeric(cmd)) return;
            user_id = Long.parseLong(cmd);
        }
        if (BotHandler.isBot(user_id)) return;
        SimpleMessageUtils.setGroupAdmin(chain.getSession(), GroupAdmin.groupAdmin(chain.getGroup_id(), user_id, flag));
    }

    @BotCommand(cmd = "我要头衔", permit = false, description = "用于机器人是群主的群，设置个人头衔")
    public void setGroupSpecialTitle(MetadataChain chain) {
        if (!BotHandler.isGroupOwner(chain.getGroup_id())) return;
        String title = chain.getBasicCommand().substring(4).trim();
        if (title.isEmpty()) return;
        SimpleMessageUtils.setGroupSpecialTitle(chain.getSession(), new GroupSpecialTitleMessage(
                chain.getGroup_id(), chain.getSender().getUser_id(), title));
        BotHandler.setChain(chain);
    }

    @BotCommand(cmd = "同意邀请", isMaster = true)
    public void approveGroupInvite(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(4).trim();
        if (cmd.isEmpty()) return;
        SimpleMessageUtils.setGroupAddRequest(chain.getSession(), GroupAddMessage.groupInvite(
                cmd, true, null));
        BotHandler.setChain(chain);
        removeCache(cmd);
    }

    @BotCommand(cmd = "拒绝邀请", isMaster = true)
    public void refuseGroupInvite(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(4).trim();
        if (cmd.isEmpty()) return;
        SimpleMessageUtils.setGroupAddRequest(chain.getSession(), GroupAddMessage.groupInvite(
                cmd, false, "主人拒绝了群邀请！"));
        BotHandler.setChain(chain);
        removeCache(cmd);
    }

    private void removeCache(String cmd) {
        try (Jedis jedis = new Jedis()) {
            String val = jedis.get("bot_invite_list");
            if (val != null) {
                List<JSONObject> list = JSON.parseArray(val, JSONObject.class);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getString("flag").equals(cmd)) {
                        list.remove(i);
                        jedis.set("bot_invite_list", JSON.toJSONString(list));
                        break;
                    }
                }
            }
        }
    }

    @BotCommand(cmd = "退群", isMaster = true)
    public void setGroupLeave(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(4).trim();
        if (cmd.isEmpty() && !StringUtil.isNumeric(cmd)) return;
        SimpleMessageUtils.setGroupLeave(chain.getSession(), new GroupLeaveMessage(Long.parseLong(cmd)));
        BotHandler.setChain(chain);
    }

    @BotCommand(cmd = "撤回", description = "消息撤回", permit = false)
    public void recall(MetadataChain chain) {
        ReplyMsg reply = chain.getReply();
        if (reply == null) return;
        if (!BotHandler.isMaster(chain.getSender().getUser_id())
                && !BotHandler.userHasGroupPerm(chain.getGroup_id(), chain.getSender().getUser_id())) return;
        SimpleMessageUtils.deleteMsg(chain.getSession(), reply.getId());
    }

    @BotCommand(cmd = "销毁历史", description = "销毁群聊记录，需要拥有管理权限，限定管理及主人使用", permit = false)
    public void recallRecent(MetadataChain chain) {
        try {
            if (chain.getGroup_id() == null || (!BotHandler.isMaster(chain.getSender().getUser_id())
                    && !BotHandler.userHasGroupPerm(chain.getGroup_id(), chain.getSender().getUser_id()))) return;
            if (!BotHandler.isGroupAdmin(chain.getGroup_id())) {
                chain.sendMsg(new TextMessage("我都没管理权限，没办法的啦"));
                return;
            }
            Long message_seq = null;
            ReplyMsg reply = chain.getReply();
            if (reply != null) message_seq = reply.getId();
            JSONObject object = SimpleMessageUtils.getHistoryMessageByGet(message_seq, chain.getGroup_id());
            object.getJSONObject("data").getJSONArray("messages").forEach( o -> {
                JSONObject obj = (JSONObject) o;
                SimpleMessageUtils.deleteMsg(chain.getSession(), obj.getLong("message_id"));
            });
        } catch (Exception e) {
            MessageHandler.getLogger().error("删除历史记录失败 {}", e.getMessage());
        }
    }
}
