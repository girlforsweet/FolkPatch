package me.bmax.apatch.ui.screen

import android.content.Context
import android.os.Build
import android.system.Os
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.pm.PackageInfoCompat
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.ui.screen.BottomBarDestination
import me.bmax.apatch.util.AppData
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.surfaceColorAtElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreenCircle(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    val showUninstallDialog = remember { mutableStateOf(false) }
    val showAuthFailedTipDialog = remember { mutableStateOf(false) }
    val showAuthKeyDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }
    if (showAuthFailedTipDialog.value) {
        AuthFailedTipDialog(showDialog = showAuthFailedTipDialog)
    }
    if (showAuthKeyDialog.value) {
        AuthSuperKey(showDialog = showAuthKeyDialog, showFailedDialog = showAuthFailedTipDialog)
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val context = LocalContext.current
        
        if (BackgroundConfig.isCustomBackgroundEnabled) {
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
        
        StatusCardCircle(kpState, apState, navigator, showUninstallDialog, showAuthKeyDialog)

        val showCoreCards = kpState != APApplication.State.UNKNOWN_STATE
        if (showCoreCards) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val superuserCount by AppData.DataRefreshManager.superuserCount.collectAsState()
                val moduleCount by AppData.DataRefreshManager.apmModuleCount.collectAsState()
                
                TonalCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigator.navigate(BottomBarDestination.SuperUser.direction) }
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.superuser),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (superuserCount > 0) "$superuserCount Apps" else "0 Apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                TonalCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigator.navigate(BottomBarDestination.AModule.direction) }
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Widgets,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.module),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (moduleCount > 0) "$moduleCount Active" else "0 Modules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
             AStatusCardCircle(apState)
        }

        InfoCardCircle(kpState, apState)

        val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
        if (!hideApatchCard) {
            LearnMoreCardCircle()
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun StatusCardCircle(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator,
    showUninstallDialog: MutableState<Boolean>,
    showAuthKeyDialog: MutableState<Boolean>
) {
    val isWorking = kpState == APApplication.State.KERNELPATCH_INSTALLED
    val isUpdate = kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || kpState == APApplication.State.KERNELPATCH_NEED_REBOOT
    val classicEmojiEnabled = BackgroundConfig.isListWorkingCardModeHidden
    
    val finalContainerColor = if (isWorking) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    
    TonalCard(
        containerColor = finalContainerColor,
        elevation = 1.dp 
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    if (isWorking) {
                        if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                            showUninstallDialog.value = true
                        }
                    } else if (kpState == APApplication.State.UNKNOWN_STATE) {
                        showAuthKeyDialog.value = true
                    } else {
                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination)
                    }
                }
                .padding(24.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWorking) {
                 Surface(
                     shape = RoundedCornerShape(16.dp),
                     color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
                 ) {
                     Icon(
                         Icons.Outlined.CheckCircle, 
                         stringResource(R.string.home_working),
                         modifier = Modifier.padding(12.dp).size(30.dp),
                         tint = MaterialTheme.colorScheme.primary
                     )
                 }
                 Column(Modifier.padding(start = 20.dp)) {
                     val modeText = if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) "FULL" else "HALF"
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                             text = if (classicEmojiEnabled) {
                                 stringResource(R.string.home_working) + "😋"
                             } else {
                                 stringResource(R.string.home_working)
                             },
                             style = MaterialTheme.typography.titleLarge,
                             fontWeight = FontWeight.ExtraBold,
                             letterSpacing = (-0.5).sp
                         )
                         Spacer(Modifier.width(10.dp))
                         
                        if (!classicEmojiEnabled) {
                            LabelText(label = modeText)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_version, getManagerVersion().second.toString()) +
                                if (classicEmojiEnabled) " - $modeText" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
            } else {
                 val icon = if (isUpdate) Icons.Outlined.SystemUpdate else Icons.Outlined.Warning
                 val title = if (isUpdate) stringResource(R.string.home_kp_need_update) else stringResource(R.string.home_not_installed)
                 
                 Surface(
                     shape = RoundedCornerShape(16.dp),
                     color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.12f)
                 ) {
                     Icon(
                         icon, 
                         title,
                         modifier = Modifier.padding(12.dp).size(30.dp),
                         tint = MaterialTheme.colorScheme.error
                     )
                 }
                 Column(Modifier.padding(start = 20.dp)) {
                     Text(
                         text = title,
                         style = MaterialTheme.typography.titleLarge,
                         fontWeight = FontWeight.ExtraBold,
                         letterSpacing = (-0.5).sp
                     )
                     Spacer(Modifier.height(2.dp))
                     Text(
                         text = stringResource(R.string.home_click_to_install),
                         style = MaterialTheme.typography.bodyMedium,
                         color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                     )
                 }
            }
        }
    }
}

