package io.github.rutar999.appshelf.ui.applist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.model.AppFilter
import io.github.rutar999.appshelf.model.SortOrder
import io.github.rutar999.appshelf.model.ViewMode
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.components.AppIconImage
import io.github.rutar999.appshelf.ui.components.AppRow
import io.github.rutar999.appshelf.ui.components.EmptyState
import io.github.rutar999.appshelf.util.Formatters

/** アプリ一覧。F-01〜F-05, F-31, F-34, F-35 に対応する画面。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppShelfViewModel,
    onOpenApp: (String) -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isEnriching by viewModel.isEnriching.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    val largeThresholdBytes = settings.largeThresholdMb.toLong() * 1024 * 1024

    val visible = remember(entries, query, filter, settings.sortOrder, settings.unusedDays, largeThresholdBytes) {
        AppListLogic.apply(
            entries = entries,
            query = query,
            filter = filter,
            sort = settings.sortOrder,
            unusedDays = settings.unusedDays,
            largeThresholdBytes = largeThresholdBytes
        )
    }

    val selectionMode = selection.isNotEmpty()

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    title = { Text(stringResource(R.string.apps_selected, selection.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTagDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = stringResource(R.string.apps_bulk_tag)
                            )
                        }
                        IconButton(onClick = { viewModel.setHidden(selection.toList(), true) }) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = stringResource(R.string.apps_bulk_hide)
                            )
                        }
                        IconButton(onClick = { showUninstallDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.apps_bulk_uninstall)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.apps_title)) },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.setViewMode(
                                    if (settings.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (settings.viewMode == ViewMode.LIST) {
                                    Icons.Default.GridView
                                } else {
                                    Icons.AutoMirrored.Filled.ViewList
                                },
                                contentDescription = stringResource(R.string.apps_view_mode)
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.apps_sort)
                                )
                            }
                            SortMenu(
                                expanded = showSortMenu,
                                current = settings.sortOrder,
                                onDismiss = { showSortMenu = false },
                                onSelect = {
                                    viewModel.setSortOrder(it)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                // placeholder が 2 行に折り返すと入力欄が縦に伸びて不格好になるので 1 行に固定する
                placeholder = {
                    Text(
                        text = stringResource(R.string.apps_search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.apps_search_clear)
                            )
                        }
                    }
                }
            )

            FilterChips(
                filter = filter,
                tags = tags.map { it.id to it.name },
                onFilterChange = { viewModel.setFilter(it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pluralStringResource(R.plurals.apps_result_count, visible.size, visible.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectionMode) {
                    TextButton(onClick = { viewModel.selectAll(visible.map { it.packageName }) }) {
                        Text(stringResource(R.string.apps_select_all))
                    }
                }
            }

            if (isLoading || isEnriching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                visible.isEmpty() && !isLoading -> EmptyState(
                    title = stringResource(R.string.apps_empty),
                    body = stringResource(R.string.apps_empty_hint),
                    icon = Icons.Default.SearchOff
                )

                settings.viewMode == ViewMode.GRID -> AppGrid(
                    entries = visible,
                    viewModel = viewModel,
                    selection = selection,
                    selectionMode = selectionMode,
                    onOpenApp = onOpenApp
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = visible, key = { it.packageName }) { entry ->
                        AppRow(
                            entry = entry,
                            iconLoader = viewModel.iconLoader,
                            subtitle = subtitleFor(context, entry, settings.sortOrder),
                            selectionMode = selectionMode,
                            selected = entry.packageName in selection,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelection(entry.packageName)
                                } else {
                                    onOpenApp(entry.packageName)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(entry.packageName) }
                        )
                    }
                }
            }
        }
    }

    if (showTagDialog) {
        BulkTagDialog(
            tags = tags.map { it.id to it.name },
            onDismiss = { showTagDialog = false },
            onSelect = { tagId ->
                viewModel.assignTag(selection.toList(), tagId)
                showTagDialog = false
                viewModel.clearSelection()
            }
        )
    }

    if (showUninstallDialog) {
        val targets = visible.filter { it.packageName in selection && it.info.canUninstall }
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            title = { Text(stringResource(R.string.uninstall_title)) },
            text = {
                Text(pluralStringResource(R.plurals.uninstall_sequential, targets.size, targets.size))
            },
            confirmButton = {
                TextButton(
                    enabled = targets.isNotEmpty(),
                    onClick = {
                        showUninstallDialog = false
                        viewModel.startUninstall(targets.map { it.packageName })
                    }
                ) { Text(stringResource(R.string.uninstall_start)) }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** 並び替えの選択肢によって、行の 2 段目に出す情報を変える。 */
