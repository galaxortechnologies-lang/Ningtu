# Nigutu ProGuard rules

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nigutu.app.**$$serializer { *; }
-keepclassmembers class com.nigutu.app.** { *** Companion; }
-keepclasseswithmembers class com.nigutu.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Media3
-dontwarn androidx.media3.**
