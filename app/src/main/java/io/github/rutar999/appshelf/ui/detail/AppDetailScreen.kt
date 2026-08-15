package io.github.rutar999.appshelf.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.model.AppPermission
import io.github.rutar999.appshelf.model.PermissionGroup
import io.github.rutar999.appshelf.model.TrendPoint
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.components.AppIconImage
import io.github.rutar999.appshelf.ui.components.SectionCard
import io.github.rutar999.appshelf.ui.components.TagChip
import io.github.rutar999.appshelf.ui.components.TrendChart
import io.github.rutar999.appshelf.util.AppIntents
import io.github.rutar999.appshelf.util.Formatters

/** アプリ詳細（F-06 / F-07 / F-20 / F-23 / F-30〜F-34）。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppDetailScreen(
    viewModel: AppShelfViewModel,
    packageName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    val entry = entries.firstOrNull { it.packageName == packageName }

    // アンインストールされたら一覧から消えるので、詳細に留まらず戻る
    LaunchedEffect(entry, isLoading, entries.size) {
        if (entry == null && !isLoading && entries.isNotEmpty()) onBack()
    }
    if (entry == null) return

    var showAllPermissions by remember { mutableStateOf(false) }
    var noteDraft by remember(entry.meta.note) { mutableStateOf(entry.meta.note.orEmpty()) }

    // 長期トレンド（F-16）。DB を読むので画面を開いたときに 1 回だけ。
    // produceState は使わない（key で内部 remember が作り直されず、前の値が残るため）
    var trend by remember(packageName) { mutableStateOf<List<TrendPoint>>(emptyList()) }
    LaunchedEffect(packageName) { trend = viewModel.loadTrend(packageName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = entry.info.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setFavorite(packageName, !entry.meta.isFavorite) }
                    ) {
                        Icon(
                            imageVector = if (entry.meta.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(
                                if (entry.meta.isFavorite) R.string.detail_favorite_off
                                else R.string.detail_favorite_on
                            )
                        )
                    }
                    IconButton(onClick = { viewModel.setHidden(packageName, !entry.meta.isHidden) }) {
                        Icon(
                            imageVector = if (entry.meta.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(
                                if (entry.meta.isHidden) R.string.detail_hide_off
                                else R.string.detail_hide_on
                            )
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(entry = entry, viewModel = viewModel)

            ActionButtons(
                entry = entry,
                onLaunch = {
                    if (!AppIntents.launch(context, packageName)) {
                        viewModel.emit(R.string.detail_launch_failed)
                    }
                },
                onSettings = {
                    runCatching { context.startActivity(AppIntents.appSettings(packageName)) }
                },
                onStore = {
                    if (!AppIntents.openPlayStore(context, packageName)) {
                        viewModel.emit(R.string.detail_store_failed)
                    }
                },
                onUninstall = { viewModel.startUninstall(listOf(packageName)) }
            )

            SectionCard(title = stringResource(R.string.detail_section_storage)) {
                val sizes = entry.sizes
                InfoRow(stringResource(R.string.detail_size_app), Formatters.bytes(context, sizes?.appBytes))
                InfoRow(
                    stringResource(R.string.detail_size_data),
                    Formatters.bytes(context, sizes?.dataExcludingCache)
                )
                InfoRow(stringResource(R.string.detail_size_cache), Formatters.bytes(context, sizes?.cacheBytes))
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                InfoRow(stringResource(R.string.detail_size_total), Formatters.bytes(context, sizes?.total))
            }

            SectionCard(title = stringResource(R.string.detail_section_usage)) {
                InfoRow(
                    stringResource(R.string.detail_last_used),
                    Formatters.lastUsed(context, entry.usage?.lastTimeUsed)
                )
                InfoRow(
                    stringResource(R.string.detail_usage_7d),
                    Formatters.duration(context, entry.usage?.foreground7d)
                )
                InfoRow(
                    stringResource(R.string.detail_usage_30d),
                    Formatters.duration(context, entry.usage?.foreground30d)
                )
            }

            TrendSection(trend = trend)

            SectionCard(title = stringResource(R.string.detail_section_info)) {
                InfoRow(stringResource(R.string.detail_version), entry.info.versionName ?: "—")
                InfoRow(stringResource(R.string.detail_package), entry.info.packageName)
                InfoRow(
                    stringResource(R.string.detail_installed_at),
                    Formatters.date(context, entry.info.firstInstallTime)
                )
                InfoRow(
                    stringResource(R.string.detail_updated_at),
                    Formatters.date(context, entry.info.lastUpdateTime)
                )
                entry.info.categoryTitle?.let {
                    InfoRow(stringResource(R.string.detail_category), it)
                }
                InfoRow(
                    stringResource(R.string.detail_type),
                    stringResource(
                        if (entry.info.isSystemApp) R.string.detail_type_system
                        else R.string.detail_type_user
                    )
                )
            }

            SectionCard(title = stringResource(R.string.detail_section_tags)) {
                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.tags_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            val assigned = entry.tags.any { it.id == tag.id }
                            FilterChip(
                                selected = assigned,
                                onClick = {
                                    if (assigned) {
                                        viewModel.unassignTag(listOf(packageName), tag.id)
                                    } else {
                                        viewModel.assignTag(listOf(packageName), tag.id)
                                    }
                                },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.detail_note)) },
                    placeholder = { Text(stringResource(R.string.detail_note_hint)) },
                    minLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.setNote(packageName, noteDraft) }) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }

            PermissionsSection(
                entry = entry,
                showAll = showAllPermissions,
                onToggleShowAll = { showAllPermissions = !showAllPermissions },
                permissionLabel = viewModel::permissionLabel,
                onOpenSettings = {
                    runCatching { context.startActivity(AppIntents.appSettings(packageName)) }
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header(entry: AppEntry, viewModel: AppShelfViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIconImage(entry.packageName, viewModel.iconLoader, size = 64)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.info.label, style = MaterialTheme.typography.titleLarge)
            Text(
                text = entry.info.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    entry.tags.forEach { TagChip(it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtons(
    entry: AppEntry,
    onLaunch: () -> Unit,
    onSettings: () -> Unit,
    onStore: () -> Unit,
    onUninstall: () -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onLaunch) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.detail_action_launch))
        }
        OutlinedButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.detail_action_settings))
        }
        OutlinedButton(onClick = onStore) {
            Icon(Icons.Default.Shop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.detail_action_store))
        }
        if (entry.info.canUninstall) {
            OutlinedButton(onClick = onUninstall) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.detail_action_uninstall))
            }
        } else {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.detail_cannot_uninstall)) }
            )
        }
    }
}

/**
 * 長期トレンド（F-16）。
 *
 * OS の使用統計は日次で約7日ぶんしか残らないため、自前で貯めた日次スナップショットを描く。
 * 記録は「アプリを開いた日に 1 回」しか増えないので、使い始めは点が足りない。
 * その状態を黙って空欄にせず、なぜ出せないのかを明示する。
 */
