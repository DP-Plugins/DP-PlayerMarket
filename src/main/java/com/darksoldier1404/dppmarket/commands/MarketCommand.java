package com.darksoldier1404.dppmarket.commands;

import com.darksoldier1404.dppc.builder.command.ArgumentIndex;
import com.darksoldier1404.dppc.builder.command.ArgumentType;
import com.darksoldier1404.dppc.builder.command.CommandBuilder;
import com.darksoldier1404.dppmarket.PlayerMarket;
import com.darksoldier1404.dppmarket.functions.MarketFunction;
import com.darksoldier1404.dppmarket.obj.BrowseSession;
import org.bukkit.entity.Player;

public final class MarketCommand {
    private MarketCommand() {
    }

    public static void register() {
        PlayerMarket plugin = PlayerMarket.plugin;
        CommandBuilder builder = new CommandBuilder(plugin);

        builder.setDefaultAction((sender, args) -> {
            if (!(sender instanceof Player)) {
                plugin.send(sender, "player_only");
                return;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("dpmarket.use")) {
                plugin.send(p, "no_permission");
                return;
            }

            BrowseSession s = MarketFunction.getSession(p);
            s.setKeyword(null);
            MarketFunction.openMarket(p, s);
        });

        builder.beginSubCommand("sell", "/market sell <price>")
                .withPermission("dpmarket.use")
                .playerOnly()
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.DOUBLE)
                .executesPlayer((p, args) -> {
                    Double price = args.getDouble(ArgumentIndex.ARG_0);
                    if (price == null) {
                        plugin.send(p, "sell_price_invalid");
                        return true;
                    }
                    MarketFunction.sell(p, price);
                    return true;
                });

        builder.beginSubCommand("search", "/market search <keyword>")
                .withPermission("dpmarket.use")
                .playerOnly()
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING)
                .executesPlayer((p, args) -> {
                    String keyword = args.getString(ArgumentIndex.ARG_0);
                    if (keyword == null || keyword.isEmpty()) return false;
                    MarketFunction.search(p, keyword);
                    return true;
                });

        builder.beginSubCommand("claim", "/market claim")
                .withPermission("dpmarket.use")
                .playerOnly()
                .executesPlayer((p, args) -> {
                    MarketFunction.openClaim(p);
                    return true;
                });

        builder.beginSubCommand("remove", "/market remove")
                .withPermission("dpmarket.use")
                .playerOnly()
                .executesPlayer((p, args) -> {
                    MarketFunction.openMyListings(p);
                    return true;
                });

        builder.beginSubCommand("filter", "/market filter")
                .withPermission("dpmarket.admin")
                .playerOnly()
                .executesPlayer((p, args) -> {
                    MarketFunction.openFilterEdit(p);
                    return true;
                });

        builder.beginSubCommand("reload", "/market reload")
                .withPermission("dpmarket.admin")
                .executes((sender, args) -> {
                    plugin.reload();
                    plugin.send(sender, "reload_done");
                    return true;
                });

        builder.build("market");
    }
}
