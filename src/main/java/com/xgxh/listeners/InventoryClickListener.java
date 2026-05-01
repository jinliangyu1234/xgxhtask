package com.xgxh.listeners;

import com.xgxh.XgxhPlugin;
import com.xgxh.models.DailyTask;
import com.xgxh.models.PlayerTaskData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final XgxhPlugin plugin;
    private static final String MENU_TITLE = "§6§l每日任务";

    public InventoryClickListener(XgxhPlugin plugin) {
        this.plugin = plugin;
    }

    private int getTaskIndex(int slot) {
        int row = slot / 9;
        int col = slot % 9;

        // 只处理边框内的区域（行1-4，列1-7）
        if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
            return (row - 1) * 7 + (col - 1);
        }
        return -1;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        String title = event.getView().getTitle();
        if (!title.equals(MENU_TITLE)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().name().equals("AIR")) return;

        // 检查是否点击了边框（蓝色玻璃）
        if (clickedItem.getType().name().contains("STAINED_GLASS")) return;

        // 计算任务索引
        int taskIndex = getTaskIndex(slot);
        if (taskIndex < 0) return;

        List<DailyTask> tasks = plugin.getTaskManager().getPlayerDailyTasks(player.getUniqueId());
        if (taskIndex >= tasks.size()) return;

        DailyTask task = tasks.get(taskIndex);
        UUID uuid = player.getUniqueId();
        PlayerTaskData data = plugin.getTaskManager().getPlayerData(uuid);

        if (data.isCompleted(task.getId()) && !data.isRewardClaimed(task.getId())) {
            if (plugin.getTaskManager().claimReward(uuid, task.getId())) {
                plugin.getVaultManager().deposit(player, task.getReward());
                player.sendMessage("§a成功领取 §e" + task.getName() + " §a任务奖励: §6" +
                    String.format("%.0f", task.getReward()) + " 金币");

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                player.closeInventory();
            }
        } else if (data.isRewardClaimed(task.getId())) {
            player.sendMessage("§c该任务奖励已经领取过了！");
        } else {
            player.sendMessage("§c该任务尚未完成！");
        }
    }
}
