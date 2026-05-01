package com.xgxh.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultManager {

    private final JavaPlugin plugin;
    private Economy economy;

    public VaultManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public void deposit(org.bukkit.entity.Player player, double amount) {
        economy.depositPlayer(player, amount);
    }

    public double getBalance(org.bukkit.entity.Player player) {
        return economy.getBalance(player);
    }
}