@Composable
private fun TrendSection(trend: List<TrendPoint>) {
    val context = LocalContext.current

    SectionCard(title = stringResource(R.string.detail_section_trend)) {
        if (trend.size < 2) {
            Text(
                text = stringResource(R.string.detail_trend_insufficient, trend.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SectionCard
        }

        val startLabel = formatDateInt(trend.first().date)
        val endLabel = formatDateInt(trend.last().date)

        Text(
            text = stringResource(R.string.detail_trend_size),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(6.dp))
        TrendChart(
            values = trend.map { it.totalBytes.toFloat() },
            startLabel = startLabel,
            endLabel = endLabel,
            maxLabel = Formatters.bytes(context, trend.maxOf { it.totalBytes })
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.detail_trend_usage),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(6.dp))
        TrendChart(
            values = trend.map { it.foregroundMs.toFloat() },
            startLabel = startLabel,
            endLabel = endLabel,
            maxLabel = Formatters.duration(context, trend.maxOf { it.foregroundMs }),
            lineColor = MaterialTheme.colorScheme.tertiary
        )
    }
}

/** yyyyMMdd の整数を「8/15」のような短い表記にする */
private fun formatDateInt(value: Int): String = "${(value / 100) % 100}/${value % 100}"

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
    }
}

/**
 * 権限一覧（F-20）。
 * 危険権限グループを上に、付与済み／未付与を区別して出す。
 * 全権限は数十件になることがあるので、既定では畳んでおく。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionsSection(
    entry: AppEntry,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    permissionLabel: (String) -> String,
    onOpenSettings: () -> Unit
) {
    SectionCard(title = stringResource(R.string.detail_section_permissions)) {
        if (entry.info.permissions.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_permissions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SectionCard
        }

        val groups = PermissionGroup.ordered.filter { it in entry.info.declaredGroups }
        if (groups.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { group ->
                    val granted = group in entry.info.grantedGroups
                    AssistChip(
                        onClick = onOpenSettings,
                        label = {
                            Text(
                                stringResource(group.labelRes) + " · " + stringResource(
                                    if (granted) R.string.detail_permissions_granted
                                    else R.string.detail_permissions_denied
                                )
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        TextButton(onClick = onOpenSettings) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.detail_permission_open_settings))
        }

        TextButton(onClick = onToggleShowAll) {
            Text(stringResource(R.string.detail_show_all_permissions, entry.info.permissions.size))
        }

        if (showAll) {
            // PackageManager への問い合わせが 1 権限につき 1 回入るので、展開時だけ計算する
            val labelled = remember(entry.packageName) {
                entry.info.permissions
                    .sortedWith(
                        compareByDescending<AppPermission> { it.group != null }
                            .thenByDescending { it.granted }
                    )
                    .map { it to permissionLabel(it.name) }
            }
            Column {
                labelled.forEach { (permission, label) ->
                    InfoRow(
                        label = label,
                        value = stringResource(
                            if (permission.granted) R.string.detail_permissions_granted
                            else R.string.detail_permissions_denied
                        )
                    )
                }
            }
        }
    }
}
