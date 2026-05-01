package com.xgxh.manager;

import com.xgxh.XgxhPlugin;
import com.xgxh.models.DailyTask;
import com.xgxh.models.PlayerTaskData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TaskManager {

    private final XgxhPlugin plugin;
    private final List<DailyTask> taskPool;
    private final Map<UUID, PlayerTaskData> playerDataMap;
    private final Map<UUID, List<DailyTask>> playerDailyTasks;
    private final File playerDataFolder;
    private final DateTimeFormatter dateFormatter;

    public TaskManager(XgxhPlugin plugin) {
        this.plugin = plugin;
        this.taskPool = new ArrayList<>();
        this.playerDataMap = new HashMap<>();
        this.playerDailyTasks = new HashMap<>();
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public void loadTasks() {
        taskPool.clear();
        playerDailyTasks.clear();
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection tasksSection = config.getConfigurationSection("tasks");
        if (tasksSection == null) {
            createDefaultTasks();
            plugin.saveConfig();
            tasksSection = config.getConfigurationSection("tasks");
        }

        for (String taskId : tasksSection.getKeys(false)) {
            ConfigurationSection taskSection = tasksSection.getConfigurationSection(taskId);
            if (taskSection != null) {
                String name = taskSection.getString("name", taskId);
                String description = taskSection.getString("description", "");
                String typeStr = taskSection.getString("type", "MINE");
                String materialStr = taskSection.getString("material", "STONE");
                int amount = taskSection.getInt("amount", 10);
                double reward = taskSection.getDouble("reward", 100.0);

                DailyTask.TaskType type;
                try {
                    type = DailyTask.TaskType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    type = DailyTask.TaskType.MINE;
                }

                Material material = null;
                String entityName = null;

                if (type == DailyTask.TaskType.KILL) {
                    entityName = materialStr;
                } else {
                    try {
                        material = Material.valueOf(materialStr);
                    } catch (IllegalArgumentException e) {
                        material = Material.STONE;
                    }
                }

                taskPool.add(new DailyTask(taskId, name, description, type, material, entityName, amount, reward));
            }
        }

        plugin.getLogger().info("已加载 " + taskPool.size() + " 个任务到任务池");
    }

    private void createDefaultTasks() {
        FileConfiguration config = plugin.getConfig();

        config.set("tasks.mine_stone.name", "挖掘石头");
        config.set("tasks.mine_stone.description", "挖掘64个石头");
        config.set("tasks.mine_stone.type", "MINE");
        config.set("tasks.mine_stone.material", "STONE");
        config.set("tasks.mine_stone.amount", 64);
        config.set("tasks.mine_stone.reward", 500.0);

        config.set("tasks.mine_iron.name", "挖掘铁矿");
        config.set("tasks.mine_iron.description", "挖掘16个铁矿石");
        config.set("tasks.mine_iron.type", "MINE");
        config.set("tasks.mine_iron.material", "IRON_ORE");
        config.set("tasks.mine_iron.amount", 16);
        config.set("tasks.mine_iron.reward", 800.0);

        config.set("tasks.kill_zombie.name", "击杀僵尸");
        config.set("tasks.kill_zombie.description", "击杀20只僵尸");
        config.set("tasks.kill_zombie.type", "KILL");
        config.set("tasks.kill_zombie.material", "ZOMBIE");
        config.set("tasks.kill_zombie.amount", 20);
        config.set("tasks.kill_zombie.reward", 600.0);

        config.set("tasks.fish.name", "钓鱼达人");
        config.set("tasks.fish.description", "钓上10条鱼");
        config.set("tasks.fish.type", "FISH");
        config.set("tasks.fish.material", "COD");
        config.set("tasks.fish.amount", 10);
        config.set("tasks.fish.reward", 800.0);

        config.set("tasks.farm_wheat.name", "种植小麦");
        config.set("tasks.farm_wheat.description", "收获32个小麦");
        config.set("tasks.farm_wheat.type", "FARM");
        config.set("tasks.farm_wheat.material", "WHEAT");
        config.set("tasks.farm_wheat.amount", 32);
        config.set("tasks.farm_wheat.reward", 400.0);

        config.set("tasks.walk.name", "长途跋涉");
        config.set("tasks.walk.description", "行走1000格距离");
        config.set("tasks.walk.type", "WALK");
        config.set("tasks.walk.material", "FEATHER");
        config.set("tasks.walk.amount", 1000);
        config.set("tasks.walk.reward", 600.0);
    }

    public List<DailyTask> getPlayerDailyTasks(UUID playerUUID) {
        FileConfiguration config = plugin.getConfig();
        boolean randomEnabled = config.getBoolean("daily-tasks.random-enabled", true);
        int dailyCount = config.getInt("daily-tasks.count", 20);

        if (!randomEnabled) {
            return taskPool;
        }

        List<DailyTask> playerTasks = playerDailyTasks.get(playerUUID);
        if (playerTasks != null) {
            return playerTasks;
        }

        PlayerTaskData data = getPlayerData(playerUUID);
        String today = LocalDate.now().format(dateFormatter);

        if (data.getDailyTaskDate() != null && today.equals(data.getDailyTaskDate())) {
            List<String> dailyTaskIds = data.getDailyTaskIds();
            playerTasks = new ArrayList<>();
            for (String taskId : dailyTaskIds) {
                DailyTask task = getTaskById(taskId);
                if (task != null) {
                    playerTasks.add(task);
                }
            }
            if (!playerTasks.isEmpty()) {
                playerDailyTasks.put(playerUUID, playerTasks);
                return playerTasks;
            }
        }

        playerTasks = generateRandomTasks(dailyCount, playerUUID);
        playerDailyTasks.put(playerUUID, playerTasks);

        data.setDailyTaskDate(today);
        data.setDailyTaskIds(playerTasks.stream().map(DailyTask::getId).collect(Collectors.toList()));

        return playerTasks;
    }

    private List<DailyTask> generateRandomTasks(int count, UUID playerUUID) {
        List<DailyTask> shuffled = new ArrayList<>(taskPool);
        Collections.shuffle(shuffled, new Random(playerUUID.hashCode() + LocalDate.now().toEpochDay()));

        List<DailyTask> selected = new ArrayList<>();
        Set<DailyTask.TaskType> usedTypes = new HashSet<>();

        for (DailyTask task : shuffled) {
            if (selected.size() >= count) break;

            if (selected.size() < count / 2 || !usedTypes.contains(task.getType())) {
                selected.add(task);
                usedTypes.add(task.getType());
            }
        }

        for (DailyTask task : shuffled) {
            if (selected.size() >= count) break;
            if (!selected.contains(task)) {
                selected.add(task);
            }
        }

        return selected.subList(0, Math.min(count, selected.size()));
    }

    public PlayerTaskData getPlayerData(UUID playerUUID) {
        PlayerTaskData data = playerDataMap.get(playerUUID);
        if (data == null) {
            data = loadPlayerData(playerUUID);
            playerDataMap.put(playerUUID, data);
        }

        FileConfiguration config = plugin.getConfig();
        boolean useTimeReset = config.getBoolean("task-reset.enabled", false);

        if (!useTimeReset) {
            String today = LocalDate.now().format(dateFormatter);
            if (!today.equals(data.getLastResetDate())) {
                resetPlayerTasks(data);
                data.setLastResetDate(today);
            }
        }

        return data;
    }

    private PlayerTaskData loadPlayerData(UUID playerUUID) {
        File file = new File(playerDataFolder, playerUUID.toString() + ".yml");
        PlayerTaskData data = new PlayerTaskData(playerUUID);

        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            data.setLastResetDate(config.getString("lastResetDate", ""));
            data.setDailyTaskDate(config.getString("dailyTaskDate", ""));
            data.setDailyTaskIds(config.getStringList("dailyTaskIds"));

            ConfigurationSection progressSection = config.getConfigurationSection("progress");
            if (progressSection != null) {
                for (String taskId : progressSection.getKeys(false)) {
                    data.setProgress(taskId, progressSection.getInt(taskId));
                }
            }

            ConfigurationSection completedSection = config.getConfigurationSection("completed");
            if (completedSection != null) {
                for (String taskId : completedSection.getKeys(false)) {
                    data.setCompleted(taskId, completedSection.getBoolean(taskId));
                }
            }

            ConfigurationSection claimedSection = config.getConfigurationSection("claimed");
            if (claimedSection != null) {
                for (String taskId : claimedSection.getKeys(false)) {
                    data.setRewardClaimed(taskId, claimedSection.getBoolean(taskId));
                }
            }
        }

        return data;
    }

    public void savePlayerData(UUID playerUUID) {
        PlayerTaskData data = playerDataMap.get(playerUUID);
        if (data == null) return;

        File file = new File(playerDataFolder, playerUUID.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("lastResetDate", data.getLastResetDate());
        config.set("dailyTaskDate", data.getDailyTaskDate());
        config.set("dailyTaskIds", data.getDailyTaskIds());

        ConfigurationSection progressSection = config.createSection("progress");
        for (Map.Entry<String, Integer> entry : data.getAllProgress().entrySet()) {
            progressSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection completedSection = config.createSection("completed");
        for (Map.Entry<String, Boolean> entry : data.getAllCompleted().entrySet()) {
            completedSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection claimedSection = config.createSection("claimed");
        for (Map.Entry<String, Boolean> entry : data.getAllRewardClaimed().entrySet()) {
            claimedSection.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + playerUUID);
            e.printStackTrace();
        }
    }

    public void saveAllPlayerData() {
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
    }

    public void autoSaveAllPlayerData() {
        saveAllPlayerData();
        plugin.getLogger().info("自动保存所有玩家任务数据完成");
    }

    public void resetAllPlayerData() {
        for (PlayerTaskData data : playerDataMap.values()) {
            resetPlayerTasks(data);
            data.setLastResetDate(LocalDate.now().format(dateFormatter));
            data.setDailyTaskDate("");
            data.setDailyTaskIds(new ArrayList<>());
        }

        playerDailyTasks.clear();

        for (UUID uuid : playerDataMap.keySet()) {
            File file = new File(playerDataFolder, uuid.toString() + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }

        plugin.getLogger().info("已重置所有玩家任务数据，共 " + playerDataMap.size() + " 个玩家");
    }

    public void resetAndSaveAllPlayerData() {
        resetAllPlayerData();
        saveAllPlayerData();
        plugin.getLogger().info("重置并保存所有玩家任务数据完成");
    }

    private void resetPlayerTasks(PlayerTaskData data) {
        for (String taskId : data.getAllProgress().keySet()) {
            data.setProgress(taskId, 0);
            data.setCompleted(taskId, false);
            data.setRewardClaimed(taskId, false);
        }
        data.setDailyTaskDate("");
        data.setDailyTaskIds(new ArrayList<>());
    }

    public void updateTaskProgress(UUID playerUUID, DailyTask.TaskType type, Material material, int amount) {
        PlayerTaskData data = getPlayerData(playerUUID);
        List<DailyTask> dailyTasks = getPlayerDailyTasks(playerUUID);
        boolean taskCompleted = false;
        DailyTask completedTask = null;

        for (DailyTask task : dailyTasks) {
            if (task.getType() == type && task.getTargetMaterial() == material) {
                if (!data.isCompleted(task.getId())) {
                    data.addProgress(task.getId(), amount);

                    if (data.getProgress(task.getId()) >= task.getTargetAmount()) {
                        data.setCompleted(task.getId(), true);
                        data.setProgress(task.getId(), task.getTargetAmount());
                        taskCompleted = true;
                        completedTask = task;
                    }
                }
            }
        }

        if (taskCompleted && completedTask != null) {
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                handleTaskCompletion(player, completedTask, data);
            }
        }
    }

    public void updateTaskProgressByEntity(UUID playerUUID, DailyTask.TaskType type, String entityName, int amount) {
        PlayerTaskData data = getPlayerData(playerUUID);
        List<DailyTask> dailyTasks = getPlayerDailyTasks(playerUUID);
        boolean taskCompleted = false;
        DailyTask completedTask = null;

        for (DailyTask task : dailyTasks) {
            if (task.getType() == type && entityName.equals(task.getTargetEntity())) {
                if (!data.isCompleted(task.getId())) {
                    data.addProgress(task.getId(), amount);

                    if (data.getProgress(task.getId()) >= task.getTargetAmount()) {
                        data.setCompleted(task.getId(), true);
                        data.setProgress(task.getId(), task.getTargetAmount());
                        taskCompleted = true;
                        completedTask = task;
                    }
                }
            }
        }

        if (taskCompleted && completedTask != null) {
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                handleTaskCompletion(player, completedTask, data);
            }
        }
    }

    private void handleTaskCompletion(Player player, DailyTask task, PlayerTaskData data) {
        FileConfiguration config = plugin.getConfig();
        boolean autoClaim = config.getBoolean("notifications.auto-claim", true);
        boolean notify = config.getBoolean("notifications.task-complete-notify", true);
        boolean playSound = config.getBoolean("notifications.task-complete-sound", true);

        if (playSound) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        if (notify) {
            String header = config.getString("messages.task-complete.header", "");
            String message = config.getString("messages.task-complete.message", "§a§l✦ 任务完成！§e%task%");
            String desc = config.getString("messages.task-complete.description", "§7%description%");
            String rewardMsg = config.getString("messages.task-complete.reward", "§7奖励: §6%reward% 金币");
            String footer = config.getString("messages.task-complete.footer", "");

            if (!header.isEmpty()) player.sendMessage(header);
            player.sendMessage(message.replace("%task%", task.getName()));
            player.sendMessage(desc.replace("%description%", task.getDescription()));
            player.sendMessage(rewardMsg.replace("%reward%", String.format("%.0f", task.getReward())));
            if (!footer.isEmpty()) player.sendMessage(footer);
        }

        if (autoClaim && !data.isRewardClaimed(task.getId())) {
            data.setRewardClaimed(task.getId(), true);
            plugin.getVaultManager().deposit(player, task.getReward());

            if (notify) {
                String autoClaimMsg = config.getString("messages.task-complete.auto-claim", "§a§l✓ 奖励已自动领取！§6+%reward% 金币");
                player.sendMessage(autoClaimMsg.replace("%reward%", String.format("%.0f", task.getReward())));
            }

            if (playSound) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    public boolean claimReward(UUID playerUUID, String taskId) {
        PlayerTaskData data = getPlayerData(playerUUID);

        DailyTask task = getTaskById(taskId);
        if (task == null) return false;

        if (!data.isCompleted(taskId)) return false;
        if (data.isRewardClaimed(taskId)) return false;

        data.setRewardClaimed(taskId, true);
        return true;
    }

    public DailyTask getTaskById(String taskId) {
        for (DailyTask task : taskPool) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    public List<DailyTask> getTaskPool() {
        return taskPool;
    }

    public int getCompletedTaskCount(UUID playerUUID) {
        PlayerTaskData data = getPlayerData(playerUUID);
        List<DailyTask> dailyTasks = getPlayerDailyTasks(playerUUID);
        int count = 0;
        for (DailyTask task : dailyTasks) {
            if (data.isCompleted(task.getId())) {
                count++;
            }
        }
        return count;
    }

    public int getClaimedTaskCount(UUID playerUUID) {
        PlayerTaskData data = getPlayerData(playerUUID);
        List<DailyTask> dailyTasks = getPlayerDailyTasks(playerUUID);
        int count = 0;
        for (DailyTask task : dailyTasks) {
            if (data.isRewardClaimed(task.getId())) {
                count++;
            }
        }
        return count;
    }

    public int getTotalTaskCount(UUID playerUUID) {
        return getPlayerDailyTasks(playerUUID).size();
    }
}
