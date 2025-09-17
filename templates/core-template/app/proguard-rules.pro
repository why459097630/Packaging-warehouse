# 供上游资源在合并 R8 时保类：目前保标记 @Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * { @androidx.annotation.Keep *; }

# NDJC:INTEGRATION_KEEP_RULES
# （生成器可在此追加：三方 SDK keep / -dontwarn / -keepnames 等）
# 示例：
# -keep class com.tencent.mm.** { *; }
# -dontwarn com.tencent.mm.**
