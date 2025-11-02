package com.arth.bot.plugin.custom.pjsk.func;

import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.common.exception.ResourceNotFoundException;
import com.arth.bot.core.utils.FileUtils;
import com.arth.bot.plugin.custom.pjsk.Pjsk;
import com.arth.bot.plugin.custom.pjsk.objects.PjskCard;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class AssetsBundleResources {

    private static final String thumbnailsDir = "/thumbnails/{assetsbundle_name}_{status}.png";


    /**
     * 用于缓存或获取卡缩略图
     * @param card
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static BufferedImage getOrCacheThumbnailByCard(Pjsk.BeanContext ctx,PjskCard card)
    {
        try {
            if (ctx.cache_cards_thumbnails()) {
                FileUtils.createFolders(Path.of(ctx.cardsDataPath() + "/thumbnails"));
                Path path = Path.of(ctx.cardsDataPath() + thumbnailsDir
                        .replace("{assetsbundle_name}", card.getAssetsbundleName())
                        .replace("{status}", card.getSpecialTrainingStatus()));
                if (path.toFile().exists()) {
                    return ImageIO.read(path.toFile());
                } else {
                    BufferedImage bufferedImage = getThumbnailOnline(ctx,card.getAssetsbundleName(), card.getSpecialTrainingStatus());
                    ImageIO.write(bufferedImage, "png", path.toFile());
                    return bufferedImage;
                }
            }
            return getThumbnailOnline(ctx,card.getAssetsbundleName(), card.getSpecialTrainingStatus());
        }catch (FileNotFoundException e){
            throw new ResourceNotFoundException();
        }catch (IOException e){
            throw new InternalServerErrorException();
        }
    }



    private static BufferedImage getThumbnailOnline(Pjsk.BeanContext ctx, String assetsbundleName, String specialTrainingStatus){
        return ctx.imgService().getBufferedImg(ctx.thumbnailApi()
                .replace("{assetbundle_name}",assetsbundleName)
                .replace("{status}",specialTrainingStatus));
    }
}
