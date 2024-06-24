package com.bot.inori.bot.utils;

import com.alibaba.fastjson2.JSONArray;
import com.bot.inori.bot.action.*;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.model.data.ActionData;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.tidy.Tidy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WrapHtmlUtils {

    private static final Logger logger = LoggerFactory.getLogger(WrapHtmlUtils.class);

    public static String captureHtml(String url) {
        ChromeDriver driver = null;
        try {
            driver = ScreenShotUtils.chromeDriver(true);
            driver.get(url);
            long height = (long) driver.executeScript("return document.documentElement.scrollHeight");
            driver.manage().window().setSize(new Dimension(1440, (int) height));
            driver.executeScript("document.body.style.overflow = 'hidden';");
            // 等待一段时间，确保页面加载完全
            Thread.sleep(200);
            File screenshotFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            File file = BotHandler.getFile("working/screenshot.png");
            assert file != null;
            FileUtils.copyFile(screenshotFile, file);
            return com.bot.inori.bot.utils.FileUtils.dlOrMoveImage2LS(file.getAbsolutePath(), false);
        } catch (Exception e) {
            logger.error("网页截图报错 {}", e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.close();
                    driver.quit();
                } catch (Exception ignored) {}
            }
        }
    }

    public static String generateMusic(JSONArray array) {
        ChromeDriver driver = null;
        try {
            Configuration configuration = new Configuration(Configuration.getVersion());
            configuration.setDirectoryForTemplateLoading(BotHandler.getDir("static"));
            configuration.setDefaultEncoding("utf-8");
            Template template = configuration.getTemplate("music.ftl");
            int width = 1000, height = 40 + (array.size() + 1) * 32 + 50;

            HashMap<String, Object> map = new HashMap<>();
            File[] files = BotHandler.getDir("background").listFiles();
            if (files != null) map.put("bg", files[new Random().nextInt(files.length)].getAbsolutePath().replace("\\", "/"));
            map.put("list", array);
            map.put("width", width);
            map.put("height", height);

            File f1 = BotHandler.getFile("working/f1.html");
            File f2 = BotHandler.getFile("working/f2.html");
            assert f1 != null && f2 != null;
            FileWriter fileWriter = new FileWriter(f1, StandardCharsets.UTF_8);
            template.process(map, fileWriter);
            fileWriter.close();
            htmlCovertToHtml(f1, f2);
            driver = ScreenShotUtils.chromeDriver(true);
            driver.get(f2.getAbsolutePath());
            driver.manage().window().setSize(new Dimension(width + 20, height + 20));
            // 隐藏滚动条
            driver.executeScript("document.body.style.overflow = 'hidden';");
            // 等待一段时间，确保页面加载完全
            Thread.sleep(200);
            File screenshot = driver.getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, Objects.requireNonNull(BotHandler.getFile("working/music.png")));
            return com.bot.inori.bot.utils.FileUtils.dlOrMoveImage2LS(Objects.requireNonNull(
                    BotHandler.getFile("working/music.png")).getAbsolutePath(), false);
        } catch (Exception e) {
            logger.error("生成点歌列表报错 {}", e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.close();
                    driver.quit();
                } catch (Exception ignored) {}
            }
        }
    }

    public static String generateMenu(List<ActionData> actions) {
        ChromeDriver driver = null;
        try {
            Configuration configuration = new Configuration(Configuration.getVersion());
            configuration.setDirectoryForTemplateLoading(BotHandler.getDir("static"));
            configuration.setDefaultEncoding("utf-8");
            Template template = configuration.getTemplate("menu.ftl");
            int width = 1000, height = 80;

            HashMap<String, Object> map = new HashMap<>();
            List<Map<String, Object>> list = new ArrayList<>();
            //BaseAction和GroupManageAction设置为基础功能
            Map<String, Object> sector = new HashMap<>();
            sector.put("name", "基础功能");
            List<ActionData> data = actions.stream().filter(action -> action.getClazz().equals(BaseAction.class) ||
                    action.getClazz().equals(GroupManageAction.class)).toList();
            if (!data.isEmpty()) {
                sector.put("list", data);
                list.add(sector);
                height += 40 + 51 * ((int) Math.ceil((double) data.size() / 4));
            }
            //AI和naifu设置为AI功能
            sector = new HashMap<>();
            sector.put("name", "AI功能");
            data = actions.stream().filter(action -> action.getClazz().equals(AIAction.class) ||
                    action.getClazz().equals(NaifuAction.class)).toList();
            if (!data.isEmpty()) {
                sector.put("list", data);
                list.add(sector);
                height += 40 + 51 * ((int) Math.ceil((double) data.size() / 4));
            }
            //Bili Blhx Fun Gif MoeHu Music Pet Pixiv设置为娱乐功能
            sector = new HashMap<>();
            sector.put("name", "娱乐功能");
            data = actions.stream().filter(action -> action.getClazz().equals(BiliAction.class) ||
                    action.getClazz().equals(FunAction.class) ||
                    action.getClazz().equals(GifAction.class) ||
                    action.getClazz().equals(MeoHuAction.class) ||
                    action.getClazz().equals(MusicAction.class) ||
                    action.getClazz().equals(PixivAction.class)).toList();
            if (!data.isEmpty()) {
                sector.put("list", data);
                list.add(sector);
                height += 40 + 51 * ((int) Math.ceil((double) data.size() / 4));
            }
            //Search Twitter设置为杂项
            sector = new HashMap<>();
            sector.put("name", "其他功能");
            data = actions.stream().filter(action -> !action.getClazz().equals(BaseAction.class) &&
                    !action.getClazz().equals(GroupManageAction.class) &&
                    !action.getClazz().equals(AIAction.class) &&
                    !action.getClazz().equals(NaifuAction.class) &&
                    !action.getClazz().equals(BiliAction.class) &&
                    !action.getClazz().equals(FunAction.class) &&
                    !action.getClazz().equals(GifAction.class) &&
                    !action.getClazz().equals(MeoHuAction.class) &&
                    !action.getClazz().equals(MusicAction.class) &&
                    !action.getClazz().equals(PixivAction.class)).toList();
            if (!data.isEmpty()) {
                sector.put("list", data);
                list.add(sector);
                height += 40 + 51 * ((int) Math.ceil((double) data.size() / 4));
            }
            File[] files = BotHandler.getDir("background").listFiles();
            if (files != null) map.put("bg", files[new Random().nextInt(files.length)].getAbsolutePath().replace("\\", "/"));
            map.put("list", list);
            map.put("width", width);
            map.put("height", height);

            File f1 = BotHandler.getFile("working/f1.html");
            File f2 = BotHandler.getFile("working/f2.html");
            assert f1 != null && f2 != null;
            FileWriter fileWriter = new FileWriter(f1, StandardCharsets.UTF_8);
            template.process(map, fileWriter);
            fileWriter.close();
            htmlCovertToHtml(f1, f2);
            driver = ScreenShotUtils.chromeDriver(true);
            driver.get(f2.getAbsolutePath());
            driver.manage().window().setSize(new Dimension(width + 20, height + 20));
            // 隐藏滚动条
            driver.executeScript("document.body.style.overflow = 'hidden';");
            // 等待一段时间，确保页面加载完全
            Thread.sleep(200);
            File screenshot = driver.getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, Objects.requireNonNull(BotHandler.getFile("working/menu.png")));
            driver.close();
            driver.quit();
            return com.bot.inori.bot.utils.FileUtils.dlOrMoveImage2LS(Objects.requireNonNull(
                    BotHandler.getFile("working/menu.png")).getAbsolutePath(), false);
        } catch (Exception e) {
            logger.error("生成菜单报错 {}", e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.close();
                    driver.quit();
                } catch (Exception ignored) {}
            }
        }
    }

    // 转换成标准HTML
    private static void htmlCovertToHtml(File sourceFilename, File targetFilename) {
        try {
            Tidy tidy = new Tidy();
            tidy.setInputEncoding("UTF-8");
            tidy.setOutputEncoding("UTF-8");
            // 每行的最多字符，如果为0，不自动换行
            tidy.setWraplen(0);
            // 是否保持属性中的空白字符
            tidy.setLiteralAttribs(true);
            FileInputStream in = new FileInputStream(sourceFilename);
            FileOutputStream out = new FileOutputStream(targetFilename);
            tidy.parse(in, out);
            out.close();
            in.close();
        } catch (Exception e) {
            logger.error("转换为html失败 {}", e.getMessage());
        }
    }
}
