package com.darksoldier1404.dppmarket.obj;

import com.darksoldier1404.dppc.data.DataCargo;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class FilterEntry implements DataCargo {
    private String id;
    private ItemStack item;
    private FilterMatchType type = FilterMatchType.ALL;

    public FilterEntry() {
    }

    public FilterEntry(String id, ItemStack item, FilterMatchType type) {
        this.id = id;
        this.item = item;
        this.type = type;
    }

    @Override
    public YamlConfiguration serialize() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("id", id);
        data.set("item", item);
        data.set("type", type != null ? type.name() : FilterMatchType.ALL.name());
        return data;
    }

    @Override
    public FilterEntry deserialize(YamlConfiguration data) {
        this.id = data.getString("id");
        this.item = data.getItemStack("item");
        String t = data.getString("type", FilterMatchType.ALL.name());
        try {
            this.type = FilterMatchType.valueOf(t);
        } catch (IllegalArgumentException e) {
            this.type = FilterMatchType.ALL;
        }
        return this;
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
        return item;
    }

    public FilterMatchType getType() {
        return type;
    }

    public void setType(FilterMatchType type) {
        this.type = type;
    }

    public boolean matches(ItemStack target) {
        return type != null && type.matches(item, target);
    }
}
