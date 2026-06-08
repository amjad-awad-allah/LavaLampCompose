plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}
 
// Library Artifact Metadata for JitPack/Maven Central Publishing
val libraryGroupId = "com.github.amjad-awad-allah"
val libraryArtifactId = "LavaLampCompose"
val libraryVersion = "2.5.5"

android {
    namespace = "com.example.lavalamp"
    publishing {
        singleVariant("release")
    }
    compileSdk = 35


    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = libraryGroupId
                artifactId = libraryArtifactId
                version = libraryVersion
            }
        }
    }
}
