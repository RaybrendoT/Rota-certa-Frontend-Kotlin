# Mantém os modelos de dados (DTOs) usados pela serialização kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class com.rotacerta.driver.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.rotacerta.driver.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rotacerta.driver.**$$serializer { *; }
-keepclassmembers class com.rotacerta.driver.** {
    *** Companion;
}
