pluginManagement {

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
        }
    }

    repositories {
        google()

        maven {
            url = uri("https://repo1.maven.org/maven2")
        }

        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()

        maven {
            url = uri("https://repo1.maven.org/maven2")
        }

        mavenCentral()
    }
}

rootProject.name = "PermAlert"

include(":app")
