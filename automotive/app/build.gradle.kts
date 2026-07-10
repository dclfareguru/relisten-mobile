import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.dynamixwebdesign.relisten.car"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ttblisten"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    // Upload-key credentials live in ~/.gradle/gradle.properties (never in the repo).
    val uploadStoreFile = providers.gradleProperty("RELISTEN_CAR_UPLOAD_STORE_FILE").orNull
    if (uploadStoreFile != null) {
        signingConfigs {
            create("upload") {
                storeFile = file(uploadStoreFile)
                storePassword = providers.gradleProperty("RELISTEN_CAR_UPLOAD_STORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("RELISTEN_CAR_UPLOAD_KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("RELISTEN_CAR_UPLOAD_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (uploadStoreFile != null) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val media3 = "1.10.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
}
