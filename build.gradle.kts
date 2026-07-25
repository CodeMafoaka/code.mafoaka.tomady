plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

// Local Maven repos from React Native NPM packages (resolved at configuration time)
// This is the traditional RN approach: allprojects.repositories lets each subproject
// resolve com.facebook.react:react-native from the node_modules AAR directory.
allprojects {
    repositories {
        google()
        mavenCentral()
        val rnAndroidDir = rootProject.file("demo/node_modules/react-native/android")
        if (rnAndroidDir.exists()) {
            maven { url = rnAndroidDir.toURI() }
        }
        val jscDistDir = rootProject.file("demo/node_modules/jsc-android/dist")
        if (jscDistDir.exists()) {
            maven { url = jscDistDir.toURI() }
        }
    }
}
