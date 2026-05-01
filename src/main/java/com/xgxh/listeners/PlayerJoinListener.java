package com.xgxh.listeners;

import com.xgxh.XgxhPlugin;
import com.xgxh.models.DailyTask;
import com.xgxh.models.PlayerTaskData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final XgxhPlugin plugin;

    public PlayerJoinListener(XgxhPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        FileConfiguration config = plugin.getConfig();
        boolean joinReminder = config.getBoolean("notifications.join-reminder", true);

        if (!joinReminder) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            PlayerTaskData data = plugin.getTaskManager().getPlayerData(uuid);
            List<DailyTask> dailyTasks = plugin.getTaskManager().getPlayerDailyTasks(uuid);
            int totalTasks = dailyTasks.size();
            int completedTasks = plugin.getTaskManager().getCompletedTaskCount(uuid);
            int claimedTasks = plugin.getTaskManager().getClaimedTaskCount(uuid);

            String title = config.getString("messages.join.title", "");
            String subtitle = config.getString("messages.join.subtitle", "§6§l     §e§l每日任务状态 §6§l");
            String footer = config.getString("messages.join.footer", "");
            String totalTasksMsg = config.getString("messages.join.total-tasks", "§7今日共有 §e%total% §7个任务");
            String completedMsg = config.getString("messages.join.completed", "§7已完成: §a%completed% §7个");
            String claimedMsg = config.getString("messages.join.claimed", "§7已领取: §e%claimed% §7个");
            String remainingMsg = config.getString("messages.join.remaining", "§e§l还有 §c%remaining% §e§l个任务等你完成！");
            String unclaimedMsg = config.getString("messages.join.unclaimed", "§a§l有 §e%unclaimed% §a§l个任务奖励待领取！");
            String helpMsg = config.getString("messages.join.help", "§7输入 §e/task 查看 §7查看详情");
            String menuHelpMsg = config.getString("messages.join.menu-help", "§7输入 §e/task 菜单 §7打开任务界面");
            String separator = config.getString("messages.join.separator", "");

            if (!title.isEmpty()) player.sendMessage(title);
            player.sendMessage(subtitle);
            if (!footer.isEmpty()) player.sendMessage(footer);
            player.sendMessage("");
            player.sendMessage(totalTasksMsg.replace("%total%", String.valueOf(totalTasks)));
            player.sendMessage(completedMsg.replace("%completed%", String.valueOf(completedTasks)));
            player.sendMessage(claimedMsg.replace("%claimed%", String.valueOf(claimedTasks)));

            if (completedTasks < totalTasks) {
                player.sendMessage("");
                player.sendMessage(remainingMsg.replace("%remaining%", String.valueOf(totalTasks - completedTasks)));
            }

            if (completedTasks > claimedTasks) {
                player.sendMessage("");
                player.sendMessage(unclaimedMsg.replace("%unclaimed%", String.valueOf(completedTasks - claimedTasks)));
                player.sendMessage(helpMsg);
            }

            player.sendMessage("");
            player.sendMessage(menuHelpMsg);
            if (!separator.isEmpty()) player.sendMessage(separator);
            player.sendMessage("");
        }, 40L);
    }
}
