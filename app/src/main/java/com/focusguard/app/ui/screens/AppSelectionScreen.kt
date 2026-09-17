package com.focusguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.data.AppInfo
import com.focusguard.app.data.AppManager
import com.focusguard.app.data.FocusPreferences
import com.focusguard.app.ui.components.AppIconImage
import com.focusguard.app.ui.theme.CyanAccent
import com.focusguard.app.ui.theme.DarkBg
import com.focusguard.app.ui.theme.DarkSurface
import com.focusguard.app.ui.theme.DarkSurfaceVariant
import com.focusguard.app.ui.theme.EmeraldPrimary
import com.focusguard.app.ui.theme.TextMuted
import com.focusguard.app.ui.theme.TextPrimary
import com.focusguard.app.ui.theme.TextSecondary
import com.focusguard.app.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@Composable
fun AppSelectionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { FocusPreferences.getInstance(context) }
    val appManager = remember { AppManager(context) }

    val blockedPackages by prefs.blockedPackagesFlow.collectAsState(initial = emptySet())

    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load installed apps
    LaunchedEffect(blockedPackages) {
        installedApps = appManager.getInstalledApps(blockedPackages)
        isLoading = false
    }

    val filteredApps = remember(searchQuery, installedApps, blockedPackages) {
        installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "차단 앱 관리",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "집중 시 실행을 막을 앱을 선택하세요",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${blockedPackages.size}개 선택됨",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("앱 이름 검색...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Action Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Auto select common distracting apps
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .clickable {
                        scope.launch {
                            val newSet = blockedPackages.toMutableSet()
                            val installedPackages = installedApps.map { it.packageName }.toSet()
                            for (pkg in appManager.commonDistractingPackages) {
                                if (installedPackages.contains(pkg)) {
                                    newSet.add(pkg)
                                }
                            }
                            prefs.setBlockedPackages(newSet)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SNS/유튜브 일괄 선택",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            // Clear All
            if (blockedPackages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .clickable {
                            scope.launch { prefs.setBlockedPackages(emptySet()) }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "전체 해제",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "검색된 앱이 없습니다.",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isBlocked = blockedPackages.contains(app.packageName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
                            .border(
                                1.dp,
                                if (isBlocked) EmeraldPrimary.copy(alpha = 0.4f) else DarkSurfaceVariant,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                scope.launch { prefs.toggleBlockedPackage(app.packageName) }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AppIconImage(
                                drawable = app.icon,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = app.appName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        Switch(
                            checked = isBlocked,
                            onCheckedChange = {
                                scope.launch { prefs.toggleBlockedPackage(app.packageName) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
