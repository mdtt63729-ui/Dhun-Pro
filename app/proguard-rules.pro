# --- Dhun: R8 missing-class suppressions ---

# Rhino JS engine (pulled in transitively by the translator library) references
# javax.script.* which does not exist on Android. The engine is never used at
# runtime on device, so silence the missing-class errors.
-dontwarn javax.script.**

# The JSR-223 service descriptor Rhino ships is meaningless on Android.
-dontwarn org.mozilla.javascript.engine.**
