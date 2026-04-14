# AirstrikePointers

[English](#english) | [中文](#中文)

---

<a name="english"></a>
## English

A Minecraft Forge mod that adds a laser pointer tool for tactical target marking and navigation in multiplayer environments.

### Overview

This mod introduces the Laser Pointer tool, allowing players to create target markers in the world. The pointer provides a spyglass-like interface where players can aim at blocks or entities to place visible markers for sharing with other players, or to guide firepower.

### Features

#### Laser Pointer Tool
Hold right-click to enter the spyglass view, then release to mark your target. Release while holding Shift to cancel marking. Shift+right-click switches between operational modes. Shift+left-click clears all markers you have placed.

#### Marker Modes

**Point Mode** - Use the pointer to mark individual targets. Targets can be blocks or entities. Markers can track moving entities and update their positions dynamically.

**Path Mode** - Create heading indicators by marking a start point and an end point. The heading marker shows the direction from start to finish, useful for guiding airstrikes.

#### Marker Features
When players join a team, markers automatically adopt team colors for easy identification of friend or foe. Players without teams receive unique colors based on their UUID.

Markers are visible to all players within approximately 600 blocks of the marker location (may be affected by clouds, shaders, and render distance settings).

Includes a configurable guidance system that can guide any entity (arrows, snowballs, or Create Big Cannons shells) toward marked targets. Configure which entities can be guided, guidance range, guidance strength, etc. via the config file.

All markers are fully synchronized across the server, with configurable lifetimes, cooldowns between uses, and automatic cleanup when markers expire. Configuration supports hot-reloading.

### Known Issues

- **Shader Compatibility** - Some shaders may cause markers to become invisible or discolored
- **Cloud Obstruction** - Clouds may obstruct or interfere with marker rendering
- **Water Color Distortion** - Markers viewed through water surfaces may experience color distortion

### Requirements

- Minecraft 1.20.1

---

<a name="中文"></a>
## 中文

一个 Minecraft Forge 模组，添加了激光指示器工具，用于多人环境中的战术目标标记和导航。

### 概述

本模组引入了激光指示器这一工具，允许玩家在世界中创建目标指示标记。指示器提供了类似望远镜的功能，玩家可以瞄准方块或实体来放置可见标记，以向其他玩家分享，或者导引火力。

### 功能

#### 激光指示器工具
使用激光指示器时，按住右键进入望远镜视角，松开即可标记目标。在按住Shift的情况下松开右键可以取消标记。Shift+右键可在操作模式之间切换。Shift+左键可以清除自己标记的所有目标。

#### 标记模式

**点模式** - 使用指示器标记单个目标。目标可以是方块或实体。标记可以跟踪移动实体并动态更新位置。

**路径模式** - 通过标记起点和终点创建航向指示。航向标记指示从起点到终点的方向，适用于引导对面打击空袭。

#### 标记功能
当玩家加入队伍时，标记会自动采用队伍颜色，方便一眼识别友军和敌军。未加入队伍的玩家会根据其 UUID 获得唯一颜色。

标记会对标记位置约 600 格范围内的所有玩家显示（可能受云，光影和视距设置影响）。

包含可配置的制导系统，可以将任何实体（箭矢、雪球、或来自机械动力火炮的炮弹）引导到标记目标。可通过配置文件设置哪些实体可以被制导，制导范围，制导力度等。

所有标记在服务器上完全同步，具有可配置的生命周期、使用冷却时间，标记过期时自动清理。配置支持热重载。

### 已知问题

- **着色器兼容性** - 某些光影可能导致标记不可见或变色
- **云层遮挡** - 云层可能遮挡或干扰标记渲染
- **水面颜色失真** - 透过水面查看的标记可能会出现变色

### 要求

- Minecraft 1.20.1

---

**P.S.** Well, this project indeed made heavy use of Vibe Coding, since I lack the ability to complete this mod on my own ———— but at least it performs well under limited testing... I guess, hopefully.

**附注：** 呃，这个项目确实大量使用了 Vibe Coding，因为我缺乏独自完成这个 mod 的能力 ———— 但至少在有限的测试下表现良好... 大概吧，希望是这样。
