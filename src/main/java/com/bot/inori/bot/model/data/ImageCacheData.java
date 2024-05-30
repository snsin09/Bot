package com.bot.inori.bot.model.data;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImageCacheData {

    //1000条包含图片url的缓存数据
    final static Integer maxSize = 200;

    final static String KEY = "BotForShamrock_ImageCacheData";

    static Map<String, String> map = new LinkedHashMap<>(maxSize) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > maxSize;
        }
    };

    static {
        try (Jedis jedis = new Jedis()) {
            String value = jedis.get(KEY);
            if (value != null) map = JSON.parseObject(value, new TypeReference<>() {});
        }
    }

    public static void putData(Long message_id, String url) {
        try (Jedis jedis = new Jedis()) {
            map.put("icd-" + message_id, url);
            jedis.set(KEY, JSON.toJSONString(map), new SetParams().ex(86400));
        }
    }

    public static String gutData(Long message_id) {
        return map.get("icd-" + message_id);
    }
}
