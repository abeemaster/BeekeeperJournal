// /app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // ✅ ПЛАГІН РОЗМІЩЕНО ТУТ, У БЛОЦІ PLUGINS
}

android {
    namespace = "com.beemaster.beekeeperjournal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.beemaster.beekeeperjournal.dev"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        jvmTarget = "11"
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
    // Gson для роботи з JSON (поки що залишаємо)
    implementation(libs.google.gson)
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("com.google.android.material:material:1.12.0")
    // Vosk для розпізнавання мовлення
    implementation("com.alphacephei:vosk-android:0.3.47")
    // Залежності для тестування
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    val roomVersion = "2.6.1"

    // ✅ ROOM ЗАЛЕЖНОСТІ
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    // ❌ ВИДАЛЕНО: annotationProcessor є дублюванням kapt
}