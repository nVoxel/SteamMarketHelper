package com.voxeldev.steammarkethelper.models.listings;

public class ListingModel {
    public String id;
    public String iconUrl;
    public String name;
    public String marketUrl;
    public String additional;
    public String price;

    public ListingModel(String id, String iconUrl, String name, String marketUrl, String additional, String price) {
        this.id = id;
        this.iconUrl = iconUrl;
        this.name = name;
        this.marketUrl = marketUrl;
        this.additional = additional;
        this.price = price;
    }
}
