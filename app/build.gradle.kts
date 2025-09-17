// /app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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

    // Vosk для розпізнавання мовлення
    implementation(libs.alphacephei.vosk.android)

    // Залежності для тестування
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.google.gson)

    // Hilt залежності з KSP
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // ROOM залежності
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.ambilwarna)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.fragment)
}