package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicMessage {

    private String type;

    private Object data;

    public static MusicMessage music(String type, String id) {
        if ((!"qq".equals(type) && !"163".equals(type))) return null;
        return new MusicMessage("music", new Music(type, id));
    }

    public static MusicMessage musicCustom(String url, String audio, String title, String singer, String image) {
        if (!StringUtils.hasLength(url)
                || !StringUtils.hasLength(audio)
                || !StringUtils.hasLength(title)
                || !StringUtils.hasLength(singer))
            return null;
        return new MusicMessage("music", new MusicCustom(url, audio, title, singer, image));
    }
}

@Data
@AllArgsConstructor
class Music {

    /**
     * 音乐类型(qq/163)
     */
    private String type;

    /**
     * 音乐 ID
     */
    private String id;
}

@Data
class MusicCustom {

    private String type;

    /**
     * 跳转链接
     */
    private String url;

    /**
     * 音乐音频链接
     */
    private String audio;

    /**
     * 标题
     */
    private String title;

    /**
     * 歌手
     */
    private String singer;

    /**
     * 封面图片链接
     */
    private String image;

    public MusicCustom(String url, String audio, String title, String singer, String image) {
        this.type = "custom";
        this.url = url;
        this.audio = audio;
        this.title = title;
        this.singer = singer;
        this.image = image;
    }
}