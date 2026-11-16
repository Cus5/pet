import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// local.properties: 개발자 PC마다 다른 값(SDK 경로, Google Maps API 키)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")
// 익명 사용 통계·피드백·체중 기록을 받는 Supabase 프로젝트 (공개 키만 넣습니다. secret 키는 절대 넣지 마세요)
val supabaseUrl: String = localProps.getProperty("SUPABASE_URL", "").trim().trimEnd('/')
val supabaseKey: String = localProps.getProperty("SUPABASE_KEY", "").trim()

// keystore.properties: 플레이 스토어 업로드 키 정보 (git 에 올리지 않음)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.petfather.gyeonsimi2"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.petfather.gyeonsimi2"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "2.0.0"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("boolean", "HAS_MAPS_KEY", (mapsApiKey.isNotBlank()).toString())
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("upload") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("upload")?.let { signingConfig = it }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                // 스크린샷 테스트는 -Proborazzi.record=true 일 때만 이미지를 저장합니다.
                it.systemProperty(
                    "roborazzi.test.record",
                    (project.findProperty("roborazzi.record") ?: "false").toString(),
                )
                it.maxHeapSize = "2g"
                // Robolectric 이 내려받는 Android 런타임 저장소를 바꿔야 할 때: -Probolectric.repo=https://...
                (project.findProperty("robolectric.repo") as String?)?.let { repo ->
                    it.systemProperty("robolectric.dependency.repo.url", repo)
                }
                // 실제 서버로 보내 보는 테스트는 -Pgyeonsimi.liveBackendTest=true 일 때만 실행합니다.
                it.systemProperty(
                    "gyeonsimi.liveBackendTest",
                    (project.findProperty("gyeonsimi.liveBackendTest") ?: "false").toString(),
                )
            }
        }
    }
}

// 서버 설정 없이 만든 릴리스는 사용 통계·피드백 기능이 빠진 채 배포됩니다.
// 그러면 실사용자 수와 유지율을 나중에 되살릴 수 없으므로 빌드를 막습니다.
run {
    val backendMissing = supabaseUrl.isBlank() || supabaseKey.isBlank()
    val allowMissing = providers.gradleProperty("gyeonsimi.allowNoBackend").orNull == "true"
    tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
        doFirst {
            if (backendMissing && !allowMissing) {
                throw GradleException(
                    "local.properties 에 SUPABASE_URL 과 SUPABASE_KEY 가 없습니다. README 의 '서버 설정'을 따라 넣어 주세요. " +
                        "(서버 없이 빌드하려면 -Pgyeonsimi.allowNoBackend=true)",
                )
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.maps.compose)
    implementation(libs.play.services.location)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
