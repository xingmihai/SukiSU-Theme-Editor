# Release builds enable R8 code shrinking and unused-resource shrinking.
# Compose / AndroidX keep rules are merged automatically by AGP, so only a
# minimal set of app-specific rules is needed here.

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# No kotlinx.serialization / kotlin-reflect is used; org.json comes from the
# Android framework and needs no keep rules.
-dontwarn kotlinx.**
-dontwarn org.jetbrains.annotations.**

# Manifest entry point (also covered by AGP defaults, kept explicit for safety).
-keep class com.xingmihai.sukisu.themeeditor.MainActivity { *; }
