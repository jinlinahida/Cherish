package com.cherish.app.sync.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.event.repository.EventRepository
import com.cherish.app.storage.EventImageStorage
import com.cherish.app.sync.CherishHttpServer
import com.cherish.app.sync.QrCodeBitmapGenerator
import com.cherish.app.sync.SyncConnectionState
import com.cherish.app.sync.SyncSession
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Phone QR Code Synchronisation screen for Cherish on Wear OS.
 *
 * Hosts the local HTTP server, generates an ephemeral connection QR Code,
 * and maintains the temporary peer-to-peer sync session with the phone browser.
 */
@Composable
fun PhoneSyncScreen(
    repository: EventRepository,
    imageStorage: EventImageStorage?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    calendar: LunarCalendar = remember { DefaultLunarCalendar() },
) {
    val haptics = rememberShirokoWearHaptics()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var connectionUrl by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var localIp by remember { mutableStateOf<String?>(null) }
    var isConnected by remember { mutableStateOf(false) }

    val session = remember { SyncSession() }
    var actualPort by remember { mutableStateOf(session.port) }
    val server = remember(session) {
        CherishHttpServer(
            session = session,
            repository = repository,
            imageStorage = imageStorage,
            calendar = calendar,
            onEventSynced = {
                haptics.click()
                isConnected = true
            },
        )
    }

    // Keep screen awake while displaying connection QR Code to allow phone camera scanning
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Intercept back gesture to safely terminate server
    BackHandler {
        haptics.back()
        server.stop()
        onBack()
    }

    // Manage server lifecycle and clean up orphaned images on exit
    DisposableEffect(server) {
        onDispose {
            server.stop()
            val activePaths = repository.getAll().mapNotNull {
                (it.background as? com.cherish.app.event.model.EventBackground.Image)?.path
            }.toSet()
            imageStorage?.cleanupOrphanedImages(activePaths)
        }
    }

    fun checkNetworkAndStart() {
        coroutineScope.launch(Dispatchers.IO) {
            val ip = session.resolveLocalIpAddress()
            localIp = ip

            if (ip != null) {
                val boundPort = server.start()
                actualPort = boundPort
                val url = "http://$ip:$boundPort/?token=${session.token}"
                connectionUrl = url

                val bitmap = runCatching {
                    QrCodeBitmapGenerator.generateQrBitmap(url, size = 220)
                }.getOrNull()
                qrBitmap = bitmap
            }
        }
    }

    LaunchedEffect(Unit) {
        checkNetworkAndStart()
    }

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(
                text = "手机编辑",
                modifier = Modifier.semantics { heading() },
            )
        }

        if (localIp == null) {
            // No local network connection
            item(key = "no_network_card") {
                ShirokoWearCard(
                    shape = ShirokoWearShapes.card,
                    highlighted = true,
                    highlightColor = ShirokoWearTheme.colors.accentCopper,
                    innerPadding = PaddingValues(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "未检测到本地网络",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ShirokoWearTheme.colors.accentCopper,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "请先将手表连接至与手机同一 Wi-Fi 网络，或开启手机热点供手表连接。",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item(key = "btn_retry_network") {
                ShirokoWearCardButton(
                    onClick = {
                        haptics.click()
                        checkNetworkAndStart()
                    },
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = "重新检测本地网络"
                    },
                ) {
                    Text(
                        text = "🔄 重新检测网络",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ShirokoWearTheme.colors.accentGold,
                    )
                }
            }
        } else {
            // Network connected - Show QR Code
            item(key = "qr_hint") {
                Text(
                    text = "手机相机扫码即可在浏览器中编辑",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { },
                )
            }

            if (qrBitmap != null) {
                item(key = "qr_code_card") {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(8.dp)
                            .semantics {
                                contentDescription = "用于手机扫码连接的二维码"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // Connection state badge
            item(key = "connection_status") {
                val stateText = if (isConnected || session.state == SyncConnectionState.CONNECTED) {
                    "🟢 手机已连接 (实时同步中)"
                } else {
                    "⚪ 等待手机扫码连接..."
                }
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isConnected) ShirokoWearTheme.colors.accentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Address text
            item(key = "ip_address_text") {
                Text(
                    text = "http://$localIp:$actualPort/",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "ip_hint_text") {
                Text(
                    text = "请确保手机与手表连接同一 Wi-Fi 或手机热点",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "btn_done") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(
                onClick = {
                    haptics.back()
                    server.stop()
                    onBack()
                },
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "断开连接并返回"
                },
            ) {
                Text(
                    text = "完成 / 断开连接",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ShirokoWearTheme.colors.accentGold,
                )
            }
        }
    }
}
