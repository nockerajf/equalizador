plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.grupo11.equalizador"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.grupo11.equalizador"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    ndkVersion = "27.0.12077973"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)

    // Add Mockito dependencies
    testImplementation(libs.mockito.core) // Core Mockito library
    androidTestImplementation(libs.mockito.android) // Mockito for Android tests

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.core:core:1.1.0")
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.emoji2:emoji2:1.0.0")
    testImplementation(libs.robolectric) // Use the latest version
}


tasks.withType<Test> {
    // Enable the HTML report
    reports.html.required.set(true)

    // Enable the XML report
    reports.junitXml.required.set(true)

    // Optionally, configure the report destination directory for XML
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("reports/tests/xml"))

    // Optionally, configure the report destination directory for HTML
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/html"))

    // Optionally, configure test logging for more detailed output in the report
    testLogging {
        events("passed", "skipped", "failed") // Include passed, skipped, and failed tests in the log
        showStandardStreams = true // Show standard output and error streams
    }
}

tasks.named("build") {
    dependsOn("testDebugUnitTest")
}