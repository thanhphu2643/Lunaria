# JNA + UniFFI bindings are accessed reflectively.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class uniffi.lunaria_core.** { *; }
-dontwarn java.awt.**
