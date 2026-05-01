package com.xgxh;

import com.xgxh.commands.TaskCommand;
import com.xgxh.listeners.InventoryClickListener;
import com.xgxh.listeners.PlayerJoinListener;
import com.xgxh.listeners.TaskListener;
import com.xgxh.manager.TaskManager;
import com.xgxh.manager.VaultManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class XgxhPlugin extends JavaPlugin {

    private static XgxhPlugin instance;
    private TaskManager taskManager;
    private VaultManager vaultManager;
    private BukkitTask autoSaveTask;
    private BukkitTask taskResetTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        vaultManager = new VaultManager(this);
        if (!vaultManager.setupEconomy()) {
            getLogger().severe("Vault经济系统未找到，插件禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        taskManager = new TaskManager(this);
        taskManager.loadTasks();

        getCommand("task").setExecutor(new TaskCommand(this));
        getServer().getPluginManager().registerEvents(new TaskListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);

        startAutoSaveTask();
        startTaskResetTask();

        getLogger().info("xgxhtask每日任务插件已启用！作者: 雪糕小豪");
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        if (taskResetTask != null) {
            taskResetTask.cancel();
        }
        if (taskManager != null) {
            taskManager.saveAllPlayerData();
        }
        getLogger().info("xgxhtask每日任务插件已禁用！");
    }

    private void startAutoSaveTask() {
        FileConfiguration config = getConfig();
        boolean autoSaveEnabled = config.getBoolean("auto-save.enabled", true);

        if (!autoSaveEnabled) {
            getLogger().info("自动保存功能已关闭");
            return;
        }

        int intervalMinutes = config.getInt("auto-save.interval-minutes", 5);
        long intervalTicks = intervalMinutes * 60L * 20L;

        autoSaveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            taskManager.autoSaveAllPlayerData();
        }, intervalTicks, intervalTicks);

        getLogger().info("自动保存任务已启动，间隔: " + intervalMinutes + " 分钟");
    }

    private void startTaskResetTask() {
        FileConfiguration config = getConfig();
        boolean resetEnabled = config.getBoolean("task-reset.enabled", false);

        if (!resetEnabled) {
            getLogger().info("定时重置功能已关闭");
            return;
        }

        int intervalMinutes = config.getInt("task-reset.interval-minutes", 1440);
        long intervalTicks = intervalMinutes * 60L * 20L;

        taskResetTask = getServer().getScheduler().runTaskTimer(this, () -> {
            taskManager.resetAndSaveAllPlayerData();
            taskManager.loadTasks();

            getServer().getOnlinePlayers().forEach(player -> {
                player.sendMessage("§a§l✦ 每日任务已重置！新的任务等待你完成！");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            });

            getLogger().info("定时重置任务完成，已通知所有在线玩家");
        }, intervalTicks, intervalTicks);

        getLogger().info("定时重置任务已启动，间隔: " + intervalMinutes + " 分钟");
    }

    public void restartAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        startAutoSaveTask();
    }

    public void restartTaskResetTask() {
        if (taskResetTask != null) {
            taskResetTask.cancel();
        }
        startTaskResetTask();
    }

    public static XgxhPlugin getInstance() {
        return instance;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }
}
