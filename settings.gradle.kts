pluginManagement {
    val isGithubActions = System.getenv("GITHUB_ACTIONS") == "true"
    repositories {
        if (isGithubActions) {
            google()
            mavenCentral()
            gradlePluginPortal()
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        } else {
            // Prefer mirror when dl.google.com fails (TLS / network). Remove these lines if you are not in a restricted network.
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}
dependencyResolutionManagement {
    val isGithubActions = System.getenv("GITHUB_ACTIONS") == "true"
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (isGithubActions) {
            google()
            mavenCentral()
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        } else {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            google()
            mavenCentral()
        }
    }
}
rootProject.name = "MobileBot"

include(":app")
include(":core:model")
include(":core:bus")
include(":core:network")
include(":core:bridge")
include(":core:domain")
include(":core:data")
include(":core:systemruntime")
include(":feature:chat")
include(":scenarios:pet-grooming")
include(":scenarios:runtime")
include(":scenarios:one-hour-flow")
include(":scenarios:family-shopping")
include(":scenarios:coldchain-delivery")
include(":scenarios:health-supply")
