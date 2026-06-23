package com.darksoldier1404.dppmarket.functions;

import com.darksoldier1404.dppc.api.essentials.MoneyAPI;
import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.utils.InventoryUtils;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppmarket.PlayerMarket;
import com.darksoldier1404.dppmarket.gui.MarketGUI;
import com.darksoldier1404.dppmarket.obj.BrowseSession;
import com.darksoldier1404.dppmarket.obj.ClaimBox;
import com.darksoldier1404.dppmarket.obj.FilterEntry;
import com.darksoldier1404.dppmarket.obj.FilterMatchType;
import com.darksoldier1404.dppmarket.obj.MarketKeys;
import com.darksoldier1404.dppmarket.obj.MarketListing;
import com.darksoldier1404.dppmarket.obj.SellContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MarketFunction {
    private MarketFunction() {
    }

    private static final String SLOT_PERM_PREFIX = "dpmarket.slot.";

    private static final Map<UUID, BrowseSession> sessions = new HashMap<>();

    private static final Set<UUID> awaitingSearch = new HashSet<>();

    private static PlayerMarket plugin() {
        return PlayerMarket.plugin;
    }

    public static BrowseSession getSession(Player p) {
        return sessions.computeIfAbsent(p.getUniqueId(), k -> new BrowseSession());
    }

    public static void openMarket(Player p, BrowseSession session) {
        sessions.put(p.getUniqueId(), session);
        List<MarketListing> list = filteredSorted(session);

        DInventory inv = new DInventory(plugin().lang("gui_market_title"), 54, true, false, plugin());
        inv.setChannel(MarketKeys.CH_MARKET);
        inv.setObj(session);

        List<ItemStack> display = new ArrayList<>();
        for (MarketListing l : list) {
            display.add(MarketGUI.listingItem(l, false));
        }
        inv.addPageItems(display);
        inv.setPageTools(MarketGUI.marketTools(session));
        inv.update();
        inv.openInventory(p);
    }

    public static void reopenMarket(Player p) {
        openMarket(p, getSession(p));
    }

    private static List<MarketListing> filteredSorted(BrowseSession s) {
        List<MarketListing> result = new ArrayList<>();
        for (MarketListing l : plugin().listings.values()) {
            if (l == null || l.getItem() == null) continue;
            if (s.getCategory() != null && !s.getCategory().equals(l.getCategory())) continue;
            if (s.getKeyword() != null && !matches(l, s.getKeyword())) continue;
            result.add(l);
        }
        switch (s.getSort()) {
            case PRICE:
                result.sort(Comparator.comparingDouble(MarketListing::getPrice));
                break;
            case SALES:
                result.sort(Comparator.comparingInt(
                        (MarketListing l) -> plugin().getSalesCount(l.getItem().getType())).reversed());
                break;
            case RECENT:
            default:
                result.sort(Comparator.comparingLong(MarketListing::getListedAt).reversed());
                break;
        }
        return result;
    }

    private static boolean matches(MarketListing l, String keyword) {
        String kw = keyword.toLowerCase();
        ItemStack it = l.getItem();
        if (it != null) {
            if (it.getType().name().toLowerCase().contains(kw)) return true;
            if (it.hasItemMeta() && it.getItemMeta() != null && it.getItemMeta().hasDisplayName()) {
                String dn = ChatColor.stripColor(it.getItemMeta().getDisplayName()).toLowerCase();
                if (dn.contains(kw)) return true;
            }
        }
        return l.getCategory() != null && l.getCategory().toLowerCase().contains(kw);
    }

    public static void search(Player p, String keyword) {
        BrowseSession s = getSession(p);
        s.setKeyword(keyword);
        List<MarketListing> list = filteredSorted(s);
        if (list.isEmpty()) {
            plugin().send(p, "search_no_result", keyword);
        } else {
            plugin().send(p, "search_result", keyword);
        }
        openMarket(p, s);
    }

    public static void startSearchInput(Player p) {
        awaitingSearch.add(p.getUniqueId());
        p.closeInventory();
        plugin().send(p, "search_prompt");
    }

    public static boolean isAwaitingSearch(Player p) {
        return awaitingSearch.contains(p.getUniqueId());
    }

    public static void handleSearchInput(Player p, String message) {
        awaitingSearch.remove(p.getUniqueId());
        if (message.equalsIgnoreCase("cancel")) {
            plugin().send(p, "search_cancelled");
            return;
        }
        Bukkit.getScheduler().runTask(plugin(), () -> search(p, message));
    }

    public static void sell(Player p, double price) {
        if (!MoneyAPI.isEnabled()) {
            plugin().send(p, "money_disabled");
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            plugin().send(p, "sell_no_item");
            return;
        }
        if (isBlocked(hand)) {
            plugin().send(p, "filter_blocked");
            return;
        }
        double min = plugin().config.getDouble("Market.min-price", 1);
        double max = plugin().config.getDouble("Market.max-price", 0);
        if (price < min) {
            plugin().send(p, "sell_price_too_low", MarketGUI.fmt(min));
            return;
        }
        if (max > 0 && price > max) {
            plugin().send(p, "sell_price_too_high", MarketGUI.fmt(max));
            return;
        }
        int used = getUsedSlots(p.getUniqueId());
        int maxSlots = getMaxSlots(p);
        if (used >= maxSlots) {
            plugin().send(p, "sell_no_slot", String.valueOf(used), slotLabel(maxSlots));
            return;
        }
        openCategorySelect(p, new SellContext(hand.clone(), price));
    }

    public static void openCategorySelect(Player p, SellContext ctx) {
        List<String> categories = plugin().config.getStringList("Market.categories");
        int size = 27;
        int needed = 9 + categories.size();
        while (size < needed && size < 54) size += 9;

        DInventory inv = new DInventory(plugin().lang("gui_category_title"), size, plugin());
        inv.setChannel(MarketKeys.CH_CATEGORY);
        inv.setObj(ctx);

        inv.setItem(4, feePreviewItem(ctx.getPrice()));
        int slot = 9;
        for (String cat : categories) {
            if (slot >= size) break;
            inv.setItem(slot++, MarketGUI.categorySelectButton(cat));
        }
        inv.openInventory(p);
        plugin().send(p, "sell_select_category");
    }

    private static ItemStack feePreviewItem(double price) {
        int tax = plugin().config.getInt("Market.tax", 5);
        double payout = payout(price);
        ItemStack item = com.darksoldier1404.dppc.builder.itemstack.ItemStackBuilder.of(org.bukkit.Material.GOLD_NUGGET)
                .name(plugin().lang("sell_fee_preview", MarketGUI.fmt(price), String.valueOf(tax), MarketGUI.fmt(payout)))
                .build();
        return NBT.setStringTag(item, MarketKeys.CORE_CLICK_CANCEL, "true");
    }

    public static void registerListing(Player p, SellContext ctx, String category) {

        int used = getUsedSlots(p.getUniqueId());
        int maxSlots = getMaxSlots(p);
        if (used >= maxSlots) {
            plugin().send(p, "sell_no_slot", String.valueOf(used), slotLabel(maxSlots));
            p.closeInventory();
            return;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        ItemStack snapshot = ctx.getItem();
        if (hand == null || hand.getType().isAir() || !hand.isSimilar(snapshot) || hand.getAmount() < snapshot.getAmount()) {
            plugin().send(p, "sell_no_item");
            p.closeInventory();
            return;
        }
        if (isBlocked(snapshot)) {
            plugin().send(p, "filter_blocked");
            p.closeInventory();
            return;
        }
        hand.setAmount(hand.getAmount() - snapshot.getAmount());
        p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);

        String id = UUID.randomUUID().toString();
        MarketListing listing = new MarketListing(id, p.getUniqueId(), p.getName(),
                snapshot.clone(), ctx.getPrice(), category, System.currentTimeMillis());
        plugin().listings.put(id, listing);
        plugin().listings.save(id);

        p.closeInventory();
        plugin().send(p, "sell_success", itemName(snapshot), MarketGUI.fmt(ctx.getPrice()), MarketGUI.fmt(payout(ctx.getPrice())));
    }

    public static void openBuyConfirm(Player p, String listingId) {
        MarketListing l = plugin().listings.get(listingId);
        if (l == null) {
            plugin().send(p, "buy_listing_gone");
            reopenMarket(p);
            return;
        }
        DInventory inv = new DInventory(plugin().lang("gui_buy_title"), 27, plugin());
        inv.setChannel(MarketKeys.CH_BUY);
        inv.setObj(listingId);
        inv.setItem(13, MarketGUI.listingItem(l, false));
        inv.setItem(11, MarketGUI.confirmButton());
        inv.setItem(15, MarketGUI.cancelButton());
        inv.openInventory(p);
    }

    public static void buy(Player p, String listingId) {
        MarketListing l = plugin().listings.get(listingId);

        if (l == null) {
            plugin().send(p, "buy_listing_gone");
            reopenMarket(p);
            return;
        }
        if (p.getUniqueId().equals(l.getSeller())) {
            plugin().send(p, "buy_own_listing");
            return;
        }
        if (!MoneyAPI.isEnabled()) {
            plugin().send(p, "money_disabled");
            return;
        }
        double price = l.getPrice();
        if (!MoneyAPI.hasEnoughMoney(p, price)) {
            plugin().send(p, "buy_not_enough_money", MarketGUI.fmt(price));
            return;
        }

        MoneyAPI.takeMoney(p, price);

        double payout = payout(price);
        ClaimBox sellerBox = getOrCreateClaim(l.getSeller());
        sellerBox.addMoney(payout);
        plugin().claims.save(l.getSeller());

        ItemStack item = l.getItem().clone();
        plugin().addSalesCount(item.getType(), item.getAmount());

        plugin().listings.delete(listingId);
        plugin().listings.remove(listingId);

        addToClaim(p.getUniqueId(), item);

        plugin().send(p, "buy_success", itemName(item), MarketGUI.fmt(price));
        plugin().send(p, "buy_to_claim");
        reopenMarket(p);
    }

    public static void openClaim(Player p) {
        ClaimBox box = plugin().claims.get(p.getUniqueId());
        double money = box == null ? 0 : box.getMoney();
        List<ItemStack> items = box == null ? new ArrayList<>() : box.getItems();

        DInventory inv = new DInventory(plugin().lang("gui_claim_title"), 54, true, false, plugin());
        inv.setChannel(MarketKeys.CH_CLAIM);

        List<ItemStack> display = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            display.add(MarketGUI.claimItem(items.get(i), i));
        }
        inv.addPageItems(display);
        inv.setPageTools(MarketGUI.claimTools(money));
        inv.update();
        inv.openInventory(p);
    }

    public static void claimMoney(Player p) {
        ClaimBox box = plugin().claims.get(p.getUniqueId());
        if (box == null || box.getMoney() <= 0) {
            plugin().send(p, "claim_no_money");
            return;
        }
        if (!MoneyAPI.isEnabled()) {
            plugin().send(p, "money_disabled");
            return;
        }
        double amount = box.getMoney();
        MoneyAPI.addMoney(p, amount);
        box.setMoney(0);
        if (box.isEmpty()) {
            plugin().claims.delete(p.getUniqueId());
            plugin().claims.remove(p.getUniqueId());
        } else {
            plugin().claims.save(p.getUniqueId());
        }
        plugin().send(p, "claim_money_done", MarketGUI.fmt(amount));
        openClaim(p);
    }

    public static void withdrawClaimItem(Player p, int index) {
        ClaimBox box = plugin().claims.get(p.getUniqueId());
        if (box == null || index < 0 || index >= box.getItems().size()) {
            openClaim(p);
            return;
        }
        ItemStack item = box.getItems().get(index);

        if (!InventoryUtils.hasEnoughSpace(p.getInventory().getStorageContents(), item)) {
            plugin().send(p, "claim_inventory_full");
            openClaim(p);
            return;
        }
        p.getInventory().addItem(item.clone());
        box.getItems().remove(index);
        plugin().send(p, "claim_item_done");
        if (box.isEmpty()) {
            plugin().claims.delete(p.getUniqueId());
            plugin().claims.remove(p.getUniqueId());
        } else {
            plugin().claims.save(p.getUniqueId());
        }
        openClaim(p);
    }

    public static void openMyListings(Player p) {
        DInventory inv = new DInventory(plugin().lang("gui_mylist_title"), 54, true, false, plugin());
        inv.setChannel(MarketKeys.CH_MYLIST);

        List<ItemStack> display = new ArrayList<>();
        for (MarketListing l : plugin().listings.values()) {
            if (l != null && p.getUniqueId().equals(l.getSeller())) {
                display.add(MarketGUI.listingItem(l, true));
            }
        }
        inv.addPageItems(display);
        inv.setPageTools(MarketGUI.simpleNavTools());
        inv.update();
        inv.openInventory(p);
    }

    public static void cancelListing(Player p, String listingId) {
        MarketListing l = plugin().listings.get(listingId);
        if (l == null || !p.getUniqueId().equals(l.getSeller())) {
            openMyListings(p);
            return;
        }
        plugin().listings.delete(listingId);
        plugin().listings.remove(listingId);

        ItemStack item = l.getItem().clone();
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack rest : leftover.values()) {
                addToClaim(p.getUniqueId(), rest);
            }
            plugin().send(p, "remove_to_claim");
        } else {
            plugin().send(p, "remove_success");
        }
        openMyListings(p);
    }

    public static void startExpiryScheduler() {
        long minutes = plugin().config.getLong("Market.expire-check-period-minutes", 60);
        if (minutes <= 0) minutes = 60;
        long ticks = minutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin(), MarketFunction::checkExpiry, ticks, ticks);
    }

    public static void checkExpiry() {
        int days = plugin().config.getInt("Market.listing-expire-days", 7);
        if (days <= 0) return;
        long cutoff = System.currentTimeMillis() - days * 86400000L;

        List<String> expiredIds = new ArrayList<>();
        Map<UUID, Integer> perSeller = new HashMap<>();
        for (MarketListing l : new ArrayList<>(plugin().listings.values())) {
            if (l == null || l.getItem() == null) continue;
            if (l.getListedAt() < cutoff) {
                addToClaim(l.getSeller(), l.getItem().clone());
                expiredIds.add(l.getId());
                perSeller.merge(l.getSeller(), 1, Integer::sum);
            }
        }
        for (String id : expiredIds) {
            plugin().listings.delete(id);
            plugin().listings.remove(id);
        }
        for (Map.Entry<UUID, Integer> e : perSeller.entrySet()) {
            Player online = Bukkit.getPlayer(e.getKey());
            if (online != null && online.isOnline()) {
                plugin().send(online, "expire_moved", String.valueOf(e.getValue()));
            }
        }
    }

    public static void openFilterEdit(Player p) {
        DInventory inv = new DInventory(plugin().lang("gui_filter_title"), 54, true, false, plugin());
        inv.setChannel(MarketKeys.CH_FILTER_EDIT);

        List<ItemStack> display = new ArrayList<>();
        for (FilterEntry e : plugin().filters.values()) {
            if (e != null && e.getItem() != null) {
                display.add(MarketGUI.filterEditItem(e));
            }
        }
        inv.addPageItems(display);
        inv.setPageTools(MarketGUI.filterEditTools());
        inv.update();
        inv.openInventory(p);
    }

    public static void openFilterAdd(Player p) {
        DInventory inv = new DInventory(plugin().lang("gui_filter_add_title"), 54, plugin());
        inv.setChannel(MarketKeys.CH_FILTER_ADD);
        inv.openInventory(p);
        plugin().send(p, "filter_add_open");
    }

    public static void captureFilterAdd(Player p, DInventory inv) {
        int count = 0;
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType().isAir()) continue;

            String id = UUID.randomUUID().toString();
            FilterEntry entry = new FilterEntry(id, it.clone(), FilterMatchType.ALL);
            plugin().filters.put(id, entry);
            plugin().filters.save(id);
            count++;

            Map<Integer, ItemStack> leftover = p.getInventory().addItem(it);
            for (ItemStack rest : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
            }
        }
        if (count > 0) {
            plugin().send(p, "filter_add_done", String.valueOf(count));

            Bukkit.getScheduler().runTask(plugin(), () -> openFilterEdit(p));
        }
    }

    public static void cycleFilterOption(Player p, String id) {
        FilterEntry e = plugin().filters.get(id);
        if (e == null) {
            openFilterEdit(p);
            return;
        }
        e.setType(e.getType().next());
        plugin().filters.save(id);
        plugin().send(p, "filter_option_changed", plugin().lang(e.getType().langKey()));
        openFilterEdit(p);
    }

    public static void removeFilter(Player p, String id) {
        if (plugin().filters.containsKey(id)) {
            plugin().filters.delete(id);
            plugin().filters.remove(id);
            plugin().send(p, "filter_removed");
        }
        openFilterEdit(p);
    }

    public static boolean isBlocked(ItemStack item) {
        if (item == null) return false;
        for (FilterEntry e : plugin().filters.values()) {
            if (e != null && e.matches(item)) return true;
        }
        return false;
    }

    public static int getMaxSlots(Player p) {
        if (p.hasPermission("dpmarket.slot.unlimited")) {
            return Integer.MAX_VALUE;
        }
        int max = -1;
        for (PermissionAttachmentInfo pai : p.getEffectivePermissions()) {
            if (!pai.getValue()) continue;
            String perm = pai.getPermission().toLowerCase();
            if (perm.startsWith(SLOT_PERM_PREFIX)) {
                String num = perm.substring(SLOT_PERM_PREFIX.length());
                if (num.equals("unlimited")) return Integer.MAX_VALUE;
                try {
                    int n = Integer.parseInt(num);
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (max < 0) {
            max = plugin().config.getInt("Market.default-slots", 5);
        }
        return max;
    }

    public static int getUsedSlots(UUID uuid) {
        int count = 0;
        for (MarketListing l : plugin().listings.values()) {
            if (l != null && uuid.equals(l.getSeller())) count++;
        }
        return count;
    }

    private static String slotLabel(int maxSlots) {
        return maxSlots == Integer.MAX_VALUE ? "∞" : String.valueOf(maxSlots);
    }

    private static ClaimBox getOrCreateClaim(UUID uuid) {
        ClaimBox box = plugin().claims.get(uuid);
        if (box == null) {
            box = plugin().claims.create(uuid, ClaimBox.class);
        }
        return box;
    }

    public static void addToClaim(UUID uuid, ItemStack item) {
        ClaimBox box = getOrCreateClaim(uuid);
        box.addItem(item);
        plugin().claims.save(uuid);
    }

    private static double payout(double price) {
        int tax = plugin().config.getInt("Market.tax", 5);
        double net = price * (100.0 - tax) / 100.0;
        return net < 0 ? 0 : net;
    }

    private static String itemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name() + " x" + item.getAmount();
    }
}
