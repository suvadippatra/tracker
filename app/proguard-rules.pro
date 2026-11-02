# Project specific ProGuard rules for compact R8 minified builds

# Preserve Room Database schemas, entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.model.** { *; }
-keep class com.example.data.dao.** { *; }
-keep class com.example.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Preserve Biometric authentication
-keep class androidx.biometric.** { *; }

# Preserve ZXing QR scanning & generation
-keep class com.google.zxing.** { *; }

# Preserve Coil image loader
-keep class coil.** { *; }

# Suppress warnings for optional libraries
-dontwarn org.bouncycastle.**


