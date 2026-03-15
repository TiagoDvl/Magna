# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.tick.magna.**$$serializer { *; }
-keepclassmembers class com.tick.magna.** {
    *** Companion;
}
-keepclasseswithmembers class com.tick.magna.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.client.engine.** { *; }

# OkHttp (engine do Ktor no Android)
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Koin
-keep class org.koin.core.annotation.** { *; }
-dontwarn org.koin.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# Napier
-dontwarn io.github.aakira.napier.**

# Data models usados em navegação (Kotlinx Serialization @Serializable)
-keep @kotlinx.serialization.Serializable class com.tick.magna.** { *; }
