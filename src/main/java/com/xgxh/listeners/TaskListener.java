package com.xgxh.listeners;

import com.xgxh.XgxhPlugin;
import com.xgxh.models.DailyTask;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TaskListener implements Listener {

    private final XgxhPlugin plugin;
    private final Map<UUID, org.bukkit.Location> lastLocations;
    private final Map<UUID, Long> lastMoveTime;
    private static final long MOVE_CHECK_INTERVAL = 500; // 500毫秒检查一次

    public TaskListener(XgxhPlugin plugin) {
        this.plugin = plugin;
        this.lastLocations = new HashMap<>();
        this.lastMoveTime = new HashMap<>();
    }

    private boolean isAllowedWorld(Player player) {
        FileConfiguration config = plugin.getConfig();
        List<String> allowedWorlds = config.getStringList("allowed-worlds");
        if (allowedWorlds.isEmpty()) {
            return true;
        }
        String worldName = player.getWorld().getName();
        return allowedWorlds.contains(worldName);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isAllowedWorld(player)) return;

        Block block = event.getBlock();
        Material material = block.getType();

        Material taskMaterial = getOreType(material);
        if (taskMaterial != null) {
            plugin.getTaskManager().updateTaskProgress(
                player.getUniqueId(),
                DailyTask.TaskType.MINE,
                taskMaterial,
                1
            );
        }

        Material farmMaterial = getFarmType(material);
        if (farmMaterial != null) {
            plugin.getTaskManager().updateTaskProgress(
                player.getUniqueId(),
                DailyTask.TaskType.FARM,
                farmMaterial,
                1
            );
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!isAllowedWorld(player)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.SWEET_BERRY_BUSH) {
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() >= 2) {
                    plugin.getTaskManager().updateTaskProgress(
                        player.getUniqueId(),
                        DailyTask.TaskType.FARM,
                        Material.SWEET_BERRIES,
                        1
                    );
                }
            }
        }
    }

    private Material getFarmType(Material material) {
        switch (material) {
            case WHEAT:
                return Material.WHEAT;
            case CARROTS:
                return Material.CARROT;
            case POTATOES:
                return Material.POTATO;
            case BEETROOTS:
                return Material.BEETROOT;
            case SUGAR_CANE:
                return Material.SUGAR_CANE;
            case MELON:
                return Material.MELON;
            case PUMPKIN:
                return Material.PUMPKIN;
            case CACTUS:
                return Material.CACTUS;
            case BAMBOO:
                return Material.BAMBOO;
            case KELP:
                return Material.KELP;
            case SWEET_BERRY_BUSH:
                return Material.SWEET_BERRIES;
            case COCOA:
                return Material.COCOA_BEANS;
            case NETHER_WART:
                return Material.NETHER_WART;
            default:
                return null;
        }
    }

    private Material getOreType(Material material) {
        switch (material) {
            case STONE:
            case COBBLESTONE:
                return Material.STONE;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return Material.IRON_ORE;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return Material.GOLD_ORE;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return Material.DIAMOND_ORE;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return Material.EMERALD_ORE;
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return Material.COAL_ORE;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return Material.LAPIS_ORE;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return Material.REDSTONE_ORE;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return Material.COPPER_ORE;
            default:
                return null;
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!isAllowedWorld(killer)) return;

        EntityType entityType = event.getEntityType();
        String entityName = entityType.name();

        plugin.getTaskManager().updateTaskProgressByEntity(
            killer.getUniqueId(),
            DailyTask.TaskType.KILL,
            entityName,
            1
        );
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            if (!isAllowedWorld(player)) return;

            org.bukkit.entity.Entity caught = event.getCaught();
            if (caught instanceof org.bukkit.entity.Item) {
                ItemStack itemStack = ((org.bukkit.entity.Item) caught).getItemStack();
                Material fishType = itemStack.getType();
                
                plugin.getTaskManager().updateTaskProgress(
                    player.getUniqueId(),
                    DailyTask.TaskType.FISH,
                    fishType,
                    1
                );
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 限制检查频率，每500毫秒检查一次
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMoveTime.get(uuid);
        if (lastTime != null && (currentTime - lastTime) < MOVE_CHECK_INTERVAL) {
            return;
        }
        lastMoveTime.put(uuid, currentTime);

        if (!isAllowedWorld(player)) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        org.bukkit.Location from = lastLocations.get(uuid);
        org.bukkit.Location to = event.getTo();

        if (from != null && from.getWorld().equals(to.getWorld())) {
            double distance = from.distance(to);

            // 防止传送导致步数增加（距离超过10格视为传送）
            if (distance > 10.0) {
                lastLocations.put(uuid, to.clone());
                return;
            }

            if (distance >= 1.0) {
                plugin.getTaskManager().updateTaskProgress(
                    uuid,
                    DailyTask.TaskType.WALK,
                    Material.FEATHER,
                    (int) distance
                );
                lastLocations.put(uuid, to.clone());
            }
        } else {
            lastLocations.put(uuid, to.clone());
        }
    }

    // 玩家退出时清理数据
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastLocations.remove(uuid);
        lastMoveTime.remove(uuid);
    }
}
