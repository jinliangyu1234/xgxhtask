package com.xgxh.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerTaskData {

    private final UUID playerUUID;
    private final Map<String, Integer> taskProgress;
    private final Map<String, Boolean> taskCompleted;
    private final Map<String, Boolean> rewardClaimed;
    private String lastResetDate;
    private String dailyTaskDate;
    private List<String> dailyTaskIds;

    public PlayerTaskData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.taskProgress = new HashMap<>();
        this.taskCompleted = new HashMap<>();
        this.rewardClaimed = new HashMap<>();
        this.lastResetDate = "";
        this.dailyTaskDate = "";
        this.dailyTaskIds = new ArrayList<>();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public int getProgress(String taskId) {
        return taskProgress.getOrDefault(taskId, 0);
    }

    public void setProgress(String taskId, int progress) {
        taskProgress.put(taskId, progress);
    }

    public void addProgress(String taskId, int amount) {
        taskProgress.put(taskId, getProgress(taskId) + amount);
    }

    public boolean isCompleted(String taskId) {
        return taskCompleted.getOrDefault(taskId, false);
    }

    public void setCompleted(String taskId, boolean completed) {
        taskCompleted.put(taskId, completed);
    }

    public boolean isRewardClaimed(String taskId) {
        return rewardClaimed.getOrDefault(taskId, false);
    }

    public void setRewardClaimed(String taskId, boolean claimed) {
        rewardClaimed.put(taskId, claimed);
    }

    public String getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(String date) {
        this.lastResetDate = date;
    }

    public String getDailyTaskDate() {
        return dailyTaskDate;
    }

    public void setDailyTaskDate(String date) {
        this.dailyTaskDate = date;
    }

    public List<String> getDailyTaskIds() {
        return dailyTaskIds;
    }

    public void setDailyTaskIds(List<String> taskIds) {
        this.dailyTaskIds = taskIds;
    }

    public Map<String, Integer> getAllProgress() {
        return taskProgress;
    }

    public Map<String, Boolean> getAllCompleted() {
        return taskCompleted;
    }

    public Map<String, Boolean> getAllRewardClaimed() {
        return rewardClaimed;
    }
}
