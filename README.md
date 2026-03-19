# AirstrikePointers

[English](#english) | [中文](#中文)

---

<a name="english"></a>
## English

A Minecraft Forge mod that adds a laser pointer tool for tactical target marking and navigation in multiplayer environments.

### Overview

AirstrikePointers introduces a **Laser Pointer** item that allows players to mark targets and create navigation waypoints in the world. Using a spyglass-inspired interface, players can aim at locations, blocks, or entities to place visible markers that help coordinate team strategies and call out points of interest during gameplay.

### Features

#### Laser Pointer Tool
The laser pointer is used by holding right-click to enter a spyglass-like view, then releasing to mark your target. Shift+right-click switches between operational modes.

#### Three Operational Modes

**Point Mode** - Mark individual targets with colored indicators. Targets can be blocks, entities, or distant locations. Each marker displays the owner's name and can be viewed by all players within range.

**Path Mode** - Create directional waypoints by marking a start point and an end point. The path marker displays a heading indicator showing the direction from start to finish, useful for navigation and coordinating movement.

**Clear Mode** - Quickly remove all markers you have placed in the world.

#### Team-Aware Visualization
Markers automatically adopt team colors when players are on teams, making it easy to identify friend from foe at a glance. Players without teams receive unique colors based on their UUID to ensure distinguishability.

#### Visibility
Markers are rendered for all players within approximately 600 blocks of the marker location, allowing for long-range coordination across large distances.

#### Multiplayer Synchronization
All markers are fully synchronized across the server, with configurable lifetimes and automatic cleanup when markers expire.

### Known Issues

- **Shader Compatibility** - Some shader packs may cause rendering issues with marker visibility or colors
- **Cloud Obstruction** - Clouds may occasionally obstruct or interfere with marker rendering
- **Water Color Distortion** - Markers viewed through water surfaces may experience color distortion

### Requirements

- Minecraft 1.20.1

---

<a name="中文"></a>
## 中文

一个 Minecraft Forge 模组，添加了激光指示器工具，用于多人环境中的战术目标标记和导航。

### 概述

AirstrikePointers 引入了一个**激光指示器**物品，允许玩家标记目标并在世界中创建导航路标。使用类似望远镜的界面，玩家可以瞄准位置、方块或实体来放置可见标记，帮助协调团队策略并在游戏过程中标注兴趣点。

### 功能

#### 激光指示器工具
使用激光指示器时，按住右键进入望远镜视角，松开即可标记目标。Shift+右键可在操作模式之间切换。

#### 三种操作模式

**点模式** - 使用彩色指示器标记单个目标。目标可以是方块、实体或远处位置。每个标记显示所有者名称，范围内的所有玩家均可查看。

**路径模式** - 通过标记起点和终点创建定向路标。路径标记显示方向指示器，指示从起点到终点的方向，适用于导航和协调移动。

**清除模式** - 快速移除你在世界中放置的所有标记。

#### 队伍感知可视化
当玩家加入队伍时，标记会自动采用队伍颜色，方便一眼识别友军和敌军。未加入队伍的玩家会根据其 UUID 获得唯一颜色，确保可区分性。

#### 可见性
标记会对标记位置约 600 格范围内的所有玩家显示，支持大范围内的远程协调。

#### 多人同步
所有标记在服务器上完全同步，具有可配置的生命周期，标记过期时自动清理。

### 已知问题

- **着色器兼容性** - 某些着色器包可能导致标记可见性或颜色的渲染问题
- **云层遮挡** - 云层可能偶尔遮挡或干扰标记渲染
- **水面颜色失真** - 透过水面查看的标记可能会出现颜色失真

### 要求

- Minecraft 1.20.1

---

**P.S.** Well, this project indeed made heavy use of Vibe Coding, since I lack the ability to complete this mod on my own ———— but at least it performs well under limited testing... I guess, hopefully.

**附注：** 呃，这个项目确实大量使用了 Vibe Coding，因为我缺乏独自完成这个 mod 的能力 ———— 但至少在有限的测试下表现良好... 大概吧，希望是这样。
