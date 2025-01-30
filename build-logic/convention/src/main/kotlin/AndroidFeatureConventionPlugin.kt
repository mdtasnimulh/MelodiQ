import com.android.build.gradle.LibraryExtension
import com.tasnimulhasan.melodiq.AppConfig
import com.tasnimulhasan.melodiq.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("melodiq.android.library")
                apply("melodiq.android.hilt")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner =
                        AppConfig.testInstrumentationRunner
                }
                testOptions.animationsDisabled = true
            }

            dependencies {
                add("implementation", project(":core:di"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:model:entity"))
                add("implementation", project(":core:shared-preference"))

                add("implementation", project(":core:common"))
                add("implementation", project(":core:design-system"))
                add("implementation", project(":core:ui"))

                add("implementation", libs.findBundle("androidx.core.dependencies").get())
                add("implementation", libs.findBundle("androidx.material.dependencies").get())
                add("implementation", libs.findBundle("androidx.lifecycle.dependencies").get())
                add("implementation", libs.findBundle("androidx.navigation.dependencies").get())
                add("implementation", libs.findBundle("media3.dependencies").get())

                add("implementation", libs.findLibrary("timber").get())
                add("implementation", libs.findLibrary("gson").get())

                add("implementation", libs.findLibrary("kotlin.coroutines").get())

                add("implementation", libs.findLibrary("androidx.hilt.navigation.compose").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.runtimeCompose").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.viewModelCompose").get())
                add("implementation", libs.findLibrary("kotlinx.serialization.json").get())

                add("androidTestImplementation", libs.findLibrary("androidx.lifecycle.runtimeTesting").get())
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("hilt.android.testing").get())
                add("androidTestImplementation", libs.findLibrary("androidx.junit").get())
                add( "androidTestImplementation",libs.findLibrary("androidx.espresso.core").get())
            }
        }
    }
}
