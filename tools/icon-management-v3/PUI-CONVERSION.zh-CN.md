# 用户自备 PUI 主题的本地转换

公开版不包含 `pui-coloros17.zip`。PUI 原图作者为 **天伞桜&PanL**；转换代码的 GPL-3.0-only 许可不改变原素材权利。请使用有权使用的本地主题，不把转换产物当作 C17 原创图集再分发。

转换器支持 `PUI Theme For ColorOS 17_v17.0.0.137.zip`，源文件 SHA-256 为 `0dabda20b8d1d47643aa4c4198c68c6fa2bc4a8bb68deee2d11b3f446c9317a3`。它通过 Android SDK 的 `aapt2 dump resources/xmltree` 读取资源表与二进制 XML，不安装 APK、不加载 DEX、不运行主题脚本。

```powershell
python tools/icon-management-v3/convert_pui.py '<本地主题目录>/PUI Theme For ColorOS 17_v17.0.0.137.zip' --aapt2 '<Android SDK>/build-tools/36.0.0/aapt2.exe'
```

工具生成的图包仅用于本地导入与检查。`pui-provenance.json` 保存来源文件名、摘要和角色映射，不含原图。完整源文件校验需要另行提供对应主题；公开检查使用原创及合成素材验证通用导入、选择、双排画布、颜色与恢复逻辑，不把缺少私有主题时的跳过算作原图核验成功。

转换结果为103个正式语义别名与181个原始候选，共284条资源记录；多条记录可能共享同一原图。保持原路径、透明度、描边和共同画布，缺失状态保留系统原生效果，不借用其他图补齐。控制中心映射遵循[真实状态合同](../../docs/ICON-V3-QS-STATE-CONTRACT.zh-CN.md)。双排信号保留19×16共同画布，不分别裁边拉满。

应用内支持用户导入的 v3 图包，`PuiIconAliases` 为身份、来源和摘要均吻合的旧库提供映射兼容；它不重写内容 ID、图案文件或用户选择。导入和套装应用仍是两个独立步骤，预览取消不改配置。
