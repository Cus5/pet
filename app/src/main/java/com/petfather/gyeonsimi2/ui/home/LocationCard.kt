package com.petfather.gyeonsimi2.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.petfather.gyeonsimi2.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class PlaceState(
    val permissionGranted: Boolean,
    val latLng: LatLng? = null,
    val address: String? = null,
    val loading: Boolean = false,
)

/** 홈 화면 지도 카드: 현재 위치와 '주변 동물병원' 바로가기 */
@Composable
fun LocationCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var place by remember { mutableStateOf(PlaceState(permissionGranted = hasLocationPermission(context))) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        place = place.copy(permissionGranted = result.values.any { it })
    }
    LaunchedEffect(place.permissionGranted) {
        if (place.permissionGranted && place.address == null) {
            place = place.copy(loading = true)
            val (latLng, address) = findCurrentPlace(context)
            place = place.copy(latLng = latLng, address = address, loading = false)
        }
    }
    LocationCardContent(
        place = place,
        showMap = BuildConfig.HAS_MAPS_KEY,
        modifier = modifier,
        onRequestPermission = {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        },
        onFindHospitals = { openNearbyHospitals(context, place.latLng) },
    )
}

@Composable
fun LocationCardContent(
    place: PlaceState,
    showMap: Boolean,
    onRequestPermission: () -> Unit,
    onFindHospitals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            val latLng = place.latLng
            if (showMap && latLng != null) {
                val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(latLng, 15f) }
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camera,
                    googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                ) {
                    Marker(state = rememberUpdatedMarkerState(position = latLng))
                }
            } else {
                MapPlaceholder(Modifier.fillMaxSize())
            }

            AssistChip(
                onClick = onFindHospitals,
                label = { Text("주변 동물병원") },
                leadingIcon = { Icon(Icons.Filled.LocalHospital, null, Modifier.size(AssistChipDefaults.IconSize)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            )

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).padding(0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) {}
                        Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("현재 위치", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val text = when {
                            !place.permissionGranted -> "위치를 허용하면 주소를 보여드려요"
                            place.loading -> "위치를 찾는 중…"
                            place.address != null -> place.address
                            else -> "위치를 찾지 못했어요"
                        }
                        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (!place.permissionGranted) {
                        TextButton(onClick = onRequestPermission) { Text("허용") }
                    }
                }
            }
        }
    }
}

/** 지도 API 키가 없을 때 보여주는 단순한 지도 그림 */
@Composable
private fun MapPlaceholder(modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    val road = MaterialTheme.colorScheme.surfaceContainerLowest
    val block = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    val pin = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        drawRect(bg)
        val w = size.width
        val h = size.height
        drawCircle(block, radius = h * 0.32f, center = Offset(w * 0.18f, h * 0.2f))
        drawRect(block, topLeft = Offset(w * 0.62f, h * 0.08f), size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.28f))
        val roads = listOf(
            Offset(0f, h * 0.42f) to Offset(w, h * 0.30f),
            Offset(w * 0.35f, 0f) to Offset(w * 0.48f, h),
            Offset(w * 0.70f, 0f) to Offset(w * 0.92f, h),
            Offset(0f, h * 0.78f) to Offset(w, h * 0.70f),
        )
        roads.forEach { (a, b) -> drawLine(road, a, b, strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round) }
        val c = Offset(w * 0.45f, h * 0.36f)
        val r = 11.dp.toPx()
        val pinPath = Path().apply {
            moveTo(c.x, c.y + r * 2.1f)
            cubicTo(c.x - r * 1.6f, c.y + r * 0.4f, c.x - r, c.y - r * 1.1f, c.x, c.y - r * 1.1f)
            cubicTo(c.x + r, c.y - r * 1.1f, c.x + r * 1.6f, c.y + r * 0.4f, c.x, c.y + r * 2.1f)
            close()
        }
        drawPath(pinPath, pin)
        drawCircle(road, radius = r * 0.42f, center = Offset(c.x, c.y - r * 0.15f))
    }
}

fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@SuppressLint("MissingPermission") // 호출 전에 hasLocationPermission 으로 확인합니다
private suspend fun findCurrentPlace(context: Context): Pair<LatLng?, String?> {
    if (!hasLocationPermission(context)) return null to null
    val client = LocationServices.getFusedLocationProviderClient(context)
    val location = runCatching {
        suspendCancellableCoroutine<Location?> { cont ->
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } ?: suspendCancellableCoroutine<Location?> { cont ->
            client.lastLocation.addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resume(null) }
        }
    }.getOrNull() ?: return null to null
    val latLng = LatLng(location.latitude, location.longitude)
    return latLng to reverseGeocode(context, location.latitude, location.longitude)
}

private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, java.util.Locale.KOREA)
    val address: Address? = if (Build.VERSION.SDK_INT >= 33) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
                override fun onError(errorMessage: String?) = cont.resume(null)
            })
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
        }
    }
    return address?.let(::shortAddress)
}

/** "대한민국 서울특별시 중구 명동2가 31-1" → "서울특별시 중구 명동2가" */
fun shortAddress(address: Address): String? {
    val line = address.getAddressLine(0) ?: return listOfNotNull(address.adminArea, address.subLocality, address.thoroughfare)
        .joinToString(" ").ifBlank { null }
    return shortAddress(line)
}

fun shortAddress(line: String): String =
    line.removePrefix("대한민국").trim().split(" ").filter { it.isNotBlank() }.take(3).joinToString(" ")

private fun openNearbyHospitals(context: Context, latLng: LatLng?) {
    val query = Uri.encode("동물병원")
    val geo = if (latLng != null) "geo:${latLng.latitude},${latLng.longitude}?q=$query" else "geo:0,0?q=$query"
    val intents = listOf(
        Intent(Intent.ACTION_VIEW, geo.toUri()),
        Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/search/$query".toUri()),
    )
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
}
