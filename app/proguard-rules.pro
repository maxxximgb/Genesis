# Most of our deps (Compose, Hilt, Room, Media3, Glance, Coil) ship `consumer-rules.pro`
# inside their AARs, so the default `proguard-android-optimize.txt` is enough for them.
# This file is for the gaps.

# --- Domain models we hand to MediaItem extras / DataStore via reflection-adjacent paths.
# Keep the `domain.model` package's data classes intact — they're constructed by Room,
# Moshi-style serializers via QueueEntrySerializer's split parsing, and Media3 metadata
# round-trips. Class names appear in stack traces too, which helps debugging release crashes.
-keep class dev.maxxximgb.genesis.domain.model.** { *; }

# --- Hilt entry points: defensive. Hilt's generated code is annotation-only at compile
# time, but the `@HiltAndroidApp`/`@HiltViewModel` runtime shims access them by name.
-keep,allowobfuscation @interface dagger.hilt.**
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory$* { *; }

# Hilt's generated DaggerGenesisApp_HiltComponents builds an immutable Set<String> of
# ViewModel-key FQNs in `getViewModelKeys()`. With R8 merging/renaming, two distinct
# @HiltViewModel classes can collapse to the same obfuscated name → `Set.of(...)`
# throws `Multiple entries with same key` on activity create, before any code runs.
# Keeping every ViewModel's class name distinct prevents that. Members can still be
# obfuscated/shrunk; only the type name is pinned.
-keep class * extends androidx.lifecycle.ViewModel

# --- Glance widget receivers/actions are referenced from the manifest by FQN.
-keep class dev.maxxximgb.genesis.widget.** { *; }

# --- PlayerService is a foreground MediaSession service; framework looks it up by name.
-keep class dev.maxxximgb.genesis.service.PlayerService { *; }
