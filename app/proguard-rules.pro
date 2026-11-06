# WoodOS proguard rules
-dontwarn org.json.**
-keep class org.json.** { *; }

# Compose
-keep class androidx.compose.runtime.** { *; }

# Keep notification listener service entry points
-keep class dev.woodos.launcher.notifications.** { *; }
-keep class dev.woodos.launcher.services.** { *; }
