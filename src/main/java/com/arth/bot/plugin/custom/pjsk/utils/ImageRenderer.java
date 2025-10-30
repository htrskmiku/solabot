package com.arth.bot.plugin.custom.pjsk.utils;

import com.arth.bot.adapter.fetcher.http.ImgService;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.cache.service.ImageCacheService;
import com.arth.bot.plugin.custom.pjsk.Pjsk;
import com.arth.bot.plugin.custom.pjsk.objects.PjskCard;
import com.arth.bot.plugin.custom.pjsk.objects.enums.CardAttributes;
import com.arth.bot.plugin.custom.pjsk.objects.enums.CardRarities;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.*;

@RequiredArgsConstructor
public class ImageRenderer {


//    public static void main(String[] args) throws IOException {
//        //BufferedImage image = getBufferedImage("static/box/res021_no057_normal.png");
//        //image = resize(image,140,140);
//        BufferedImage boarder = getBufferedImage("static/box/boarder/rarity_4.png");
//        BufferedImage rarity = getBufferedImage("static/box/star.png");
//        BufferedImage attribute = getBufferedImage("static/box/attribute/cool.png");
//
//        rarity = resize(rarity,25,25);
//        attribute = resize(attribute,30,30);
//        //BufferedImage card = mergeImages(boarder,image,8,8);
//        BufferedImage card = boarder;
//        //card = mergeImages(boarder,rarity,5,130);
//        for (int i=0;i<4;i++){
//            card = mergeImages(card,rarity,9+25*i,124);
//        }
//        card = mergeImages(card,attribute,8,8);
//        generateSaveFile(card,"static/box/out.png");
//    }
    //懒得学，ai不香吗
    private static BufferedImage mergeImages(BufferedImage background,
                                            BufferedImage foreground,
                                            int x, int y) {
        // 创建与背景图相同大小的新图像
        BufferedImage combined = new BufferedImage(
                background.getWidth(),
                background.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        // 获取 Graphics2D 对象
        Graphics2D g2d = combined.createGraphics();
        // 设置渲染提示以获得更好的质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        // 绘制背景图
        g2d.drawImage(background, 0, 0, null);
        // 绘制前景图（自动处理Alpha通道）
        g2d.drawImage(foreground, x, y, null);
        g2d.dispose();
        return combined;
    }

    /**
     * 扩展图片
     * @param image1
     * @param image2
     * @param times
     * @return
     */
    private static BufferedImage extendImage(BufferedImage image1,BufferedImage image2,int times){
        int targetWidth = image1.getWidth() + image2.getWidth();
        int targetHeight = image1.getHeight() + image2.getHeight();
        BufferedImage combined = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = combined.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(image1, 0, 0, null);
        for (int i = 0; i < times; i++) {
            g2d.drawImage(image1, image2.getWidth() + i*image2.getWidth(), 0, null);
        }
        g2d.dispose();
        return combined;
    }

//    private static BufferedImage getBufferedImage(String fileUrl)
//            throws IOException {
//        File f = new File(fileUrl);
//        return ImageIO.read(f);
//    }
//
    private static void generateSaveFile(BufferedImage buffImg, String savePath) {
        int temp = savePath.lastIndexOf(".") + 1;
        try {
            File outFile = new File(savePath);
            if(!outFile.exists()){
                outFile.createNewFile();
            }
            ImageIO.write(buffImg, savePath.substring(temp), outFile);
            System.out.println("ImageIO write...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 缩放图片
     * @param originalImage
     * @param width
     * @param height
     * @return
     */
    //卡面缩略图原大小为128x128，需要缩放至140x140
    private static BufferedImage resize(BufferedImage originalImage,int width,int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        return resizedImage;
    }

    //卡片渲染，大小固定为156*156
    public static class Card{
        private String endpoint;    //常用静态资源在src/main/resources/static/pjsk/box中
        private CardAttributes attributes;
        private CardRarities rarities;
        private BufferedImage thumbnails;
        private BufferedImage boarder;
        private BufferedImage rarityImage;
        private BufferedImage attributeImage;
        private BufferedImage output;
        private boolean shouldDrawRarity = true;
        private boolean shouldDrawBoarder = true;
        private boolean shouldDrawAttribute = true;
        //https://assets.unipjsk.com/startapp/thumbnail/chara/{assetbundle_name}_{status}.png
        public Card(Pjsk.BeanContext ctx,PjskCard pjskCard){
            ImgService imgService = ctx.imgService();
            endpoint = ctx.networkEndpoint();
            attributes = pjskCard.getAttributes();
            rarities = pjskCard.getRarities();
            thumbnails = imgService.getBufferedImg(ctx.thumbnailApi()
                    .replace("{assetbundle_name}",pjskCard.getAssetsbundleName())
                    .replace("{status}",pjskCard.getSpecialTrainingStatus()));

            boarder = imgService.getBufferedImg(endpoint + "/pjsk/box/boarder/" + pjskCard.getRarities().name().toLowerCase() + ".png");
            rarityImage = imgService.getBufferedImg(endpoint + "/pjsk/box/" +
                    (pjskCard.getRarities().equals(CardRarities.RARITY_BIRTHDAY) ? "rarity_birthday.png" : "star.png"));
            attributeImage = imgService.getBufferedImg(endpoint + "/pjsk/box/attribute/" + pjskCard.getAttributes().name().toLowerCase() + ".png");
        }
        public void noDrawRarity(){shouldDrawRarity = false;}
        public void noDrawBoarder(){shouldDrawBoarder = false;}
        public void noDrawAttribute(){shouldDrawAttribute = false;}
        public BufferedImage draw(){
            rarityImage = resize(rarityImage,25,25);
            attributeImage = resize(attributeImage,30,30);//缩小稀有度与属性标识

            if(shouldDrawBoarder){
                output = mergeImages(boarder,thumbnails,8,8);//框+图
            }else{
                output = thumbnails;
            }
            if (shouldDrawRarity) {
                for (int i = 0; i < rarities.getDrawQuantity(); i++) {
                    output = mergeImages(output, rarityImage, 9 + 25 * i, 124);//画稀有度
                }
            }
            if (shouldDrawAttribute) {
                output = mergeImages(output,attributeImage,8,8);
            }

            return output;}
    }


    //输出渲染box,单个背景最多可放60张卡(5x10)
    public static class Box{
        private String endpoint;
        private ArrayList<PjskCard> cards;
        private BufferedImage background;
        private BufferedImage output;
        private boolean descend = true;
        public Box(Pjsk.BeanContext ctx,ArrayList<PjskCard> cards) {
            endpoint = ctx.networkEndpoint();
            this.background = ctx.imgService().getBufferedImg(endpoint + "/pjsk/box/background.png");
            this.output = background;
            this.cards = cards;
        }

        /**
         * 关闭降序排列，将按卡id升序绘制
         * @return
         */
        public Box noDescend(){
            this.descend = false;
            return this;
        }
        public BufferedImage draw(){
            if(descend){
                Collections.sort(cards);
            }
            //纵向排列
            int extendTimes = cards.size() / 10 + 1;//需要扩展几次背景
            output = extendImage(output,background,extendTimes);
            int singleColumnDrawn = 0;
            int columns = 0;
            for (PjskCard card : cards) {
                if (singleColumnDrawn > 10) {
                    columns++;
                    singleColumnDrawn = 0;
                }
                mergeImages(output,card.getThumbnails(),45 + columns * 233,26 +  singleColumnDrawn * 233);
                singleColumnDrawn++;
            }
            generateSaveFile(output,"static/out.png");
            return output;
        }
    }

}
