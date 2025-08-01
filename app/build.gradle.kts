import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android") version "1.9.0"
    id("io.gitlab.arturbosch.detekt") version("1.23.8")
    `maven-publish`
}

android {
    namespace = "com.example.appero_sdk_android"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            xml.required.set(false)
            html.required.set(false)
            sarif.required.set(false)
            md.required.set(true)
        }
    }
}

detekt {
    toolVersion = "1.23.8"
    config.setFrom(file("../config/detekt/detekt.yml"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.example.appero"
                artifactId = "appero-sdk-android"
                version = "1.0.0"
            }
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Built-in foundation and window insets (no external deps needed)
    implementation("androidx.compose.foundation:foundation")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Compose testing
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.google.android.play:core:1.10.3")
}