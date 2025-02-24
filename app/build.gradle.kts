import com.android.tools.r8.internal.de

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")//firebase eklentisi
}

android {
    namespace = "com.example.deussenger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.deussenger"
        minSdk = 30
        targetSdk = 35
        multiDexEnabled
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.picasso:picasso:2.71828")

    //firebase kütüphaneleri
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    implementation(libs.firebase.firestore)//firastore kütüphane
    implementation(libs.firebase.auth)//authentication kütüphane

    implementation(libs.circleimageview)//circleImageView kütüphanesi
    implementation(libs.material.v110)//Material kütüphanesi

    implementation(libs.multidex)
    implementation(libs.firebase.storage)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}