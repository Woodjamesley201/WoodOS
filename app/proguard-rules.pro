# WoodOS uses no reflection, so the default optimize rules are sufficient.
# Keep framework widget constructors referenced from layouts.
-keep class com.woodos.** { *; }
