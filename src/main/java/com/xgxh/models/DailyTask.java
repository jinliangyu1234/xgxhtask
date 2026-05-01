package com.xgxh.models;

import org.bukkit.Material;

public class DailyTask {

    private final String id;
    private final String name;
    private final String description;
    private final TaskType type;
    private final Material targetMaterial;
    private final String targetEntity;
    private final int targetAmount;
    private final double reward;

    public DailyTask(String id, String name, String description, TaskType type, Material targetMaterial, String targetEntity, int targetAmount, double reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.targetMaterial = targetMaterial;
        this.targetEntity = targetEntity;
        this.targetAmount = targetAmount;
        this.reward = reward;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TaskType getType() {
        return type;
    }

    public Material getTargetMaterial() {
        return targetMaterial;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public double getReward() {
        return reward;
    }

    public enum TaskType {
        MINE,       // 挖矿
        KILL,       // 击杀怪物
        FISH,       // 钓鱼
        FARM,       // 农业种植
        WALK        // 行走距离
    }
}
