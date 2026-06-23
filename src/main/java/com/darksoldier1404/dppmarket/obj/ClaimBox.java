package com.darksoldier1404.dppmarket.obj;

import com.darksoldier1404.dppc.data.DataCargo;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClaimBox implements DataCargo {
    private double money;
    private List<ItemStack> items = new ArrayList<>();

    public ClaimBox() {
    }

    @Override
    public YamlConfiguration serialize() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("money", money);
        data.set("items", new ArrayList<>(items));
        return data;
    }

    @Override
    public ClaimBox deserialize(YamlConfiguration data) {
        this.money = data.getDouble("money");
        this.items = new ArrayList<>();
        List<?> raw = data.getList("items");
        if (raw != null) {
            for (Object o : raw) {
                if (o instanceof ItemStack) {
                    this.items.add((ItemStack) o);
                }
            }
        }
        return this;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public void addMoney(double amount) {
        this.money += amount;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void addItem(ItemStack item) {
        this.items.add(item);
    }

    public boolean isEmpty() {
        return money <= 0 && items.isEmpty();
    }
}
