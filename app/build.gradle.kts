import org.gradle.api.GradleException
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
        // GeckoView 145 and newer need Android 8.
        minSdk = 26
        targetSdk = 36
        versionCode = 104
        versionName = "3.4.4"
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

    // android.util.Log and friends are no-ops in JVM tests instead of throwing.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        viewBinding = false
    }
}

// scripts/fetch-proot.sh writes .source when it builds jniLibs. Verify it here so a stale or
// hand-edited native directory cannot silently ship. CI builds libfastpath.so directly and has no
// .source stamp, so this check is skipped there.
val verifyProotSourceHash = tasks.register("verifyProotSourceHash") {
    group = "verification"
    description = "Checks app/src/main/jniLibs/arm64-v8a/.source against the current proot sources."

    val repoRoot = rootProject.rootDir
    val sourceStamp = rootProject.file("app/src/main/jniLibs/arm64-v8a/.source")
    val sourceScript = rootProject.file("scripts/fetch-proot.sh")
    val patchFiles = rootProject.fileTree("patches/proot").files
        .filter { it.isFile && !it.name.startsWith(".") }
        .map { it.relativeTo(repoRoot).invariantSeparatorsPath }
        .sorted()

    // The stamp is generated by fetch-proot.sh and is absent in CI builds; only declare it
    // as an input when it exists, otherwise Gradle's file validation fails the build.
    inputs.files(providers.provider { if (sourceStamp.isFile) listOf(sourceStamp) else emptyList<java.io.File>() })
    inputs.file(sourceScript)
    inputs.files(patchFiles.map { repoRoot.resolve(it) })

    doLast {
        if (!sourceStamp.isFile) {
            logger.info("Proot .source stamp is absent; skipping source hash verification.")
            return@doLast
        }

        val digest = MessageDigest.getInstance("SHA-256")
        // fetch-proot.sh pipes the sorted file list through xargs cat, so only file bytes are hashed.
        digest.update(sourceScript.readBytes())
        for (relativePath in patchFiles) {
            digest.update(repoRoot.resolve(relativePath).readBytes())
        }

        val expected = sourceStamp.readText(Charsets.UTF_8).trim()
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!expected.equals(actual, ignoreCase = true)) {
            throw GradleException(
                "Proot .source hash mismatch: expected $expected, computed $actual. " +
                    "Rebuild app/src/main/jniLibs/arm64-v8a with scripts/fetch-proot.sh."
            )
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(verifyProotSourceHash)
}

dependencies {
    implementation(project(":owl"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Embedded Lua for ~/.config/bellatrix/native-ui/ui.lua. It runs inside App.runtime;
    // it is not a second launcher runtime and needs no native library or rebuild by guests.
    implementation("org.luaj:luaj-jse:3.0.1")
    // Optional ADB/root backend for shell-level package operations (self-update installs).
    // Shizuku is an IPC bridge in the same process/runtime, not a second launcher.
    // 12.2.0 is the newest line that still supports Bellatrix's minSdk 23.
    implementation("dev.rikka.shizuku:api:12.2.0")
    implementation("dev.rikka.shizuku:provider:12.2.0")

    // OWL's Compose renderer and the UI composables it shares with the launcher.
    // Compose 1.12.x pulls androidx.compose 1.12.1, which needs AGP 9.1 and
    // compileSdk 37; this project is intentionally still on AGP 8.9.2/SDK 36.
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
    implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
    implementation("org.jetbrains.compose.ui:ui:1.11.1")
    implementation("com.caverock:androidsvg-aar:1.4")
    // The Browser app's engine: Firefox's Gecko. The app is arm64-only, so only that build is pulled in.
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:150.0.20260511200624")
    testImplementation("junit:junit:4.13.2")
    // android.jar only has stubs of org.json.
    testImplementation("org.json:json:20260814")

    // Rootfs tarball extraction, as in Omalauncher.
    implementation("org.apache.commons:commons-compress:1.26.2")
    // commons-compress only delegates XZ to this library. Without it the Void
    // rootfs extract dies at "unpack the system" with NoClassDefFoundError.
    implementation("org.tukaani:xz:1.10")
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
