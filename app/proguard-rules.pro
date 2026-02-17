# MontageZeit ProGuard Rules
# Production-ready configuration for Release builds

# ============================================================================
# ROOM DATABASE
# ============================================================================
# Keep Room Database classes to prevent reflection issues
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Keep all Room-generated implementations
-keep class * implements androidx.room.RoomDatabase$Callback {
    *;
}

# Keep database entities with all fields
-keepclassmembers class de.montagezeit.app.data.local.entity.** {
    *;
}

# ============================================================================
# HILT / DAGGER DEPENDENCY INJECTION
# ============================================================================
# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Keep all @Inject annotated constructors and fields
-keepclasseswithmembernames class * {
    @javax.inject.* <fields>;
}
-keepclasseswithmembernames class * {
    @javax.inject.* <methods>;
}
-keepclasseswithmembernames class * {
    @javax.inject.* <init>(...);
}

# Keep Hilt modules
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *

# ============================================================================
# WORKMANAGER
# ============================================================================
# Keep Worker classes for background jobs
-keep class * extends androidx.work.Worker {
    public <init>(...);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# Keep WorkManager configuration
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }

# Keep Worker constructors (used by WorkerFactory)
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================================================================
# KOTLIN COROUTINES
# ============================================================================
# Keep coroutines dispatcher classes
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep coroutines debug info
-keepnames class kotlinx.coroutines.** { *; }

# ============================================================================
# MOSHI SERIALIZATION
# ============================================================================
# Keep Moshi adapters
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

# ============================================================================
# DATA CLASSES & ENUMS
# ============================================================================
# Keep all data classes in domain model
-keep class de.montagezeit.app.domain.model.** { *; }

# Keep all enums (prevents string comparison issues)
-keep enum de.montagezeit.app.** { *; }

# Keep sealed classes
-keep class de.montagezeit.app.domain.model.LocationResult { *; }
-keep class de.montagezeit.app.domain.model.LocationResult$* { *; }

# ============================================================================
# ANDROID COMPONENTS
# ============================================================================
# Keep BroadcastReceivers
-keep class * extends android.content.BroadcastReceiver {
    <init>(...);
}

# Keep Services
-keep class * extends android.app.Service {
    <init>(...);
}

# ============================================================================
# DEBUGGING & OPTIMIZATION
# ============================================================================
# Keep source file and line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Remove verbose logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove debug logging from RingBufferLogger
-assumenosideeffects class de.montagezeit.app.logging.RingBufferLogger {
    public void d(...);
    public void v(...);
    public void i(...);
}

# ============================================================================
# GENERAL KOTLIN
# ============================================================================
# Keep Kotlin metadata
-keepattributes *Annotation*

# Keep Kotlin intrinsics
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }

# Keep companion objects
-keepclassmembers class ** {
    public static ** Companion;
}

# ============================================================================
# ANDROIDX LIFECYCLE
# ============================================================================
# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep LiveData
-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.MutableLiveData { *; }

# ============================================================================
# JETPACK COMPOSE
# ============================================================================
# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keep @androidx.compose.runtime.Composable interface * { *; }

# ============================================================================
# LOCATION SERVICES
# ============================================================================
# Keep Google Play Services Location
-keep class com.google.android.gms.location.** { *; }

# ============================================================================
# OPTIMIZATION FLAGS
# ============================================================================
# Enable aggressive optimizations
-optimizationpasses 5
-allowaccessmodification

# Don't warn about missing classes (Play Services)
-dontwarn com.google.android.gms.**
-dontwarn kotlinx.coroutines.debug.**
