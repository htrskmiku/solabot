package com.arth.bot.plugin.custom.pjsk.utils.pair;


import com.arth.bot.plugin.custom.pjsk.objects.enums.CardAttributes;
import com.arth.bot.plugin.custom.pjsk.objects.enums.CardRarities;

/**
 * @param assetsBundle  AssetsBundle
 * @param cardAttribute CardAttribute
 * @param rarities CardRarities
 */
public record PjskCardInfo(String assetsBundle, CardAttributes cardAttribute, CardRarities rarities) {

    public String getAssetsBundle(String left) {
        return assetsBundle;
    }

    public CardAttributes getType() {
        return cardAttribute;
    }

    public CardRarities getRarities() {return rarities;}
}

