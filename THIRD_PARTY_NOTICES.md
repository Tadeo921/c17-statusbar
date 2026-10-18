# 第三方声明

## COUI 可选界面组件

COUI 界面使用 [suqi8/coui](https://github.com/suqi8/coui) 的 **1.1.0** 发布组件（Apache License 2.0），它基于 compose-miuix-ui/miuix，按 ColorOS 16 设计校准组件颜色、尺寸和交互。

- Maven 依赖：`io.github.suqi8.coui.kmp:coui-ui-android:1.1.0` 和 `coui-preference-android:1.1.0`。
- 本项目通过 `C17UiAdapter.kt` 调用实际 COUI 组件；Miuix 界面仍可选择。COUI 不是 OPPO 官方 SDK，本项目与 OPPO 无官方关联。
- 完整上游 Apache-2.0 许可和来源声明保留在 APK 的 `assets/coui/`；本项目新增接入代码仍采用 GPL-3.0-only。

## 自定义图标库归档解码

图标库使用 [zhanghai/libarchive-android](https://github.com/zhanghai/libarchive-android) 的官方 Maven AAR `me.zhanghai.android.libarchive:library:1.1.6`，源代码标签 `v1.1.6`（提交 `d3ee9c472173fcaf28e737f59dd34ef6cf3d1c88`）。Java/JNI 接口 Copyright 2023 Google LLC，采用 Apache License 2.0；该项目并非 Google 官方支持产品。

RAR4/RAR5 使用其实际原生只读解码器，ZIP 使用 Java 平台接口；本模块不执行包内代码，不按归档自带路径解压，拒绝加密、分卷、链接和超限资源。Android 原生 RAR 解码与真实视觉效果仍需分别实机验证，桌面 JSON/ZIP 检查不代表这些场景已验收。

该 AAR 内含 libarchive、bzip2、liblzma、lz4、zstd、mbedtls；各自固定源提交、完整许可与版权声明随 APK 放在 `assets/icon-packs/NOTICE.txt` 与 `assets/icon-packs/licenses/`。Android zlib 由系统提供。独立[图标包规范仓库](https://github.com/SANWU5/c17-icon-packs)中的内容按各自所附许可使用；本应用内置C17示例库的图案与生成器采用GPL-3.0-only。用户导入资源保留各自许可，不按模块GPL许可自动重新授权。

本文记录项目当前依赖与历史技术来源。软件、字体和第三方资源分别保留其自身许可，不统一改为本项目的软件许可。

## 控制中心磁贴圆角技术参考

圆角适配参考用户提供的 MCGA `TwoXOneTileHook.kt`，Copyright (C) 2026 Zhuangzhi Meng (Gustate XiaoMeng)，原文件采用 `GPL-3.0-or-later`。本项目 `QsTileCorners.java` 按 `GPL-3.0-only` 分发并保留该来源说明。

针对本机 ColorOS 17 的系统实现，本项目改为各个磁贴实例独立持有原生路径提供者，未移植共享资源池的圆角改写、图标、文本或填充功能。各类磁贴按统一半径应用；本项目另编写 `QsPanelCorners.java` 接入亮度、音量、音乐与设备卡片的原生几何接口。关闭与安全模式恢复各自的原生路径。APK 中的 `assets/qs-corners/NOTICE.txt` 同样保留来源与作者声明。

控制中心适配也参考了 [Maga-King/coloros16-control-center](https://github.com/Maga-King/coloros16-control-center) 的思路。核对时该仓库未提供 LICENSE 文件；本项目没有复制其源代码或资源，具体实现依据当前设备的原生接口独立编写。该参考与上面的 MCGA GPL 技术来源分别记录，不推定其代码采用 GPL 或其他开源许可。

## 通知中心同应用小堆叠功能参考

“同应用小堆叠”（通知栏 → 通知卡片）参考 OplusX「启用小堆叠（同应用）」。感谢 OplusX 作者 xxxxawl 提供实现参考，作者酷安主页：<https://www.coolapk.com/u/18956155>。OplusX 的通知堆叠基础合并自 `VivoGroupDeck-oos16-test21`，该技术来源一并保留。

核对时参考源码包未提供整体 LICENSE 文件。本项目没有复制其源代码，而是按本机 ColorOS 17 的原生接口和本模块的钩子框架以 Java 8 重新实现（`NotificationMiniStack.java`），未移植其 48dp 单条下拉手势、大堆叠和排序功能。鸣谢与许可是两件事，不推定参考代码采用 GPL 或其他开源许可。

## 软件源代码

仓库中的模块源码采用 GNU GPLv3（`GPL-3.0-only`），完整许可证见根目录 `LICENSE`，版权声明见 `COPYRIGHT.md`。

## 内置苹方字体

`app/src/main/assets/fonts/PingFangSC-VF.ttf` 是 Apple 苹方 UI 的精简可变字体重建版本，由 alphaArgon / ACT-02 处理。本仓库仅为状态栏保留所需字符。

字体资源保留其原有条款。其转载与修改依据上游项目 README 中的许可说明，并保留来源和作者标记：

- 上游项目：https://github.com/ACT-02/PingFangUI-VF
- 本地来源说明：`FONT.md` 与 `app/src/main/assets/fonts/SOURCE.txt`

上游允许转载和修改，并要求注明来源。字体的原始设计归 Apple；此字体重建及其分发条款与本仓库的软件许可证分别处理。仓库不声称与 Apple 有官方关联。

## 按需下载的开源可变字体

字体目录包含 Noto Sans SC、Noto Serif SC、Inter、Roboto、Montserrat、Manrope、Open Sans、JetBrains Mono、Nunito、Oswald 共 10 种不同设计，不作为热门排名。前两种自身覆盖简体中文，其余拉丁字体使用系统中文回退。Noto 与 Adobe 思源为同源设计，未重复计为两套字体。

字体与许可文件固定取自官方 [google/fonts 提交 9710da1eacb3be272583c3224dcb70f9da6eadbb](https://github.com/google/fonts/tree/9710da1eacb3be272583c3224dcb70f9da6eadbb)。各字体采用 SIL Open Font License 1.1，保留各自原始版权声明与保留字体名称条款。完整许可随 APK 放在 `assets/fonts/catalog/licenses/`；每款的上游项目、下载 URL、SHA256、字重轴和许可路径记录在 `assets/fonts/catalog/manifest.json`，项目入口汇总见 [字体库说明](docs/FONT-CATALOG.zh-CN.md)。

APK 不内置这 10 个字体文件；首次选择时按需下载，缓存及导入文件存于应用私有目录，不进入配置 JSON、云备份或设备迁移。完整 OFL 许可随目录元数据分发，字体不按本项目的 GPL 软件许可重新授权。

## PUI 电池外观

已有电池矢量资源取自用户提供的 PUI Theme For OPlus 17 模块。资源作者的权利保留，不随本项目软件许可证变更而重新授权；来源与校验见 [PUI电池来源](docs/PUI-battery-evidence.md)。公开版保留已有电池样式，不包含完整的PUI模块。

## C17示例库与PUI图标套装

公开版内置的C17示例库为本项目原创几何图案，图案及生成器采用GPL-3.0-only。资源包内保留README、完整LICENSE及作者声明，源文件见 `tools/icon-management-v3/original/`，生成说明见 [原创资源说明](tools/icon-management-v3/README.zh-CN.md)。这些图案不包含Apple、SF Symbols、OEM或PUI素材。

PUI ColorOS17图案的原作者为天伞桜&PanL。当前公开源码和APK不内置整套PUI图标资源；用户可自行导入有权使用的v3图标包，已导入的兼容图库及原有电池样式不因此停用。PUI兼容映射和转换工具不改变原素材权利，也不构成对该图库的GPL重新授权。

独立iOS27部分测试素材不属于内置C17示例库，也不随本APK提供。用户导入的字体和图标仍按相应来源条款使用；软件许可不替代素材许可。

## 构建与检查依赖

LibXposed API 101 仅作编译依赖，未将其 JAR 打包进 APK。Android 平台类由系统提供。

`validation/test-libs/json-20240303.jar` 仅用于桌面检查，未打包进 APK，其声明保留在 JAR 内。第三方依赖保留各自的许可证与声明。

## 历史液态玻璃方案

早期预览曾使用 [QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android) 的 `v2.0.11` 光学核心，固定上游提交 `fc6bd34fa3290638c3594cc39386f549d1316ab5`，作者 pandadog，采用 MIT License。该方案的Java接入、着色器、后景捕获和软件回退已移除；当前可选液态玻璃底栏采用下文所述的Miuix与Kyant0来源，两者分别记录。

- 上游源文件：`liquidglass/src/main/java/com/example/liquidglass/GlassLensRenderer.kt` 中的 `LENS_AGSL`。
- 完整历史许可与版权声明保存在 `docs/third-party/liquidglass-LICENSE-MIT-history.txt`，仅用于记录早期方案出处。
- 当前 APK 不包含该着色器、光学适配类或未使用的许可资产，不运行相关渲染路径。

私人设备截图、录屏和原始研究材料不随公开源码和APK分发。普通底栏在项目支持的Android版本上按所选界面风格使用Miuix或COUI组件。

## Miuix 设置界面组件

设置界面正式使用 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) 的 `v0.9.4` 发布组件，固定上游提交 `39c40f99844227b853f0049a0933b1f3ae6c00ba`，作者 compose-miuix-ui contributors，采用 Apache License 2.0。

- Maven 依赖：`top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`、`miuix-preference-android:0.9.4`、`miuix-icons-android:0.9.4`、`miuix-blur-android:0.9.4`（可选液态玻璃底栏）。
- 大标题、圆角卡片、偏好设置行、开关、滑杆、文字输入、HSV 颜色选择、弹窗及导航项目均直接调用上游正式 Compose 组件；未把其实现复制为自制原生 View。
- `ModernUiComponents.kt` 是本项目编写的页面接入封装，保持 `GPL-3.0-only`。Miuix 库及其图标保留原有 Apache-2.0 许可与版权。
- `app/src/main/assets/miuix/LICENSE-APACHE2.txt` 保留完整上游 Apache 2.0 许可，`NOTICE.txt` 保留来源和作者声明，两者均随 APK 作为资产分发。

界面使用 Miuix 官方库，并参考连接设备的 LSPosed 管理器布局；交付源码与 APK 不包含设备上的 LSPosed 安装包或私有截图，也未将其反编译代码移入项目。项目最低版本保持 Android 8（API 26）。

### 液态玻璃底栏（可选）

“关于 → 界面设置”的“液态玻璃底栏”依赖 `top.yukonga.miuix.kmp:miuix-blur-android:0.9.4`（要求 API 33，清单以 `tools:overrideLibrary` 覆盖，运行时仅 Android 13 及以上启用）。`app/src/main/java/dev/puitheme/liquid/` 下 7 个文件复制自 Miuix `v0.9.4` 示例（`example/shared/.../component/liquid` 与 `component/animation`），仅改包名和深色模式判断；上游注明改编自 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)，均为 Apache License 2.0，文件头保留原版权与许可。默认开启，保留已保存的关闭值；关闭或低于 Android 13 时使用所选风格的普通底栏。`CouiGlassNavigation.kt` 是本项目新增的GPL-3.0-only布局，调用真实COUI文字／图标，并复用既有blur／Lens／Vibrancy；没有新增第三方依赖或复制新的上游玻璃源文件。省电模式停止背景采样，使用实色承托。

## 界面设置的开源主题

“界面设置 → 开源主题”提供两套外部发布的配色，只用于设置应用自身，不进入 SystemUI。二者均只使用公开发布的颜色数值，未复制任何源码或资源文件。

- **Catppuccin**：[catppuccin/catppuccin](https://github.com/catppuccin/catppuccin)，Copyright (c) 2021 Catppuccin，采用 MIT License。浅色使用 Latte、深色使用 Mocha，强调色取 Mauve；文字取 Text，次要文字取 Subtext，卡片与边线取 Crust、Surface0–2、Overlay0 等官方色板中的颜色。
- **Material 3**：Google Material Design 3 基线配色（<https://m3.material.io/styles/color/static/baseline>），官方设计令牌（design tokens）以 Apache License 2.0 发布。浅色与深色各取其已发布的主色、容器色、表面色、文字色、描边色和错误色数值。

颜色常量在 `app/src/main/java/dev/puitheme/UiThemes.kt`，Catppuccin 的 MIT 许可全文保存在 `docs/third-party/catppuccin-LICENSE-MIT.txt`；本项目的取色映射与组合代码保持 `GPL-3.0-only`。
