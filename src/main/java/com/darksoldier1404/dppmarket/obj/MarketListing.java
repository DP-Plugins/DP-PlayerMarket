package com.darksoldier1404.dppmarket.obj;

import com.darksoldier1404.dppc.data.DataCargo;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarketListing implements DataCargo {
    private String id;
    private UUID seller;
    private String sellerName;
    private ItemStack item;
    private double price;
    private String category;
    private long listedAt;

    public MarketListing() {
    }

    public MarketListing(String id, UUID seller, String sellerName, ItemStack item, double price, String category, long listedAt) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.category = category;
        this.listedAt = listedAt;
    }

    @Override
    public YamlConfiguration serialize() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("id", id);
        data.set("seller", seller != null ? seller.toString() : null);
        data.set("sellerName", sellerName);
        data.set("item", item);
        data.set("price", price);
        data.set("category", category);
        data.set("listedAt", listedAt);
        return data;
    }

    @Override
    public MarketListing deserialize(YamlConfiguration data) {
        this.id = data.getString("id");
        String s = data.getString("seller");
        this.seller = (s != null && !s.isEmpty()) ? UUID.fromString(s) : null;
        this.sellerName = data.getString("sellerName", "Unknown");
        this.item = data.getItemStack("item");
        this.price = data.getDouble("price");
        this.category = data.getString("category", "");
        this.listedAt = data.getLong("listedAt");
        return this;
    }

    public String getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public long getListedAt() {
        return listedAt;
    }
}
