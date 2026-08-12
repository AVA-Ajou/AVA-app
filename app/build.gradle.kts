import java.util.Properties

plugins {
    // AGP 9 부터 Kotlin 지원이 내장이라 kotlin.android 플러그인은 적용하지 않는다.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ava.proto"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ava.proto"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        // local.properties에서 직접 읽는다 — project.findProperty()는 local.properties를 못 읽음.
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localFile.inputStream().use { localProps.load(it) }
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps["GEMINI_API_KEY"] ?: ""}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${localProps["GROQ_API_KEY"] ?: ""}\"")
        // 파인튜닝 모델 서버. 비어 있으면 Groq/키워드 경로로 떨어진다.
        // 에뮬레이터·실기기에서는 `adb reverse tcp:8000 tcp:8000` 으로 붙인다 —
        // 10.0.2.2 직결은 맥 방화벽이 TCP를 막아 타임아웃난다(ping은 통과한다).
        buildConfigField(
            "String", "DETECTION_SERVER_URL",
            "\"${localProps["DETECTION_SERVER_URL"] ?: ""}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    debugImplementation(libs.androidx.ui.tooling)
}
