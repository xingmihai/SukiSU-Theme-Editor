# SukiSU 主题编辑器（SukiSU Theme Editor）

一个用 **Material 3 / M3 Expressive** 设计语言写的 Android 端 `.sukisu` 主题包编辑器，
面向 Android 17 设备（以 API 36 构建，Android 应用向前兼容，已适配 edge-to-edge 与预测式返回）。

导入、编辑、重新导出 SukiSU-Ultra 管理器使用的主题包，不依赖 root、不依赖管理器本身。

## 功能

- **导入 / 导出 `.sukisu`**：通过系统文件选择器（SAF），也支持从文件管理器直接「打开方式」。
- **完整 `theme.json` 编辑**：按分组覆盖 `ThemePackageData` 的全部字段，未知字段原样保留（与管理器 `ignoreUnknownKeys` 行为一致）。
- **高级 JSON 区块**：`appearance` 叠加层、`light` / `dark` 自适应变体、`customTexts`、`homeCardStickerTransforms` 以原始 JSON 编辑，保证新版本字段不被丢弃。
- **图片管理**：背景、卡片背景、贴纸，以及自适应主题的亮/暗两套图片槽；支持自定义条目名。
- **格式 v1–v4**：版本切换、自适应与叠加层的版本约束校验、限额校验（128 MB 整包 / 64 KB JSON / 64 MB 单图 / 256 MB 图片总量）、重复条目与非法条目名检查。
- **包内容查看**：列出 ZIP 条目、大小、压缩方式，可复制格式化后的 `theme.json`。

## .sukisu 格式

不是普通 ZIP，而是一个加密容器：

```
offset 0   : b"SKTH"                      magic (4 B)
offset 4   : 随机 IV                       (12 B)
offset 16  : AES-256-GCM(plaintext ZIP)   (末尾 16 B 为 GCM tag)
```

- 密钥：SukiSU-Ultra 管理器 `ThemePackageCrypto.kt` 中硬编码 48 字节 `HASH_BYTES` 的前 32 字节。
  这是**混淆与完整性保护，不是版权保护**，密钥随 APK 分发。
- 明文是一个 ZIP：首个条目固定 `theme.json`（UTF-8 JSON），其余为无扩展名的图片原始字节。
  条目为 DEFLATED / level 0（`setLevel(Deflater.NO_COMPRESSION)`，未调用 `setMethod(STORED)`）。
- 条目名：普通主题用 `background`、`card_background`、`card_sticker_0…`；
  自适应主题（v3+）用 `background_light` / `background_dark`、
  `card_background_light` / `card_background_dark`、`card_sticker_light_0…` / `card_sticker_dark_0…`。
- `theme.json` 版本：`v2` 基础、`v3` 自适应（`light` + `dark`）、`v4` 增加 `appearance` 叠加层。
  旧版管理器会拒绝过高版本，不会「只应用一半主题」。

格式实现参考 SukiSU-Ultra 管理器源码，本仓库为独立第三方工具，与 SukiSU-Ultra 项目无隶属关系。

## UI

按 [Material Design 3](https://m3.material.io/) / M3 Expressive 的表达性设计原则构建：

- Android 12+ 动态取色（`dynamicLightColorScheme` / `dynamicDarkColorScheme`），低版本回退到 M3 基线色板
- 对比鲜明的形状：28–36 dp 大圆角卡片，展开时通过 `animateDpAsState` 做形状过渡
- `LargeTopAppBar`、`SingleChoiceSegmentedButtonRow`、`FilterChip`、`FilledTonalButton`、
  `ExtendedFloatingActionButton`、`NavigationBar`、`Snackbar`
- `AnimatedVisibility` 折叠动效，Compose Material 3 稳定版（BOM 2024.12.01）

## 构建

```sh
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`（debug 签名，可直接安装）。

要求：JDK 17、Android SDK Platform 36、Build Tools 35。用 Android Studio 打开即可，
仓库已包含标准 Gradle 8.11.1 wrapper。

### GitHub Actions

`.github/workflows/build-apk.yml` 在 `main` 分支 push、PR、`v*` tag 和手动触发时自动构建，
上传 APK 到 Actions Artifacts；打 `v*` tag 时额外创建 Release 并附加 APK。

## 使用注意

- 导出的包请先在管理器里试导入；改坏的包不会变砖，但可能被拒绝。
- 主题包里的背景图建议 WebP/PNG，管理器用 `BitmapFactory` 校验尺寸。
- 若启用了 `CONFIG_KSU_SUSFS_SPOOF_CMDLINE_OR_BOOTCONFIG` 一类的伪装，
  `/proc/cmdline` 显示值不可作为校验依据。

## 许可

MIT，见 [LICENSE](LICENSE)。
