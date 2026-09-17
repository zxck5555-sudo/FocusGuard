package com.focusguard.app.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.focusguard.app.service.AppBlockerAccessibilityService
import com.focusguard.app.ui.theme.DarkBg
import com.focusguard.app.ui.theme.DarkSurface
import com.focusguard.app.ui.theme.DarkSurfaceVariant
import com.focusguard.app.ui.theme.EmeraldPrimary
import com.focusguard.app.ui.theme.TextMuted
import com.focusguard.app.ui.theme.TextPrimary
import com.focusguard.app.ui.theme.TextSecondary
import com.focusguard.app.ui.theme.WarningOrange

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityGranted by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }
    var isNotificationGranted by remember { mutableStateOf(false) }
    var isBatteryIgnored by remember { mutableStateOf(false) }

    fun refreshPermissionStates() {
        isAccessibilityGranted = isAccessibilityServiceEnabled(context)
        isOverlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        isNotificationGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "필수 권한 설정",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Text(
            text = "앱 차단이 정상 작동하기 위해 권한을 켜주세요",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Accessibility Service
        PermissionItemCard(
            title = "1. 접근성 서비스 권한 (핵심)",
            description = "사용자가 차단 대상 앱을 열었을 때 0초 지연으로 실시간 감지하여 차단합니다.",
            icon = Icons.Default.Accessibility,
            isGranted = isAccessibilityGranted,
            onRequest = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Overlay Permission
        PermissionItemCard(
            title = "2. 다른 앱 위에 표시 (오버레이)",
            description = "차단된 앱 실행 시 '집중 시간입니다' 전체화면 잠금 팝업을 띄웁니다.",
            icon = Icons.Default.Layers,
            isGranted = isOverlayGranted,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Notifications Permission
        PermissionItemCard(
            title = "3. 알림 권한",
            description = "상태 표시줄에 남은 집중 시간을 실시간으로 카운트다운합니다.",
            icon = Icons.Default.Notifications,
            isGranted = isNotificationGranted,
            onRequest = {
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Battery Optimization Exemption
        PermissionItemCard(
            title = "4. 배터리 절전 예외",
            description = "화면이 꺼져도 시스템에 의해 타이머가 강제 종료되지 않도록 보호합니다.",
            icon = Icons.Default.BatteryChargingFull,
            isGranted = isBatteryIgnored,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (isGranted) EmeraldPrimary.copy(alpha = 0.3f) else DarkSurfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) EmeraldPrimary else WarningOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "허용됨",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "필요함",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextSecondary
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(text = "권한 설정하러 가기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    val expectedServiceName = "${context.packageName}/${AppBlockerAccessibilityService::class.java.name}"
    val expectedSimpleName = AppBlockerAccessibilityService::class.java.name

    for (service in enabledServices) {
        val id = service.id
        if (id.contains(expectedServiceName) || id.contains(expectedSimpleName)) {
            return true
        }
    }
    return AppBlockerAccessibilityService.isServiceRunning
}
