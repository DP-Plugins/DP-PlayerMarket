package com.darksoldier1404.dppmarket.listeners;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.events.dinventory.DInventoryClickEvent;
import com.darksoldier1404.dppc.events.dinventory.DInventoryCloseEvent;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppmarket.PlayerMarket;
import com.darksoldier1404.dppmarket.functions.MarketFunction;
import com.darksoldier1404.dppmarket.obj.BrowseSession;
import com.darksoldier1404.dppmarket.obj.MarketKeys;
import com.darksoldier1404.dppmarket.obj.SellContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MarketListener implements Listener {

    private PlayerMarket plugin() {
        return PlayerMarket.plugin;
    }

    @EventHandler
    public void onClick(DInventoryClickEvent e) {
        DInventory inv = e.getDInventory();
        if (inv == null || !inv.isValidHandler(plugin())) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (inv.isValidChannel(MarketKeys.CH_MARKET)) {
            e.setCancelled(true);
            handleMarketClick(p, inv, e.getCurrentItem());
        } else if (inv.isValidChannel(MarketKeys.CH_CATEGORY)) {
            e.setCancelled(true);
            handleCategoryClick(p, inv, e.getCurrentItem());
        } else if (inv.isValidChannel(MarketKeys.CH_BUY)) {
            e.setCancelled(true);
            handleBuyClick(p, inv, e.getCurrentItem());
        } else if (inv.isValidChannel(MarketKeys.CH_CLAIM)) {
            e.setCancelled(true);
            handleClaimClick(p, e.getCurrentItem());
        } else if (inv.isValidChannel(MarketKeys.CH_MYLIST)) {
            e.setCancelled(true);
            handleMyListClick(p, e.getCurrentItem());
        } else if (inv.isValidChannel(MarketKeys.CH_FILTER_EDIT)) {
            e.setCancelled(true);
            handleFilterEditClick(p, e.getCurrentItem(), e.isRightClick());
        }

    }

    private void handleMarketClick(Player p, DInventory inv, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (!NBT.hasTagKey(item, MarketKeys.BTN)) {

            if (NBT.hasTagKey(item, MarketKeys.LISTING)) {
                MarketFunction.openBuyConfirm(p, NBT.getStringTag(item, MarketKeys.LISTING));
            }
            return;
        }
        String btn = NBT.getStringTag(item, MarketKeys.BTN);
        BrowseSession s = inv.getObj() instanceof BrowseSession ? (BrowseSession) inv.getObj() : MarketFunction.getSession(p);
        switch (btn) {
            case MarketKeys.BTN_CATEGORY:
                cycleCategory(s);
                MarketFunction.openMarket(p, s);
                break;
            case MarketKeys.BTN_SORT:
                s.setSort(s.getSort().next());
                MarketFunction.openMarket(p, s);
                break;
            case MarketKeys.BTN_SEARCH:
                MarketFunction.startSearchInput(p);
                break;
            case MarketKeys.BTN_CLAIM:
                MarketFunction.openClaim(p);
                break;
            default:

                if (NBT.hasTagKey(item, MarketKeys.LISTING)) {
                    MarketFunction.openBuyConfirm(p, NBT.getStringTag(item, MarketKeys.LISTING));
                }
                break;
        }
    }

    private void cycleCategory(BrowseSession s) {
        List<String> cats = plugin().config.getStringList("Market.categories");
        String cur = s.getCategory();
        if (cur == null) {
            s.setCategory(cats.isEmpty() ? null : cats.get(0));
            return;
        }
        int idx = cats.indexOf(cur);
        if (idx < 0 || idx + 1 >= cats.size()) {
            s.setCategory(null);
        } else {
            s.setCategory(cats.get(idx + 1));
        }
    }

    private void handleCategoryClick(Player p, DInventory inv, ItemStack item) {
        if (item == null || !NBT.hasTagKey(item, MarketKeys.BTN)) return;
        if (!MarketKeys.BTN_CATEGORY_SELECT.equals(NBT.getStringTag(item, MarketKeys.BTN))) return;
        if (!(inv.getObj() instanceof SellContext)) return;
        SellContext ctx = (SellContext) inv.getObj();
        String category = NBT.getStringTag(item, MarketKeys.CAT);
        MarketFunction.registerListing(p, ctx, category);
    }

    private void handleBuyClick(Player p, DInventory inv, ItemStack item) {
        if (item == null || !NBT.hasTagKey(item, MarketKeys.BTN)) return;
        String btn = NBT.getStringTag(item, MarketKeys.BTN);
        if (MarketKeys.BTN_CONFIRM.equals(btn)) {
            Object obj = inv.getObj();
            if (obj instanceof String) {
                MarketFunction.buy(p, (String) obj);
            }
        } else if (MarketKeys.BTN_CANCEL.equals(btn)) {
            MarketFunction.reopenMarket(p);
        }
    }

    private void handleClaimClick(Player p, ItemStack item) {
        if (item == null || !NBT.hasTagKey(item, MarketKeys.BTN)) return;
        String btn = NBT.getStringTag(item, MarketKeys.BTN);
        if (MarketKeys.BTN_MONEY.equals(btn)) {
            MarketFunction.claimMoney(p);
        } else if (MarketKeys.BTN_CLAIM_ITEM.equals(btn)) {
            int index = NBT.getIntegerTag(item, MarketKeys.CLAIM_IDX);
            MarketFunction.withdrawClaimItem(p, index);
        }
    }

    private void handleMyListClick(Player p, ItemStack item) {
        if (item == null) return;
        if (NBT.hasTagKey(item, MarketKeys.LISTING)
                && MarketKeys.BTN_MYLISTING.equals(NBT.getStringTag(item, MarketKeys.BTN))) {
            MarketFunction.cancelListing(p, NBT.getStringTag(item, MarketKeys.LISTING));
        }
    }

    private void handleFilterEditClick(Player p, ItemStack item, boolean rightClick) {
        if (item == null || !NBT.hasTagKey(item, MarketKeys.BTN)) return;
        String btn = NBT.getStringTag(item, MarketKeys.BTN);
        if (MarketKeys.BTN_FILTER_ADD.equals(btn)) {
            MarketFunction.openFilterAdd(p);
        } else if (MarketKeys.BTN_FILTER_ITEM.equals(btn) && NBT.hasTagKey(item, MarketKeys.FILTER)) {
            String id = NBT.getStringTag(item, MarketKeys.FILTER);
            if (rightClick) {
                MarketFunction.removeFilter(p, id);
            } else {
                MarketFunction.cycleFilterOption(p, id);
            }
        }
    }

    @EventHandler
    public void onClose(DInventoryCloseEvent e) {
        DInventory inv = e.getDInventory();
        if (inv == null || !inv.isValidHandler(plugin())) return;
        if (inv.isValidChannel(MarketKeys.CH_FILTER_ADD) && e.getPlayer() instanceof Player) {
            MarketFunction.captureFilterAdd((Player) e.getPlayer(), inv);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!MarketFunction.isAwaitingSearch(p)) return;
        e.setCancelled(true);
        MarketFunction.handleSearchInput(p, e.getMessage());
    }
}
