package io.github.rutar999.appshelf.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.components.AppIconImage
import io.github.rutar999.appshelf.ui.components.BarItem
import io.github.rutar999.appshelf.ui.components.HorizontalBarRow
import io.github.rutar999.appshelf.ui.components.SectionCard
import io.github.rutar999.appshelf.ui.components.UsageAccessBanner
import io.github.rutar999.appshelf.util.Formatters

/** ホーム（ダッシュボード）。F-10 / F-11 / F-12 / F-13 / F-15 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppShelfViewModel,
    onOpenApp: (String) -> Unit,
    onOpenAppList: () -> Unit,
    onOpenUsageAccess: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val usageGranted by viewModel.usageAccessGranted.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isEnriching by viewModel.isEnriching.collectAsStateWithLifecycle()

    val stats = remember(entries, settings.unusedDays, usageGranted) {
        DashboardLogic.calculate(
            entries = entries,
            unusedDays = settings.unusedDays,
            usageAvailable = usageGranted
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading || isEnriching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!usageGranted) {
                    item {
                        UsageAccessBanner(onClick = onOpenUsageAccess)
                    }
                }

                item {
                    SummaryGrid(
                        totalApps = stats.totalApps,
                        totalBytes = Formatters.bytes(context, stats.totalBytes),
                        unusedCount = stats.unusedCount,
                        reclaimable = Formatters.bytes(context, stats.reclaimableBytes),
                        usageGranted = usageGranted
                    )
                }

                if (usageGranted) {
                    item {
                        FilledTonalButton(
                            onClick = {
                                viewModel.showUnusedOnly()
                                onOpenAppList()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.home_start_review))
                                Text(
                                    text = stringResource(R.string.home_start_review_desc, settings.unusedDays),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                if (stats.topBySize.isNotEmpty()) {
                    item {
                        SectionCard(
                            title = stringResource(R.string.home_size_ranking),
                            actionLabel = stringResource(R.string.home_see_all),
                            onAction = onOpenAppList
                        ) {
                            SizeRanking(
                                entries = stats.topBySize,
                                viewModel = viewModel,
                                onOpenApp = onOpenApp
                            )
                        }
                    }
                }

                if (stats.topByUsage.isNotEmpty()) {
                    item {
                        SectionCard(title = stringResource(R.string.home_usage_ranking)) {
                            UsageRanking(
                                entries = stats.topByUsage,
                                viewModel = viewModel,
                                onOpenApp = onOpenApp
                            )
                        }
                    }
                }

                // 使用状況が取れないときの代替表示（F-15）
                if (!usageGranted && stats.oldestInstalls.isNotEmpty()) {
                    item {
                        SectionCard(title = stringResource(R.string.home_old_apps)) {
                            Text(
                                text = stringResource(R.string.home_old_apps_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OldestInstalls(
                                entries = stats.oldestInstalls,
                                viewModel = viewModel,
                                onOpenApp = onOpenApp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryGrid(
    totalApps: Int,
    totalBytes: String,
    unusedCount: Int,
    reclaimable: String,
    usageGranted: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.home_summary_total),
                value = totalApps.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.home_summary_size),
                value = totalBytes,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.home_summary_unused),
                value = if (usageGranted) unusedCount.toString() else stringResource(R.string.value_unknown),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.home_summary_reclaimable),
                value = reclaimable,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SizeRanking(
    entries: List<AppEntry>,
    viewModel: AppShelfViewModel,
    onOpenApp: (String) -> Unit
) {
    val context = LocalContext.current
    val max = entries.maxOfOrNull { it.sizes?.total ?: 0L }?.coerceAtLeast(1L) ?: 1L
    val appColor = MaterialTheme.colorScheme.primary
    val dataColor = MaterialTheme.colorScheme.tertiary
    val cacheColor = MaterialTheme.colorScheme.outline

    Column {
        entries.forEach { entry ->
            val sizes = entry.sizes ?: return@forEach
            HorizontalBarRow(
                item = BarItem(
                    key = entry.packageName,
                    label = entry.info.label,
                    valueLabel = Formatters.bytes(context, sizes.total),
                    ratio = sizes.total.toFloat() / max.toFloat(),
                    segments = listOf(
                        sizes.appBytes.toFloat() to appColor,
                        sizes.dataExcludingCache.toFloat() to dataColor,
                        sizes.cacheBytes.toFloat() to cacheColor
                    ).filter { it.first > 0f }
                ),
                leading = {
                    AppIconImage(entry.packageName, viewModel.iconLoader, size = 32)
                },
                onClick = { onOpenApp(entry.packageName) }
            )
        }
    }
}

@Composable
private fun UsageRanking(
    entries: List<AppEntry>,
    viewModel: AppShelfViewModel,
    onOpenApp: (String) -> Unit
) {
    val context = LocalContext.current
    val max = entries.maxOfOrNull { it.usage?.foreground7d ?: 0L }?.coerceAtLeast(1L) ?: 1L
    Column {
        entries.forEach { entry ->
            val usage = entry.usage ?: return@forEach
            HorizontalBarRow(
                item = BarItem(
                    key = entry.packageName,
                    label = entry.info.label,
                    valueLabel = Formatters.duration(context, usage.foreground7d),
                    ratio = usage.foreground7d.toFloat() / max.toFloat()
                ),
                leading = { AppIconImage(entry.packageName, viewModel.iconLoader, size = 32) },
                onClick = { onOpenApp(entry.packageName) }
            )
        }
    }
}

@Composable
private fun OldestInstalls(
    entries: List<AppEntry>,
    viewModel: AppShelfViewModel,
    onOpenApp: (String) -> Unit
) {
    val maxDays = entries.maxOfOrNull { it.daysSinceInstall() }?.coerceAtLeast(1) ?: 1
    Column {
        entries.forEach { entry ->
            val days = entry.daysSinceInstall()
            HorizontalBarRow(
                item = BarItem(
                    key = entry.packageName,
                    label = entry.info.label,
                    valueLabel = pluralStringResource(R.plurals.installed_days_ago, days, days),
                    ratio = days.toFloat() / maxDays.toFloat()
                ),
                leading = { AppIconImage(entry.packageName, viewModel.iconLoader, size = 32) },
                onClick = { onOpenApp(entry.packageName) }
            )
        }
    }
}
