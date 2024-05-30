package com.bot.inori.bot.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.bot.inori.bot.config.ActionConfig;
import com.bot.inori.bot.model.data.ActionData;
import com.bot.inori.bot.model.req.TextMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 沙比json转换
 */
@Component
public class PermissionHandler {

    private static Map<String, List<String>> permissions;

    @Bean
    public void init() {
        try (Jedis jedis = new Jedis()) {
            MessageHandler.getLogger().info("初始化权限管理");
            permissions = new HashMap<>();
            String perms = jedis.get("bot_permission");
            if (perms != null) permissions = JSON.parseObject(perms, new TypeReference<>() {});
        }
    }

    public static Boolean checkPermission(String cmd, Long group_id) {
        if (group_id == null) return BotHandler.allowPrivate;
        List<String> list = listPerm(group_id);
        return list.contains(cmd);
    }

    public static Boolean addPerm(Long group_id, String cmd) {
        try {
            if ("全部".equals(cmd) && group_id != null) {
                List<String> perms = ActionConfig.actions.stream()
                        .filter(data -> !data.isMaster())
                        .map(ActionData::getCmd)
                        .collect(Collectors.toList());
                permissions.put("perm-" + group_id, perms);
                refresh();
                return true;
            } else {
                ActionData data = ActionConfig.actions.stream().filter(action -> action.hasCmd(cmd)).findFirst().orElse(null);
                if (data != null) {
                    if (group_id != null) {
                        List<String> perms = listPerm(group_id);
                        if (!perms.contains(cmd)) {
                            perms.add(cmd);
                            permissions.put("perm-" + group_id, perms);
                        }
                    } else {
                        permissions.forEach((gid, perms) -> {
                            if (!perms.contains(cmd)) {
                                perms.add(cmd);
                                permissions.put(gid, perms);
                            }
                        });
                    }
                    refresh();
                    return true;
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("添加权限报错 {}", e.getMessage());
        }
        return false;
    }

    public static Boolean removePerm(Long group_id, String cmd) {
        try {
            if ("全部".equals(cmd) && group_id != null) {
                permissions.put("perm-" + group_id, new ArrayList<>());
                refresh();
                return true;
            } else {
                ActionData data = ActionConfig.actions.stream().filter(action ->
                        action.getCmd().equals(cmd)).findFirst().orElse(null);
                if (data != null) {
                    if (group_id != null) {
                        List<String> perms = listPerm(group_id);
                        if (perms.contains(cmd)) {
                            perms.remove(cmd);
                            permissions.put("perm-" + group_id, perms);
                        }
                    } else {
                        permissions.forEach((gid, perms) -> {
                            if (perms.contains(cmd)) {
                                perms.remove(cmd);
                                permissions.put(gid, perms);
                            }
                        });
                    }
                    refresh();
                    return true;
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("移除权限报错 {}", e.getMessage());
        }
        return false;
    }

    public static List<String> listPerm(Long group_id) {
        List<String> list = permissions.get("perm-" + group_id);
        if (list == null) {
            list = new ArrayList<>();
            for (ActionData data : ActionConfig.actions) {
                if (!data.isPermit()) list.add(data.getCmd());
            }
            permissions.put("perm-" + group_id, list);
            refresh();
        }
        return list;
    }

    public static List<Object> listAll() {
        List<Object> list = new ArrayList<>();
        BotHandler.getGROUPS().forEach(group -> {
            StringBuilder sb = new StringBuilder();
            sb.append("群 ")
                    .append(group.getGroup_name())
                    .append("(")
                    .append(group.getGroup_id())
                    .append(")").append(" 权限如下：");
            List<String> perms = permissions.get("perm-" + group.getGroup_id());
            if (perms == null) {
                perms = new ArrayList<>();
                for (ActionData data : ActionConfig.actions) {
                    if (!data.isPermit()) perms.add(data.getCmd());
                }
                permissions.put("perm-" + group.getGroup_id(), perms);
            }
            perms.forEach(p -> sb.append("\n").append(p));
            list.add(new TextMessage(sb.toString()));
        });
        refresh();
        return list;
    }

    private static void refresh() {
        try (Jedis jedis = new Jedis()) {
            jedis.set("bot_permission", JSON.toJSONString(permissions),
                    new SetParams().ex(Integer.MAX_VALUE));
        }
    }
}
