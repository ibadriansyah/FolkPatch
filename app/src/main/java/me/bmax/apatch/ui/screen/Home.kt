package me.bmax.apatch.ui.screen

import android.os.Build
import android.system.Os
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.ProvideMenuShape
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils

private val managerVersion = getManagerVersion()

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    var showPatchFloatAction by remember { mutableStateOf(true) }

    val kpState by APApplication.kpStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val apState by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    if (kpState != APApplication.State.UNKNOWN_STATE) {
        showPatchFloatAction = false
    }

    Scaffold(topBar = {
        TopBar(onInstallClick = dropUnlessResumed {
            navigator.navigate(InstallModeSelectScreenDestination)
        }, navigator, kpState)
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(0.dp))
            KStatusCard(kpState, apState, navigator)
            if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                AStatusCard(apState)
            }
            InfoCard(kpState, apState)
            val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", true)
            if (!hideApatchCard) {
                LearnMoreCard()
            }
            Spacer(Modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallDialog(showDialog: MutableState<Boolean>, navigator: DestinationsNavigator) {
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_dialog_uninstall_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        showDialog.value = false
                        APApplication.uninstallApatch()
                    }) {
                        Text(text = stringResource(id = R.string.home_dialog_uninstall_ap_only))
                    }

                    TextButton(onClick = {
                        showDialog.value = false
                        APApplication.uninstallApatch()
                        navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
                    }) {
                        Text(text = stringResource(id = R.string.home_dialog_uninstall_all))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthFailedTipDialog(showDialog: MutableState<Boolean>) {
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOff
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                // Title
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_dialog_auth_fail_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                // Content
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 24.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_dialog_auth_fail_content),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }

}

val checkSuperKeyValidation: (superKey: String) -> Boolean = { superKey ->
    superKey.length in 8..63 && superKey.any { it.isDigit() } && superKey.any { it.isLetter() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSuperKey(showDialog: MutableState<Boolean>, showFailedDialog: MutableState<Boolean>) {
    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var enable by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOff
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                // Title
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_auth_key_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                // Content
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_auth_key_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Content2
                Box(
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    OutlinedTextField(
                        value = key,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        onValueChange = {
                            key = it
                            enable = checkSuperKeyValidation(key)
                        },
                        shape = RoundedCornerShape(50.0f),
                        label = { Text(stringResource(id = R.string.super_key)) },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    IconButton(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(top = 15.dp, end = 5.dp),
                        onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(onClick = {
                        showDialog.value = false

                        val preVerifyKey = Natives.nativeReady(key)
                        if (preVerifyKey) {
                            APApplication.superKey = key
                        } else {
                            showFailedDialog.value = true
                        }

                    }, enabled = enable) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
        val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
        APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
    }
}

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(text = {
        Text(stringResource(id))
    }, onClick = {
        reboot(reason)
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onInstallClick: () -> Unit, navigator: DestinationsNavigator, kpState: APApplication.State
) {
    val uriHandler = LocalUriHandler.current
    var showDropdownMoreOptions by remember { mutableStateOf(false) }
    var showDropdownReboot by remember { mutableStateOf(false) }
    val prefs = APApplication.sharedPreferences
    val currentTitle = prefs.getString("app_title", "folkpatch") ?: "folkpatch"
    val titleResId = when (currentTitle) {
        "fpatch" -> R.string.app_title_fpatch
        "apatch_folk" -> R.string.app_title_apatch_folk
        "apatchx" -> R.string.app_title_apatchx
        "apatch" -> R.string.app_title_apatch
        "kernelpatch" -> R.string.app_title_kernelpatch
        "supersu" -> R.string.app_title_supersu
        "folksu" -> R.string.app_title_folksu
        "superuser" -> R.string.app_title_superuser
        "superpatch" -> R.string.app_title_superpatch
        "magicpatch" -> R.string.app_title_magicpatch
        else -> R.string.app_title_folkpatch
    }

    TopAppBar(title = {
        Text(stringResource(titleResId))
    }, actions = {
        IconButton(onClick = onInstallClick) {
            Icon(
                imageVector = Icons.Filled.InstallMobile,
                contentDescription = stringResource(id = R.string.mode_select_page_title)
            )
        }

        if (kpState != APApplication.State.UNKNOWN_STATE) {
            IconButton(onClick = {
                showDropdownReboot = true
            }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(id = R.string.reboot)
                )

                WallpaperAwareDropdownMenu(
                    expanded = showDropdownReboot,
                    onDismissRequest = { showDropdownReboot = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(id = R.string.reboot)) },
                        onClick = { reboot() }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(id = R.string.reboot_recovery)) },
                        onClick = { reboot("recovery") }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(id = R.string.reboot_bootloader)) },
                        onClick = { reboot("bootloader") }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(id = R.string.reboot_download)) },
                        onClick = { reboot("download") }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(id = R.string.reboot_edl)) },
                        onClick = { reboot("edl") }
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { showDropdownMoreOptions = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.settings)
                )
                WallpaperAwareDropdownMenu(
                    expanded = showDropdownMoreOptions,
                    onDismissRequest = { showDropdownMoreOptions = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.home_more_menu_feedback_or_suggestion)) },
                        onClick = {
                            showDropdownMoreOptions = false
                            uriHandler.openUri("https://github.com/matsuzaka-yuki/FolkPatch/issues/new/choose")
                        }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.home_more_menu_about)) },
                        onClick = {
                            navigator.navigate(AboutScreenDestination)
                            showDropdownMoreOptions = false
                        }
                    )
                }
            }
        }
    })
}

