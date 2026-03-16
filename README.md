这是面向 `zotero/zotero-android` fork 的目录覆盖包。

目录结构已经对齐到公开仓库的 `app` 模块与已存在的 `org.zotero.android.pdf.reader`、`org.zotero.android.screens.settings` 路径。

建议用法：

1. 将本包内容覆盖到你 fork 的仓库根目录。
2. 检查 `PdfReaderViewModel.kt` 中与 Nutrient/PSPDFKit `PopupToolbarMenuItem` 相关的构造器签名是否与本地 SDK 版本一致。
3. 编译后再提交 PR。

本包新增：

- 阅读器划词翻译入口与结果弹窗
- 翻译设置页
- 本地加密存储翻译凭证
- 百度翻译 / DeepL 服务接入
