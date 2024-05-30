package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.bot.inori.bot.utils.FileUtils;
import com.bot.inori.bot.utils.annotation.BotCommand;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.data.ImageCacheData;
import com.bot.inori.bot.model.res.AtMsg;
import com.bot.inori.bot.model.res.FileMsg;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.StringUtil;
import org.apache.tomcat.util.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public class NaifuAction {

    private static Map<String, String> PROMPTS = new HashMap<>();

    static {
        try {
            InputStream is = NaifuAction.class.getClassLoader().getResourceAsStream("naifu_prompt.json");
            if (is != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    PROMPTS = JSON.parseObject(stringBuilder.toString(), new TypeReference<>() {});
                    MessageHandler.getLogger().info("加载绘图预设{}种", PROMPTS.size());
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("加载绘图预设失败 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "绘画", alias = "画画,绘图", description = "t4在线绘图接口", permit = false)
    public void drawPic(MetadataChain chain) {
        try {
            InputStream stream = null;
            FileMsg image = chain.getFirstImage();
            if (image != null) stream = HttpUtils.getStream(image.getUrl(), false);
            else if (chain.getReply() != null) {
                String img = ImageCacheData.gutData(chain.getReply().getId());
                if (img != null) stream = HttpUtils.getStream(img, false);
            } else if (!chain.getAtMsg().isEmpty()) {
                AtMsg at = chain.getFirstAt();
                stream = HttpUtils.getStream(BotHandler.getQQHeadUrl(at.getQq()), false);
            }
            chain.sendMsg("涂鸦中。。。");
            String tags = chain.getBasicCommand().substring(2);
            String base64 = generateNaifu(stream, tags);
            if (base64 != null) {
                chain.sendReplyMsg(MediaMessage.imageMedia(FileUtils.dlBase642LS(base64)));
            } else chain.sendMsg("涂鸦被撕了。。。");
        } catch (Exception e) {
            MessageHandler.getLogger().error("绘图报错 {}", e.getMessage());
        }
    }

    public String generateNaifu(InputStream stream, String tags) {
        try {
            String prompt = "masterpiece, best quality, ";
            if (!Objects.isNull(tags) && !tags.isEmpty()) prompt += tags;
            else prompt += PROMPTS.get(String.valueOf(new Random().nextInt(PROMPTS.size())));
            MessageHandler.getLogger().info("NAIFU预设 {}", prompt);
            JSONObject params = new JSONObject();
            Random random = new Random();
            if (stream != null) {
                params.put("noise", 0.2);
                params.put("strength", 0.7);
                params.put("image", Base64.encodeBase64String(StringUtil.inputStreamToByte(stream)));
                HttpUtils.closeStream(stream);
            }
            params.put("height", 768);
            params.put("width", 512);
            params.put("n_samples", 1);
            params.put("prompt", prompt);
            params.put("sampler", "Euler a");
            params.put("scale", 12);
            params.put("seed", random.nextLong());
            params.put("steps", 20 + random.nextInt(50));
            params.put("uc", "nsfw, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry");
            params.put("ucPreset", 0);
            String result = HttpUtils.sendJSONPost("https://draw.ecs2.t4wefan.pub/generate-stream", params, false);
            if (result != null) {
                String[] arr = result.split("\n");
                if (arr.length == 3) {
                    return arr[2].substring(5);
                } else {
                    MessageHandler.getLogger().error("生成图片失败！ {}", result);
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("生成AI图片失败！", e);
        }
        return null;
    }
}
