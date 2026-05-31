# R8 full-mode rules for the release build. Keep this list minimal and add
# library-specific keeps only when a real reflective access breaks.

# --- Media3 / ExoPlayer ---
# ExoPlayer instantiates renderers/decoders reflectively in some paths.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# --- NanoHTTPD (onboarding server) ---
-dontwarn fi.iki.elonen.**

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**

# Keep enums used by Room/serialization mappers via valueOf.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
