package com.darksoldier1404.dppmarket.obj;

import org.bukkit.inventory.ItemStack;

public class SellContext {
    private final ItemStack item;
    private final double price;

    public SellContext(ItemStack item, double price) {
        this.item = item;
        this.price = price;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }
}
