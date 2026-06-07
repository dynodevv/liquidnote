// Top-level build file
buildscript {
    dependencies {
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.9")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.3.0")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.0")
    }
}

plugins {
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
}
