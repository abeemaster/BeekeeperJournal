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
        targetSdk = 35
        versionCode = 5
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                // Додаємо прапорець для генерації налагоджувальної інформації в нативному коді
                arguments += "-DANDROID_ALIGNED_16K=ON"
                arguments += "-DCMAKE_BUILD_TYPE=RelWithDebInfo"
            }
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Важливо: Google Play потребує саме ці налаштування
            ndk {
                debugSymbolLevel = "FULL"
            }

            // Додаємо примусове збереження символів нативних бібліотек
            matchingFallbacks += listOf("release")
        }
    }

    packaging {
        jniLibs {
            // Змінюємо на false, якщо це можливо для вашої версії AGP,
            // оскільки Google Play краще обробляє бандли без Legacy пакування.
            // Якщо виникнуть проблеми з запуском Vosk — поверніть true.
            useLegacyPackaging = false

            // Додаємо виключення, щоб символи точно потрапили в бандл
            keepDebugSymbols.add("**/*.so")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-Xno-call-assertions",
            "-Xno-param-assertions"
        )
    }
}

dependencies {
    // ... ваші залежності залишаються без змін ...
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.documentFile)

    implementation(libs.alphacephei.vosk.android)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.hilt.android)
    implementation(libs.androidx.ui.test)
    ksp(libs.androidx.work.hilt.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.fragment)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.google.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}