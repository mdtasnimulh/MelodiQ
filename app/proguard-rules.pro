# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

#-----------------------SAFE ARGS and model--------------------------------
#noinspection ShrinkerUnresolvedReference
-keepnames class com.path.to.your.ParcelableArg
-keepnames class com.path.to.your.SerializableArg
-keepnames class com.path.to.your.EnumArg
-keep class com.tasnimulhasan.domain.** { *; }
-keep class com.tasnimulhasan.entity.** { *; }
-keep class com.tasnimulhasan.apiresponse.** { *; }

#-----------------------------  COMMON KEEP ENTRY ----------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions


#------------------------------  TIMBER PROGUARD RULES ------------------------------
-dontwarn org.jetbrains.annotations.**


#------------------------------  GSON PROGUARD RULES ------------------------------
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
 @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.google.gson.** { *; }
-keep class com.google.inject.** { *; }
-keep class org.apache.http.** { *; }
-keep class org.apache.james.mime4j.** { *; }
-keep class javax.inject.** { *; }
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn sun.misc.**
#------------------------------  FIREBASE PROGUARD RULES ------------------------------
-keep class com.google.firebase.quickstart.database.java.viewholder.** { *; }
-keepclassmembers class com.google.firebase.quickstart.database.java.models.** { *; }
#------------------image picker--------------------------
-keep class androidx.appcompat.widget.** { *; }
#---------------------life cycle----------------------
## Android architecture components: Lifecycle
# LifecycleObserver's empty constructor is considered to be unused by proguard
-keepclassmembers class * implements android.arch.lifecycle.LifecycleObserver {
    <init>(...);
}
# ViewModel's empty constructor is considered to be unused by proguard
-keepclassmembers class * extends android.arch.lifecycle.ViewModel {
    <init>(...);
}
# keep Lifecycle State and Event enums values
-keepclassmembers class android.arch.lifecycle.Lifecycle$State { *; }
-keepclassmembers class android.arch.lifecycle.Lifecycle$Event { *; }
# keep methods annotated with @OnLifecycleEvent even if they seem to be unused
# (Mostly for LiveData.LifecycleBoundObserver.onStateChange(), but who knows)
-keepclassmembers class * {
    #noinspection ShrinkerUnresolvedReference
    @android.arch.lifecycle.OnLifecycleEvent *;
}
-keepclassmembers class * implements android.arch.lifecycle.LifecycleObserver {
    <init>(...);
}
-keep class * implements android.arch.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class android.arch.** { *; }
-keep class android.arch.** { *; }
-dontwarn android.arch.**

# Keep POI classes (poi-android relocates/shades them, but this is safe)
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class javax.xml.stream.** { *; }

-keep class com.tasnimulhasan.** { *; }
-keepnames class com.tasnimulhasan.**

-dontwarn com.android.extensions.xr.XrExtensionResult
-dontwarn com.android.extensions.xr.XrExtensions
-dontwarn com.android.extensions.xr.function.Consumer
-dontwarn com.android.extensions.xr.node.InputEvent$HitInfo
-dontwarn com.android.extensions.xr.node.InputEvent
-dontwarn com.android.extensions.xr.node.Mat4f
-dontwarn com.android.extensions.xr.node.Node
-dontwarn com.android.extensions.xr.node.NodeTransaction
-dontwarn com.android.extensions.xr.node.NodeTransform
-dontwarn com.android.extensions.xr.node.Vec3
-dontwarn com.android.extensions.xr.splitengine.BufferHandle
-dontwarn com.android.extensions.xr.splitengine.MessageGroupCallback
-dontwarn com.android.extensions.xr.splitengine.RequestCallback
-dontwarn com.android.extensions.xr.splitengine.SystemRendererConnection
-dontwarn com.android.extensions.xr.subspace.Subspace