private fun subtitleFor(
    context: android.content.Context,
    entry: AppEntry,
    sort: SortOrder
): String = when (sort) {
    SortOrder.SIZE_DESC ->
        Formatters.bytes(context, entry.sizes?.total)

    SortOrder.LAST_USED_ASC ->
        Formatters.lastUsed(context, entry.usage?.lastTimeUsed)

    SortOrder.USAGE_DESC ->
        Formatters.duration(context, entry.usage?.foreground30d)

    SortOrder.INSTALLED_DESC ->
        Formatters.date(context, entry.info.firstInstallTime)

    SortOrder.UPDATED_DESC ->
        Formatters.date(context, entry.info.lastUpdateTime)

    SortOrder.NAME -> {
        val size = entry.sizes?.total
        if (size != null) {
            "${Formatters.bytes(context, size)} · ${Formatters.lastUsed(context, entry.usage?.lastTimeUsed)}"
        } else {
            entry.info.packageName
        }
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    current: SortOrder,
    onDismiss: () -> Unit,
    onSelect: (SortOrder) -> Unit
) {
    val labels = listOf(
        SortOrder.NAME to R.string.sort_name,
        SortOrder.INSTALLED_DESC to R.string.sort_installed,
        SortOrder.UPDATED_DESC to R.string.sort_updated,
        SortOrder.SIZE_DESC to R.string.sort_size,
        SortOrder.LAST_USED_ASC to R.string.sort_last_used,
        SortOrder.USAGE_DESC to R.string.sort_usage_time
    )
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        labels.forEach { (order, labelRes) ->
            DropdownMenuItem(
                text = { Text(stringResource(labelRes)) },
                leadingIcon = {
                    RadioButton(selected = order == current, onClick = { onSelect(order) })
                },
                onClick = { onSelect(order) }
            )
        }
    }
}

@Composable
private fun FilterChips(
    filter: AppFilter,
    tags: List<Pair<Long, String>>,
    onFilterChange: (AppFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filter.isEmpty,
            onClick = { onFilterChange(AppFilter()) },
            label = { Text(stringResource(R.string.apps_filter_all)) }
        )
        FilterChip(
            selected = filter.favoriteOnly,
            onClick = { onFilterChange(filter.copy(favoriteOnly = !filter.favoriteOnly)) },
            label = { Text(stringResource(R.string.apps_filter_favorite)) }
        )
        FilterChip(
            selected = filter.unusedOnly,
            onClick = { onFilterChange(filter.copy(unusedOnly = !filter.unusedOnly)) },
            label = { Text(stringResource(R.string.apps_filter_unused)) }
        )
        FilterChip(
            selected = filter.largeOnly,
            onClick = { onFilterChange(filter.copy(largeOnly = !filter.largeOnly)) },
            label = { Text(stringResource(R.string.apps_filter_large)) }
        )
        FilterChip(
            selected = filter.untaggedOnly,
            onClick = { onFilterChange(filter.copy(untaggedOnly = !filter.untaggedOnly)) },
            label = { Text(stringResource(R.string.apps_filter_untagged)) }
        )
        tags.forEach { (id, name) ->
            FilterChip(
                selected = filter.tagId == id,
                onClick = { onFilterChange(filter.copy(tagId = if (filter.tagId == id) null else id)) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
private fun AppGrid(
    entries: List<AppEntry>,
    viewModel: AppShelfViewModel,
    selection: Set<String>,
    selectionMode: Boolean,
    onOpenApp: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 92.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LazyGridScope のメンバ関数 items(count) を使う。
        // LazyColumn 側の items 拡張関数と名前が同じなので、import を増やさない形にしている。
        items(count = entries.size, key = { entries[it].packageName }) { index ->
            val entry = entries[index]
            val selected = entry.packageName in selection
            Column(
                modifier = Modifier
                    .clickable {
                        if (selectionMode) {
                            viewModel.toggleSelection(entry.packageName)
                        } else {
                            onOpenApp(entry.packageName)
                        }
                    }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIconImage(
                    packageName = entry.packageName,
                    iconLoader = viewModel.iconLoader,
                    size = 48
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.info.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun BulkTagDialog(
    tags: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_assign_title)) },
        text = {
            if (tags.isEmpty()) {
                Text(stringResource(R.string.tags_empty))
            } else {
                Column {
                    tags.forEach { (id, name) ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(id) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}
