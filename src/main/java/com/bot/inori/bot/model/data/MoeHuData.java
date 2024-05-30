package com.bot.inori.bot.model.data;

import com.bot.inori.bot.handler.MessageHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoeHuData {

    public static List<Map<String, String>> list = new ArrayList<>();

    public static String SUMMARY = "喵喵喵";

    static {
        try {
            Document doc = Jsoup.connect("https://img.moehu.org/").get();
            Elements elements = doc.getElementsByTag("pre");
            for (Element ele : elements) {
                String text = ele.text();
                if (text.contains(" → ID：") && !text.contains("随机")) {
                    String[] arr = text.split(" → ID：");
                    Map<String, String> map = new HashMap<>();
                    String[] arr2 = arr[1].trim().split(" ");
                    map.put("id", arr2[0]);
                    map.put("num", arr2[1].substring(4));
                    map.put("name", arr[0].trim());
                    list.add(map);
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("加载MoeHu资源报错 {}", e.getMessage());
        }
    }

    public static List<String> getNames() {
        return list.stream().map(map -> map.get("name")).collect(Collectors.toList());
    }

    public static String getIdByName(String name) {
        for (Map<String, String> map : list) {
            if (map.get("name").equals(name)) return map.get("id");
        }
        return null;
    }
}