@Composable
private fun StatusBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = containerColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = contentColor
        )
    }
}

@Composable
private fun KStatusCard(
    kpState: APApplication.State, apState: APApplication.State, navigator: DestinationsNavigator
) {

    val showAuthFailedTipDialog = remember { mutableStateOf(false) }
    if (showAuthFailedTipDialog.value) {
        AuthFailedTipDialog(showDialog = showAuthFailedTipDialog)
    }

    val showAuthKeyDialog = remember { mutableStateOf(false) }
    if (showAuthKeyDialog.value) {
        AuthSuperKey(showDialog = showAuthKeyDialog, showFailedDialog = showAuthFailedTipDialog)
    }

    val showUninstallDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }

    val prefs = APApplication.sharedPreferences
    val simpleKernelVersionMode = remember { mutableStateOf(prefs.getBoolean("simple_kernel_version_mode", false)) }
    val darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", true)
    val nightModeEnabled = prefs.getBoolean("night_mode_enabled", false)
    val isDarkTheme = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }

    val (cardBackgroundColor, cardContentColor) = when (kpState) {
        APApplication.State.KERNELPATCH_INSTALLED -> {
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                val opacity = BackgroundConfig.customBackgroundOpacity
                val contentColor = if (opacity <= 0.1f) {
                    if (isDarkTheme) Color.White else Color.Black
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
                MaterialTheme.colorScheme.primary.copy(alpha = opacity) to contentColor
            } else {
                MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            }
        }

        APApplication.State.KERNELPATCH_NEED_UPDATE, APApplication.State.KERNELPATCH_NEED_REBOOT -> {
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.secondary.copy(alpha = BackgroundConfig.customBackgroundOpacity) to MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
            }
        }

        else -> {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardBackgroundColor,
            contentColor = cardContentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 4.dp
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Status Section - hanya untuk status NEED_UPDATE dan lainnya
            if (kpState != APApplication.State.KERNELPATCH_INSTALLED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (kpState) {
                        APApplication.State.KERNELPATCH_NEED_UPDATE, APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                            Icon(
                                Icons.Outlined.SystemUpdate,
                                stringResource(R.string.home_kp_need_update),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        else -> {
                            Icon(
                                Icons.AutoMirrored.Outlined.HelpOutline,
                                "Unknown",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when (kpState) {
                            APApplication.State.KERNELPATCH_NEED_UPDATE, APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                                Text(
                                    text = stringResource(R.string.home_kp_need_update),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(
                                        R.string.kpatch_version_update,
                                        Version.installedKPVString(),
                                        Version.buildKPVString()
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            else -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_install_unknown),
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = stringResource(R.string.home_install_unknown_summary),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        val onAction = {
                            when (kpState) {
                                APApplication.State.UNKNOWN_STATE -> {
                                    showAuthKeyDialog.value = true
                                }

                                APApplication.State.KERNELPATCH_NEED_UPDATE -> {
                                    if (Version.installedKPVUInt() < 0x900u) {
                                        navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
                                    } else {
                                        navigator.navigate(InstallModeSelectScreenDestination)
                                    }
                                }

                                APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                                    reboot()
                                }

                                APApplication.State.KERNELPATCH_UNINSTALLING -> {
                                    // Do nothing
                                }

                                else -> {}
                            }
                        }

                        Button(
                            onClick = { onAction() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            when (kpState) {
                                APApplication.State.UNKNOWN_STATE -> {
                                    Text(
                                        text = stringResource(id = R.string.super_key),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                APApplication.State.KERNELPATCH_NEED_UPDATE -> {
                                    Text(
                                        text = stringResource(id = R.string.home_kp_cando_update),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                                    Text(
                                        text = stringResource(id = R.string.home_ap_cando_reboot),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                APApplication.State.KERNELPATCH_UNINSTALLING -> {
                                    Icon(
                                        Icons.Outlined.Cached,
                                        contentDescription = "busy",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }

            // Info Section (Working, APVersion, System) - hanya tampil jika INSTALLED
            if (kpState == APApplication.State.KERNELPATCH_INSTALLED) {
                val showApv = apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Working (large) | APVersion (small)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Working Card dengan status dan button
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top: Icon dan Working Full badge
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = stringResource(R.string.home_working),
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.home_working),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) "Full" else "Half",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Bottom: Uninstall Button
                                val onUninstall = {
                                    if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                                        showUninstallDialog.value = true
                                    } else {
                                        navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
                                    }
                                }

                                Button(
                                    onClick = { onUninstall() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.home_ap_cando_uninstall),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        // Right Column: APVersion (top) + System (bottom)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // APVersion Card
                            if (showApv) {
                                DeviceInfoSmallCard(
                                    icon = Icons.Filled.CheckCircle,
                                    title = stringResource(R.string.home_apatch_version),
                                    value = managerVersion.second.toString()
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // System Card (below APVersion)
                            DeviceInfoSmallCard(
                                icon = Icons.Filled.InstallMobile,
                                title = "System",
                                value = getSystemVersion()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AStatusCard(apState: APApplication.State) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 4.dp
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.android_patch),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (apState) {
                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                        Icon(
                            Icons.Outlined.Block,
                            stringResource(R.string.home_not_installed),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    APApplication.State.ANDROIDPATCH_INSTALLING -> {
                        Icon(
                            Icons.Outlined.InstallMobile,
                            stringResource(R.string.home_installing),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    APApplication.State.ANDROIDPATCH_INSTALLED -> {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            stringResource(R.string.home_working),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    else -> {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            stringResource(R.string.home_install_unknown),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (apState) {
                        APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                            Text(
                                text = stringResource(R.string.home_not_installed),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_INSTALLING -> {
                            Text(
                                text = stringResource(R.string.home_installing),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_INSTALLED -> {
                            Text(
                                text = stringResource(R.string.home_working),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        else -> {
                            Text(
                                text = stringResource(R.string.home_install_unknown),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                if (apState != APApplication.State.UNKNOWN_STATE) {
                    Button(
                        onClick = {
                            when (apState) {
                                APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                    APApplication.installApatch()
                                }

                                APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                    // Do nothing
                                }

                                else -> {
                                    APApplication.uninstallApatch()
                                }
                            }
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        when (apState) {
                            APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                Text(
                                    text = stringResource(id = R.string.home_ap_cando_install),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                Icon(
                                    Icons.Outlined.Cached,
                                    contentDescription = "busy",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            else -> {
                                Text(
                                    text = stringResource(id = R.string.home_ap_cando_uninstall),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun WarningCard() {
    var show by rememberSaveable { mutableStateOf(apApp.getBackupWarningState()) }
    if (show) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "warning",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.patch_warnning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                IconButton(
                    onClick = {
                        show = false
                        apApp.updateBackupWarningState(false)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "close",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun getSystemVersion(): String {
    return "${Build.VERSION.RELEASE} ${if (Build.VERSION.PREVIEW_SDK_INT != 0) "Preview" else ""} (API ${Build.VERSION.SDK_INT})"
}

private fun getDeviceInfo(): String {
    var manufacturer =
        Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
    if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
        manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
    }
    manufacturer += " " + Build.MODEL + " "
    return manufacturer
}

@Composable
private fun InfoCard(kpState: APApplication.State, apState: APApplication.State) {
    val hideKpatchVersion = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_kpatch_version", false)) }
    val hideFingerprint = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_fingerprint", false)) }
    
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 4.dp
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val uname = Os.uname()
            val contents = StringBuilder()

            @Composable
            fun InfoItem(label: String, value: String) {
                contents.appendLine(label).appendLine(value).appendLine()
            }

            val showKpv = kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion.value

            // Row 1: Device (Full width)
            DeviceInfoFullCard(
                icon = Icons.Filled.Devices,
                title = "Device",
                value = getDeviceInfo(),
                modifier = Modifier.fillMaxWidth()
            )

            // Row 2: Kernel (Full width)
            DeviceInfoFullCard(
                icon = Icons.Filled.Memory,
                title = "Kernel",
                value = uname.release,
                modifier = Modifier.fillMaxWidth()
            )

            // Row 3: SELinux (small) | Build (small)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                DeviceInfoSmallCard(
                    icon = Icons.Filled.Security,
                    title = "SELinux",
                    value = getSELinuxStatus(),
                    modifier = Modifier.weight(1f)
                )

                if (!hideFingerprint.value) {
                    DeviceInfoSmallCard(
                        icon = Icons.Filled.DeveloperMode,
                        title = "Build",
                        value = Build.FINGERPRINT.takeLast(8),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Row 4: KernelPatch Version (Full width)
            if (showKpv) {
                contents.appendLine(stringResource(R.string.home_kpatch_version)).appendLine(Version.installedKPVString()).appendLine()
                DeviceInfoFullCard(
                    icon = Icons.Outlined.SystemUpdate,
                    title = stringResource(R.string.home_kpatch_version),
                    value = Version.installedKPVString(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Row 5: Fingerprint (small) | ABI List (small)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!hideFingerprint.value) {
                    DeviceInfoSmallCard(
                        icon = Icons.Filled.DeveloperMode,
                        title = "Fingerprint",
                        value = Build.FINGERPRINT,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                DeviceInfoSmallCard(
                    icon = Icons.Filled.Memory,
                    title = "ABI List",
                    value = "arm64-v8a",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DeviceInfoLargeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Devices,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(90.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceInfoSmallCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Devices,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 60.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DeviceInfoFullCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Devices,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 50.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DeviceInfoMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Devices,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(120.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Transparent)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WarningCard(
    message: String,
    color: Color = MaterialTheme.colorScheme.errorContainer,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://apatch.dev")
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.InstallMobile,
                contentDescription = "Learn APatch",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}