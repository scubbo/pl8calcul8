pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pl8calcul8"
// The app module needs an Android SDK to even configure; skip it where none
// exists (e.g. the server's Docker build).
if (File(rootDir, "local.properties").exists() || System.getenv("ANDROID_HOME") != null) {
    include(":app")
}
include(":shared")
include(":server")
