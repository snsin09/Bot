package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.AtMsg;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import com.bot.inori.bot.utils.annotation.BotCommand;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unused")
public class FunAction {

    @BotCommand(cmd = "点赞", permit = false, description = "资料卡片点赞")
    public void thumbUp(MetadataChain chain) {
        String cmd = chain.getBasicCommand();
        if (cmd.length() == 2) {
            SimpleMessageUtils.sendThumbUp(chain.getSender().getUser_id());
            BotHandler.setChain(chain);
        }
    }

    @BotCommand(cmd = "骂我", alias = "钉宫", description = "钉宫骂人语音")
    public void fuckMe(MetadataChain chain) {
        try {
            File dg = BotHandler.getDir("dinggong");
            if (dg.isDirectory()) {
                File[] files = dg.listFiles();
                if (files != null && files.length > 0) {
                    Random random = new Random();
                    dg = files[random.nextInt(files.length)];
                    String msg = dg.getName().split("_")[1];
                    chain.sendMsg(new TextMessage(msg));
                    chain.sendMsg(MediaMessage.audioMedia(dg));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("骂我出错！ {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "一言", description = "随机一言", permit = false)
    public void yiy(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 2) return;
        chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.oick.cn/api/yiyan")));
    }

    @BotCommand(cmd = "vw50", alias = "kfc,KFC", description = "疯狂星期四KFC 一言", permit = false)
    public void kfc(MetadataChain chain) {
        chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/kfc.php?")));
    }

    @BotCommand(cmd = "土味情话", description = "随机土味情话", permit = false)
    public void qinghua(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 4) return;
        chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/qinghua.php?")));
    }

    @BotCommand(cmd = "柴郡", description = "柴郡表情包", permit = false)
    public void cj(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 2) return;
        chain.sendMsg(MediaMessage.imageMedia("http://api.yujn.cn/api/chaijun.php?"));
    }

    @BotCommand(cmd = "动漫一言", description = "随机动漫一言", permit = false)
    public void yiy2(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 4) return;
        chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/dmyiyan.php?")));
    }

    @BotCommand(cmd = "少女", alias = "少女", description = "今天你、我、TA是什么少女", regexAlias = true, permit = false)
    public void ecy(MetadataChain chain) {
        if (chain.getGroup_id() != null) {
            Long user_id = chain.getSender().getUser_id();
            AtMsg at = chain.getFirstAt();
            if (at != null) user_id = at.getQq();
            chain.sendMsg(new TextMessage(HttpUtils.getResp(String.format("https://api.lolimi.cn/API/Ser/?name=%s&type=text",
                    URLEncoder.encode(BotHandler.getMemberInfo(chain.getGroup_id(), chain.getSender().getUser_id()).getNickname(), StandardCharsets.UTF_8)))));
        }
    }

    @BotCommand(cmd = "二次元", description = "随机高质量二次元图片")
    public void ACG(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.imageMedia(HttpUtils.getRedirectUrl("https://api.yujn.cn/api/gzl_ACG.php?type=image&form=pc")), "撤回");
        }
    }

    @BotCommand(cmd = "白丝", description = "随机白丝图片")
    public void baisi(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 2) {
            chain.sendMsg(MediaMessage.imageMedia("https://api.yujn.cn/api/baisi.php?"), "撤回");
        }
    }

    @BotCommand(cmd = "白丝视频", alias = "看白丝", description = "随机白丝视频")
    public void bsVideo(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4 || chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/baisis.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "黑丝", description = "随机黑丝图片")
    public void heisi(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 2) {
            chain.sendMsg(MediaMessage.imageMedia("http://api.yujn.cn/api/heisi.php?"), "撤回");
        }
    }

    @BotCommand(cmd = "黑丝视频", alias = "看黑丝", description = "随机黑丝视频")
    public void hsVideo(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4 || chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/heisis.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "萝莉视频", alias = "看萝莉", description = "随机萝莉视频")
    public void loliVideo(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4 || chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/luoli.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "狱卒视频", alias = "看狱卒,看玉足", description = "随机狱卒视频")
    public void yzVideo(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4 || chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/jpmt.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "美女视频", alias = "看美女,看摇摆", description = "随机美女摇摆视频")
    public void mnVideo(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4 || chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/manyao.php?type=video")), "撤回");
        }
    }
    @BotCommand(cmd = "JK视频", alias = "看JK,看jk,看Jk", description = "随机JK视频")
    public void jkVideo(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4 || chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/nvgao.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "美腿", description = "随机美腿视频")
    public void yuzu(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 2) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/yuzu.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "腿", description = "随机腿图片")
    public void tui(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 1) {
            chain.sendMsg(MediaMessage.imageMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/tui.php?")), "撤回");
        }
    }

    @BotCommand(cmd = "小姐姐", description = "随机小姐姐视频")
    public void xjj(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 3) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/zzxjj.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "女大学生", description = "随机女大学生视频")
    public void nvda(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("https://api.yujn.cn/api/nvda.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "漫展视频", description = "随机漫展视频")
    public void manzhan(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("https://api.yujn.cn/api/manzhan.php?type=video")), "撤回");
        }
    }

    @BotCommand(cmd = "动漫视频", description = "随机动漫视频")
    public void dmsp(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 4) {
            chain.sendMsg(MediaMessage.videoMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/dmsp.php?")), "撤回");
        }
    }

    @BotCommand(cmd = "cosplay", description = "随机cosplay图片")
    public void cosplay(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 7) {
            JSONObject res = HttpUtils.sendGet("https://api.yujn.cn/api/cosplay.php?type=json", false);
            if (res.getIntValue("code") == 200) {
                res = res.getJSONObject("data");
                List<Object> list = new ArrayList<>();
                list.add(new TextMessage("标题：" + res.getString("title") + "\n数量：" + res.getString("count")));
                res.getJSONArray("images").forEach(img -> list.add(MediaMessage.imageMedia(img.toString())));
                chain.sendListForwardMsg(list);
            }
        }
    }

    @BotCommand(cmd = "jk", alias = "JK,Jk,jK", description = "随机JK图片")
    public void jk(MetadataChain chain) {
        if (chain.getBasicCommand().length() == 2) {
            chain.sendMsg(MediaMessage.imageMedia("http://api.yujn.cn/api/jk.php??"), "撤回");
        }
    }
}
