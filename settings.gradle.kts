pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}

rootProject.name = "TherabotMobile"
include(":app")
