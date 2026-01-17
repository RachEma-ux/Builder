# Add project specific ProGuard rules here.

# Keep Tink classes
-keep class com.google.crypto.tink.** { *; }

# Ignore missing dependencies (used by Tink)
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

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
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class **ViewModel { *; }
-keep class **UseCase { *; }

# Keep Hilt generated classes
-keep class *_HiltModules* { *; }
-keep class *_Factory { *; }
-keep class *_MembersInjector { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep data classes for serialization
-keep class com.builder.core.model.** { *; }
-keep class com.builder.data.local.db.entities.** { *; }

# Keep GitHub API model classes (Gson serialization)
-keep class com.builder.core.model.github.** { *; }
-keepclassmembers class com.builder.core.model.github.** { *; }

# Keep data layer models
-keep class com.builder.data.remote.** { *; }
-keepclassmembers class com.builder.data.remote.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# General Android rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
