/**
 * Compile-time stubs for the parts of android.hardware.radio that MelodiQ's hardware FM tuner uses.
 *
 * RadioManager / RadioTuner / ProgramSelector are @SystemApi: they exist in the framework on the
 * device but are NOT in the public android.jar, so they cannot be referenced directly. These stubs
 * only describe the signatures so the code compiles. The module is consumed with `compileOnly`, so
 * it is never packaged in the APK and the real framework classes are used at runtime.
 */
plugins {
    alias(libs.plugins.melodiq.android.library)
}

android {
    namespace = "com.tasnimulhasan.radiostubs"
}
