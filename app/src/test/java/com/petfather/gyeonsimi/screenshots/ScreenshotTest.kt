package com.petfather.gyeonsimi.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.petfather.gyeonsimi.R
import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.data.db.MealLog
import com.petfather.gyeonsimi.data.db.MealSlot
import com.petfather.gyeonsimi.data.db.Sex
import com.petfather.gyeonsimi.data.db.Species
import com.petfather.gyeonsimi.domain.LifeStage
import com.petfather.gyeonsimi.domain.WeightStatus
import com.petfather.gyeonsimi.ui.GyeonsimiBottomBar
import com.petfather.gyeonsimi.ui.Tab
import com.petfather.gyeonsimi.ui.ai.AiContent
import com.petfather.gyeonsimi.ui.community.CommunityContent
import com.petfather.gyeonsimi.ui.components.PetTopBar
import com.petfather.gyeonsimi.ui.feed.FeedListContent
import com.petfather.gyeonsimi.ui.health.HealthContent
import com.petfather.gyeonsimi.ui.home.HealthEntryForm
import com.petfather.gyeonsimi.ui.home.HomeContent
import com.petfather.gyeonsimi.ui.home.HomeUiState
import com.petfather.gyeonsimi.ui.home.LocationCardContent
import com.petfather.gyeonsimi.ui.home.PlaceState
import com.petfather.gyeonsimi.ui.register.PetForm
import com.petfather.gyeonsimi.ui.register.RegisterContent
import com.petfather.gyeonsimi.ui.theme.GyeonsimiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

private const val OUT = "build/outputs/roborazzi"
private const val PHONE = "w412dp-h915dp-xhdpi"
private const val STORE = "w411dp-h731dp-420dpi"

