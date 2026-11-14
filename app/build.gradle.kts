import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun localProperty(key: String) = localProperties.getProperty(key, "")

android {
    namespace = "com.windrr.hyundaesub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.windrr.hyundaesub"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 서버 주소: local.properties에 없으면 팀 개발 서버 (끝 '/'는 있어도 없어도 됨 — ApiClient가 맞춤)
        buildConfigField("String", "CHAGOK_API_BASE_URL", "\"${localProperty("CHAGOK_API_BASE_URL").ifBlank { "https://chagokchagok-api.duckdns.org" }}\"")
        // 서버 Basic 인증 계정: 공개 레포라 반드시 local.properties에만 (커밋 금지)
        buildConfigField("String", "CHAGOK_API_USERNAME", "\"${localProperty("CHAGOK_API_USERNAME")}\"")
        buildConfigField("String", "CHAGOK_API_PASSWORD", "\"${localProperty("CHAGOK_API_PASSWORD")}\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.arcore)
    // 글래스모피즘 하단 탭바 (실블러 API 31+, 미만은 반투명 폴백)
    implementation(libs.haze)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}