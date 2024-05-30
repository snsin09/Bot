package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.model.data.MoeHuData;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.utils.annotation.BotCommand;

@SuppressWarnings("unused")
public class MeoHuAction {

    @BotCommand(cmd = "来点", description = "随机 MoeHu 图片")
    public void moeHu(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(2).trim();
        if (StringUtil.isBlank(cmd)) return;
        String id = MoeHuData.getIdByName(cmd);
        if (id != null) chain.sendMsg(MediaMessage.imageMedia("https://img.moehu.org/pic.php?id=" + id), "撤回");
    }

    @BotCommand(cmd = "Moe关键字", description = "MoeHu 可使用关键字")
    public void moeHuKey(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 6) return;
        chain.sendMsg(new TextMessage(String.join("\n", MoeHuData.getNames())));
    }

    @BotCommand(cmd = "60秒读世界", alias = "60秒看世界", permit = false, description = "每日60s读世界")
    public void everyDay60s(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 6) return;
        JSONObject res = HttpUtils.sendGet("https://api.2xb.cn/zaob", false);
        if (res != null && res.getInteger("code") == 200) chain.sendMsg(MediaMessage.imageMedia(res.getString("imageUrl")));
    }

    @BotCommand(cmd = "摸鱼日历", permit = false, description = "摸鱼日历")
    public void moYu(MetadataChain chain) {
        if (chain.getBasicCommand().length() != 4) return;
        chain.sendMsg(MediaMessage.imageMedia("https://dayu.qqsuu.cn/moyuribao/apis.php"));
    }

    @BotCommand(cmd = "狗屁不通", permit = false, description = "狗屁不通文章生成 支持一个参数")
    public void gp(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(4).trim();
        if (StringUtil.isBlank(cmd)) cmd = chain.getSender().getNickname();
        chain.sendMsg(new TextMessage(HttpUtils.getResp(String.format("https://api.lolimi.cn/API/dog/api.php?msg=%s&num=500&type=text", cmd))));
    }
}
