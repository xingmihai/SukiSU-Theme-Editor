package com.xingmihai.sukisu.themeeditor.theme

/**
 * Field catalogue for SukiSU-Ultra `ThemePackageData` / `ThemeVariantData`.
 * Values are stored back into the raw JSONObject so unknown fields from newer
 * manager builds survive an edit-and-export round trip.
 */

data class Choice(val label: String, val value: Any?)

enum class Kind {
    BOOL, INT, FLOAT, COLOR, STRING, CHOICE, PERCENT
}

data class FieldSpec(
    val key: String,
    val label: String,
    val kind: Kind,
    val hint: String = "",
    val choices: List<Choice> = emptyList(),
    val min: Float = 0f,
    val max: Float = 100f,
)

data class GroupSpec(
    val title: String,
    val subtitle: String,
    val fields: List<FieldSpec>,
)

data class JsonBlock(
    val key: String,
    val title: String,
    val subtitle: String,
    val isArray: Boolean = false,
)

data class ImageSlot(val name: String, val label: String, val group: String)

private fun ch(vararg items: Choice) = items.toList()

private val COLOR_STYLES = listOf(
    "TONAL_SPOT", "NEUTRAL", "VIBRANT", "EXPRESSIVE", "RAINBOW",
    "FRUIT_SALAD", "MONOCHROME", "FIDELITY", "CONTENT", "CUSTOM",
)

val THEME_GROUPS: List<GroupSpec> = listOf(
    GroupSpec("格式与颜色", "版本、主题模式、取色与 UI 风格", listOf(
        FieldSpec("version", "格式版本", Kind.CHOICE,
            "v1–v4；v3 起支持自适应，v4 起支持 appearance 叠加层",
            ch(Choice("未设置", null), Choice("v1", 1), Choice("v2", 2),
                Choice("v3", 3), Choice("v4", 4))),
        FieldSpec("themeMode", "主题模式", Kind.CHOICE,
            choices = ch(Choice("未设置", null), Choice("跟随系统", 0),
                Choice("浅色", 1), Choice("深色", 2))),
        FieldSpec("miuixMonet", "Miuix Monet 取色", Kind.BOOL),
        FieldSpec("keyColor", "关键色 (ARGB)", Kind.COLOR, "例如 #FF6750A4 或 #6750A4"),
        FieldSpec("colorStyle", "配色风格", Kind.CHOICE, "M3 变体名",
            ch(Choice("未设置", null)) + COLOR_STYLES.map { Choice(it, it) }),
        FieldSpec("colorSpec", "配色规范", Kind.CHOICE,
            choices = ch(Choice("未设置", null), Choice("2021", "2021"),
                Choice("2025", "2025"), Choice("default", "default"))),
        FieldSpec("uiMode", "界面模式", Kind.CHOICE,
            choices = ch(Choice("未设置", null), Choice("MIUIX", "MIUIX"),
                Choice("MATERIAL", "MATERIAL"))),
        FieldSpec("appIconId", "应用图标 ID", Kind.STRING),
        FieldSpec("adaptive", "自适应主题 (亮/暗双套)", Kind.BOOL, "需要格式 v3 及以上"),
    )),
    GroupSpec("背景与卡片", "图片标志、透明度、模糊与首页卡片", listOf(
        FieldSpec("hasBackground", "包含背景图", Kind.BOOL),
        FieldSpec("hasCardBackground", "包含卡片背景图", Kind.BOOL),
        FieldSpec("cardBackgroundAlphaPercent", "卡片背景不透明度 %", Kind.PERCENT,
            min = 0f, max = 100f),
        FieldSpec("customBackgroundBlurPercent", "背景模糊 %", Kind.PERCENT,
            min = 0f, max = 100f),
        FieldSpec("customBackgroundScrimPercent", "背景遮罩 %", Kind.PERCENT,
            min = 0f, max = 100f),
        FieldSpec("hideHomeCardStatusText", "隐藏首页状态文字", Kind.BOOL),
        FieldSpec("hideHomeCardDecoration", "隐藏首页装饰", Kind.BOOL),
    )),
    GroupSpec("界面与交互", "底栏、导航、手势与列表", listOf(
        FieldSpec("enableFloatingBottomBar", "悬浮底栏", Kind.BOOL),
        FieldSpec("enableFloatingBottomBarBlur", "悬浮底栏模糊", Kind.BOOL),
        FieldSpec("enableNavigationBadge", "导航角标", Kind.BOOL),
        FieldSpec("enablePredictiveBack", "预测式返回", Kind.BOOL),
        FieldSpec("enableSwipeDismiss", "滑动关闭", Kind.BOOL),
        FieldSpec("enableBlur", "全局模糊", Kind.BOOL),
        FieldSpec("pagerInterceptionMode", "翻页拦截模式", Kind.CHOICE,
            choices = ch(Choice("未设置", null), Choice("0", 0), Choice("1", 1), Choice("2", 2))),
        FieldSpec("pageScale", "页面缩放", Kind.FLOAT, min = 0.5f, max = 1.5f),
        FieldSpec("moduleDescriptionMaxLines", "模块描述最大行数", Kind.INT),
    )),
    GroupSpec("模块与文本", "模块排序、自定义标题与文案", listOf(
        FieldSpec("moduleSortEnabledFirst", "已启用优先", Kind.BOOL),
        FieldSpec("moduleSortActionFirst", "Action 优先", Kind.BOOL),
        FieldSpec("moduleSortWebUiFirst", "WebUI 优先", Kind.BOOL),
        FieldSpec("moduleSortZygiskFirst", "Zygisk 优先", Kind.BOOL),
        FieldSpec("moduleSortMetamoduleFirst", "元模块优先", Kind.BOOL),
        FieldSpec("moduleSortNameOrder", "名称排序", Kind.CHOICE,
            choices = ch(Choice("未设置", null), Choice("ASC", "ASC"), Choice("DESC", "DESC"))),
        FieldSpec("moduleSortManual", "手动排序", Kind.BOOL),
        FieldSpec("customHomeTitle", "自定义首页标题", Kind.STRING),
    )),
)

val JSON_BLOCKS: List<JsonBlock> = listOf(
    JsonBlock("appearance", "Appearance 叠加层", "静态颜色 / 图像效果；需要格式 v4"),
    JsonBlock("light", "Light 变体", "自适应主题浅色全套 ThemeVariantData（v3+）"),
    JsonBlock("dark", "Dark 变体", "自适应主题深色全套 ThemeVariantData（v3+）"),
    JsonBlock("customTexts", "自定义文本", "Map<String, String>，JSON 对象"),
    JsonBlock("homeCardStickerTransforms", "贴纸变换", "JSON 数组", isArray = true),
)

val IMAGE_SLOTS: List<ImageSlot> = buildList {
    val plain = "普通主题（v1 / v2 / v4）"
    add(ImageSlot("background", "背景图", plain))
    add(ImageSlot("card_background", "卡片背景", plain))
    for (i in 0..5) add(ImageSlot("card_sticker_$i", "贴纸 $i", plain))

    listOf("light" to "浅色", "dark" to "深色").forEach { (suffix, label) ->
        val group = "自适应主题 · $label（v3+）"
        add(ImageSlot("background_$suffix", "背景图（$label）", group))
        add(ImageSlot("card_background_$suffix", "卡片背景（$label）", group))
        for (i in 0..2) {
            add(ImageSlot("card_sticker_${suffix}_$i", "贴纸 $i（$label）", group))
        }
    }
}
