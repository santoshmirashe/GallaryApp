# Default ProGuard rules for Android
-keepattributes *Annotation*
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Coil
-keep class coil.** { *; }
# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
