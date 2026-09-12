# MoreInformation 1.0.2

Minecraft 1.21.1 / NeoForge

同步来源 / Ported from: main `33b22ef`，另含固定药水区布局修复。

## 中文

- 开启状态效果时固定预留两行及溢出空间，避免效果出现、消失或换行时信息区跳动；容器打开时保留占位。
- 默认预留高度为 44 个 HUD 逻辑像素，随图标倍率调整；关闭状态效果显示后取消预留。
- 同步零基日历：新世界显示 0 年 0 日，并随原版 DayTime 推进。
- 新增 HUD 整体缩放与状态效果图标缩放设置。
- 新增紧凑状态效果区：左侧图标、右侧名称与倒计时，从 HUD 左边缘排列。最多两行、六项，小屏幕自动减少列数，超出显示 `+N`。
- 有害效果优先显示，排序稳定；长名称自动省略。药水区为下方信息预留空间，避免重叠、顶部裁切及效果过多导致整块 HUD 缩小。
- 实体统计合并为“实体、敌对生物、渲染数量”。
- 月相与 Minecraft 时间合并显示，补齐八种月相的中英翻译。
- 新增主世界/下界 XYZ 对应坐标，仅 X/Z 按 8:1 换算，Y 保留当前高度。修复原点附近坐标消失和负坐标取整偏差。
- 关闭 HUD 时保留原版状态效果图标，容器界面使用原版效果列表。
- 保持原有第三方 HUD provider 默认启用机制，兼容旧版月相与敌对生物开关。
- 更新双语说明并补充回归测试。

## English

- Reserve two effect rows and overflow space while effects are enabled, including empty and container states, so effect changes do not move the information block.
- Reserve 44 HUD logical pixels at the default icon scale; adjust with icon scaling and release the space when effects are disabled.
- Match the zero-based calendar: new worlds start at year 0, day 0 and follow vanilla DayTime.
- Added overall HUD scaling and a separate status-effect icon scale.
- Added a compact, left-aligned effect area with icons beside names and timers. Displays up to six effects across two rows, uses fewer columns on narrow screens, and shows `+N` for overflow.
- Harmful effects appear first with stable ordering; long names are shortened. Reserved space prevents overlap and top-edge clipping, and additional effects no longer shrink the entire HUD.
- Combined entity, hostile mob and rendered counts into one line.
- Combined moon phase with Minecraft time and added English and Chinese names for all eight phases.
- Added corresponding overworld/nether XYZ coordinates. Only X/Z use the 8:1 conversion; Y retains the current height. Fixed disappearing coordinates near the origin and incorrect rounding for negative coordinates.
- Vanilla effect icons remain available when the HUD is disabled; container screens retain the vanilla effect list.
- Preserved default-enabled third-party HUD providers and compatibility with legacy moon and hostile-mob switches.
- Updated bilingual documentation and added regression tests.
