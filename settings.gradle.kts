pluginManagement {
    repositories {
        google()
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

rootProject.name = "cherish"

include(":app")

includeBuild("../ShirokoWearUI") {
    dependencySubstitution {
        substitute(module("io.github.jinlinahida:shirokowear-ui"))
            .using(project(":shirokowear"))
        substitute(module("io.github.jinlinahida:shirokowear-navigation"))
            .using(project(":shirokowear-navigation"))
    }
}
