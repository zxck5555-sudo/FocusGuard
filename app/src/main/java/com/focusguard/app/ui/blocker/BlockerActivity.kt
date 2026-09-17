package com.focusguard.app.ui.blocker

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.data.FocusPreferences
import com.focusguard.app.service.FocusTimerService
import com.focusguard.app.ui.components.AppIconImage
import com.focusguard.app.ui.theme.DarkBg
import com.focusguard.app.ui.theme.DarkSurface
import com.focusguard.app.ui.theme.DarkSurfaceVariant
import com.focusguard.app.ui.theme.EmeraldPrimary
import com.focusguard.app.ui.theme.FocusGuardTheme
import com.focusguard.app.ui.theme.TextMuted
import com.focusguard.app.ui.theme.TextPrimary
import com.focusguard.app.ui.theme.TextSecondary
import com.focusguard.app.ui.theme.WarningOrange
import com.focusguard.app.ui.theme.WarningRed
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.delay
import java.util.Locale

class BlockerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_BLOCKED_APP_NAME = "extra_blocked_app_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val blockedAppName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: "차단된 앱"
        val appIcon: Drawable? = try {
            packageManager.getApplicationIcon(blockedPackage)
        } catch (e: Exception) {
            null
        }

        setContent {
            FocusGuardTheme {
                BackHandler {
                    goToHomeScreen()
                }

                BlockerScreen(
                    appName = blockedAppName,
                    appIcon = appIcon,
                    onGoHome = { goToHomeScreen() },
                    onEmergencyUnlock = {
                        FocusTimerService.stopService(this)
                        finish()
                    }
                )
            }
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun BlockerScreen(
    appName: String,
    appIcon: Drawable?,
    onGoHome: () -> Unit,
    onEmergencyUnlock: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusPreferences = remember { FocusPreferences.getInstance(context) }
    val endTime by focusPreferences.focusEndTimeFlow.collectAsState(initial = 0L)
    val isStrict by focusPreferences.strictModeFlow.collectAsState(initial = true)

    var remainingMillis by remember { mutableLongStateOf(0L) }
    var showUnlockDialog by remember { mutableStateOf(false) }

    // Live countdown update
    LaunchedEffect(endTime) {
        while (true) {
            val diff = endTime - System.currentTimeMillis()
            remainingMillis = if (diff > 0) diff else 0
            if (diff <= 0) {
                // Time is up
                onGoHome()
                break
            }
            delay(500)
        }
    }

    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val inspiringQuotes = remember {
        listOf(
            "지금 느끼는 이 유혹은 10분만 지나면 사라집니다. 호흡을 가다듬고 원래 하려던 일로 돌아가세요.",
            "성공하는 사람과 실패하는 사람의 차이는 '유혹을 느꼈을 때 스마트폰을 내려놓는 1초'에 있습니다.",
            "목표를 위해 투자한 소중한 시간입니다. 오늘의 당신을 실망시키지 마세요.",
            "스마트폰의 알고리즘에 시간을 빼앗기지 마세요. 주도권은 당신에게 있습니다."
        )
    }
    val randomQuote = remember { inspiringQuotes.random() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Warning Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(WarningRed.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                        .border(2.dp, WarningRed.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "차단됨",
                        tint = WarningRed,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "접속이 차단되었습니다",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Blocked app chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    AppIconImage(drawable = appIcon, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$appName 실행 제한 중",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningOrange
                    )
                }
            }

            // Countdown Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "남은 집중 시간",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "집중 세션이 철저하게 보호되고 있습니다",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Inspiring Quote Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "💡 $randomQuote",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "홈 화면으로 나가기 (추천)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { showUnlockDialog = true }
                ) {
                    Text(
                        text = "그래도 지금 해제해야 하나요?",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }

    if (showUnlockDialog) {
        EmergencyUnlockDialog(
            isStrict = isStrict,
            onDismiss = { showUnlockDialog = false },
            onConfirmUnlock = {
                showUnlockDialog = false
                onEmergencyUnlock()
            }
        )
    }
}

@Composable
fun EmergencyUnlockDialog(
    isStrict: Boolean,
    onDismiss: () -> Unit,
    onConfirmUnlock: () -> Unit
) {
    val penaltySentence = "지금 포기하면 후회할 것을 압니다."
    var typedText by remember { mutableStateOf("") }
    val isMatch = typedText.trim() == penaltySentence

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    text = "집중 세션 조기 해제",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column {
                if (isStrict) {
                    Text(
                        text = "엄격 모드가 켜져 있습니다. 무의식적인 해제를 방지하기 위해 아래 문장을 오타 없이 그대로 입력해야 해제할 수 있습니다:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = penaltySentence,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        placeholder = { Text("위 문장을 입력하세요", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isMatch) EmeraldPrimary else WarningOrange,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = "정말로 집중 모드를 종료하시겠습니까? 설정된 시간 전에 종료하면 목표 달성률이 낮아집니다.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmUnlock,
                enabled = !isStrict || isMatch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningRed,
                    disabledContainerColor = DarkSurfaceVariant
                )
            ) {
                Text(text = "집중 종료", color = if (!isStrict || isMatch) Color.White else TextMuted)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = "계속 집중하기", color = TextPrimary)
            }
        }
    )
}
