package com.petfather.gyeonsimi.ui.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petfather.gyeonsimi.data.db.Species
import com.petfather.gyeonsimi.domain.FeedCatalog
import com.petfather.gyeonsimi.domain.FeedItem
import com.petfather.gyeonsimi.domain.LifeStage
import com.petfather.gyeonsimi.domain.WeightStatus
import com.petfather.gyeonsimi.ui.appViewModel
import com.petfather.gyeonsimi.ui.components.IconBadge
import com.petfather.gyeonsimi.ui.components.StatusPill
import com.petfather.gyeonsimi.ui.health.OverviewState
import com.petfather.gyeonsimi.ui.health.OverviewViewModel

@Composable
fun FeedListScreen(onBack: () -> Unit) {
    val vm = appViewModel(key = "feeds") { OverviewViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val overview = (state as? OverviewState.Ready)?.overview
    val context = LocalContext.current
    FeedListContent(
        petName = overview?.pet?.name,
        species = overview?.pet?.species ?: Species.DOG,
        stage = overview?.stage ?: LifeStage.ADULT,
        status = overview?.report?.weightStatus ?: WeightStatus.UNKNOWN,
        onBack = onBack,
        onOpen = { item ->
            val q = Uri.encode("${item.brand} ${item.product}")
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://search.shopping.naver.com/search/all?query=$q".toUri()))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListContent(
    petName: String?,
    species: Species,
    stage: LifeStage,
    status: WeightStatus,
    onBack: () -> Unit,
    onOpen: (FeedItem) -> Unit,
) {
    val feeds = FeedCatalog.recommend(species, stage, status)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("인기 사료 10가지") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    (petName?.let { "$it(${species.label} · ${stage.label})에게 맞는 제품을 위에 보여드려요. " } ?: "") +
                        "많이 찾는 브랜드를 정리한 참고용 목록이며 순위나 광고가 아니에요. 사료를 바꾸기 전에는 수의사와 상담해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            items(feeds) { item ->
                val match = FeedCatalog.matches(item, stage, status)
                Card(
                    onClick = { onOpen(item) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            Icons.Filled.SetMeal,
                            if (match) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            if (match) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.brand, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.product, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (match) {
                                    StatusPill("맞춤", container = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                item.keywords.take(2).forEach { StatusPill(it) }
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "검색해 보기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
