plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }
    jvm("desktop")
    wasmJs {
        browser()
    }

    // The default hierarchy template does not generate a jvmMain intermediate for a
    // named jvm("desktop") target (only plain jvm() triggers it).  We create jvmMain
    // manually below and wire androidMain + desktopMain into it.  Calling
    // applyDefaultHierarchyTemplate() here keeps the template active for the other
    // standard intermediates (nativeMain, etc.) and suppresses the "template not
    // applied" warning.
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.core)
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.ui)
                // R46: use the Android-specific Ktor engine which delegates to
                // OkHttp / Android's system CA store, rather than inheriting CIO
                // from jvmMain.  CIO bypasses the Android system certificate store,
                // which breaks TLS on devices with custom/enterprise CA roots.
                implementation(libs.ktor.client.android)
            }
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.ui)
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(compose.ui)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "org.lambertland.kxpilot.shared"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
