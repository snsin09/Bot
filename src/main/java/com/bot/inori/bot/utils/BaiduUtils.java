package com.bot.inori.bot.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.config.BaseConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BaiduUtils {

    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();

    private static final Logger logger = LoggerFactory.getLogger(BaiduUtils.class);

    private static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY";

    private static String getToken() {
        String access_token = null;
        try (Jedis jedis = new Jedis()) {
            if (jedis.get(ACCESS_TOKEN_KEY) != null) return jedis.get(ACCESS_TOKEN_KEY);
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create("", mediaType);
            Request request = new Request.Builder()
                    .url(String.format("https://aip.baidubce.com/oauth/2.0/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
                            BaseConfig.client_id, BaseConfig.client_secret))
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();
            Response response = HTTP_CLIENT.newCall(request).execute();
            if (response.body() != null) {
                JSONObject res = JSON.parseObject(response.body().string());
                access_token = res.getString("access_token");
                jedis.set(ACCESS_TOKEN_KEY, access_token, new SetParams().ex(res.getLong("expires_in")));
                response.close();
            }
        } catch (Exception e) {
            logger.error("获取access_token报错", e);
        }
        return access_token;
    }

    public static Boolean checkLegal(String imgStr) {
        boolean flag = false;
        try {
            String imgParam = URLEncoder.encode(imgStr, StandardCharsets.UTF_8);
            String param = "image=" + imgParam;
            String result = postData("https://aip.baidubce.com/rest/2.0/solution/v1/img_censor/v2/user_defined?access_token=" +
                    getToken(), param);
            logger.info(result);
            JSONObject res = JSON.parseObject(result);
            if ("合规".equals(res.get("conclusion"))) flag = true;
        } catch (Exception e) {
            logger.error("图片鉴权报错", e);
        }
        return flag;
    }

    public static String postData(String requestUrl, String params) throws Exception {
        String encoding = "UTF-8";
        if (requestUrl.contains("nlp")) encoding = "GBK";
        URL url = new URL(requestUrl);

        // 打开和URL之间的连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        // 设置通用的请求属性
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);

        // 得到请求的输出流对象
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.write(params.getBytes(encoding));
        out.flush();
        out.close();

        // 建立实际的连接
        connection.connect();
        // 获取所有响应头字段
//        Map<String, List<String>> headers = connection.getHeaderFields();
//        // 遍历所有的响应头字段
//        for (String key : headers.keySet()) {
//            System.err.println(key + "--->" + headers.get(key));
//        }
        // 定义 BufferedReader输入流来读取URL的响应
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
        StringBuilder result = new StringBuilder();
        String getLine;
        while ((getLine = in.readLine()) != null) result.append(getLine);
        in.close();
        return result.toString();
    }
}
