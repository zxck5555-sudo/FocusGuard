package com.focusguard.app.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.data.FocusPreferences
import com.focusguard.app.service.FocusTimerService
import com.focusguard.app.ui.blocker.EmergencyUnlockDialog
import com.focusguard.app.ui.theme.CyanAccent
import com.focusguard.app.ui.theme.DarkSurface
import com.focusguard.app.ui.theme.DarkSurfaceVariant
import com.focusguard.app.ui.theme.EmeraldPrimary
import com.focusguard.app.ui.theme.TextMuted
import com.focusguard.app.ui.theme.TextPrimary
import com.focusguard.app.ui.theme.TextSecondary
import com.focusguard.app.ui.theme.WarningOrange
import com.focusguard.app.ui.theme.WarningRed
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TimerScreen(
    onNavigateToPermissions: () -> Unit,
    onNavigateToAppSelection: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { FocusPreferences.getInstance(context) }

    val isActive by prefs.isFocusActiveFlow.collectAsState(initial = false)
    val endTime by prefs.focusEndTimeFlow.collectAsState(initial = 0L)
    val totalConfiguredSeconds by prefs.focusTotalSecondsFlow.collectAsState(initial = 25 * 60)
    val blockedPackages by prefs.blockedPackagesFlow.collectAsState(initial = emptySet())
    val totalBlockedAttempts by prefs.totalBlockedAttemptsFlow.collectAsState(initial = 0)
    val isStrict by prefs.strictModeFlow.collectAsState(initial = true)

    var selectedDurationMinutes by remember { mutableIntStateOf(25) }
    var remainingMillis by remember { mutableLongStateOf(0L) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showPermissionRequiredDialog by remember { mutableStateOf(false) }
    var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Live countdown update when timer is running
    LaunchedEffect(isActive, endTime) {
        if (isActive) {
            while (true) {
                val diff = endTime - System.currentTimeMillis()
                remainingMillis = if (diff > 0) diff else 0
                if (diff <= 0) break
                delay(500)
            }
        }
    }

    val displaySeconds = if (isActive) (remainingMillis / 1000).toInt() else (selectedDurationMinutes * 60)
    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val progress = if (isActive && totalConfiguredSeconds > 0) {
        (displaySeconds.toFloat() / totalConfiguredSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

    val durationPresets = listOf(15, 25, 45, 60, 90, 120)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FocusGuard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = if (isActive) "집중 모드 활성화 중 🛡️" else "나만의 집중 시간 설정",
                    fontSize = 13.sp,
                    color = if (isActive) EmeraldPrimary else TextSecondary
                )
            }

            // Quick Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) EmeraldPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant)
                    .border(1.dp, if (isActive) EmeraldPrimary else Color.Transparent, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isActive) "집중 진행 중" else "대기 상태",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) EmeraldPrimary else TextSecondary
                )
            }
        }

        // Accessibility Missing Warning Banner
        if (!isAccessibilityGranted) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarningRed.copy(alpha = 0.15f))
                    .border(1.dp, WarningRed.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .clickable {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚠️ 접근성 권한이 꺼져 있습니다!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningRed
                    )
                    Text(
                        text = "이 권한을 켜야 앱 차단이 작동합니다. 터치하여 켜기 >",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Circular Progress Timer
        Box(
            modifier = Modifier
                .size(270.dp)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()

                // Background track
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Foreground active arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(EmeraldPrimary, CyanAccent, EmeraldPrimary)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isActive) "남은 집중 시간" else "설정 시간",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Blocked app summary chip / button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp))
                .clickable { onNavigateToAppSelection() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (blockedPackages.isEmpty()) "차단할 앱을 선택해주세요" else "${blockedPackages.size}개의 앱 차단 설정됨",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Text(
                text = "관리 >",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanAccent
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Duration Selection Controls (Only when timer is NOT running)
        AnimatedVisibility(visible = !isActive) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "집중 시간 프리셋",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(durationPresets) { mins ->
                        val isSelected = selectedDurationMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) EmeraldPrimary else DarkSurfaceVariant)
                                .clickable { selectedDurationMinutes = mins }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mins == 25) "25분 (뽀모도로)" else "${mins}분",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "세밀한 시간 조절", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        text = "${selectedDurationMinutes}분",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Slider(
                    value = selectedDurationMinutes.toFloat(),
                    onValueChange = { selectedDurationMinutes = it.toInt() },
                    valueRange = 5f..180f,
                    steps = 34,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start / Stop Main Action Button
        Button(
            onClick = {
                if (isActive) {
                    showUnlockDialog = true
                } else {
                    if (!isAccessibilityGranted) {
                        showPermissionRequiredDialog = true
                    } else {
                        FocusTimerService.startService(context, selectedDurationMinutes * 60)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) WarningRed else EmeraldPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isActive) "집중 종료하기" else "집중 모드 시작 (${selectedDurationMinutes}분)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Statistics & Strict mode card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Block attempt counter card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "방어한 유혹", fontSize = 12.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${totalBlockedAttempts}회",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }

            // Strict mode status card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp))
                    .clickable {
                        scope.launch { prefs.setStrictMode(!isStrict) }
                    }
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isStrict) EmeraldPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "엄격 모드", fontSize = 12.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isStrict) "ON (반성문)" else "OFF (일반)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStrict) EmeraldPrimary else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showUnlockDialog) {
        EmergencyUnlockDialog(
            isStrict = isStrict,
            onDismiss = { showUnlockDialog = false },
            onConfirmUnlock = {
                showUnlockDialog = false
                FocusTimerService.stopService(context)
            }
        )
    }

    if (showPermissionRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRequiredDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "접근성 권한 필요",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    text = "유튜브, 크롬 등의 앱 실행을 감지하고 차단 화면을 띄우기 위해서는 안드로이드 시스템의 [접근성 서비스] 권한이 반드시 필요합니다.\n\n설정 화면으로 이동하여 'FocusGuard'를 켜주시겠습니까?",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRequiredDialog = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("설정으로 이동", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRequiredDialog = false }) {
                    Text("닫기", color = TextMuted)
                }
            }
        )
    }
}
