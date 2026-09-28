# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities
-keep class com.superfastscan.data.local.entity.** { *; }

# Keep navigation route classes
-keep class com.superfastscan.navigation.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }

# PDFBox Android & Gemalto JP2
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.bouncycastle.**
-keep class com.tom_roush.pdfbox.** { *; }

