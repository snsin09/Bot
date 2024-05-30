package com.bot.inori.bot.utils;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.action.func.ai.ChatModel;
import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.luciad.imageio.webp.WebPReadParam;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.net.ssl.SSLContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

public class HttpUtils {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36";

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static CloseableHttpClient getClientWithSSL() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        //忽略证书
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", csf)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        //连接池的最大连接数，0代表不限；如果取0，需要考虑连接泄露导致系统崩溃的后果
        connectionManager.setMaxTotal(1000);
        //每个路由的最大连接数,如果只调用一个地址,可以将其设置为最大连接数
        connectionManager.setDefaultMaxPerRoute(300);
        return HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(connectionManager).build();
    }

    public static JSONObject sendGet(String url, Boolean bproxy) {
        JSONObject jsonObject = null;
        try (CloseableHttpClient client = getClientWithSSL(); Jedis jedis = new Jedis()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Content-Type", "application/json");
            httpGet.setHeader("user-agent", USER_AGENT);
            RequestConfig.Builder builder = RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(30_000);
            if (bproxy) {
                if (url.contains("pixiv")) httpGet.setHeader("cookie", jedis.get("pixiv_cookie"));
                builder.setProxy(getPostProxy());
            } else if (url.contains("api.bilibili.com"))
                httpGet.setHeader("cookie", jedis.get("bili_cookie"));
            httpGet.setConfig(builder.build());
            CloseableHttpResponse response = client.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                if (!result.startsWith("{")) result = result.substring(result.indexOf("{"));
                jsonObject = JSONObject.parseObject(result);
            }
            response.close();
        } catch (Exception e) {
            logger.error("获取URL连接错误！ {}", e.getMessage());
        }
        return jsonObject;
    }

    public static String getHtml(String urlStr, Boolean bproxy) {
        String result = null;
        HttpURLConnection conn = null;
        try (Jedis jedis = new Jedis()){
            URL url = new URL(urlStr);
            if (bproxy) {
                conn = getProxyConn(url);
                conn.setRequestProperty("cookie", jedis.get("pixiv_cookie"));
            } else conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("user-agent", USER_AGENT);
            conn.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) buffer.append(line.trim());
            result = buffer.toString();
            reader.close();
            conn.disconnect();
        } catch (IOException e) {
            logger.error("获取URL连接错误！ {}", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result;
    }

    public static String getResp(String url) {
        HttpGet httpGet = new HttpGet(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10 * 1000)
                .setSocketTimeout(10 * 1000)
                .build();
        httpGet.setConfig(requestConfig);
        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity).trim();
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                return response.getFirstHeader("location").getValue();
            }
        } catch (Exception e) {
            logger.error("请求失败！ {}", e.getMessage());
        }
        return null;
    }

    public static JSONObject sendGptGet(List<ChatModel> list) {
        JSONObject object = null;
        try (CloseableHttpClient httpClient = HttpUtils.getClientWithSSL()) {
            HttpPost httpPost = new HttpPost("https://free.oneai.buzz/v1/chat/completions");
            JSONObject obj = new JSONObject();
            obj.put("model", "gpt-3.5-turbo");
            obj.put("temperature", 0.5);
            obj.put("max_tokens", 1024);
            obj.put("top_p", 1);
            obj.put("frequency_penalty", 0);
            obj.put("presence_penalty", 0);
            obj.put("messages", list);
            httpPost.setEntity(new StringEntity(obj.toString(), StandardCharsets.UTF_8));
            httpPost.setHeader("connection", "keep-alive");
            httpPost.setHeader("accept", "*/*");
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
            httpPost.setHeader("Authorization", "Bearer a u ok ?");
            RequestConfig config = RequestConfig.custom().setConnectTimeout(20_000).build();
            httpPost.setConfig(config);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null)
                object = JSONObject.parseObject(EntityUtils.toString(responseEntity, StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("OPENAI请求报错！", e);
        }
        return object;
    }

    public static Boolean isUrlOk(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (url.contains("bilivideo")) connection.setRequestProperty("referer", "https://www.bilibili.com");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) return Boolean.TRUE;
        } catch (Exception e) {
            logger.error("查看链接可用性报错 {}", e.getMessage());
        }
        return Boolean.FALSE;
    }

    public static Integer getUrlSize(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (url.contains("bilivideo")) connection.setRequestProperty("referer", "https://www.bilibili.com");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestMethod("HEAD");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                return connection.getContentLength();
        } catch (Exception e) {
            logger.error("获取资源大小报错！ {}", e.getMessage());
        }
        return null;
    }

    public static String getRedirectUrl(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.connect();
            String redirectLocation = conn.getHeaderField("Location");
            conn.disconnect();
            return redirectLocation;
        } catch (Exception e) {
            logger.error("请求失败！ {}", e.getMessage());
        }
        return null;
    }

    public static String sendJSONPost(String url, JSONObject params, Boolean flag) {
        String result = null;
        try (CloseableHttpClient httpClient = getClientWithSSL(); Jedis jedis = new Jedis()){
            HttpPost httpPost = new HttpPost(url);
            int time_out = 10_000;
            if (url.contains("t4wefan")) time_out = 60_000;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(time_out)
                    .setSocketTimeout(time_out)
                    .build();
            httpPost.setConfig(requestConfig);
            HttpEntity entity = new StringEntity(params.toJSONString(), "UTF-8");
            if (flag) httpPost.setConfig(RequestConfig.custom().setProxy(getPostProxy()).build());
            httpPost.setEntity(entity);
            httpPost.setHeader("user-agent", USER_AGENT);
            httpPost.setHeader("connection", "keep-alive");
            httpPost.setHeader("accept", "*/*");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer");
            if (url.contains("api.bilibili.com")) {
                httpPost.setHeader("cookie", jedis.get("bili_cookie"));
                httpPost.setHeader("referer", "https://www.bilibili.com");
            }
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null)
                result = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("请求失败！ {}", e.getMessage());
        }
        return result;
    }

    public static String sendAnimatePost(String urlStr) {
        String result = null;
        try (CloseableHttpClient httpClient = getClientWithSSL()){
            HttpPost httpPost = new HttpPost(urlStr);
            httpPost.setHeader("user-agent", USER_AGENT);
            httpPost.setHeader("connection", "keep-alive");
            httpPost.setHeader("accept", "*/*");
            RequestConfig config = RequestConfig.custom()
                    .setProxy(getPostProxy())
                    .setConnectTimeout(10 * 1000)
                    .setSocketTimeout(10 * 1000)
                    .build();
            httpPost.setConfig(config);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null)
                result = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("POST请求失败！ {}", e.getMessage());
        }
        return result;
    }

    public static JSONObject sendAnimatePost(String url, String imgurl) {
        JSONObject object = new JSONObject();
        try (InputStream stream = getStream(imgurl, false); CloseableHttpClient httpClient = getClientWithSSL()){
            HttpPost httpPost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addBinaryBody("image", stream, ContentType.MULTIPART_FORM_DATA, "post.png");
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            httpPost.setHeader("user-agent", USER_AGENT);
            httpPost.setHeader("connection", "keep-alive");
            httpPost.setHeader("accept", "*/*");
            RequestConfig config = RequestConfig.custom()
                    .setProxy(getPostProxy())
                    .setConnectTimeout(10 * 1000)
                    .setSocketTimeout(10 * 1000)
                    .build();
            httpPost.setConfig(config);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null)
                object = JSONObject.parseObject(EntityUtils.toString(responseEntity,
                        StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("POST文件请求失败！ {}", e.getMessage());
        }
        return object;
    }

    public static File getWebpImage(String url) {
        try {
            File webpFile = BotHandler.getFile("working/temp.webp");
            InputStream is = getStream(url, false);
            if (is == null) return null;
            byte[] bytes = StringUtil.inputStreamToByte(is);
            FileOutputStream fos = new FileOutputStream(Objects.requireNonNull(webpFile));
            if (bytes != null) fos.write(bytes);
            is.close();
            fos.close();
            File res = BotHandler.getFile("working/temp.png");
            if (res != null) webpToPng(webpFile.getAbsolutePath(), res.getAbsolutePath());
            return res;
        } catch (Exception e) {
            logger.error("获取文件失败！ {}", e.getMessage());
            return null;
        }
    }

    public static void webpToPng(String webpPath, String pngPath) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByMIMEType("image/webp").next();
        WebPReadParam readParam = new WebPReadParam();
        readParam.setBypassFiltering(true);
        FileImageInputStream stream = new FileImageInputStream(new File(webpPath));
        reader.setInput(stream);
        BufferedImage image = reader.read(0, readParam);
        ImageIO.write(image, "png", new File(pngPath));
        stream.close();
    }

    public static InputStream getStream(String url, Boolean proxy) {
        try {
            HttpURLConnection conn;
            if (proxy) conn = getProxyConn(new URL(url));
            else conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("user-agent", USER_AGENT);
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("accept", "*/*");
            return conn.getInputStream();
        } catch (Exception e) {
            logger.error("获取流报错！ {}", e.getMessage());
            return null;
        }
    }

    public static void downloadFiles(String path, String name, String url) {
        HttpURLConnection conn = null;
        try {
            File f = new File(path);
            if (!f.exists() && !f.mkdirs()) return;
            if (new File(path + name).exists()) return;
            if (url.startsWith("https://i.pixiv.cat/")) conn = getProxyConn(new URL(url));
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("user-agent", USER_AGENT);
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("accept", "*/*");
            File file = new File(path + name);
            InputStream is = conn.getInputStream();
            OutputStream os = new FileOutputStream(file);
            byte[] bs = new byte[1024];
            int len;
            while ((len = is.read(bs)) != -1) os.write(bs, 0, len);
            os.close();
            is.close();
        } catch (Exception e) {
            logger.error("保存文件报错！ {}", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static void closeStream(InputStream stream) {
        try {
            if (stream != null) stream.close();
        } catch (Exception e) {
            logger.error("关闭流报错！", e);
        }
    }

    public static HttpURLConnection getProxyConn(URL url) throws IOException {
        URL proxyUrl = new URL(BaseConfig.proxy);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
        return (HttpURLConnection) url.openConnection(proxy);
    }

    public static HttpHost getPostProxy() throws MalformedURLException {
        URL proxyUrl = new URL(BaseConfig.proxy);
        return new HttpHost(proxyUrl.getHost(), proxyUrl.getPort(), "http");
    }

    public static Proxy getProxy() throws MalformedURLException {
        URL proxyUrl = new URL(BaseConfig.proxy);
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
    }

    public static Boolean checkProxy(String url) {
        try {
            if (url == null) url = "https://www.google.com/";
            URL url2 = new URL(url);
            URL proxyUrl = new URL(BaseConfig.proxy);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
            HttpURLConnection proxyConnection = (HttpURLConnection) url2.openConnection(proxy);
            proxyConnection.setUseCaches(false);
            proxyConnection.setRequestMethod("HEAD");
            proxyConnection.setDoOutput(true);
            proxyConnection.setConnectTimeout(5_000);
            proxyConnection.connect();
            int responseCode = proxyConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) return true;
        } catch (Exception ignore) {
        }
        return false;
    }
}
