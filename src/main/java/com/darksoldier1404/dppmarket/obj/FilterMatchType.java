package com.darksoldier1404.dppmarket.obj;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public enum FilterMatchType {
    ALL,
    NAME,
    LORE,
    TYPE;

    public FilterMatchType next() {
        FilterMatchType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public String langKey() {
        switch (this) {
            case NAME:
                return "filter_opt_name";
            case LORE:
                return "filter_opt_lore";
            case TYPE:
                return "filter_opt_type";
            case ALL:
            default:
                return "filter_opt_all";
        }
    }

    public boolean matches(ItemStack filter, ItemStack target) {
        if (filter == null || target == null) return false;
        switch (this) {
            case TYPE:
                return filter.getType() == target.getType();
            case NAME: {
                String fn = displayName(filter);
                return fn != null && fn.equals(displayName(target));
            }
            case LORE: {
                List<String> fl = lore(filter);
                return fl != null && !fl.isEmpty() && fl.equals(lore(target));
            }
            case ALL:
            default:
                return filter.isSimilar(target);
        }
    }

    private static String displayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        return null;
    }

    private static List<String> lore(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore()) {
                return meta.getLore();
            }
        }
        return null;
    }
}
