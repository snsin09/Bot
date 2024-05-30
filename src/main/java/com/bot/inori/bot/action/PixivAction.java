package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.action.func.pixiv.PixivCenter;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.PixivUtils;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.utils.annotation.BotCommand;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unused")
public class PixivAction {

    @BotCommand(cmd = "萝莉", description = "基于萝莉控接口实现的涩涩功能")
    public void lolicon(MetadataChain chain) {
        try (Jedis jedis = new Jedis()) {
            Long uid = chain.getSender().getUser_id();
            if (jedis.get("lolicon" + uid) != null && !BotHandler.isMaster(uid)) {
                chain.sendMsg(String.format("你先别急， %s秒后再来吧~", jedis.ttl("lolicon" + uid)));
                return;
            }
            String cmd = chain.getBasicCommand().substring(2).trim();
            chain.sendMsg("萝莉控开始搜图...");
            jedis.set("lolicon" + uid, "贤者时间", new SetParams().ex(120));
            PixivCenter.lolicon(chain, cmd);
        }
    }

    @BotCommand(cmd = "涩涩", alias = "涩图,色图", description = "代理p站图片欣赏")
    public void pic(MetadataChain chain) {
        try (Jedis jedis = new Jedis()) {
            Long uid = chain.getSender().getUser_id();
            if (jedis.get("vilipix" + uid) != null && !BotHandler.isMaster(uid)) {
                chain.sendMsg(String.format("你先别急， %s秒后再来吧~", jedis.ttl("vilipix" + uid)));
                return;
            }
            Random random = new Random();
            JSONObject res = HttpUtils.sendGet(String.format("https://www.vilipix.com/api/v1/picture/public?limit=20&offset=%s&sort=hot&type=0",
                    random.nextInt(1000)), false);
            if (res.getInteger("code") == 0) {
                JSONArray data = res.getJSONObject("data").getJSONArray("rows");
                if (!data.isEmpty()) {
                    int count = 0;
                    res = data.getJSONObject(random.nextInt(data.size()));
                    while (res.toJSONString().contains("男")) {
                        res = data.getJSONObject(random.nextInt(data.size()));
                        if (count++ >= 50) break;
                    }
                    List<Object> list = new ArrayList<>();
                    list.add(new TextMessage(String.format("标题：%s\n标签：%s\n", res.getString("title"), res.getString("tags"))));
                    list.add(MediaMessage.imageMedia(res.getString("original_url")));
                    chain.sendMsg(list, "撤回");
                    jedis.set("vilipix" + uid, "贤者时间", new SetParams().ex(30));
                }
            }
        }
    }

    @BotCommand(cmd = "排行", description = "排行1 5，前面数字代表类别（0 每日 1 每日H 2 每周 3 每周H 4 每月 5 每月H，默认0），后面代表数量（默认3）", isMaster = true)
    public void ranking(MetadataChain chain) {
        PixivCenter.ranking(chain, chain.getBasicCommand().substring(2).trim());
    }

    @BotCommand(cmd = "画师", description = "查看关注画师情况", isMaster = true)
    public void artist(MetadataChain chain) {
        PixivCenter.artist(chain, chain.getBasicCommand().substring(2).trim());
    }

    @BotCommand(cmd = "设置PC", description = "设置P站cookie", isMaster = true)
    public void cookie(MetadataChain chain) {
        try (Jedis jedis = new Jedis()) {
            jedis.set("pixiv_cookie", chain.getBasicCommand().substring(4).trim(),
                    new SetParams().ex(Integer.MAX_VALUE));
            chain.sendMsg("设置P站cookie成功！");
        }
    }

    @BotCommand(cmd = "设置P站代理", description = "设置P站代理网址", isMaster = true)
    public void setProxy(MetadataChain chain) {
        String content = chain.getBasicCommand().substring(6).trim();
        if (content.startsWith("https://")) {
            PixivUtils.setPIXY(content);
            chain.sendMsg("设置代理成功！");
        } else {
            String builder = """
                    可使用地址：
                    https://i.pixiv.re/
                    https://i.pixiv.cat/
                    https://pixiv.cat/""";
            chain.sendMsg(builder);
        }
    }

    @BotCommand(cmd = "设置AI", description = "设置P站获取图片是否包含AI，0 全部 1 禁止AI 2 AI图", isMaster = true)
    public void setAI(MetadataChain chain) {
        String content = chain.getBasicCommand().substring(4).trim();
        if (StringUtil.isNumeric(content)) {
            int type = Integer.parseInt(content);
            if (type < 0 || type > 2) chain.sendMsg("模式错误（0 全部 1 禁止AI 2 AI图）");
            else {
                PixivUtils.setAI(type);
                chain.sendMsg("设置AI类型成功");
            }
        }
    }

    @BotCommand(cmd = "设置喜欢数", description = "设置P站获取图片喜欢数阈值", isMaster = true)
    public void setLikeCount(MetadataChain chain) {
        String content = chain.getBasicCommand().substring(5).trim();
        if (StringUtil.isNumeric(content)) {
            PixivUtils.setLIKECOUNT(Math.abs(Integer.parseInt(content)));
        }
    }
}
