plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.btl_androidnc_music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.btl_androidnc_music"
        minSdk = 24
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
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.security:security-crypto:1.0.0")
    implementation ("androidx.room:room-runtime:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.media3:media3-exoplayer:1.3.1")
    implementation ("androidx.media3:media3-ui:1.3.1")
    implementation ("androidx.media3:media3-session:1.3.1")
    implementation ("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}