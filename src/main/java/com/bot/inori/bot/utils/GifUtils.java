package com.bot.inori.bot.utils;

import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.img.gif.GifDecoder;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GifUtils {

    /**
     * 多图片转GIF
     */
    public static InputStream imagesToGIF(List<BufferedImage> images, String path, List<Integer> delay) {
        try {
            // 拆分一帧一帧的压缩之后合成
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(path);
            encoder.setRepeat(0);
            //取默认值
            encoder.setQuality(10);
            int width = 0, height = 0, limitSize = 300;
            for (BufferedImage img : images) {
                if (img.getWidth() > width) width = img.getWidth();
                if (img.getHeight() > height) height = img.getHeight();
            }
            for (int i = 0; i < images.size(); i++) {
                BufferedImage bufferedImage = images.get(i);
                if (width > limitSize || height > limitSize) {
                    if (width > limitSize) {
                        height = height * limitSize / width;
                        width = limitSize;
                    }
                    if (height > limitSize) {
                        width = width * limitSize / height;
                        height = limitSize;
                    }
                    bufferedImage = Thumbnails.of(bufferedImage).size(width, height).asBufferedImage();
                }
                encoder.setDelay(delay == null ? 100 : delay.get(i));
                BufferedImage zoomImage = new BufferedImage(width, height, 3);
                Image image = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                Graphics gc = zoomImage.getGraphics();
                gc.setColor(Color.WHITE);
                gc.drawImage(image, 0, 0, null);
                encoder.addFrame(zoomImage);
            }
            encoder.finish();
            File outFile = new File(path);
            BufferedImage image = ImageIO.read(outFile);
            ImageIO.write(image, outFile.getName(), outFile);
            return new FileInputStream(outFile);
        } catch (IOException e) {
            MessageHandler.getLogger().error("多图片转GIF报错！", e);
            return null;
        }
    }

    /**
     * 图片旋转
     */
    public static void imagesRotate(BufferedImage img, String path, int secends) {
        try {
            // 拆分一帧一帧的压缩之后合成
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(path);
            encoder.setRepeat(0);
            int width = 0, height = 0;
            if (img.getWidth() > width) width = img.getWidth();
            if (img.getHeight() > height) height = img.getHeight();
            for (int i = 0; i < 4; i++) {
                encoder.setDelay(secends);
                BufferedImage zoomImage = new BufferedImage(width, height, 3);
                img = rotateImage(img, i * 90);
                Image image = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                Graphics gc = zoomImage.getGraphics();
                gc.setColor(Color.WHITE);
                gc.drawImage(image, 0, 0, null);
                encoder.addFrame(zoomImage);
            }
            encoder.finish();
            File outFile = new File(path);
            BufferedImage image = ImageIO.read(outFile);
            ImageIO.write(image, outFile.getName(), outFile);
        } catch (IOException e) {
            MessageHandler.getLogger().error("多图片转GIF报错！", e);
        }
    }

    private static BufferedImage rotateImage(BufferedImage bufferedImage, int angel) {
        if (bufferedImage == null) return null;
        if (angel < 0) angel = angel + 360;
        int imageWidth = bufferedImage.getWidth(null);
        int imageHeight = bufferedImage.getHeight(null);
        // 计算重新绘制图片的尺寸
        Rectangle rectangle = calculatorRotatedSize(new Rectangle(new Dimension(imageWidth, imageHeight)), angel);
        // 获取原始图片的透明度
        int type = bufferedImage.getColorModel().getTransparency();
        BufferedImage newImage = new BufferedImage(rectangle.width, rectangle.height, type);
        Graphics2D graphics = newImage.createGraphics();
        // 平移位置
        graphics.translate((rectangle.width - imageWidth) / 2, (rectangle.height - imageHeight) / 2);
        // 旋转角度
        graphics.rotate(Math.toRadians(angel), (double) imageWidth / 2, (double) imageHeight / 2);
        // 绘图
        graphics.drawImage(bufferedImage, null, null);
        return newImage;
    }

    /**
     * 计算旋转后的尺寸
     */
    private static Rectangle calculatorRotatedSize(Rectangle src, int angel) {
        if (angel >= 90) {
            if (angel / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }
        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angel_dalta_width = Math.atan((double) src.height / src.width);
        double angel_dalta_height = Math.atan((double) src.width / src.height);
        int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_width));
        int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_height));
        int des_width = src.width + len_dalta_width * 2;
        int des_height = src.height + len_dalta_height * 2;
        return new Rectangle(new Dimension(des_width, des_height));
    }

    /**
     * GIF转图片
     */
    public static List<String> gifToImages(InputStream steam, String outputDirPath) {
        List<String> list = new ArrayList<>();
        try {
            File f = BotHandler.getDir(outputDirPath);
            GifDecoder decoder = new GifDecoder();
            int status = decoder.read(steam);
            if (status != GifDecoder.STATUS_OK)
                throw new IOException("read image error!");
            long name = System.currentTimeMillis();
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                BufferedImage bufferedImage = decoder.getFrame(i);// 获取每帧BufferedImage流
                String file = outputDirPath + "/" + name + "_" + i + ".png";
                File outFile = new File(file);
                ImageIO.write(bufferedImage, "png", outFile);
                list.add(file + "?" + decoder.getDelay(i));
            }
        } catch (IOException e) {
            MessageHandler.getLogger().error("GIF转图片报错！", e);
        }
        return list;
    }

    public static InputStream reduceGif(InputStream stream, int w, int h) {
        List<String> list = null;
        try {
            list = gifToImages(stream, Objects.requireNonNull(BotHandler.getFile("working/gif")).getAbsolutePath());
            List<BufferedImage> images = new ArrayList<>();
            List<Integer> delay = new ArrayList<>();
            for (String str : list) {
                String[] arr = str.split("\\?");
                Image image = ImageIO.read(new File(arr[0]));
                int width = image.getWidth(null);
                int height = image.getHeight(null);
                if (width < w) w = width;
                if (height < h) h = height;
                ScreenShotUtils.cutImage(arr[0], arr[0], (width - w) / 2, (height - h) / 2, w, h, "png");
                images.add(ImageIO.read(new File(arr[0])));
                delay.add(Integer.parseInt(arr[1]));
            }
            return imagesToGIF(images, Objects.requireNonNull(BotHandler.getFile("working/temp.gif")).getAbsolutePath(), delay);
        } catch (Exception e) {
            MessageHandler.getLogger().error("缩小gif失败！", e);
            return null;
        } finally {
            if (list != null && !list.isEmpty()) {
                list.forEach(str -> {
                    String[] arr = str.split("\\?");
                    File file = new File(arr[0]);
                    if (file.exists() && file.isFile() && file.delete()) MessageHandler.getLogger().debug("删除临时文件");
                });
            }
        }
    }

    /**
     * GIF转图流列表
     */
    public static List<InputStream> gifToSteam(String url) {
        List<InputStream> list = new ArrayList<>();
        try {
            GifDecoder decoder = new GifDecoder();
            int status = decoder.read(HttpUtils.getStream(url, false));
            if (status != GifDecoder.STATUS_OK)
                throw new IOException("read image " + url + " error!");
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                BufferedImage bi = decoder.getFrame(i);// 获取每帧BufferedImage流
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.write(bi, "png", bos);
                InputStream is = new ByteArrayInputStream(bos.toByteArray());
                list.add(is);
            }
        } catch (IOException e) {
            MessageHandler.getLogger().error("GIF转图片流报错！", e);
        }
        return list;
    }

    /**
     * 视频倒放
     */
    public static void reverseGif(InputStream stream, String outputPath) {
        try {
            GifDecoder decoder = new GifDecoder();
            int status = decoder.read(stream);
            if (status != GifDecoder.STATUS_OK)
                throw new IOException("read image error!");
            // 拆分一帧一帧的压缩之后合成
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(outputPath);
            encoder.setRepeat(decoder.getLoopCount());
            for (int i = decoder.getFrameCount() -1; i >= 0; i--) {
                encoder.setDelay(decoder.getDelay(i));// 设置播放延迟时间
                BufferedImage bufferedImage = decoder.getFrame(i);// 获取每帧BufferedImage流
                int height = bufferedImage.getHeight();
                int width = bufferedImage.getWidth();
                BufferedImage zoomImage = new BufferedImage(width, height, bufferedImage.getType());
                Image image = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                Graphics gc = zoomImage.getGraphics();
                gc.setColor(Color.WHITE);
                gc.drawImage(image, 0, 0, null);
                encoder.addFrame(zoomImage);
            }
            encoder.finish();
            File outFile = new File(outputPath);
            BufferedImage image = ImageIO.read(outFile);
            ImageIO.write(image, outFile.getName(), outFile);
        } catch (Exception e) {
            MessageHandler.getLogger().error("视频倒放报错！", e);
        }
    }

    /**
     * 视频倒放
     */
    public static void speed(InputStream stream, String outputPath, boolean flag) {
        try {
            GifDecoder decoder = new GifDecoder();
            int status = decoder.read(stream);
            if (status != GifDecoder.STATUS_OK)
                throw new IOException("read image error!");
            // 拆分一帧一帧的压缩之后合成
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(outputPath);
            encoder.setRepeat(decoder.getLoopCount());
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                encoder.setDelay(flag ? Math.max(10, decoder.getDelay(i) / 2) : decoder.getDelay(i) * 2);// 设置播放延迟时间
                BufferedImage bufferedImage = decoder.getFrame(i);// 获取每帧BufferedImage流
                int height = bufferedImage.getHeight();
                int width = bufferedImage.getWidth();
                BufferedImage zoomImage = new BufferedImage(width, height, bufferedImage.getType());
                Image image = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                Graphics gc = zoomImage.getGraphics();
                gc.setColor(Color.WHITE);
                gc.drawImage(image, 0, 0, null);
                encoder.addFrame(zoomImage);
            }
            encoder.finish();
            File outFile = new File(outputPath);
            BufferedImage image = ImageIO.read(outFile);
            ImageIO.write(image, outFile.getName(), outFile);
        } catch (Exception e) {
            MessageHandler.getLogger().error("动图变速报错！", e);
        }
    }
}
