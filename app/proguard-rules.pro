# Mantém os modelos de dados (DTOs) usados pela serialização kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class com.rotacerta.mobile.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.rotacerta.mobile.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rotacerta.mobile.**$$serializer { *; }
-keepclassmembers class com.rotacerta.mobile.** {
    *** Companion;
}
