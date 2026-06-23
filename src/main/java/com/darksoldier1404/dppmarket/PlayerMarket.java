package com.darksoldier1404.dppmarket;

import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.data.DataContainer;
import com.darksoldier1404.dppc.data.DataType;
import com.darksoldier1404.dppc.utils.PluginUtil;
import com.darksoldier1404.dppmarket.commands.MarketCommand;
import com.darksoldier1404.dppmarket.functions.MarketFunction;
import com.darksoldier1404.dppmarket.listeners.MarketListener;
import com.darksoldier1404.dppmarket.obj.ClaimBox;
import com.darksoldier1404.dppmarket.obj.FilterEntry;
import com.darksoldier1404.dppmarket.obj.MarketListing;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class PlayerMarket extends DPlugin {
    public static PlayerMarket plugin;
    private static final int BSTATS_ID = 32172;

    public DataContainer<String, MarketListing> listings;

    public DataContainer<UUID, ClaimBox> claims;

    public DataContainer<String, FilterEntry> filters;

    private YamlConfiguration statsConfig;
    private File statsFile;

    public PlayerMarket() {
        super(true);
        plugin = this;
        init();
    }

    @Override
    public void onLoad() {

        listings = loadDataContainer(new DataContainer<>(this, DataType.CUSTOM, "listings"), MarketListing.class);
        claims = loadDataContainer(new DataContainer<>(this, DataType.CUSTOM, "claims"), ClaimBox.class);
        filters = loadDataContainer(new DataContainer<>(this, DataType.CUSTOM, "filters"), FilterEntry.class);

        statsFile = new File(getDataFolder(), "stats.yml");
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        PluginUtil.addPlugin(plugin, BSTATS_ID);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new MarketListener(), this);
        MarketCommand.register();
        MarketFunction.startExpiryScheduler();

        MarketFunction.checkExpiry();
    }

    @Override
    public void onDisable() {
        saveStats();
        saveAllData();
    }

    public int getSalesCount(Material material) {
        if (material == null) return 0;
        return statsConfig.getInt(material.name(), 0);
    }

    public void addSalesCount(Material material, int amount) {
        if (material == null) return;
        statsConfig.set(material.name(), getSalesCount(material) + amount);
    }

    public void saveStats() {
        if (statsConfig == null || statsFile == null) return;
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            log.warning("Failed to save stats.yml: " + e.getMessage(), true);
        }
    }

    public void send(CommandSender sender, String key, String... args) {
        String msg = (args == null || args.length == 0) ? getLang().get(key) : getLang().getWithArgs(key, args);
        sender.sendMessage(getPrefix() + msg);
    }

    public String lang(String key, String... args) {
        return (args == null || args.length == 0) ? getLang().get(key) : getLang().getWithArgs(key, args);
    }
}
