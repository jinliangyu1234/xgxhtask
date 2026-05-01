# xgxhtask - 每日任务插件

**作者：雪糕小豪**

一款功能丰富的 Minecraft 每日任务插件，支持随机任务、多种任务类型、自动奖励领取等功能。

---

## 📋 功能特性

### 🎯 核心功能
- **每日随机任务** - 每天随机显示指定数量的任务
- **多种任务类型** - 挖矿、击杀、钓鱼、农业、行走
- **自动奖励领取** - 完成任务自动发放金币奖励
- **自定义提示语** - 所有提示信息可自定义
- **世界限制** - 可设置任务生效的世界
- **定时重置** - 支持定时重置所有任务
- **自动保存** - 玩家数据定时自动保存

### 🎮 任务类型
| 类型 | 说明 | 示例 |
|------|------|------|
| MINE | 挖矿任务 | 挖掘石头、铁矿、钻石等 |
| KILL | 击杀任务 | 击杀僵尸、骷髅、末影人等 |
| FISH | 钓鱼任务 | 钓鳕鱼、鲑鱼、宝藏等 |
| FARM | 农业任务 | 收获小麦、胡萝卜、西瓜等 |
| WALK | 行走任务 | 行走指定距离 |

### 📊 任务池
- **200+ 个任务配置**
- 每种类型多个难度等级
- 奖励金额按难度递增

---

## 📦 安装

### 前置插件
- [Vault](https://www.spigotmc.org/resources/vault.34315/) - 经济系统
- 经济插件（如 EssentialsX、CMI 等）

### 安装步骤
1. 下载 `xgxhtask-1.0.0.jar`
2. 放入服务器 `plugins` 文件夹
3. 安装 Vault 和经济插件
4. 重启服务器

### 支持版本
- Minecraft 1.21 - 1.21.4
- Paper / Spigot 服务端
- Java 21+

---

## 🎮 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/task` | 打开任务菜单 | `xgxh.use` |
| `/task 菜单` | 打开任务菜单 | `xgxh.use` |
| `/task 查看` | 查看任务进度 | `xgxh.use` |
| `/task 领取 <ID>` | 领取任务奖励 | `xgxh.use` |
| `/task 重置` | 重置所有任务 | `xgxh.admin` |
| `/task 重载` | 重载插件配置 | `xgxh.admin` |

**别名：** `/rw`、`/xgxh`

---

## ⚙️ 配置文件

### 基础配置

```yaml
# 允许使用任务的世界
allowed-worlds:
  - "world"
  - "world_nether"
  - "world_the_end"

# 自动保存
auto-save:
  enabled: true
  interval-minutes: 5

# 任务重置
task-reset:
  enabled: false
  interval-minutes: 1440  # 24小时

# 每日任务设置
daily-tasks:
  count: 20              # 每日显示任务数量
  random-enabled: true   # 启用随机任务

# 提醒设置
notifications:
  join-reminder: true        # 加入时提醒
  auto-claim: true           # 自动领取奖励
  task-complete-notify: true # 完成时提醒
  task-complete-sound: true  # 完成时音效
```

### 自定义提示语

```yaml
messages:
  join:
    subtitle: "§6§l     §e§l每日任务状态 §6§l"
    total-tasks: "§7今日共有 §e%total% §7个任务"
    completed: "§7已完成: §a%completed% §7个"
    claimed: "§7已领取: §e%claimed% §7个"
    remaining: "§e§l还有 §c%remaining% §e§l个任务等你完成！"
    menu-help: "§7输入 §e/task 菜单 §7打开任务界面"

  task-complete:
    message: "§a§l✦ 任务完成！§e%task%"
    reward: "§7奖励: §6%reward% 金币"
    auto-claim: "§a§l✓ 奖励已自动领取！§6+%reward% 金币"
```

**可用变量：**
| 变量 | 说明 |
|------|------|
| `%total%` | 任务总数 |
| `%completed%` | 已完成数 |
| `%claimed%` | 已领取数 |
| `%remaining%` | 剩余数 |
| `%unclaimed%` | 待领取数 |
| `%task%` | 任务名称 |
| `%description%` | 任务描述 |
| `%reward%` | 奖励金额 |
| `%progress%` | 当前进度 |
| `%amount%` | 目标数量 |

---

## 🎯 任务配置示例

```yaml
tasks:
  # 挖矿任务
  mine_diamond:
    name: "挖掘钻石"
    description: "挖掘5个钻石矿"
    type: "MINE"
    material: "DIAMOND_ORE"
    amount: 5
    reward: 400.0

  # 击杀任务
  kill_zombie:
    name: "击杀僵尸"
    description: "击杀20只僵尸"
    type: "KILL"
    material: "ZOMBIE"
    amount: 20
    reward: 120.0

  # 钓鱼任务
  fish_cod:
    name: "钓鳕鱼"
    description: "钓上10条鳕鱼"
    type: "FISH"
    material: "COD"
    amount: 10
    reward: 160.0

  # 农业任务
  farm_wheat:
    name: "收获小麦"
    description: "收获32个小麦"
    type: "FARM"
    material: "WHEAT"
    amount: 32
    reward: 80.0

  # 行走任务
  walk_1000:
    name: "长途跋涉"
    description: "行走1000格距离"
    type: "WALK"
    material: "FEATHER"
    amount: 1000
    reward: 120.0
```

---

## 📁 文件结构

```
plugins/
└── xgxhtask/
    ├── config.yml           # 配置文件
    └── playerdata/          # 玩家数据
        ├── <UUID>.yml
        └── ...
```

---

## 🔧 常见问题

### Q: 任务不生效？
A: 检查以下配置：
1. `allowed-worlds` 是否包含当前世界
2. 是否安装了 Vault 和经济插件
3. 检查控制台是否有报错

### Q: 如何修改任务奖励？
A: 编辑 `config.yml` 中的 `reward` 字段，然后执行 `/task reload`

### Q: 如何添加自定义任务？
A: 在 `config.yml` 的 `tasks` 部分添加新任务配置

### Q: 玩家数据存储在哪？
A: `plugins/xgxhtask/playerdata/<玩家UUID>.yml`

---

## 📝 更新日志

### v1.0.0
- 初始发布
- 支持 5 种任务类型
- 200+ 个任务配置
- 每日随机任务
- 自动奖励领取
- 自定义提示语
- 世界限制功能
- 定时重置功能

---

## 📜 许可证

MIT License

---

## 💬 支持

如有问题或建议，请联系作者：**雪糕小豪**
