pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // React Native 0.73+ Gradle plugin (handles RN AAR resolution automatically)
    val rnGradlePlugin = file("demo/node_modules/@react-native/gradle-plugin")
    if (rnGradlePlugin.exists()) {
        includeBuild(rnGradlePlugin.path)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Local React Native Android AAR from NPM package (fallback for compileOnly)
        val rnAndroidDir = file("demo/node_modules/react-native/android")
        if (rnAndroidDir.exists()) {
            maven { url = rnAndroidDir.toURI() }
        }
        val jscDistDir = file("demo/node_modules/jsc-android/dist")
        if (jscDistDir.exists()) {
            maven { url = jscDistDir.toURI() }
        }
    }
}

rootProject.name = "tomady-nutrition"
include(":app")
