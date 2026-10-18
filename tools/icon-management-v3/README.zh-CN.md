# C17示例库 原创资源

本目录是原创几何源、VectorDrawable XML 母版与可复现制作脚本，不包含 Apple、SF Symbols、OEM、PUI 或下载图标集素材。通用功能符号由脚本中的路径、圆、圆角矩形及贝塞尔曲线绘制。使用项目相同的 GPL-3.0-only 许可，安装包内附完整许可证。

在项目根目录运行：

```powershell
python tools/icon-management-v3/generate_original.py
```

需要 Python 3、Pillow 和 JDK 17+。JDK 从 `C17_JDK`、`JAVA_HOME` 或 `PATH` 读取；脚本也支持项目现用 Windows JDK 默认位置。无需联网、图形编辑器、OEM APK 或手机。

脚本即时编译并运行生产 `IconRoleRegistry`，只用独立的 `NativeStatusIcons` 名称列表桩避免依赖 Android。每个已登记角色都必须有明确原创图形；新增目标缺几何时整次生成失败，并列出缺项，绝不提供通用占位图。`roles.json` 是本次生成清单，不能替代原生适配能力或手机视觉验收记录。

- `original/*.xml`：通常为24×24矢量母版；双排信号两个系列共同使用19×16画布。
- `original/*.json`：三个400ms纯图形 Lottie 切换样例；不表示宿主已支持播放。
- `app/src/main/assets/icon-management-v3/c17-rounded-27.zip`：离线 XML 主包，只在用户安装入库并选择范围后应用。
- `build/c17-dev/icon-management-v3-original/png96/`：同一几何的96px透明 PNG。
- 同一忽略目录下 `c17-rounded-27-png.zip`、`contact-sheet.png`、两份 manifest 与 `report.json`：备用包、浅深色18/24/36px接触表及摘要。

ZIP 条目顺序、时间、属性与 JSON 排序固定；同一生产角色表和脚本可重复生成相同 ZIP。静态素材统一 `render=mask`，颜色和透明度来自实际宿主及用户独立设置。电池普通/充电两系列分别提供完整档位，但都不内嵌数字或闪电，避免与原生重复绘制。动作入口仅提供 `default`；开关的 `off` 有斜线、`on` 保留主图形、`unavailable` 有独立不可用徽记，区别不依赖颜色。

双排信号新增 `cellular.stacked_primary`（上柱）与 `cellular.stacked_secondary`（下点），各有`none/0/1/2/3/4`。两行都保存完整19×16透明画布，PNG为114×96；叠合时保持原位置，不分别裁边撑满高度。亮格实心、余格空心，无服务在该行加斜线。单排`cellular.*`仍使用原有图形，不能借用另一行或另一个信号档位。含这些新角色的189静态角色示例包要求beta96/code189；旧beta94/beta95会将其视为已知蜂窝目标的非法状态，不能导入新版示例包。

生成阶段使用真实生产 `IconManagementManifest` / SHA-256 / `IconAnimationAsset` 准入，并检查 PNG 非空且具有透明背景。XML 和 PNG 从同一份路径数据导出；接触表用 Pillow 栅格化这些路径。后端还应对实际归档运行 `IconManagementRepository.prepare`，以覆盖生产 XML/PNG 解码和归档合同。生成成功不等于 Android 绘制效果或动画播放通过。

三个动画为 `tile.flashlight.off_to_on`、`tile.dark.off_to_on`、`tile.rotation.off_to_on`：关闭斜线淡出、主字形作6%短脉冲，并回到精确静态末态；不循环，不反播，没有外部图片、字体或表达式。原生动态能力没有通过前仍按真实支持状态回退静态，不能将资源准入称为动态验收。

当前只覆盖固定登记的角色。媒体自定义动作、品牌/远端设备图片、未登记的二级页图标等保留原生；不要将当前清单写成“整个控制中心已完成”。扩展时先核对真实身份/语义，再添加对应绘图函数，重跑脚本和生产归档校验。
