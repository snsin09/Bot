package com.bot.inori.bot.model.req;

import com.bot.inori.bot.model.data.MoeHuData;
import com.bot.inori.bot.utils.StringUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;

import java.io.*;

/**
 * 媒体消息
 */
@Data
public class MediaMessage {

    private String type;

    private MediaData data;

    private MediaMessage(String type, MediaData data) {
        this.type = type;
        this.data = data;
    }

    //图片
    public static MediaMessage imageMedia(String url) {
        return media("image", url);
    }

    //图片
    public static MediaMessage imageMedia(byte[] bytes) {
        return media("image", bytes);
    }

    //图片
    public static MediaMessage imageMedia(File file) {
        return media("image", file);
    }

    //语音
    public static MediaMessage audioMedia(String url) {
        return media("record", url);
    }

    //语音
    public static MediaMessage audioMedia(byte[] bytes) {
        return media("record", bytes);
    }

    //语音
    public static MediaMessage audioMedia(File file) {
        return media("record", file);
    }

    //视频
    public static MediaMessage videoMedia(String url) {
        return media("video", url);
    }

    public static MediaMessage videoMedia(byte[] bytes) {
        return media("video", bytes);
    }

    public static MediaMessage videoMedia(File file) {
        return media("video", file);
    }

    private static MediaMessage media(String type, Object obj) {
        try {
            if (obj instanceof String)
                return new MediaMessage(type, new MediaData(String.valueOf(obj)));
            else if (obj instanceof byte[])
                return new MediaMessage(type, new MediaData("base64://" + Base64.encodeBase64String((byte[]) obj)));
            else if (obj instanceof File)
                return new MediaMessage(type, new MediaData("base64://" + Base64.encodeBase64String(
                        StringUtil.inputStreamToByte(new FileInputStream((File) obj)))));
        } catch (Exception ignored) {
        }
        return null;
    }
}

@Data
@NoArgsConstructor
class MediaData {

    private String file;

    private String summary;

    public MediaData(String file) {
        this.summary = MoeHuData.SUMMARY;
        this.file = file;
    }
}