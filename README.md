# MoreInformation

Minecraft 1.21.1 NeoForge 客户端 HUD。默认只显示 TPS / MSPT / RAM 和玩家信息，其余行和模组兼容可在配置里打开。

## 默认显示

- TPS、MSPT、内存
- 玩家名、FPS、现实时间、延迟图标

## 可开启的原版信息

- Minecraft 日历时间
- 维度和已加载区块
- 生物群系、天气
- 实体数量
- 坐标和朝向
- 光照

## 配置

配置文件：`config/moreinformation-client.toml`

- `hud.position`：`top_left`、`middle_left`、`bottom_left`、`top_right`、`middle_right`、`bottom_right`
- `hud.offsetX` / `hud.offsetY`：相对锚点的像素偏移
- `hud.templates`：每行格式，可用 `{占位符}` 和 `&` 颜色代码（`&a` 绿、`&e` 黄、`&c` 红、`&r` 重置，`&&` 表示普通 `&`）
- 重置某一项会恢复当前语言的默认格式

## 模组兼容

安装对应模组后，在 `integrations` 里打开开关即可。未安装时不会报错。

| 模组 | 开关 | 显示内容 |
| --- | --- | --- |
| 节气 Ecliptic Seasons | `showEclipticSeasons` | 季节、节气 |
| 群峦传说 TerraFirmaCraft | `showTerraFirmaCraft` | 降雨量、温度 |
| L2Hostility | `showL2Hostility` | 玩家 / 区块 / 最大恶意 |
| 等价交换 ProjectE | `showProjectE` | 玩家 EMC。大数用单位，按住 Shift 显示完整数值 |
| 自然指南针 Nature's Compass | `showNaturesCompass` | 搜索群系时显示状态，并隐藏原 HUD |
| 探险家指南针 Explorer's Compass / Edited | `showExplorersCompass` | 搜索结构时显示状态，并隐藏原 HUD |
| 自然灵气 Nature's Aura | `showNaturesAura` | 当前位置周围的灵气强度 |

自然指南针和探险家指南针默认开启；其余兼容默认关闭。总开关为 `integrations.enabled`。

## 给其他模组

可用 `HudInfoRegistry.register(...)` 注册自定义 HUD 行。也可把 provider id 加进 `integrations.enabledProviders`。
