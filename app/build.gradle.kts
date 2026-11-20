import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing material is deliberately not stored in this repository.
//
// Supply it either through environment variables (CI / GitHub Actions):
//   ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD,
//   ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
//
// or through local.properties for local builds:
//   signing.keystore=/path/to/keystore.jks
//   signing.storePassword=...
//   signing.keyAlias=...
//   signing.keyPassword=...
//
// When no keystore is configured, release builds fall back to the standard
// Android debug keystore so that CI can still produce an installable APK.

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val signingStoreFile = signingValue("ANDROID_KEYSTORE_FILE", "signing.keystore")
    ?.let { rootProject.file(it) }
val signingStorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "signing.storePassword")
val signingKeyAlias = signingValue("ANDROID_KEY_ALIAS", "signing.keyAlias")
val signingKeyPassword = signingValue("ANDROID_KEY_PASSWORD", "signing.keyPassword")

val hasReleaseKeystore = signingStoreFile?.exists() == true
        && !signingStorePassword.isNullOrBlank()
        && !signingKeyAlias.isNullOrBlank()
        && !signingKeyPassword.isNullOrBlank()

android {
    namespace = "com.bellatrixos"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    // Omalauncher builds its patched proot libraries against this exact NDK.
    ndkVersion = "30.0.16248370"

    defaultConfig {
        applicationId = "com.bellatrixos"
        minSdk = 23
        targetSdk = 36
        versionCode = 27
        versionName = "2.7.6"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

        // Omalauncher's proot and wlbridge binaries are arm64 only.
        ndk {
            abiFilters += "arm64-v8a"
        }
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.1"
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = signingStoreFile!!
                storePassword = signingStorePassword!!
                keyAlias = signingKeyAlias!!
                keyPassword = signingKeyPassword!!
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                // Default AGP debug keystore; used only when no keystore is supplied.
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-Xjvm-default=all", "-opt-in=kotlin.RequiresOptIn")
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            res.setSrcDirs(listOf("src/main/res"))
            assets.setSrcDirs(listOf("src/main/assets"))
            kotlin.setSrcDirs(listOf("src/main/kotlin"))
            java.setSrcDirs(emptyList<String>())
        }
    }

    packaging {
        jniLibs {
            // proot is exec'd from its extracted nativeLibraryDir (as in Omalauncher).
            useLegacyPackaging = true
            keepDebugSymbols += listOf("**/libproot.so", "**/libproot-loader.so")
        }
        resources {
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/*.properties",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "DebugProbesKt.bin"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        buildConfig = true
        viewBinding = false
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Rootfs tarball extraction, as in Omalauncher.
    implementation("org.apache.commons:commons-compress:1.26.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.appcompat:appcompat-resources:1.7.1")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.drawerlayout:drawerlayout:1.1.1")
    implementation("androidx.documentfile:documentfile:1.0.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    implementation("androidx.print:print:1.0.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.transition:transition:1.4.1")
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-process:2.9.0")
    implementation("androidx.savedstate:savedstate-ktx:1.3.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.0")
    implementation("androidx.datastore:datastore:1.1.1")

    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-transformer:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")

    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")

    implementation("com.google.android.gms:play-services-ads:25.3.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
}
