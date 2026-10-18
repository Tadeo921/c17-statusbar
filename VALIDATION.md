# 1.8.3 发布验证

日期：2026-10-18。正式身份：**1.8.3 / versionCode 192**；Runtime 为 `c17-runtime-20261018-1.8.3-code192`。配置保持schema48、完整979项／便携966项、JSON1，图标包协议v3；Root附属模块仍为1.7/code8。

| 验证 | 结果 |
| --- | --- |
| Gradle | Debug Java编译、Release打包、lintVitalRelease通过 |
| Java hooks字节码 | Debug及Release共2258个类，均为major52 |
| 桌面基础检查 | 最终260/260组通过 |
| 图库公开包专项 | PublicIconPackageCheck 63项：真实原创图包导入、内置资源可用性、旧库身份映射及不改写配置 |
| 图库运行专项 | IconManagementRuntimeCheck 812项：真实文件、Provider、解码与绘制；双排共同19×16画布及透明度恢复 |
| 自备PUI包额外检查 | 显式提供转换ZIP、provenance与原主题后，PuiBuiltinIconCheck 2364项通过；该素材不在公开源码或APK内，此检查不计入基础260组 |
| 签名 | APK Signature Scheme v2验证通过，与公开1.8.2现用证书一致 |
| APK对齐 | zipalign 16KB核对通过 |
| 安装包内容 | 192版本与Runtime一致，含原创C17示例库，不含整套PUI图标ZIP；11项Root附属资源与已验证开发版逐字节一致 |

首次全量运行有258组通过，另两组新公开素材fixture分别遇到临时目录缺失和抗锯齿边界差异。修正测试素材后，重新编译全部fixture并定向重跑这两组，均通过；共同画布、高度和边界断言保留。生产代码未因这两处测试问题改动。

APK：`c17-statusbar-1.8.3.apk`，24,902,923字节。

SHA-256：`5ccd978bb1a2d2512c5c977c11980a7b3b03101f04325e07da43dcbaa341f534`

现用签名证书SHA-256：`fa5825bd1a0ed867432f71c57bf40ce2445d14f4c961a65edb7157ae040a9966`

构建方法见 [Windows构建](docs/WINDOWS-BUILD.zh-CN.md)，检查入口见 [validation/README.md](validation/README.md)。构建产物、完整日志、签名配置及用户自备素材留在本机，不加入公开源码。

本次发布没有安装或重载用户手机。桌面检查与签名核对不代表所有机型、系统天气权限、系统深浅色、流体云材质及动画观感均已真机验收；实验性排序和下拉壁纸等使用边界见 [完整更新日志](docs/RELEASE-1.8.3.zh-CN.md)。
