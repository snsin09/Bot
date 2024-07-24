package com.bot.inori.bot.action;

import com.bot.inori.bot.model.data.MoeHuData;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.utils.annotation.BotCommand;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unused")
@Component
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
        chain.sendMsg(new TextMessage(String.join(", ", MoeHuData.getNames())));
    }

    @BotCommand(cmd = "看看", description = "看看age17的p站图片")
    public void age17(MetadataChain chain) {
        try {
            String tags = chain.getBasicCommand().substring(2).trim();
            String url = "https://acg17.com/category/meitu/pixiv-wallpaper?archiveSearch=" + tags.replace(" ", "+");
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.getElementsByClass("thumb-link");
            if (!links.isEmpty()) {
                Random random = new Random();
                url = links.get(random.nextInt(links.size())).attr("href");
                doc = Jsoup.connect(url).get();
                Elements entry = doc.getElementsByClass("entry-content");
                if (!entry.isEmpty()) {
                    Elements images = entry.get(0).getElementsByTag("img");
                    List<String> data = images.stream().map(image -> {
                        String img = image.attr("data-lazy-src");
                        if (!StringUtil.isBlank(img) && img.contains("url=")) return img;
                        return null;
                    }).filter(str -> !StringUtil.isBlank(str)).limit(5).toList();
                    if (!data.isEmpty()) {
                        List<Object> list = new ArrayList<>();
                        list.add(new TextMessage(doc.title()));
                        data.forEach(img -> list.add(MediaMessage.imageMedia(img)));
                        chain.sendListForwardMsg(list);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
