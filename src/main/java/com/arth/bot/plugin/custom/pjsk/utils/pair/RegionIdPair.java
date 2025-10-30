package com.arth.bot.plugin.custom.pjsk.utils.pair;

import lombok.Data;

@Data
public class RegionIdPair {
    private String left = "";//左值，Region
    private String right = "";//右值，Id
    public RegionIdPair(String left, String right) {
        this.left = left;
        this.right = right;
    }
    public RegionIdPair(){}

    public String left() {
        return left;
    }
    public String right() {
        return right;
    }
    public String getRegion(String left) {
        return left;
    }
    public String getId() {
        return right;
    }
}
