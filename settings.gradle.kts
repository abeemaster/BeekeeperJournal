// /root/settings.gradle.kts

pluginManagement {

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlin/p/libraries")
        // НОВЕ: Додаємо JitPack репозиторій для Vosk
        maven { url = uri("https://jitpack.io") }
    }
}


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlin/p/libraries")
        // НОВЕ: Додаємо JitPack репозиторій для Vosk
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "BeekeeperJournal"
include(":app")
