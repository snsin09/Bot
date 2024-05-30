package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 链接分享
 */
@Data
public class ShareMessage {

    private String type;

    private Share data;

    public ShareMessage(String url, String title, String content, String image, String file) {
        if (!StringUtils.hasLength(url)) url = "/";
        this.type = "share";
        this.data = new Share(url, title, content, image, file);
    }
}

@Data
@AllArgsConstructor
class Share {

    /**
     * 链接地址 收发
     */
    private String url;

    /**
     * 标题 收
     */
    private String title;

    /**
     * 内容 收
     */
    private String content;

    /**
     * 图片链接 收
     */
    private String image;

    /**
     * 文件链接 收
     */
    private String file;
}