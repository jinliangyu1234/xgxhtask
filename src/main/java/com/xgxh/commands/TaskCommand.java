package com.xgxh.commands;

import com.xgxh.XgxhPlugin;
import com.xgxh.models.DailyTask;
import com.xgxh.models.PlayerTaskData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TaskCommand implements CommandExecutor, TabCompleter {

    private final XgxhPlugin plugin;
    private static final String MENU_TITLE = "§6§l每日任务";

    public TaskCommand(XgxhPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            FileConfiguration config = plugin.getConfig();
            sender.sendMessage(config.getString("messages.errors.player-only", "§c只有玩家可以使用此命令！"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("xgxh.use")) {
            FileConfiguration config = plugin.getConfig();
            player.sendMessage(config.getString("messages.errors.no-permission", "§c你没有权限使用此命令！"));
            return true;
        }

        if (args.length == 0) {
            openTaskMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu":
            case "菜单":
                openTaskMenu(player);
                break;
            case "view":
            case "查看":
                showTaskProgress(player);
                break;
            case "claim":
            case "领取":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /task claim <任务ID>");
                    return true;
                }
                claimReward(player, args[1]);
                break;
            case "reset":
            case "重置":
                if (!player.hasPermission("xgxh.admin")) {
                    FileConfiguration config = plugin.getConfig();
                    player.sendMessage(config.getString("messages.errors.no-permission", "§c你没有权限执行此命令！"));
                    return true;
                }
                resetTasks(player);
                break;
            case "reload":
            case "重载":
                if (!player.hasPermission("xgxh.admin")) {
                    FileConfiguration config = plugin.getConfig();
                    player.sendMessage(config.getString("messages.errors.no-permission", "§c你没有权限执行此命令！"));
                    return true;
                }
                reloadPlugin(player);
                break;
            default:
                player.sendMessage("§e用法: /task [菜单|查看|领取|重置|重载]");
                break;
        }

        return true;
    }

    private void openTaskMenu(Player player) {
        List<DailyTask> tasks = plugin.getTaskManager().getPlayerDailyTasks(player.getUniqueId());

        // 固定6排9列 = 54格
        int size = 54;
        Inventory menu = Bukkit.createInventory(null, size, MENU_TITLE);

        // 创建蓝色玻璃边框
        ItemStack blueGlass = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blueGlass.getItemMeta();
        glassMeta.displayName(net.kyori.adventure.text.Component.text(" "));
        blueGlass.setItemMeta(glassMeta);

        // 填充边框
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;

            // 第一行、最后一行、第一列、最后一列
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                menu.setItem(i, blueGlass);
            }
        }

        // 放置任务物品（中间4行7列 = 28个位置）
        PlayerTaskData data = plugin.getTaskManager().getPlayerData(player.getUniqueId());

        int taskIndex = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                if (taskIndex >= tasks.size()) break;

                int slot = row * 9 + col;
                DailyTask task = tasks.get(taskIndex);
                ItemStack item = createTaskItem(task, data);
                menu.setItem(slot, item);
                taskIndex++;
            }
        }

        player.openInventory(menu);
    }

    private ItemStack createTaskItem(DailyTask task, PlayerTaskData data) {
        Material material;
        switch (task.getType()) {
            case MINE:
                material = task.getTargetMaterial();
                break;
            case KILL:
                material = Material.IRON_SWORD;
                break;
            case FISH:
                material = Material.FISHING_ROD;
                break;
            case FARM:
                material = Material.WHEAT;
                break;
            case WALK:
                material = Material.FEATHER;
                break;
            default:
                material = Material.PAPER;
                break;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        int progress = data.getProgress(task.getId());
        boolean completed = data.isCompleted(task.getId());
        boolean claimed = data.isRewardClaimed(task.getId());

        if (completed || claimed) {
            org.bukkit.enchantments.Enchantment sharpness = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("sharpness"));
            if (sharpness != null) {
                meta.addEnchant(sharpness, 1, true);
            }
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        meta.displayName(net.kyori.adventure.text.Component.text("§6" + task.getName())
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§7" + task.getDescription()));
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text("§e进度: §f" + progress + " §7/ §f" + task.getTargetAmount()));

        double percentage = (double) progress / task.getTargetAmount() * 100;
        String progressBar = getProgressBar(percentage);
        lore.add(net.kyori.adventure.text.Component.text("§7" + progressBar + " §f" + String.format("%.1f", percentage) + "%"));

        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text("§e奖励: §a" + String.format("%.0f", task.getReward()) + " 金币"));

        if (claimed) {
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("§a✓ 已领取奖励"));
        } else if (completed) {
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("§a✓ 任务已完成"));
            lore.add(net.kyori.adventure.text.Component.text("§e点击领取奖励！"));
        } else {
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("§c✗ 任务未完成"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private String getProgressBar(double percentage) {
        int filled = (int) (percentage / 10);
        int empty = 10 - filled;
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) {
            bar.append("■");
        }
        bar.append("§7");
        for (int i = 0; i < empty; i++) {
            bar.append("■");
        }
        return bar.toString();
    }

    private void showTaskProgress(Player player) {
        FileConfiguration config = plugin.getConfig();
        PlayerTaskData data = plugin.getTaskManager().getPlayerData(player.getUniqueId());
        List<DailyTask> tasks = plugin.getTaskManager().getPlayerDailyTasks(player.getUniqueId());

        player.sendMessage(config.getString("messages.task-view.title", "§6§l===== 每日任务进度 ====="));
        for (DailyTask task : tasks) {
            int progress = data.getProgress(task.getId());
            boolean completed = data.isCompleted(task.getId());
            boolean claimed = data.isRewardClaimed(task.getId());

            String status;
            if (claimed) {
                status = config.getString("messages.task-view.status-claimed", "§a[已领取]");
            } else if (completed) {
                status = config.getString("messages.task-view.status-completed", "§e[可领取]");
            } else {
                status = config.getString("messages.task-view.status-progress", "§c[进行中]");
            }

            String format = config.getString("messages.task-view.format", "%status% %name% §7- §f%progress%/%amount% §7(§a%reward%金币§7)");
            player.sendMessage(format
                .replace("%status%", status)
                .replace("%name%", task.getName())
                .replace("%progress%", String.valueOf(progress))
                .replace("%amount%", String.valueOf(task.getTargetAmount()))
                .replace("%reward%", String.format("%.0f", task.getReward())));
        }
        player.sendMessage(config.getString("messages.task-view.footer", "§6§l========================"));
    }

    private void claimReward(Player player, String taskId) {
        FileConfiguration config = plugin.getConfig();
        UUID uuid = player.getUniqueId();
        PlayerTaskData data = plugin.getTaskManager().getPlayerData(uuid);
        DailyTask task = plugin.getTaskManager().getTaskById(taskId);

        if (task == null) {
            player.sendMessage(config.getString("messages.errors.task-not-exist", "§c任务不存在！"));
            return;
        }

        if (!data.isCompleted(taskId)) {
            player.sendMessage(config.getString("messages.errors.task-not-completed", "§c任务尚未完成！"));
            return;
        }

        if (data.isRewardClaimed(taskId)) {
            player.sendMessage(config.getString("messages.errors.reward-already-claimed", "§c奖励已经领取过了！"));
            return;
        }

        if (plugin.getTaskManager().claimReward(uuid, taskId)) {
            plugin.getVaultManager().deposit(player, task.getReward());
            player.sendMessage("§a成功领取 §e" + task.getName() + " §a任务奖励: §6" +
                String.format("%.0f", task.getReward()) + " 金币");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(config.getString("messages.errors.claim-failed", "§c领取奖励失败！"));
        }
    }

    private void resetTasks(Player player) {
        FileConfiguration config = plugin.getConfig();
        plugin.getTaskManager().resetAndSaveAllPlayerData();
        plugin.getTaskManager().loadTasks();

        String resetMsg = config.getString("messages.task-reset.message", "§a§l✦ 每日任务已重置！新的任务等待你完成！");
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.sendMessage(resetMsg);
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        });

        player.sendMessage("§a任务已重置！所有玩家的任务数据已清除！");
    }

    private void reloadPlugin(Player player) {
        plugin.reloadConfig();
        plugin.getTaskManager().loadTasks();
        plugin.restartAutoSaveTask();
        plugin.restartTaskResetTask();
        player.sendMessage("§a插件配置已重载！");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("menu", "view", "claim", "reset", "reload")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            Player player = (Player) sender;
            return plugin.getTaskManager().getPlayerDailyTasks(player.getUniqueId())
                .stream()
                .map(DailyTask::getId)
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
