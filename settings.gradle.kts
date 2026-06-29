pluginManagement {
  repositories {
    maven { url = uri("https://maven.myket.ir/") }
    maven { url = uri("https://mirror-maven.runflare.com/maven2") }
    maven { url = uri("https://maven.chrepo.ir") }
    maven { url = uri("https://mirror.kargadan.ir/repository/maven-central-group/") }
    /*google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()*/
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    maven { url = uri("https://maven.myket.ir/") }
    maven { url = uri("https://mirror-maven.runflare.com/maven2") }
    maven { url = uri("https://maven.chrepo.ir") }
    maven { url = uri("https://mirror.kargadan.ir/repository/maven-central-group/") }
    /* google()
     mavenCentral()*/
  }
}

rootProject.name = "FlashC"

include(":FlashC")
