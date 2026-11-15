import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
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
        // 알림 API 선택 헤더 X-Notification-Key (없으면 빈 값 → 헤더 생략)
        buildConfigField("String", "CHAGOK_NOTIFICATION_KEY", "\"${localProperty("CHAGOK_NOTIFICATION_KEY")}\"")
    }

    // release 서명: 키 파일·비밀번호는 local.properties(git 제외)에서만 읽는다. 값이 없으면 서명 없이 빌드
    val releaseStoreFile = localProperty("CHAGOK_RELEASE_STORE_FILE")
    signingConfigs {
        if (releaseStoreFile.isNotBlank()) create("release") {
            storeFile = file(releaseStoreFile)
            storePassword = localProperty("CHAGOK_RELEASE_STORE_PASSWORD")
            keyAlias = localProperty("CHAGOK_RELEASE_KEY_ALIAS")
            keyPassword = localProperty("CHAGOK_RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            if (releaseStoreFile.isNotBlank()) signingConfig = signingConfigs.getByName("release")
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
    // ARCore·Firebase가 끌고 오는 fragment 1.1.0은 registerForActivityResult와 맞지 않음 (release lint 오류) → 1.3.0 이상으로
    implementation("androidx.fragment:fragment-ktx:1.8.5")
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

    // FCM (google-services.json은 app/에 직접 넣는다)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.arcore)
    // 짐 사진 (AR로 찍은 사진·갤러리 사진)
    implementation(libs.coil.compose)
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