# FocusGuard ProGuard & R8 Optimization Rules

# Preserve line numbers and source file names for crash de-obfuscation in Google Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# FocusGuard Services and Receivers (Must not be renamed or stripped by R8)
-keep class com.focusguard.app.service.** { *; }
-keep class com.focusguard.app.receiver.** { *; }
-keep class com.focusguard.app.data.** { *; }
-keep class com.focusguard.app.ad.** { *; }

# Google Play Services Ads (AdMob)
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
-dontwarn com.google.android.gms.ads.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-dontwarn kotlinx.coroutines.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Jetpack DataStore
-keep class androidx.datastore.** { *; }
