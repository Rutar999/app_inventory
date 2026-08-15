package io.github.rutar999.appshelf.ui.permissions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.PermissionGroup
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.components.AppRow
import io.github.rutar999.appshelf.ui.components.EmptyState

/**
 * 「マイクを使えるアプリ」のような逆引き一覧（F-21）。
 * ここから各アプリの詳細 → システムの権限設定へ入っていける（F-23）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionAppsScreen(
    viewModel: AppShelfViewModel,
    group: PermissionGroup,
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    var grantedOnly by remember { mutableStateOf(true) }

    val groupName = stringResource(group.labelRes)
    val matching = remember(entries, group, grantedOnly) {
        entries
            .filter { entry ->
                if (grantedOnly) group in entry.info.grantedGroups
                else group in entry.info.declaredGroups
            }
            .sortedBy { it.info.searchKey }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.perm_apps_with, groupName)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterChip(
                selected = grantedOnly,
                onClick = { grantedOnly = !grantedOnly },
                label = { Text(stringResource(R.string.perm_filter_granted_only)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                text = pluralStringResource(R.plurals.count_apps, matching.size, matching.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            if (matching.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.perm_empty),
                    icon = Icons.Default.Shield
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = matching, key = { it.packageName }) { entry ->
                        AppRow(
                            entry = entry,
                            iconLoader = viewModel.iconLoader,
                            subtitle = entry.info.packageName,
                            onClick = { onOpenApp(entry.packageName) }
                        )
                    }
                }
            }
        }
    }
}
