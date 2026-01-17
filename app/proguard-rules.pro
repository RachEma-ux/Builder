# Add project specific ProGuard rules here.

# Keep Tink classes
-keep class com.google.crypto.tink.** { *; }

# Ignore missing error prone annotations (used by Tink)
-dontwarn com.google.errorprone.annotations.**

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep data classes for serialization
-keep class com.builder.core.model.** { *; }
-keep class com.builder.data.local.db.entities.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# General Android rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
