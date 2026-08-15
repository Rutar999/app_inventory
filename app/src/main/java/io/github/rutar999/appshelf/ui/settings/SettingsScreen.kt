package io.github.rutar999.appshelf.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.BuildConfig
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.data.prefs.SettingsRepository
import io.github.rutar999.appshelf.model.ThemeMode
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.util.AppIntents
import io.github.rutar999.appshelf.util.Exporter

/** 設定タブ（F-37 / F-42 / F-43 ほか）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppShelfViewModel,
    onOpenTags: () -> Unit,
    onOpenHidden: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenLicenses: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val usageGranted by viewModel.usageAccessGranted.collectAsStateWithLifecycle()
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()

    var showUnusedDialog by remember { mutableStateOf(false) }
    var showLargeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val hiddenCount = entries.count { it.meta.isHidden }

    // 書き出し先はユーザーに選ばせる。ストレージ権限が要らないのが利点。
    // 書き出しは画面の絞り込みを反映しない全件出力なので、その旨をファイル冒頭に入れる
    val exportNote = stringResource(R.string.settings_export_note)

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(Exporter.toCsv(entries, exportNote).toByteArray())
            }
        }.isSuccess
        viewModel.emit(if (ok) R.string.settings_export_done else R.string.settings_export_failed)
    }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(Exporter.toJson(entries, exportNote).toByteArray())
            }
        }.isSuccess
        viewModel.emit(if (ok) R.string.settings_export_done else R.string.settings_export_failed)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_section_data))
            SettingRow(
                title = stringResource(R.string.usage_access_title),
                subtitle = stringResource(
                    if (usageGranted) R.string.usage_access_granted else R.string.usage_access_not_granted
                ),
                onClick = onOpenUsageAccess
            )
            SettingRow(
                title = stringResource(R.string.settings_unused_days),
                subtitle = stringResource(R.string.settings_unused_days_value, settings.unusedDays),
                onClick = { showUnusedDialog = true }
            )
            SettingRow(
                title = stringResource(R.string.settings_large_threshold),
                subtitle = "${settings.largeThresholdMb} MB",
                onClick = { showLargeDialog = true }
            )
            SwitchRow(
                title = stringResource(R.string.settings_show_system),
                checked = settings.showSystemApps,
                onCheckedChange = { viewModel.setShowSystemApps(it) }
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_display))
            SettingRow(
                title = stringResource(R.string.settings_theme),
                subtitle = stringResource(settings.themeMode.labelRes()),
                onClick = { showThemeDialog = true }
            )
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                checked = settings.dynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onCheckedChange = { viewModel.setDynamicColor(it) }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SettingRow(
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_desc),
                    onClick = {
                        runCatching {
                            context.startActivity(AppIntents.appLocaleSettings(context.packageName))
                        }
                    }
                )
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_manage))
            SettingRow(title = stringResource(R.string.settings_tags), onClick = onOpenTags)
            SettingRow(
                title = stringResource(R.string.settings_hidden, hiddenCount),
                onClick = onOpenHidden
            )
            SettingRow(
                title = stringResource(R.string.settings_export_csv),
                onClick = { exportCsv.launch(Exporter.defaultFileName("csv")) }
            )
            SettingRow(
                title = stringResource(R.string.settings_export_json),
                onClick = { exportJson.launch(Exporter.defaultFileName("json")) }
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_about))
            SettingRow(
                title = stringResource(R.string.settings_privacy),
                onClick = { showPrivacyDialog = true }
            )
            SettingRow(
                title = stringResource(R.string.settings_licenses),
                onClick = onOpenLicenses
            )
            SettingRow(
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showUnusedDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_unused_days),
            options = SettingsRepository.UNUSED_DAY_CHOICES.map { it to stringResource(R.string.settings_unused_days_value, it) },
            selected = settings.unusedDays,
            onDismiss = { showUnusedDialog = false },
            onSelect = {
                viewModel.setUnusedDays(it)
                showUnusedDialog = false
            }
        )
    }

    if (showLargeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_large_threshold),
            options = SettingsRepository.LARGE_MB_CHOICES.map { it to "$it MB" },
            selected = settings.largeThresholdMb,
            onDismiss = { showLargeDialog = false },
            onSelect = {
                viewModel.setLargeThresholdMb(it)
                showLargeDialog = false
            }
        )
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to stringResource(it.labelRes()) },
            selected = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.settings_privacy)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.settings_privacy_body))
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .heightIn(min = 56.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
