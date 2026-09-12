# MoreInformation

Minecraft 1.21.1 NeoForge 客户端信息 HUD，当前版本 1.0.2。默认显示 TPS / MSPT / RAM 和玩家信息，其余行和模组兼容可在配置里打开。

[English](README_EN.md)

## 默认显示

- TPS、MSPT、内存
- 玩家名、FPS、现实时间、延迟图标

## 可开启的原版信息

日历与 main 保持一致：新世界显示 `0 年 0 日`，每 24,000 DayTime tick 进入下一日；睡觉后随原版 DayTime 推进。

- Minecraft 日历时间与月相合并显示，八种月相支持中英翻译
- 维度和已加载区块
- 生物群系、天气
- 实体、敌对生物、渲染数量按此顺序合并显示
- 坐标和朝向，以及主世界/下界对应的 XYZ：仅 X/Z 按 8:1 换算，Y 保留当前高度；原点附近也持续显示，负坐标向下取整。换算结果不代表实际传送门落点，其他维度不显示对应坐标
- 光照
- 状态效果：HUD 最上方用物品图标 + 名称 + 剩余时间显示，同时隐藏原版的效果图标

## 配置

配置文件：`config/moreinformation-client.toml`

- `hud.position`：`top_left`、`middle_left`、`bottom_left`、`top_right`、`middle_right`、`bottom_right`
- `hud.offsetX` / `hud.offsetY`：相对锚点的像素偏移
- `hud.scale`：HUD 缩放倍数，默认 `1.0`，范围 `0.25` 到 `4.0`
  内容超出屏幕时会自动缩小，以保持完整可见。
- `hud.effectsIconScale`：状态效果图标大小倍数，默认 `1.0`，范围 `0.5` 到 `3.0`
- `hud.templates`：每行格式，可用 `{占位符}` 和 `&` 颜色代码（`&a` 绿、`&e` 黄、`&c` 红、`&r` 重置，`&&` 表示普通 `&`）
- 重置某一项会恢复当前语言的默认格式

状态效果开在 `hud.showEffects`，从 HUD 左边缘开始排列；每项左侧显示图标，右侧分两行显示名称（含等级）和剩余时间。容器界面打开时保留原版效果列表。实体与敌对生物合并显示，MC 时间与月相合并显示；旧的 `showHostile` 和 `showMoon` 开关仍可开启对应合并行。

- 状态效果最多每行 3 项、共 2 行，小屏幕自动减少列数；超出显示 `+N`，打开背包查看完整效果。
- 有害效果优先，其余按固定顺序排列；长名称省略，倒计时保留。
- 开启状态效果显示后，固定预留两行药水区和溢出提示空间；没有效果或打开容器时仍保留，信息位置不随效果增减跳动。关闭 `hud.showEffects` 后取消预留。
- 默认图标倍率下预留高度为 44 个 HUD 逻辑像素，随整体缩放变化；调整 `hud.effectsIconScale` 时预留高度同步调整。
- 关闭 HUD 或打开调试界面时，不会继续隐藏原版效果图标。
- 实体模板支持 `{entities}`、`{hostile}`、`{rendered}`；时间模板支持 `{year}`、`{day}`、`{time}`、`{period}`、`{phase}`。

## 模组兼容

安装对应模组后，在 `integrations` 里打开开关即可。未安装时不会报错。

| 模组 | 开关 | 显示内容 |
| --- | --- | --- |
| 节气 Ecliptic Seasons | `showEclipticSeasons` | 季节、节气 |
| 群峦传说 TerraFirmaCraft | `showTerraFirmaCraft` | 降雨量、温度 |
| L2Hostility | `showL2Hostility` | 玩家 / 区块 / 最大恶意 |
| 等价交换 ProjectE | `showProjectE` | 玩家 EMC。大数用单位，按住 Shift 显示完整数值 |
| 自然指南针 Nature's Compass | `showNaturesCompass` | 搜索群系时显示状态，并隐藏原 HUD |
| 探险家指南针 Explorer's Compass | `showExplorersCompass` | 搜索结构时显示状态，并隐藏原 Overlay |
| 探险家指南针修改版 ExplorersCompassEdited | `showExplorersCompass` | 同上 |
| 自然灵气 Nature's Aura | `showNaturesAura` | 当前位置周围的灵气强度（与灵气目镜同一算法） |

自然指南针和探险家指南针默认开启；其余兼容默认关闭。总开关为 `integrations.enabled`。

其他模组通过 `HudInfoRegistry` 注册的信息默认显示，无需额外填写 provider id；仍受 `integrations.enabled` 总开关控制。

## 开发与验证

使用 JDK 21 运行 `gradlew.bat test build`。产物位于 `build/libs/`。

本分支使用 Minecraft 1.21.1 / NeoForge，不需要 main 分支的 Forge 1.20.1 本地编译依赖。兼容信息通过反射读取；集成实测需要安装对应的 NeoForge 模组和前置依赖。
