# Windows 构建

当前版本采用 Compose 设置界面与 Java SystemUI hooks。使用 JDK 17、Android SDK 37.0、Build Tools 36.0.0、Gradle 9.6.0；AGP 和 Kotlin 插件版本由根构建文件固定。

```powershell
$env:C17_JDK = '<JDK 17目录>'
$env:C17_SDK = '<Android SDK目录>'
$env:C17_GRADLE = '<Gradle目录>/bin/gradle.bat'
& tools/dev/build.ps1 -Code 192 -Version '1.8.3'
& tools/dev/checks.ps1
```

也可直接运行项目 Gradle Wrapper。首次构建需要下载依赖；全部依赖已有缓存时可设 `C17_OFFLINE=1`。脚本临时写入 SDK 路径，执行结束后恢复原 `local.properties`。

Release 签名由维护者在本地通过 `C17_KEYSTORE`、`C17_STORE_PASSWORD`、`C17_KEY_PASSWORD`、`C17_KEY_ALIAS` 提供，也可放入被忽略的 `.local-private/signing.properties`。私钥和密码不得提交。第三方自行构建使用自己的签名，无法覆盖安装官方同包名版本。

构建脚本执行 Debug Java 编译、Release 打包、`lintVitalRelease`、Java hooks 的 major52、APK 签名、16KB 对齐和版本身份检查；正式产物在 `artifacts/1.8.3-code192/`。完整桌面检查另外运行，结果参见 [验证说明](../VALIDATION.md)。历史 `Build-Standalone.ps1` 不用于构建当前 Compose 正式版。

公开版内置原创 C17 示例库，不包含用户私有 PUI 图包。用户仍可在模块内导入符合 v3 规范的图包；自行准备的本地资产不应进入公开源码或安装包。

`tools/signing/c17-public-to-current.lineage` 是历史签名轮换的公开证明，不含私钥。当前正式1.8.2与1.8.3使用相同现用证书；需要处理更旧签名时，参考 `tools/Sign-WithLineage.ps1` 并在本地提供对应旧/新密钥。密钥缺失不能用公开 lineage 文件代替。