@Composable
fun InfoCardCircle(kpState: APApplication.State, apState: APApplication.State) {
    val context = LocalContext.current

    TonalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            val uname = Os.uname()
            val prefs = APApplication.sharedPreferences

            var hideSuPath by remember { mutableStateOf(prefs.getBoolean("hide_su_path", false)) }
            var hideKpatchVersion by remember { mutableStateOf(prefs.getBoolean("hide_kpatch_version", false)) }
            var hideFingerprint by remember { mutableStateOf(prefs.getBoolean("hide_fingerprint", false)) }
            var hideZygisk by remember { mutableStateOf(prefs.getBoolean("hide_zygisk", false)) }
            var hideMount by remember { mutableStateOf(prefs.getBoolean("hide_mount", false)) }
            

            DisposableEffect(Unit) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    when (key) {
                        "hide_su_path" -> hideSuPath = sharedPreferences.getBoolean("hide_su_path", false)
                        "hide_kpatch_version" -> hideKpatchVersion = sharedPreferences.getBoolean("hide_kpatch_version", false)
                        "hide_fingerprint" -> hideFingerprint = sharedPreferences.getBoolean("hide_fingerprint", false)
                        "hide_zygisk" -> hideZygisk = sharedPreferences.getBoolean("hide_zygisk", false)
                        "hide_mount" -> hideMount = sharedPreferences.getBoolean("hide_mount", false)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            var zygiskImplement by remember { mutableStateOf("None") }
            var mountImplement by remember { mutableStateOf("None") }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                        mountImplement = me.bmax.apatch.util.getMountImplement()
                    } catch (_: Exception) {
                    }
                }
            }

            @Composable
            fun InfoCardItem(
                label: String,
                content: String,
                icon: @Composable () -> Unit
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = label, 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            @Composable
            fun InfoCardItem(icon: ImageVector, label: String, content: String) = InfoCardItem(
                label = label,
                content = content,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            val managerVersion = getManagerVersion()
            InfoCardItem(
                icon = Icons.Outlined.Apps,
                label = stringResource(R.string.home_manager_version),
                content = "${managerVersion.first} (${managerVersion.second})"
            )

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion) {
                InfoCardItem(
                    icon = Icons.Outlined.Extension,
                    label = stringResource(R.string.home_kpatch_version),
                    content = Version.installedKPVString()
                )
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath) {
                InfoCardItem(
                    icon = Icons.Outlined.Code,
                    label = stringResource(R.string.home_su_path),
                    content = Natives.suPath()
                )
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(
                    icon = Icons.Outlined.Android,
                    label = stringResource(R.string.home_apatch_version),
                    content = managerVersion.second.toString()
                )
            }

            InfoCardItem(
                icon = Icons.Outlined.PhoneAndroid,
                label = stringResource(R.string.home_device_info),
                content = getDeviceInfo()
            )

            InfoCardItem(
                icon = Icons.Outlined.DeveloperBoard,
                label = stringResource(R.string.home_kernel),
                content = uname.release
            )

            InfoCardItem(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.home_system_version),
                content = getSystemVersion()
            )

            if (!hideFingerprint) {
                InfoCardItem(
                    icon = Icons.Outlined.Fingerprint,
                    label = stringResource(R.string.home_fingerprint),
                    content = Build.FINGERPRINT
                )
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk) {
                InfoCardItem(
                    icon = Icons.Outlined.Layers,
                    label = stringResource(R.string.home_zygisk_implement),
                    content = zygiskImplement
                )
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount) {
                InfoCardItem(
                    icon = Icons.Outlined.SdStorage,
                    label = stringResource(R.string.home_mount_implement),
                    content = mountImplement
                )
            }

            InfoCardItem(
                icon = Icons.Outlined.Shield,
                label = stringResource(R.string.home_selinux_status),
                content = getSELinuxStatus()
            )
        }
    }
}

@Composable
fun LabelText(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(containerColor, containerColor.copy(alpha = 0.85f))
                ),
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        )
    }
}

@Composable
fun AStatusCardCircle(apState: APApplication.State) {
    TonalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when (apState) {
                        APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> Icons.Outlined.Block
                        APApplication.State.ANDROIDPATCH_INSTALLING -> Icons.Outlined.InstallMobile
                        APApplication.State.ANDROIDPATCH_INSTALLED -> Icons.Outlined.CheckCircle
                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> Icons.Outlined.SystemUpdate
                        else -> Icons.Outlined.Help
                    }
                    
                    Icon(
                        statusIcon, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.android_patch),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (apState != APApplication.State.UNKNOWN_STATE) {
                    Button(
                        onClick = {
                            when (apState) {
                                APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> APApplication.installApatch()
                                APApplication.State.ANDROIDPATCH_UNINSTALLING -> {}
                                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> APApplication.installApatch()
                                else -> APApplication.uninstallApatch()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        val btnText = when (apState) {
                            APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> stringResource(id = R.string.home_ap_cando_install)
                            APApplication.State.ANDROIDPATCH_UNINSTALLING -> ""
                            APApplication.State.ANDROIDPATCH_NEED_UPDATE -> stringResource(id = R.string.home_kp_cando_update)
                            else -> stringResource(id = R.string.home_ap_cando_uninstall)
                        }
                        if (apState == APApplication.State.ANDROIDPATCH_UNINSTALLING) {
                            Icon(Icons.Outlined.Cached, contentDescription = "busy", modifier = Modifier.size(20.dp))
                        } else {
                            Text(text = btnText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (apState) {
                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> stringResource(R.string.home_not_installed)
                    APApplication.State.ANDROIDPATCH_INSTALLING -> stringResource(R.string.home_installing)
                    APApplication.State.ANDROIDPATCH_INSTALLED -> stringResource(R.string.home_working)
                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> stringResource(R.string.home_kp_need_update)
                    else -> stringResource(R.string.home_install_unknown)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 38.dp)
            )
        }
    }
}

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.border(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            shape = shape
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = shape
    ) {
        content()
    }
}

@Composable
fun LearnMoreCardCircle() {
    val uriHandler = LocalUriHandler.current

    TonalCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://fp.mysqil.com/")
                }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lightbulb, 
                    null, 
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                )
            }
        }
    }
}
