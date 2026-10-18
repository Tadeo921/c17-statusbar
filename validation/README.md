# 桌面检查

在仓库根目录运行，使用 JDK 17、Android SDK 37.0、Build Tools 36.0.0 与项目 Gradle 工具链：

```powershell
& tools/dev/compile.ps1
& tools/dev/checks.ps1
```

定向检查示例：

```powershell
& tools/dev/checks.ps1 -Names 'IconManagementRuntimeCheck','SystemWeatherProviderCheck'
```

`tools/dev/checks.ps1` 和 `run-checks.ps1` 共用 `check-runtime-dependencies.ps1`，从真实 AndroidX core 1.18.0 AAR 提取 `classes.jar`，用于生产 `PathParser`；也可通过 `C17_ANDROIDX_CORE_JAR` 指定该依赖。缺少缓存时先构建 App。测试 Android 图形 fixture 先于此库加载，不替换矢量路径解析器。

公开版以原创 C17 图包与合成素材覆盖通用逻辑，不包含私有 PUI 图包；需要原始主题的来源检查须显式提供本地素材。跳过私有来源核对不等于该项验收通过。

桌面检查覆盖配置、导入、绘制、生命周期和恢复分支，不代表 Android Skia、OEM 动画或真机观感已全部验收。当前发布结果见根目录 [VALIDATION.md](../VALIDATION.md)。
