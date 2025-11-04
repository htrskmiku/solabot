package com.arth.solabot.plugin.custom.pjsk.objects;


import com.arth.solabot.plugin.custom.pjsk.objects.enums.CardAttributes;
import com.arth.solabot.plugin.custom.pjsk.objects.enums.CardCharacters;
import com.arth.solabot.plugin.custom.pjsk.objects.enums.CardRarities;

/**
 * @param assetsBundle  AssetsBundle
 * @param cardAttribute CardAttribute
 * @param rarities      CardRarities
 */
public record PjskCardInfo(String assetsBundle, CardAttributes cardAttribute, CardRarities rarities,
                           CardCharacters characters) {

    public String getAssetsBundle(String left) {
        return assetsBundle;
    }

    public CardAttributes getType() {
        return cardAttribute;
    }

    public CardRarities getRarities() {
        return rarities;
    }

    public CardCharacters getCharacters() {
        return characters;
    }
}

