// No Kotlin plugin: AGP 9 compiles Kotlin itself. The Compose compiler plugin
// is separate machinery and is still required.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "rip.failed.iris"
    compileSdk = 37

    defaultConfig {
        applicationId = "rip.failed.iris"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            // Signed with the debug key on purpose. This is a personal app and
            // there is no distribution plan; the moment there is one, this
            // block has to grow a real signingConfig, and that swap forces one
            // uninstall.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // The dictionary is 17 MB of already-compressed-mostly data. Leaving it
    // uncompressed in the apk lets the loader read it straight out of the
    // asset, which is the whole point of shipping it baked in.
    androidResources {
        noCompress += "dat"
        noCompress += "frm"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        // Must agree with compileOptions above. A mismatch compiles but
        // produces class files the D8 pass then rejects, which reads as an
        // unrelated error much later in the build.
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-core:1.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.core:core-ktx:1.13.1")
}
