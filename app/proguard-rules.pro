# R8 runs in full mode (see gradle.properties / convention plugin). Most keeps come
# from the libraries' own consumer rules; add app-specific keeps here as needed.

# Media3 / ExoPlayer reflectively instantiates some components.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# kotlinx.serialization: keep the generated serializers and @Serializable metadata so
# the Xtream / iptv-org models still parse after R8 shrinking/obfuscation.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer { *; }
-dontwarn kotlinx.serialization.**

# NanoHTTPD (embedded onboarding server) — keep the lib it reflects over internally.
-dontwarn fi.iki.elonen.**
