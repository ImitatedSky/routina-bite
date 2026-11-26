package com.routina.bite.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.app.NotificationManager
import android.os.Build
import com.routina.bite.R
import com.routina.bite.Reminders
import java.time.LocalTime
import com.routina.bite.data.BackupCodec
import com.routina.bite.data.logMealPinShortcut
import com.routina.bite.data.waterPinShortcut
import com.routina.bite.model.Targets
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BiteViewModel,
    onBack: () -> Unit
) {
    val targets by viewModel.targets.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 有任何一筆帶照片才匯出成 zip；沒有照片的使用者流程與以前完全一樣
    val hasPhotos = entries.any { it.photo.isNotEmpty() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var kcal by remember { mutableStateOf(targets.kcal.toString()) }
    var protein by remember { mutableStateOf(targets.protein.toString()) }
    var fat by remember { mutableStateOf(targets.fat?.toString().orEmpty()) }
    var carbs by remember { mutableStateOf(targets.carbs?.toString().orEmpty()) }
    var water by remember { mutableStateOf(targets.water.toString()) }

    // 匯入會改掉目標，欄位要跟著換成新值
    LaunchedEffect(targets) {
        kcal = targets.kcal.toString()
        protein = targets.protein.toString()
        fat = targets.fat?.toString().orEmpty()
        carbs = targets.carbs?.toString().orEmpty()
        water = targets.water.toString()
    }

    val kcalValue = kcal.toIntOrNull()
    val proteinValue = protein.toIntOrNull()
    val waterValue = water.toIntOrNull()
    val kcalOk = kcalValue != null && kcalValue > 0
    val proteinOk = proteinValue != null && proteinValue > 0
    val waterOk = waterValue != null && waterValue > 0

    val targetsSaved = stringResource(R.string.targets_saved)
    val exportDone = stringResource(R.string.export_done)
    val exportFailed = stringResource(R.string.export_failed)
    val importDone = stringResource(R.string.import_done)
    val importUnreadable = stringResource(R.string.import_failed_unreadable)
    val importNotBite = stringResource(R.string.import_failed_not_bite)
    val importNewer = stringResource(R.string.import_failed_newer)

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    val pinUnsupported = stringResource(R.string.shortcut_pin_unsupported)

    // 桌面同不同意由它自己決定，所以沒有「已加到桌面」這種回饋，
    // 只有「這個桌面根本不支援」才要講
    fun pinShortcut(shortcut: ShortcutInfoCompat) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            toast(pinUnsupported)
            return
        }
        runCatching { ShortcutManagerCompat.requestPinShortcut(context, shortcut, null) }
            .onFailure { toast(pinUnsupported) }
    }

    // CreateDocument 的 MIME 在建構時就固定，所以兩種格式各準備一個，按下匯出時再挑
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup(uri, asZip = false) { ok -> toast(if (ok) exportDone else exportFailed) }
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup(uri, asZip = true) { ok -> toast(if (ok) exportDone else exportFailed) }
    }

    // 檔案管理器對 .json 與 .zip 標的 MIME 不一致，一律放行由內容判斷
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importBackup(uri) { outcome ->
            when (outcome) {
                is ImportOutcome.Ok -> toast(
                    String.format(importDone, outcome.foods, outcome.entries, outcome.weights, outcome.notes)
                )
                is ImportOutcome.Failed -> toast(
                    when (outcome.reason) {
                        BackupCodec.Reject.UNREADABLE -> importUnreadable
                        BackupCodec.Reject.NOT_BITE -> importNotBite
                        BackupCodec.Reject.NEWER_SCHEMA -> importNewer
                    }
                )
            }
        }
    }

    var reminderTimes by remember { mutableStateOf(Reminders.times(context)) }
    // null 是沒開對話框，"" 是新增，其他是正在編輯的那個時間
    var editingReminder by remember { mutableStateOf<String?>(null) }
    val reminderBlocked = stringResource(R.string.reminder_blocked)

    // Android 13 以上第一次加提醒時才問通知權限；拒絕了提醒照樣存，只是提示收不到
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) toast(reminderBlocked) }

    fun saveReminder(old: String, time: LocalTime, title: String) {
        // 改了時間就是換一個鬧鐘：舊的要拿掉
        if (old.isNotEmpty()) Reminders.remove(context, old)
        Reminders.add(context, time, title)
        reminderTimes = Reminders.times(context)
        if (old.isNotEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()) {
            toast(reminderBlocked)
        }
    }

    editingReminder?.let { old ->
        val initial = remember(old) { runCatching { LocalTime.parse(old) }.getOrNull() ?: LocalTime.NOON }
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        var title by remember(old) { mutableStateOf(if (old.isEmpty()) "" else Reminders.customTitle(context, old)) }
        AlertDialog(
            onDismissRequest = { editingReminder = null },
            confirmButton = {
                TextButton(onClick = {
                    editingReminder = null
                    saveReminder(old, LocalTime.of(state.hour, state.minute), title)
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { editingReminder = null }) { Text(stringResource(R.string.action_cancel)) }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePicker(state = state)
                    // 留白就用預設標題，預設會跟著選的時間換成那一餐
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.field_reminder_title)) },
                        placeholder = {
                            Text(Reminders.defaultTitle(context, LocalTime.of(state.hour, state.minute)))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings_targets), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_targets_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NumberField(
                value = kcal,
                onValueChange = { kcal = it },
                label = stringResource(R.string.field_target_kcal),
                isError = !kcalOk,
                errorText = stringResource(R.string.error_target_required),
                modifier = Modifier.fillMaxWidth()
            )
            NumberField(
                value = protein,
                onValueChange = { protein = it },
                label = stringResource(R.string.field_target_protein),
                isError = !proteinOk,
                errorText = stringResource(R.string.error_target_required),
                modifier = Modifier.fillMaxWidth()
            )
            NumberField(
                value = fat,
                onValueChange = { fat = it },
                label = stringResource(R.string.field_target_fat),
                modifier = Modifier.fillMaxWidth()
            )
            NumberField(
                value = carbs,
                onValueChange = { carbs = it },
                label = stringResource(R.string.field_target_carbs),
                modifier = Modifier.fillMaxWidth()
            )
            NumberField(
                value = water,
                onValueChange = { water = it },
                label = stringResource(R.string.field_target_water),
                isError = !waterOk,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = kcalOk && proteinOk && waterOk,
                onClick = {
                    viewModel.setTargets(
                        Targets(
                            kcal = kcalValue ?: targets.kcal,
                            protein = proteinValue ?: targets.protein,
                            fat = fat.toIntOrNull()?.takeIf { it > 0 },
                            carbs = carbs.toIntOrNull()?.takeIf { it > 0 },
                            water = waterValue ?: targets.water
                        )
                    )
                    toast(targetsSaved)
                }
            ) { Text(stringResource(R.string.action_save)) }

            HorizontalDivider()

            Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_backup_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (hasPhotos) {
                            exportZipLauncher.launch(BackupCodec.defaultZipFileName())
                        } else {
                            exportJsonLauncher.launch(BackupCodec.defaultFileName())
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_export))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) }
                ) {
                    Text(stringResource(R.string.action_import))
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_shortcuts),
                style = MaterialTheme.typography.titleMedium.zh()
            )
            Text(
                text = stringResource(R.string.settings_shortcuts_hint),
                style = MaterialTheme.typography.bodySmall.zh(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingRow(
                label = stringResource(R.string.settings_pin_water),
                action = stringResource(R.string.action_pin_shortcut),
                onClick = { pinShortcut(waterPinShortcut(context)) }
            )
            SettingRow(
                label = stringResource(R.string.settings_pin_log_meal),
                action = stringResource(R.string.action_pin_shortcut),
                onClick = { pinShortcut(logMealPinShortcut(context)) }
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_reminders),
                style = MaterialTheme.typography.titleMedium.zh()
            )
            for (hhmm in reminderTimes) {
                SettingRow(
                    label = "$hhmm  ${Reminders.title(context, hhmm)}",
                    action = stringResource(R.string.action_delete),
                    onClick = {
                        Reminders.remove(context, hhmm)
                        reminderTimes = Reminders.times(context)
                    },
                    onLabelClick = { editingReminder = hhmm }
                )
            }
            OutlinedButton(onClick = { editingReminder = "" }) {
                Text(stringResource(R.string.action_add_reminder))
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 左邊說是哪一個（提醒可點開來改），右邊一顆按鈕（釘到桌面、刪掉提醒） */
@Composable
private fun SettingRow(label: String, action: String, onClick: () -> Unit, onLabelClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.zh(),
            modifier = Modifier
                .weight(1f)
                .then(if (onLabelClick != null) Modifier.clickable(onClick = onLabelClick) else Modifier)
                .padding(vertical = 8.dp)
        )
        OutlinedButton(onClick = onClick) {
            Text(action)
        }
    }
}
