package com.bot.inori.bot.handler;

import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.model.data.GroupData;
import com.bot.inori.bot.model.data.GroupMemberData;
import com.bot.inori.bot.model.res.MetadataChain;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BotHandler {

    @Getter
    @Setter
    private static Long BOT;

    @Getter
    @Setter
    private static List<GroupData> GROUPS = new ArrayList<>();

    private static final Map<Long, List<GroupMemberData>> MEMBERS = new HashMap<>();

    @Getter
    private static Session session;

    public static Boolean allowPrivate = Boolean.FALSE;

    @Getter
    @Setter
    private static MetadataChain chain;

    public static Integer SPEAKER_ID = 0;

    public static void putMembers(List<GroupMemberData> data) {
        if (!data.isEmpty()) {
            BotHandler.MEMBERS.put(data.get(0).getGroup_id(), data);
        }
    }

    public static List<GroupMemberData> getMembers(Long group_id) {
        return MEMBERS.get(group_id);
    }

    public static Boolean isGroupOwner(Long group_id) {
        return MEMBERS.get(group_id).stream().filter(member -> member.getUser_id().equals(BOT)).findFirst()
                .map(member -> "owner".equals(member.getRole())).orElse(false);
    }

    public static Boolean isGroupAdmin(Long group_id) {
        return userHasGroupPerm(group_id, BOT);
    }

    public static Boolean userHasGroupPerm(Long group_id, Long user_id) {
        if (group_id != null && user_id != null) {
            return MEMBERS.get(group_id).stream().filter(member -> member.getUser_id().equals(user_id)).findFirst()
                    .map(member -> "owner".equals(member.getRole()) || "admin".equals(member.getRole())).orElse(false);
        }
        return Boolean.FALSE;
    }

    public static GroupMemberData getMemberInfo(Long group_id, Long user_id) {
        return MEMBERS.get(group_id).stream().filter(member -> member.getUser_id().equals(user_id)).findFirst().orElseThrow();
    }

    public static Boolean isMaster(Long qq) {
        return qq != null && qq.equals(BaseConfig.master);
    }

    public static Boolean isBot(Long qq) {
        return BOT.equals(qq);
    }

    public static void setSession(Session session) {
        BotHandler.session = session;
    }

    public static String getGroupText(Long group_id) {
        GroupData groupInfo = GROUPS.stream().filter(info -> info.getGroup_id().longValue() == group_id.longValue()).findFirst().orElse(null);
        if (groupInfo != null) {
            return String.format("(%s-%s)", groupInfo.getGroup_name(), groupInfo.getGroup_id());
        }
        return "";
    }

    public static String getQQHeadUrl(Long qq) {
        //https://q1.qlogo.cn/g?b=qq&nk=%s&s=640
        //http://q2.qlogo.cn/headimg_dl?dst_uin=%s&spec=640
        //https://qlogo4.store.qq.com/qzone/%s/%s/640
        return String.format("https://q1.qlogo.cn/g?b=qq&nk=%s&s=640", qq);
    }

    public static File getDir(String path) {
        if (path == null) path = "/";
        if (!path.contains(":")) path = System.getProperty("user.dir") + File.separator + path;
        File file = new File(path);
        if (!file.exists() && file.mkdirs()) MessageHandler.getLogger().debug("创建文件夹成功");
        return file;
    }

    public static File getFile(String path) {
        try {
            if (path == null) return null;
            if (!path.contains(":")) path = System.getProperty("user.dir") + File.separator + path;
            if (path.contains("/")) {
                String dirPath = path.substring(0, path.lastIndexOf("/"));
                File dir = new File(dirPath);
                if (!dir.exists() && dir.mkdirs()) MessageHandler.getLogger().debug("创建文件夹成功");
            }
            File file = new File(path);
            if (!file.exists() && file.createNewFile()) MessageHandler.getLogger().debug("创建文件成功");
            return file;
        } catch (IOException e) {
            MessageHandler.getLogger().error("获取文件报错", e);
            return null;
        }
    }
}
