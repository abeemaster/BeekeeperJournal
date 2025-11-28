// /app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}
android {
    namespace = "com.beemaster.beekeeperjournal"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.beemaster.beekeeperjournal"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        @Suppress("DEPRECATION")
        jvmTarget = "11"

        @Suppress("DEPRECATION")
        freeCompilerArgs += listOf(
            "-Xno-call-assertions",
            "-Xno-param-assertions"
        )
    }
}

dependencies {
    // Стандартні залежності AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)

    // Vosk для розпізнавання мовлення
    implementation(libs.alphacephei.vosk.android)

    // WorkManager залежності для фонових задач (НОВІ)
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt інтеграція для WorkManager (НОВІ) - потрібна для @HiltWorker
    implementation(libs.androidx.work.hilt.android)
    ksp(libs.androidx.work.hilt.compiler)

    // Залежності для тестування
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.google.gson)

    // Hilt залежності з KSP
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.fragment)

    // ROOM залежності
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.documentFile)

    // OkHttp (ОКНО: використовуємо BOM для вирішення проблеми з імпортом)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // WorkManager (необхідний для нового VoskModelManager)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
// Hilt WorkManager (якщо ви використовуєте Hilt для ін'єкції воркерів)
// kapt "androidx.hilt:hilt-compiler:1.1.0"
// implementation "androidx.hilt:hilt-work:1.1.0"
// Lifecycle (якщо ще не додано, для LiveData Observer)
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}