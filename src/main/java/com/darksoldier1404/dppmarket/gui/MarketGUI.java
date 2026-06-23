package com.darksoldier1404.dppmarket.gui;

import com.darksoldier1404.dppc.builder.itemstack.ItemStackBuilder;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppmarket.PlayerMarket;
import com.darksoldier1404.dppmarket.obj.BrowseSession;
import com.darksoldier1404.dppmarket.obj.MarketKeys;
import com.darksoldier1404.dppmarket.obj.FilterEntry;
import com.darksoldier1404.dppmarket.obj.MarketListing;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MarketGUI {
    private MarketGUI() {
    }

    private static PlayerMarket plugin() {
        return PlayerMarket.plugin;
    }

    public static ItemStack listingItem(MarketListing l, boolean myListing) {
        ItemStack base = l.getItem() != null ? l.getItem().clone() : new ItemStack(Material.BARRIER);

        List<String> lore = new ArrayList<>(existingLore(base));
        lore.add(" ");
        lore.add(plugin().lang("lore_price", fmt(l.getPrice())));
        lore.add(plugin().lang("lore_seller", l.getSellerName() == null ? "?" : l.getSellerName()));
        lore.add(plugin().lang("lore_category", l.getCategory() == null ? "-" : l.getCategory()));
        lore.add(" ");
        lore.add(plugin().lang(myListing ? "lore_click_cancel" : "lore_click_buy"));

        ItemStack item = ItemStackBuilder.from(base).lore(lore).build();
        item = NBT.setStringTag(item, MarketKeys.LISTING, l.getId());
        item = NBT.setStringTag(item, MarketKeys.BTN, myListing ? MarketKeys.BTN_MYLISTING : "listing");
        return item;
    }

    public static ItemStack claimItem(ItemStack original, int index) {
        ItemStack base = original.clone();
        List<String> lore = new ArrayList<>(existingLore(base));
        lore.add(" ");
        lore.add(plugin().lang("lore_click_withdraw"));
        ItemStack item = ItemStackBuilder.from(base).lore(lore).build();
        item = NBT.setStringTag(item, MarketKeys.BTN, MarketKeys.BTN_CLAIM_ITEM);
        item = NBT.setIntTag(item, MarketKeys.CLAIM_IDX, index);
        return item;
    }

    private static List<String> existingLore(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore() != null) {
                return meta.getLore();
            }
        }
        return new ArrayList<>();
    }

    public static ItemStack categoryButton(BrowseSession s) {
        String catName = s.getCategory() == null ? plugin().lang("btn_category_all") : s.getCategory();
        return tagBtn(ItemStackBuilder.of(Material.HOPPER)
                .name(plugin().lang("btn_category", catName))
                .lore(plugin().lang("btn_category_lore"))
                .build(), MarketKeys.BTN_CATEGORY);
    }

    public static ItemStack sortButton(BrowseSession s) {
        String sortName = plugin().lang(s.getSort().langKey());
        return tagBtn(ItemStackBuilder.of(Material.COMPARATOR)
                .name(plugin().lang("btn_sort", sortName))
                .lore(plugin().lang("btn_sort_lore"))
                .build(), MarketKeys.BTN_SORT);
    }

    public static ItemStack searchButton() {
        return tagBtn(ItemStackBuilder.of(Material.OAK_SIGN)
                .name(plugin().lang("btn_search"))
                .lore(plugin().lang("btn_search_lore"))
                .build(), MarketKeys.BTN_SEARCH);
    }

    public static ItemStack claimButton() {
        return tagBtn(ItemStackBuilder.of(Material.CHEST)
                .name(plugin().lang("btn_claim"))
                .lore(plugin().lang("btn_claim_lore"))
                .build(), MarketKeys.BTN_CLAIM);
    }

    public static ItemStack infoItem(BrowseSession s) {
        String catName = s.getCategory() == null ? plugin().lang("btn_category_all") : s.getCategory();
        String sortName = plugin().lang(s.getSort().langKey());
        ItemStack item = ItemStackBuilder.of(Material.PAPER)
                .name(plugin().lang("btn_info"))
                .lore(plugin().lang("btn_info_lore", catName, sortName))
                .build();

        return NBT.setStringTag(item, MarketKeys.CORE_CLICK_CANCEL, "true");
    }

    public static ItemStack moneyButton(double money) {
        return tagBtn(ItemStackBuilder.of(Material.GOLD_INGOT)
                .name(plugin().lang("btn_money", fmt(money)))
                .lore(plugin().lang("btn_money_lore"))
                .build(), MarketKeys.BTN_MONEY);
    }

    public static ItemStack confirmButton() {
        return tagBtn(ItemStackBuilder.of(Material.LIME_WOOL)
                .name(plugin().lang("btn_confirm"))
                .build(), MarketKeys.BTN_CONFIRM);
    }

    public static ItemStack cancelButton() {
        return tagBtn(ItemStackBuilder.of(Material.RED_WOOL)
                .name(plugin().lang("btn_cancel"))
                .build(), MarketKeys.BTN_CANCEL);
    }

    public static ItemStack categorySelectButton(String category) {
        ItemStack item = ItemStackBuilder.of(categoryIcon(category))
                .name("&e" + category)
                .build();
        item = NBT.setStringTag(item, MarketKeys.BTN, MarketKeys.BTN_CATEGORY_SELECT);
        item = NBT.setStringTag(item, MarketKeys.CAT, category);
        return item;
    }

    public static ItemStack filterEditItem(FilterEntry e) {
        ItemStack base = e.getItem() != null ? e.getItem().clone() : new ItemStack(Material.BARRIER);
        List<String> lore = new ArrayList<>(existingLore(base));
        lore.add(" ");
        lore.add(plugin().lang("lore_filter_option", plugin().lang(e.getType().langKey())));
        lore.add(plugin().lang("lore_filter_left"));
        lore.add(plugin().lang("lore_filter_right"));
        ItemStack item = ItemStackBuilder.from(base).lore(lore).build();
        item = NBT.setStringTag(item, MarketKeys.FILTER, e.getId());
        item = NBT.setStringTag(item, MarketKeys.BTN, MarketKeys.BTN_FILTER_ITEM);
        return item;
    }

    public static ItemStack filterAddButton() {
        return tagBtn(ItemStackBuilder.of(Material.EMERALD)
                .name(plugin().lang("btn_filter_add"))
                .lore(plugin().lang("btn_filter_add_lore"))
                .build(), MarketKeys.BTN_FILTER_ADD);
    }

    public static ItemStack[] filterEditTools() {
        ItemStack[] tools = new ItemStack[9];
        for (int i = 0; i < 9; i++) tools[i] = filler();
        tools[1] = prevButton();
        tools[4] = filterAddButton();
        tools[7] = nextButton();
        return tools;
    }

    public static ItemStack prevButton() {
        ItemStack item = ItemStackBuilder.of(Material.ARROW).name("&f◀ Prev").build();
        return NBT.setStringTag(item, MarketKeys.CORE_PREV, "true");
    }

    public static ItemStack nextButton() {
        ItemStack item = ItemStackBuilder.of(Material.ARROW).name("&fNext ▶").build();
        return NBT.setStringTag(item, MarketKeys.CORE_NEXT, "true");
    }

    public static ItemStack filler() {
        ItemStack item = ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        return NBT.setStringTag(item, MarketKeys.CORE_CLICK_CANCEL, "true");
    }

    public static ItemStack[] marketTools(BrowseSession s) {
        ItemStack[] tools = new ItemStack[9];
        tools[0] = categoryButton(s);
        tools[1] = prevButton();
        tools[2] = filler();
        tools[3] = sortButton(s);
        tools[4] = infoItem(s);
        tools[5] = searchButton();
        tools[6] = filler();
        tools[7] = nextButton();
        tools[8] = claimButton();
        return tools;
    }

    public static ItemStack[] claimTools(double money) {
        ItemStack[] tools = new ItemStack[9];
        for (int i = 0; i < 9; i++) tools[i] = filler();
        tools[1] = prevButton();
        tools[4] = moneyButton(money);
        tools[7] = nextButton();
        return tools;
    }

    public static ItemStack[] simpleNavTools() {
        ItemStack[] tools = new ItemStack[9];
        for (int i = 0; i < 9; i++) tools[i] = filler();
        tools[1] = prevButton();
        tools[7] = nextButton();
        return tools;
    }

    private static ItemStack tagBtn(ItemStack item, String btnValue) {
        return NBT.setStringTag(item, MarketKeys.BTN, btnValue);
    }

    private static Material categoryIcon(String category) {
        if (category == null) return Material.BARREL;
        switch (category) {
            case "무기":
                return Material.DIAMOND_SWORD;
            case "방어구":
                return Material.DIAMOND_CHESTPLATE;
            case "도구":
                return Material.DIAMOND_PICKAXE;
            case "블록":
                return Material.GRASS_BLOCK;
            case "재료":
                return Material.IRON_INGOT;
            case "기타":
                return Material.CHEST;
            default:
                return Material.BARREL;
        }
    }

    public static String fmt(double v) {
        return new java.text.DecimalFormat("#,###.##").format(v);
    }
}
