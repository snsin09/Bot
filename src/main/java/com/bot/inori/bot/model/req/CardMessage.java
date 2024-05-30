package com.bot.inori.bot.model.req;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CardMessage {

    private String type;

    private String data;

    public CardMessage(String prompt, String url) {
        this.type = "json";
        this.data = JSON.toJSONString(new Card(prompt, url));
    }
}

@Data
class Card {

    private String app = "com.tencent.gxhServiceIntelligentTip";

    private String desc = "制作小卡片";

    private String view = "gxhServiceIntelligentTip";

    private String bizsrc = "";

    private String ver = "";

    private String prompt;

    private String appID = "";

    private String sourceName = "";

    private String actionData = "";

    private String actionData_A = "";

    private String sourceUrl = "";

    private Meta meta;

    public Card(String prompt, String url) {
        this.prompt = prompt;
        this.meta = new Meta(url);
    }
}

@Data
class Meta {

    private ServiceIntelligentTip gxhServiceIntelligentTip;

    private Config config;

    private String text = "";

    private List<?> extraApps;

    private String sourceAd = "";

    private String extra = "";

    public Meta(String url) {
        this.gxhServiceIntelligentTip = new ServiceIntelligentTip(url);
        this.config = new Config(System.currentTimeMillis());
        this.extraApps = new ArrayList<>();
    }
}

@Data
class ServiceIntelligentTip {

    private String action = "";

    private String appid = "gxhServiceIntelligentTip";

    private String bgImg;

    private Object reportParams = new Object();

    public ServiceIntelligentTip(String bgImg) {
        this.bgImg = bgImg;
    }
}

@Data
class Config {

    private Integer autoSize = 0;

    private Long ctime;

    private Integer forward = 1;

    private Integer height = 520;

    private String token = "4f8ae05ebca255eea79acad7f5a1edb5";

    private String type = "normal";

    private Integer width = 9999;

    public Config(Long ctime) {
        this.ctime = ctime;
    }
}