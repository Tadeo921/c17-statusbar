# 云端替代检查（没有 Android SDK 时）

这些 Linux 脚本用于**没有 Android SDK、没有 PowerShell、连不上 Google Maven（`dl.google.com`）**的环境，例如受限的云端会话。它们是 `tools/dev/compile.ps1` 与 `tools/dev/checks.ps1` 的替代，**不能代替**真实构建：没有 AGP、lintVital、打包、签名、16KB 对齐，也没有手机。每次记录结果时必须写明这是替代环境。

需要：JDK 17 以上、`curl`、`python3`、`unzip`；能访问 Maven Central（`repo1.maven.org`，高峰期会返回 429，脚本自动重试）。Kotlin 检查另需 Gradle，仅用来从 Maven Central 下载 Kotlin 编译器和库。

```bash
tools/dev/cloud/build-java.sh                 # 生产 Java → Java 8 字节码（major 52），并扫描类版本
tools/dev/cloud/run-checks.sh                 # 编译并运行 validation/run-checks.ps1 里的全部桌面检查
tools/dev/cloud/run-checks.sh Upgrade108Check NativeNetworkLifecycleCheck   # 只跑指定检查
C17_GRADLE=/opt/gradle/bin/gradle tools/dev/cloud/kotlin-check.sh           # 全部 Kotlin 源文件的替代类型检查
```

工作目录默认 `${TMPDIR:-/tmp}/c17-cloud`，可用 `C17_CLOUD_WORK` 改；检查从仓库根目录运行（部分检查读取 `app/src/main/...` 与资源）。

## 替代的是什么

| 真实流程 | 替代 | 差别 |
|---|---|---|
| Android SDK 37 的 `android.jar` | Robolectric `android-all` 17（Maven Central） | 带真实框架实现；检查桩在其前面覆盖。检查桩里的 `Resources` 在该 jar 在路径上时会被 javac 编坏，`run-checks.sh` 已单独重编一次。 |
| Gradle `compileReleaseJavaWithJavac` | `javac --release 8` | 同样的 Java 8 字节码，但不是 AGP 的产物。 |
| `compileReleaseKotlin`（AGP 9.4.1 内置 Kotlin） | Kotlin 2.4.20 K2 编译器 + Compose 编译器插件 2.4.20 | Compose 用 JetBrains 1.8.2 desktop jar（真实是 Android 1.12）；`androidx.activity` / `lifecycle` / `core` 与 `ComposeView`、`LocalContext`、`AndroidView`、`dropShadow` 等 Android 专有 API 是 `kotlin-stubs/` 里的仅编译用桩；`WindowInfo.containerDpSize`（Compose 1.12 新增）和 Android `Bitmap.asImageBitmap` 在 `CompileShims.kt`。 |
| `tools/dev/checks.ps1` | `run-checks.sh` | 同一份名单、同一批检查源码；JDK 与框架 jar 不同。 |

能发现：未解析引用、类型错误、可组合函数调用位置错误、Java/Kotlin 互相引用不一致、桌面检查回归。不能发现：真实 Android 运行时行为、界面外观、AGP/lint/打包问题、签名与对齐、手机效果。

## 真实 Gradle 构建（需要 dl.google.com 放行）

```bash
tools/dev/cloud/install-android-sdk.sh        # 下载 cmdline-tools，装 platforms;android-37.0 与 build-tools;36.0.0（已实际运行验证）
tools/dev/cloud/gradle-build.sh               # compileDebugJava + assembleRelease + lintVitalRelease，遇到 Maven Central 429 自动重试（只部分验证，见下）
```

- `gradle-central-routing.init.gradle` 是可选的 Gradle 初始化脚本（`-I`）：把 `com.android.*`、`androidx.*` 等 Google 托管的组直接指向 Google Maven，减少对 Maven Central 的请求；不改仓库文件，也不新增仓库。
- 2026-10-09 在云端会话里实际跑过：`dl.google.com` 放行后 SDK 安装成功，Gradle 9.6.0 与 AGP 9.4.1 的依赖解析被 Maven Central 的 HTTP 429（限流，出口 IP 共用）反复打断，每次仍有约 36 个下载失败，**构建没有走到编译，没有 APK**。这两个脚本因此**没有跑通过完整构建**，只是下一次可以直接复用的起点；在本机 Windows 上请直接用 `tools/dev/build.ps1`。
- 签名：脚本只在设置了 `C17_KEYSTORE`、`C17_STORE_PASSWORD`、`C17_KEY_PASSWORD`、`C17_KEY_ALIAS` 时才签名，仓库里没有也不应有这些文件；不设就得到未签名 APK，可在本机用 `apksigner` 签。