/**
 * 주요 화면을 샘플 데이터로 그려 PNG 로 저장합니다.
 *   ./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*' -Proborazzi.record=true
 * 결과: app/build/outputs/roborazzi 폴더의 PNG 파일
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PHONE)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { GyeonsimiTheme(darkTheme = dark) { Surface(color = MaterialTheme.colorScheme.background) { content() } } }
        compose.onRoot().captureRoboImage("$OUT/$name.png")
    }

    @Composable
    private fun Frame(tab: Tab, title: String?, body: @Composable (PaddingValues) -> Unit) {
        Scaffold(
            topBar = { if (title != null) PetTopBar(title, onPawClick = {}) },
            bottomBar = { GyeonsimiBottomBar(tab) {} },
            containerColor = MaterialTheme.colorScheme.background,
        ) { body(it) }
    }

    private val homeState: HomeUiState
        get() {
            val t = SampleData.today
            val at = { h: Int, m: Int -> t.atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            return HomeUiState(
                loading = false,
                pet = SampleData.dog,
                today = t,
                meals = mapOf(
                    MealSlot.BREAKFAST to MealLog(1, t, MealSlot.BREAKFAST, at(8, 12)),
                    MealSlot.LUNCH to MealLog(1, t, MealSlot.LUNCH, at(12, 40)),
                ),
                todayHealth = HealthRecord(1, t, 19.3, 38.7, 91, listOf("배변", "산책")),
                feeding = SampleData.overview().report.feeding,
            )
        }

    @Composable
    private fun Map() = LocationCardContent(PlaceState(true, address = "서울특별시 중구 명동"), showMap = false, onRequestPermission = {}, onFindHospitals = {})

    @Test
    @Config(qualifiers = "w412dp-h1160dp-xhdpi")
    fun home() = shot("01_home") {
        Frame(Tab.HOME, "안녕") { HomeContent(homeState, it, { Map() }, {}, {}, {}, {}) }
    }

    @Test
    fun homeBeforeMeals() = shot("02_home_before_meals") {
        Frame(Tab.HOME, "안녕") {
            HomeContent(homeState.copy(meals = emptyMap(), todayHealth = null), it, {
                LocationCardContent(PlaceState(false), showMap = false, onRequestPermission = {}, onFindHospitals = {})
            }, {}, {}, {}, {})
        }
    }

    @Test
    fun homeNoPet() = shot("03_home_no_pet") {
        Frame(Tab.HOME, "견심이") { HomeContent(HomeUiState(loading = false, today = SampleData.today), it, {}, {}, {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "w412dp-h1160dp-xhdpi")
    fun register() = shot("04_register") {
        Scaffold(bottomBar = { GyeonsimiBottomBar(Tab.REGISTER) {} }) { padding ->
            Box(Modifier.padding(padding)) {
                RegisterContent(
                    form = PetForm(species = Species.DOG, name = "안녕", breed = "진돗개", birthDate = LocalDate.of(2020, 5, 2), weight = "19.0", sex = Sex.MALE, neutered = true, allergies = "닭고기"),
                    today = SampleData.today,
                    onChange = {},
                    onSave = {},
                    onBack = null,
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h1300dp-xhdpi")
    fun health() = shot("05_health") {
        Frame(Tab.HEALTH, "안녕") { HealthContent(SampleData.overview(), it) }
    }

    @Test
    @Config(qualifiers = "w412dp-h1500dp-xhdpi")
    fun ai() = shot("06_ai") {
        Frame(Tab.AI, "안녕") { AiContent(SampleData.overview(), it) }
    }

    @Test
    @Config(qualifiers = "w412dp-h1500dp-xhdpi")
    fun aiWarning() = shot("07_ai_warning") {
        Frame(Tab.AI, "안녕") { AiContent(SampleData.overview(recs = SampleData.warningRecords), it) }
    }

    @Test
    fun community() = shot("08_community") {
        Scaffold(bottomBar = { GyeonsimiBottomBar(Tab.COMMUNITY) {} }) { p ->
            Box(Modifier.padding(p)) { CommunityContent(SampleData.posts, {}, {}, {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h1200dp-xhdpi")
    fun feedList() = shot("09_feed_list") {
        FeedListContent("안녕", Species.DOG, LifeStage.ADULT, WeightStatus.NORMAL, {}, {})
    }

    @Test
    @Config(qualifiers = "w412dp-h1000dp-xhdpi")
    fun healthEntry() = shot("10_health_entry") {
        Column(Modifier.padding(top = 16.dp)) {
            HealthEntryForm(Species.DOG, HealthRecord(1, SampleData.today, 19.3, 38.7, 91, listOf("배변", "산책")), 19.3, {}, {})
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h1160dp-night-xhdpi")
    fun homeDark() = shot("11_home_dark", dark = true) {
        Frame(Tab.HOME, "안녕") { HomeContent(homeState, it, { Map() }, {}, {}, {}, {}) }
    }

    // ── 플레이 스토어 휴대전화 스크린샷 (9:16, 약 1080×1920) ──
    @Test
    @Config(qualifiers = STORE)
    fun storeHome() = shot("store_1_home") {
        Frame(Tab.HOME, "안녕") { HomeContent(homeState, it, { Map() }, {}, {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = STORE)
    fun storeHealth() = shot("store_2_health") {
        Frame(Tab.HEALTH, "안녕") { HealthContent(SampleData.overview(), it) }
    }

    @Test
    @Config(qualifiers = STORE)
    fun storeAi() = shot("store_3_ai") {
        Frame(Tab.AI, "안녕") { AiContent(SampleData.overview(), it) }
    }

    @Test
    @Config(qualifiers = STORE)
    fun storeRegister() = shot("store_4_register") {
        Scaffold(bottomBar = { GyeonsimiBottomBar(Tab.REGISTER) {} }) { padding ->
            Box(Modifier.padding(padding)) {
                RegisterContent(
                    form = PetForm(species = Species.CAT, name = "나비", breed = "코리안 숏헤어", birthDate = LocalDate.of(2023, 3, 14), weight = "4.1", sex = Sex.FEMALE, neutered = true),
                    today = SampleData.today, onChange = {}, onSave = {}, onBack = null,
                )
            }
        }
    }

    @Test
    @Config(qualifiers = STORE)
    fun storeCommunity() = shot("store_5_community") {
        Scaffold(bottomBar = { GyeonsimiBottomBar(Tab.COMMUNITY) {} }) { p ->
            Box(Modifier.padding(p)) { CommunityContent(SampleData.posts, {}, {}, {}, {}) }
        }
    }

    /** 플레이 스토어 앱 아이콘 (512×512) */
    @Test
    @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun storeIcon() {
        compose.setContent {
            Box(Modifier.size(512.dp).background(Color(0xFF2F5D8C))) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize().scale(1.35f))
            }
        }
        compose.onRoot().captureRoboImage("$OUT/store_icon_512.png")
    }

    /** 플레이 스토어 그래픽 이미지 (1024×500) */
    @Test
    @Config(qualifiers = "w1024dp-h500dp-land-mdpi")
    fun featureGraphic() {
        compose.setContent {
            GyeonsimiTheme(darkTheme = false) {
                Box(
                    Modifier.size(1024.dp, 500.dp).background(Brush.linearGradient(listOf(Color(0xFF2F5D8C), Color(0xFF4F7FB3)))),
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 80.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(220.dp).clip(RoundedCornerShape(56.dp)).background(Color(0xFF2F5D8C))) {
                            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize().scale(1.45f))
                        }
                        Spacer(Modifier.width(56.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("견심이", color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.Bold)
                            Text("케어는 간편하게,\n반려동물은 건강하게", color = Color.White.copy(alpha = 0.92f), fontSize = 36.sp, lineHeight = 48.sp)
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("$OUT/store_feature_graphic.png")
    }
}
