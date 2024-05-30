package com.bot.inori.bot.utils;

import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ScreenShotUtils {

    public static File captureArticle(String url) {
        WebDriver driver = null;
        try {
            driver = chromeDriver(true);
            driver.get(url + "?jump_opus=1");
            int pageHeight = 598;
            driver.manage().window().setSize(new Dimension(1382, pageHeight));
            Thread.sleep(2000);
            WebElement content = driver.findElement(By.xpath("//div[@class='article-container']"));
            try {
                ((JavascriptExecutor) driver).executeScript("return document.getElementsByClassName(\"mini-header m-header mini-type\")[0].remove()");
                ((JavascriptExecutor) driver).executeScript("return document.getElementsByClassName(\"fixed-top-header\")[0].remove()");
                ((JavascriptExecutor) driver).executeScript("return document.getElementsByClassName(\"login-tip\")[0].remove()");
            } catch (Exception ignore) {
            }
            int height = content.getSize().getHeight();
            double num = Math.ceil((double) height / pageHeight);
            String file = ScreenShot(driver, content, "文章.png",
                    BotHandler.getDir("working/").getAbsolutePath() + "/", content.getLocation().getY(),
                    pageHeight, (int) num);
            if (file != null) {
                cutImage(file, file, 0, 0, content.getSize().getWidth(), height, "png");
                return new File(file);
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("生成截图报错！", e);
        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
        return null;
    }

    public static ChromeDriver chromeDriver(Boolean image) {
        ChromeDriver driver = null;
        try {
            System.setProperty("webdriver.chrome.whitelistedIps", "");
            System.setProperty("webdriver.chrome.driver", BotHandler.getDir("/working").getAbsolutePath() + "/chromedriver.exe");
            ChromeOptions options = new ChromeOptions();
            //无浏览器模式
            options.addArguments("--headless");
            options.addArguments("--proxy-server=" + BaseConfig.proxy);
            // 为了让root用户也能执行
            options.addArguments("--no-sandbox");
            // 优化参数
            options.addArguments("--disable-dev-shm-usage");
            if (!image) options.addArguments("blink-settings=imagesEnabled=false");
            options.addArguments("--disable-gpu");
            driver = new ChromeDriver(options);
        } catch (Exception e) {
            MessageHandler.getLogger().error("开启Chrome驱动失败！", e);
        }
        return driver;
    }

    /**
     * 生成长截图
     *
     * @param driver     驱动
     * @param content    截图元素
     * @param filename   图片名称
     * @param initHeight 元素初始高度
     * @param pageHeight 元素翻页高度
     * @param num        截图图片数量
     */
    private static String ScreenShot(WebDriver driver, WebElement content, String filename, String path, int initHeight, int pageHeight, int num) {
        try {
            String[] arr = new String[num];
            boolean flag = false;
            for (int i = 0; i < num; i++) {
                if (i > 0) initHeight += pageHeight;
                ((JavascriptExecutor) driver).executeScript(String.format("window.scrollTo(0, %s)", initHeight));
                if (filename.contains("文章")) {
                    try {
                        Thread.sleep(100);
                        ((JavascriptExecutor) driver).executeScript("return document.getElementsByClassName(\"login-tip\")[0].remove()");
                        ((JavascriptExecutor) driver).executeScript("return document.getElementsByClassName(\"geetest_wind\")[0].remove()");
                    } catch (Exception ignore) {
                    }
                }
                if (filename.contains("知乎")) {
                    Thread.sleep(2000);
                    if (!flag) {
                        ((JavascriptExecutor) driver).executeScript("return document.getElementsByClassName(\"css-1ynzxqw\")[0].remove()");
                        flag = true;
                    }
                } else Thread.sleep(1000);
                String partname = "part_" + i + ".png";
                arr[i] = path + partname;
                File filetmps = new File(path + partname);
                File srcFile = content.getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(srcFile, filetmps);
            }
            mergeImage(arr, 2, path + filename);
            Arrays.stream(arr).forEach(s -> {
                File f = new File(s);
                if (f.exists() && f.delete()) MessageHandler.getLogger().debug("删除临时文件！");
            });
            return path + filename;
        } catch (Exception e) {
            MessageHandler.getLogger().error("截图报错！", e);
            return null;
        }
    }

    /**
     * 图片裁剪通用接口
     *
     * @param src  源图片地址,图片格式PNG
     * @param dest 目的图片地址
     * @param x    图片起始点x坐标
     * @param y    图片起始点y坐标
     * @param w    图片宽度
     * @param h    图片高度
     */
    public static void cutImage(String src, String dest, int x, int y, int w, int h, String ext) {
        try {
            if (ext == null) ext = "png";
            //获取png图片的ImageReader的Iterator
            Iterator<ImageReader> iterator = ImageIO.getImageReadersByFormatName(ext);
            //根据Iterator获取ImageReader
            ImageReader reader = iterator.next();
            //获取源图片输入流
            InputStream in = new FileInputStream(src);
            //根据源图片输入流获得ImageInputStream流
            ImageInputStream iis = ImageIO.createImageInputStream(in);
            //将ImageInputStream流加载到ImageReader中
            reader.setInput(iis, true);
            //图片读取参数
            ImageReadParam param = reader.getDefaultReadParam();
            java.awt.Rectangle rect = new java.awt.Rectangle(x, y, w, h);
            //参数对象设置形状为一定大小的长方形
            param.setSourceRegion(rect);
            //ImageReader根据参数对象获得BufferedImage
            BufferedImage bi = reader.read(0, param);
            //将经过参数对象筛选的图片流写入目标文件中
            ImageIO.write(bi, ext, new File(dest));
            in.close();
            iis.close();
        } catch (IOException e) {
            MessageHandler.getLogger().error("裁剪图片报错！", e);
        }
    }

    /**
     * 拼接图片（注：图片需长宽一致）
     *
     * @param files      img1 ，img2
     * @param type       1：横向拼接  2：纵向拼接
     * @param targetFile 合成新的图片地址
     */
    private static void mergeImage(String[] files, int type, String targetFile) {
        int len = files.length;
        if (len < 1) throw new RuntimeException("图片数量小于1");
        File[] src = new File[len];
        BufferedImage[] images = new BufferedImage[len];
        int[][] ImageArrays = new int[len][];
        for (int i = 0; i < len; i++) {
            try {
                src[i] = new File(files[i]);
                images[i] = ImageIO.read(src[i]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int width = images[i].getWidth();
            int height = images[i].getHeight();
            ImageArrays[i] = new int[width * height];
            ImageArrays[i] = images[i].getRGB(0, 0, width, height, ImageArrays[i], 0, width);
        }
        int newHeight = 0;
        int newWidth = 0;
        for (BufferedImage image : images) {
            // 横向
            if (type == 1) {
                newHeight = Math.max(newHeight, image.getHeight());
                newWidth += image.getWidth();
            } else if (type == 2) {// 纵向
                newWidth = Math.max(newWidth, image.getWidth());
                newHeight += image.getHeight();
            }
        }
        if (type == 1 && newWidth < 1) return;
        if (type == 2 && newHeight < 1) return;
        // 生成新图片
        try {
            BufferedImage ImageNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            int height_i = 0;
            int width_i = 0;
            for (int i = 0; i < images.length; i++) {
                if (type == 1) {
                    ImageNew.setRGB(width_i, 0, images[i].getWidth(), newHeight, ImageArrays[i], 0,
                            images[i].getWidth());
                    width_i += images[i].getWidth();
                } else if (type == 2) {
                    ImageNew.setRGB(0, height_i, newWidth, images[i].getHeight(), ImageArrays[i], 0, newWidth);
                    height_i += images[i].getHeight();
                }
            }
            //输出想要的图片
            String[] arr = targetFile.split("\\.");
            ImageIO.write(ImageNew, arr[arr.length - 1], new File(targetFile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
