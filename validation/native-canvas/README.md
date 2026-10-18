# Android Canvas 诊断入口

这是专用的绘制检查入口，不属于应用界面，也不会修改系统电池服务。用 Android SDK 编译本目录的 ChargeBoltRenderCheck.java 和 app/src/main/java/dev/puitheme/ChargeBolt.java，将 D8 输出的 classes.dex 打包成 jar。通过已授权 ADB 连接把 jar 与输出目录放入 /data/local/tmp 后，用 CLASSPATH 和 app_process 运行 dev.puitheme.ChargeBoltRenderCheck，参数为输出目录。

输出包含真实透明 PNG、各场景逐像素检查及总计数。检查覆盖软件 Canvas；实际 SystemUI 的硬件层、系统窗口边界和其他 ROM 仍需实机观察。

## 大时钟颜色遮罩

code169 按作者要求取消字形背景采样、私有模糊请求和额外字身节点，改为在原有文字绘制中混合颜色。原 `ShadeClockRevealRenderCheck.java` 及其旧后端已移除，不能再按历史说明打包或执行。code166–168 的记录和留底仍保留，它们未完成的背景透出验收不作为当前颜色遮罩的效果证明。

当前纯颜色边界由 `validation/checks-src/dev/puitheme/ClockColorMaskCheck.java` 检查：遮罩颜色 alpha、遮罩透明度和强度相乘，保留原文字 alpha，关闭后精确恢复原色。原生文字动画、玻璃外观和实际显示仍由集成检查与后续手机验收覆盖；本轮本地检查不代表设备帧率或视觉效果已通过。
