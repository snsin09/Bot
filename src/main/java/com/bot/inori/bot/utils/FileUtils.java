package com.bot.inori.bot.utils;

import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import kotlin.Pair;
import redis.clients.jedis.Jedis;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class FileUtils {

    public static void updateTxt(String filename, Set<String> list) {
        try {
            FileOutputStream fos = new FileOutputStream(Objects.requireNonNull(BotHandler.getFile(filename)));
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            PrintWriter writer = new PrintWriter(bw);
            for (String s : list) writer.write(s + "\r\n");
            writer.flush();
            writer.close();
            bw.close();
            osw.close();
            fos.close();
        } catch (IOException e) {
            MessageHandler.getLogger().error("更新文件报错！", e);
        }
    }

    public static Set<String> loadText(String name) {
        Set<String> list = new LinkedHashSet<>();
        try {
            File file = BotHandler.getFile(name);
            if (file != null) {
                if (!file.exists() && file.createNewFile()) MessageHandler.getLogger().debug("创建文件成功！");
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                String text;
                while ((text = reader.readLine()) != null) list.add(text);
                reader.close();
                isr.close();
                fis.close();
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("加载文件报错！", e);
        }
        return list;
    }

    public static String readFile(String filepath) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(Objects.requireNonNull(BotHandler.getFile(filepath)));
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String text;
            while ((text = reader.readLine()) != null) sb.append(text);
            reader.close();
            isr.close();
            fis.close();
        } catch (Exception e) {
            MessageHandler.getLogger().error("读取文件报错！", e);
        }
        return sb.toString();
    }

    public static void writeFile(String filepath, String data) {
        try {
            File file = BotHandler.getFile(filepath);
            if (file != null) {
                if (file.exists() && file.delete()) MessageHandler.getLogger().debug("删除成功");
                if (file.createNewFile()) MessageHandler.getLogger().debug("创建文件成功");
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                BufferedWriter bw = new BufferedWriter(osw);
                PrintWriter writer = new PrintWriter(bw);
                writer.write(data);
                writer.flush();
                writer.close();
                bw.close();
                osw.close();
                fos.close();
            }
        } catch (IOException e) {
            MessageHandler.getLogger().error("写入文件报错！", e);
        }
    }

    //下载或移动图片到本地图片服务器
    public static String dlOrMoveImage2LS(String url, Boolean proxy) {
        String resUrl = null;
        try {
            String filename = System.currentTimeMillis() + ".png";
            if (url.startsWith("http")) {
                HttpURLConnection conn;
                if (proxy) conn = HttpUtils.getProxyConn(new URL(url));
                else conn = (HttpURLConnection) new URL(url).openConnection();
                InputStream inputStream = conn.getInputStream();
                OutputStream outputStream = new FileOutputStream(BaseConfig.localPicAddress + filename);
                byte[] buffer = new byte[2048];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
            } else if (url.startsWith("file:///")) {
                url = url.substring(8);
                Files.copy(Paths.get(url), Paths.get(BaseConfig.localPicAddress + filename));
            }  else if (url.startsWith("file://")) {
                url = url.substring(7);
                Files.copy(Paths.get(url), Paths.get(BaseConfig.localPicAddress + filename));
            } else {
                Files.copy(Paths.get(url), Paths.get(BaseConfig.localPicAddress + filename));
                File file = new File(url);
                if (file.isFile()) file.delete();
            }
            resUrl = BaseConfig.localPicIp + filename;
        } catch (Exception e) {
            MessageHandler.getLogger().error("下载图片到本地服务器报错！", e);
        }
        return resUrl;
    }

    //下载或移动图片到本地图片服务器
    public static void dl2path(String url, File des) {
        try (Jedis jedis = new Jedis()) {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("cookie", jedis.get("bili_cookie"));
            conn.setRequestProperty("referer", "https://www.bilibili.com");
            conn.setRequestProperty("User-Agent", "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            InputStream inputStream = conn.getInputStream();
            OutputStream outputStream = new FileOutputStream(des);
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            MessageHandler.getLogger().error("下载到本地报错！", e);
        }
    }

    //下载图片流到本地图片服务器
    public static String dlStream2LS(InputStream inputStream) {
        String resUrl = null;
        try {
            Pair<InputStream, String> pair = getExt(inputStream);
            if (pair == null) return null;
            inputStream = pair.getFirst();
            String filename = System.currentTimeMillis() + "." + pair.getSecond();
            OutputStream outputStream = new FileOutputStream(BaseConfig.localPicAddress + filename);
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            resUrl = BaseConfig.localPicIp + filename;
        } catch (Exception e) {
            MessageHandler.getLogger().error("下载图片流到本地服务器报错！", e);
        }
        return resUrl;
    }

    //下载Base64到本地图片服务器
    public static String dlBase642LS(String base64) {
        String resUrl = null;
        try {
            String filename = System.currentTimeMillis() + ".png";
            byte[] decodedBytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
            BufferedImage image = ImageIO.read(bis);
            bis.close();
            File outputfile = new File(BaseConfig.localPicAddress + filename);
            ImageIO.write(image, "png", outputfile);
            resUrl = BaseConfig.localPicIp + filename;
        } catch (Exception e) {
            MessageHandler.getLogger().error("下载图片流到本地服务器报错！", e);
        }
        return resUrl;
    }

    private static Pair<InputStream, String> getExt(InputStream inputStream) {
        try {
            PushbackInputStream pis = new PushbackInputStream(inputStream, 8);
            byte[] header = new byte[8]; // 读取文件的前8个字节
            int bytesRead = pis.read(header);
            pis.unread(header, 0, bytesRead);
            inputStream = pis;
            if (bytesRead >= 4) {
                if (isJPEG(header)) return new Pair<>(inputStream, "jpeg");
                else if (isPNG(header)) return new Pair<>(inputStream, "png");
                else if (isGIF(header)) return new Pair<>(inputStream, "gif");
                else if (isMP4(header)) return new Pair<>(inputStream, "mp4");
                else if (isAVI(header)) return new Pair<>(inputStream, "avi");
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取流类型报错！", e);
        }
        return null;
    }

    // 判断是否为JPEG格式
    private static boolean isJPEG(byte[] header) {
        return header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 &&
                header[2] == (byte) 0xFF && (header[3] & 0xF0) == 0xE0;
    }

    // 判断是否为PNG格式
    private static boolean isPNG(byte[] header) {
        return header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47 &&
                header[4] == (byte) 0x0D && header[5] == (byte) 0x0A &&
                header[6] == (byte) 0x1A && header[7] == (byte) 0x0A;
    }

    // 判断是否为GIF格式
    private static boolean isGIF(byte[] header) {
        return header[0] == (byte) 0x47 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x38 &&
                (header[4] == (byte) 0x37 || header[4] == (byte) 0x39) &&
                header[5] == (byte) 0x61;
    }

    // 判断是否为MP4格式
    private static boolean isMP4(byte[] header) {
        return header[0] == (byte) 0x00 && header[1] == (byte) 0x00 &&
                header[2] == (byte) 0x00 && (header[3] == (byte) 0x18 ||
                header[3] == (byte) 0x20 || header[3] == (byte) 0x28);
    }

    // 判断是否为AVI格式
    private static boolean isAVI(byte[] header) {
        return header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x46 &&
                header[8] == (byte) 0x41 && header[9] == (byte) 0x56 &&
                header[10] == (byte) 0x49 && header[11] == (byte) 0x20;
    }
}
