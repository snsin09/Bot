package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.bot.inori.bot.config.ActionConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.PermissionHandler;
import com.bot.inori.bot.model.data.ActionData;
import com.bot.inori.bot.model.req.MarkDownMessage;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.PokeTouchMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.AtMsg;
import com.bot.inori.bot.model.res.FileMsg;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.model.res.MetadataMsg;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.utils.WrapHtmlUtils;
import com.bot.inori.bot.utils.annotation.BotCommand;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class BaseAction {

    @BotCommand(cmd = "我的菜单", alias = "~菜单", description = "调出功能菜单")
    public void functions(MetadataChain chain) {
        String cmd = chain.getBasicCommand();
        if (cmd.length() != 3 && cmd.length() != 4) return;
        String url = WrapHtmlUtils.generateMenu(ActionConfig.actions);
        if (url != null) chain.sendReplyMsg(MediaMessage.imageMedia(url));
    }

    @BotCommand(cmd = "权限", description = "查看本群开启的权限", permit = false)
    public void perm(MetadataChain chain) {
        if (chain.getGroup_id() != null) {
            List<String> list = PermissionHandler.listPerm(chain.getGroup_id());
            if (list.isEmpty()) {
                chain.sendMsg("本群暂未开启权限");
            } else {
                List<ActionData> actions = ActionConfig.actions.stream().filter(action -> list.contains(action.getCmd())).collect(Collectors.toList());
                String url = WrapHtmlUtils.generateMenu(actions);
                chain.sendReplyMsg(MediaMessage.imageMedia(url));
            }
        } else {
            chain.sendListForwardMsg(PermissionHandler.listAll());
        }
    }

    @BotCommand(cmd = "允许私聊", alias = "禁止私聊", description = "私聊开启开关", isMaster = true)
    public void allowPrivate(MetadataChain chain) {
        String cmd = chain.getBasicCommand();
        if ("允许私聊".equals(cmd)) BotHandler.allowPrivate = Boolean.TRUE;
        else BotHandler.allowPrivate = Boolean.FALSE;
        chain.sendMsg(cmd + "成功！");
    }

    @BotCommand(cmd = "发", description = "功能调试 文字、图片(url)、视频(url)、音频(url)、json", isMaster = true)
    public void send(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(2).trim();
        if (StringUtil.isBlank(cmd)) return;
        String[] arr = cmd.split(" ");
        if (arr.length != 2) return;
        switch (arr[0]) {
            case "文字" -> chain.sendMsg(new TextMessage(arr[1]));
            case "图片" -> {
                if (arr[1].startsWith("http")) chain.sendMsg(MediaMessage.imageMedia(arr[1]));
            }
            case "视频" -> {
                if (arr[1].startsWith("http")) chain.sendMsg(MediaMessage.videoMedia(arr[1]));
            }
            case "音频" -> {
                if (arr[1].startsWith("http")) chain.sendMsg(MediaMessage.audioMedia(arr[1]));
            }
            case "json", "JSON", "Json" -> {
                if (JSON.isValid(arr[1])) chain.sendMsg(
                        "[CQ:json,data=" + arr[1]
                        .replaceAll(",", "&#44;")
                        .replaceAll("&", "&amp;")
                        .replaceAll("\\[", "&#91;")
                        .replaceAll("]", "&#93;")
                        + "]");
            }
            default -> chain.sendMsg(new TextMessage("不支持的类型"));
        }
    }

    @BotCommand(cmd = "查看代理", alias = "代理状态", description = "查看系统代理状态", isMaster = true)
    public void proxy(MetadataChain chain) {
        chain.sendMsg("百度：" + (HttpUtils.checkProxy("https://www.baidu.com/") ? "正常" : "异常") + "\n" +
                "谷歌：" + (HttpUtils.checkProxy(null) ? "正常" : "异常"));
    }

    @BotCommand(cmd = "截图", alias = "http", description = "网页截图", permit = false, regexAlias = true)
    public void screenshot(MetadataChain chain) {
        String cmd = chain.getBasicCommand().trim();
        if (cmd.startsWith("截图")) cmd = cmd.substring(2).trim();
        if (StringUtil.isBlank(cmd)) chain.sendReplyMsg("截图请带上链接");
        else {
            String regex = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(cmd);
            if (matcher.find()) {
                String name = matcher.group();
                if (name.contains("b23.tv")) BiliAction.analysisRedirectB23Url(chain, name);
                else {
                    String url = WrapHtmlUtils.captureHtml(name);
                    if (url != null) chain.sendMsg(MediaMessage.imageMedia(url));
                }
            }
        }
    }

    @BotCommand(cmd = "萌娘百科", description = "萌娘百科搜索", permit = false)
    public void moeGirl(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(4).trim();
        if (StringUtil.isBlank(cmd)) return;
        String url = String.format("https://zh.moegirl.org.cn/index.php?utm_medium=capsule_list&title=%s&utm_source=moe_homeland", cmd);
        url = WrapHtmlUtils.captureHtml(url);
        if (url != null) chain.sendMsg(MediaMessage.imageMedia(url));
    }

    @BotCommand(cmd = "戳", description = "戳人", permit = false)
    public void poke(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 1) return;
        AtMsg at = chain.getFirstAt();
        if (at != null) chain.sendMsg(PokeTouchMessage.poke(at.getQq()));
    }

    @BotCommand(cmd = "禁止", alias = "达咩,关闭", description = "关闭指定群聊功能", isMaster = true)
    public void forbid(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(2).trim();
        if (StringUtil.isBlank(cmd)) return;
        String[] arr = cmd.split(" ");
        if (arr.length == 2) {
            if (StringUtil.isNumeric(arr[0])) {
                Boolean flag = PermissionHandler.removePerm(Long.parseLong(arr[0]), arr[1]);
                if (flag) {
                    if ("全部".equals(arr[1])) chain.sendMsg(new TextMessage(arr[0] + " 全部自定义功能已禁用！"));
                    else chain.sendMsg(new TextMessage(cmd + " 关闭成功！"));
                }
            }
        } else {
            Boolean flag = PermissionHandler.removePerm(chain.getGroup_id(), cmd);
            if (flag) {
                if ("全部".equals(cmd)) chain.sendMsg(new TextMessage(cmd + " 全部自定义功能已禁用！"));
                else chain.sendMsg(new TextMessage(cmd + " 关闭成功！"));
            }
        }
    }

    @BotCommand(cmd = "开启", alias = "打开", description = "开启指定群聊功能", isMaster = true)
    public void open(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(2).trim();
        if (StringUtil.isBlank(cmd)) return;
        String[] arr = cmd.split(" ");
        if (arr.length == 2) {
            if (StringUtil.isNumeric(arr[0])) {
                Boolean flag = PermissionHandler.addPerm(Long.parseLong(arr[0]), arr[1]);
                if (flag) {
                    if ("全部".equals(arr[1])) chain.sendMsg(new TextMessage(arr[0] + " 全部自定义功能已启用！"));
                    else chain.sendMsg(new TextMessage(cmd + " 开启成功！"));
                }
            }
        } else {
            Boolean flag = PermissionHandler.addPerm(chain.getGroup_id(), cmd);
            if (flag) {
                if ("全部".equals(cmd)) chain.sendMsg(new TextMessage(cmd + " 全部自定义功能已启用！"));
                else chain.sendMsg(new TextMessage(cmd + " 开启成功！"));
            }
        }
    }

    @BotCommand(cmd = "重复", alias = "复读", description = "重复主人的话", isMaster = true)
    public void repeat(MetadataChain chain) {
        List<Object> resp = new ArrayList<>();
        boolean flag = false;
        for (MetadataMsg msg : chain.getList()) {
            switch (msg.getType()) {
                case "text" -> {
                    Map<String, String> map = JSON.parseObject(msg.getData(), new TypeReference<>() {});
                    TextMessage text = new TextMessage(map.get("text"));
                    if (text.queryContent().startsWith("复读") || text.queryContent().startsWith("重复") && !flag) {
                        resp.add(new TextMessage(text.queryContent().substring(2).trim()));
                        flag = true;
                    } else resp.add(text);
                }
                case "image" -> {
                    Map<String, String> map = JSON.parseObject(msg.getData(), new TypeReference<>() {});
                    resp.add(MediaMessage.imageMedia(map.get("url")));
                }
                default -> System.out.println("暂不支持其他消息：" + msg);
            }
        }
        chain.sendMsg(resp);
    }

    @BotCommand(cmd = "制作卡片", alias = "图转卡", description = "制作卡片，图片必须，其他参数：外显 标题 子标题")
    public void makeCard(MetadataChain chain) {
        String cmd = chain.getBasicCommand();
        if (cmd.startsWith("图")) cmd = cmd.substring(3).trim();
        else cmd = cmd.substring(4).trim();
        if (chain.getFirstImage() != null) {
            String yx = "我喜欢你", title = "", subtitle = "";
            String[] arr = cmd.split(" ");
            switch (arr.length) {
                case 0 -> {
                }
                case 1 -> {
                    if (!StringUtil.isBlank(arr[0])) yx = arr[0];
                }
                case 2 -> {
                    if (!StringUtil.isBlank(arr[0])) yx = arr[0];
                    if (!StringUtil.isBlank(arr[1])) title = arr[1];
                }
                default -> {
                    if (!StringUtil.isBlank(arr[0])) yx = arr[0];
                    if (!StringUtil.isBlank(arr[1])) title = arr[1];
                    if (!StringUtil.isBlank(arr[2])) subtitle = arr[2];
                }
            }
            String data = HttpUtils.getResp(String.format("https://api.mrgnb.cn/API/qq_ark37.php?url=%s&yx=%s&title=%s&subtitle=%s",
                    chain.getFirstImage().getUrl(), yx, title, subtitle));
            if (data == null || data.contains("错误")) return;
            chain.sendMsg("[CQ:json,data=" + data.replaceAll(",", "&#44;") + "]");
//            chain.sendMsg("[CQ:json,data={\"app\":\"com.tencent.gxhServiceIntelligentTip\"&#44;\"desc\":\"鹿子零java\"&#44;\"view\":\"gxhServiceIntelligentTip\"&#44;\"bizsrc\":\"\"&#44;\"ver\":\"\"&#44;\"prompt\":\"冒泡\"&#44;\"appID\":\"\"&#44;\"sourceName\":\"\"&#44;\"actionData\":\"\"&#44;\"actionData_A\":\"\"&#44;\"sourceUrl\":\"\"&#44;\"meta\":{\"gxhServiceIntelligentTip\":{\"action\":\"\"&#44;\"appid\":\"gxhServiceIntelligentTip\"&#44;\"bgImg\":\"https://p.qlogo.cn/homework/0/hw_h_5d37122rx4cogkc65dc8947af82f/0\"&#44;\"reportParams\":{}}}&#44;\"config\":{\"autoSize\":0&#44;\"ctime\":1691351380&#44;\"forward\":1&#44;\"height\":520&#44;\"token\":\"4f8ae05ebca255eea79acad7f5a1edb5\"&#44;\"type\":\"normal\"&#44;\"width\":9999}&#44;\"text\":\"\"&#44;\"extraApps\":&#91;&#93;&#44;\"sourceAd\":\"\"&#44;\"extra\":\"\"}]");
        }
    }

    @BotCommand(cmd = "领取猫娘", description = "领取猫娘")
    public void catGirl(MetadataChain chain) {
        chain.sendMsg(new MarkDownMessage());
    }

    @BotCommand(cmd = "文转卡", description = "文字转图片功能，内容参数必须，内容 标题 外显")
    public void wordCard(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(3).trim();
        if (!StringUtil.isBlank(cmd)) {
            String name = "", title = "", url, yx = "我喜欢你";
            String[] arr = cmd.split(" ");
            switch (arr.length) {
                case 0 -> {
                    chain.sendReplyMsg(new TextMessage("不打字不给用"));
                    return;
                }
                case 1 -> {
                    if (!StringUtil.isBlank(arr[0])) name = arr[0];
                }
                case 2 -> {
                    if (!StringUtil.isBlank(arr[0])) name = arr[0];
                    if (!StringUtil.isBlank(arr[1])) title = arr[1];
                }
                default -> {
                    if (!StringUtil.isBlank(arr[0])) name = arr[0];
                    if (!StringUtil.isBlank(arr[1])) title = arr[1];
                    if (!StringUtil.isBlank(arr[2])) yx = arr[2];
                }
            }
            AtMsg at = chain.getFirstAt();
            if (at != null) url = BotHandler.getQQHeadUrl(at.getQq());
            else url = BotHandler.getQQHeadUrl(chain.getSender().getUser_id());
            String data = HttpUtils.getResp(String.format("https://api.mrgnb.cn/API/qq_ark.php?title=%s&name=%s&url=%s&yx=%s",
                    title, name, url, StringUtil.isBlank(yx) ? "我喜欢你" : yx));
            if (data == null || data.contains("错误")) return;
            chain.sendMsg("[CQ:json,data=" + data.replaceAll("\\\\/", "/")
                    .replaceAll("\\[", "&#91;")
                    .replaceAll("]", "&#93;")
                    .replaceAll(",", "&#44;") + "]");
        }
    }

    @BotCommand(cmd = "天气", description = "查看天气")
    public void weather(MetadataChain chain) {
        String cmd = chain.getBasicCommand();
        cmd = cmd.substring(2).trim();
        if (!StringUtil.isBlank(cmd)) {
            String res = HttpUtils.getResp("https://api.yujn.cn/api/qqtq.php?msg=" + cmd);
            if (res != null) chain.sendMsg(new TextMessage(res.replaceAll(" ", "\n")));
        }
    }

    @BotCommand(cmd = "中国地图", description = "中国地图表情包制作")
    public void chinaMap(MetadataChain chain) {
        Long qq = chain.getSender().getUser_id();
        if (chain.getFirstAt() != null) qq = chain.getFirstAt().getQq();
        if (chain.getBasicCommand().length() == 4) {
            chain.sendMsg(MediaMessage.imageMedia("https://api.yujn.cn/api/zgdt.php?qq=" + qq));
        }
    }

    @BotCommand(cmd = "QQ评估", alias = "qq评估,qq估价,QQ估价", description = "QQ评估")
    public void pinggu(MetadataChain chain) {
        Long qq = chain.getSender().getUser_id();
        if (chain.getFirstAt() != null) qq = chain.getFirstAt().getQq();
        if (chain.getBasicCommand().length() == 4) {
            chain.sendMsg(MediaMessage.imageMedia("https://api.yujn.cn/api/pinggu.php?qq=" + qq));
        }
    }

    @BotCommand(cmd = "识别", description = "识别二维码")
    public void getQrcode(MetadataChain chain) {
        FileMsg image = chain.getFirstImage();
        if (image == null) return;
        try (InputStream is = HttpUtils.getStream(image.getUrl(), false)) {
            if (is != null) {
                BufferedImage bi = ImageIO.read(is);
                LuminanceSource source = new BufferedImageLuminanceSource(bi);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Reader reader = new MultiFormatReader();
                Result result = reader.decode(bitmap);
                chain.sendMsg(new TextMessage("识别结果：" + result.getText()));
            }
        } catch (Exception e) {
            chain.sendMsg(new TextMessage("识别二维码失败：" + e.getMessage()));
        }
    }
}
