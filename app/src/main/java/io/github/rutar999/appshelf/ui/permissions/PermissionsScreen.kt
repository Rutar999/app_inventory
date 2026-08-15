package io.github.rutar999.appshelf.ui.permissions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.PermissionGroup
import io.github.rutar999.appshelf.ui.AppShelfViewModel

/**
 * 権限タブ（F-21 / F-22）。
 *
 * このアプリの目玉である「逆引き」。
 * 危険権限のカテゴリごとに、宣言しているアプリ数と実際に許可されているアプリ数を出す。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: AppShelfViewModel,
    onOpenGroup: (PermissionGroup) -> Unit
) {
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()

    val counts = remember(entries) {
        PermissionGroup.ordered.associateWith { group ->
            val declared = entries.count { group in it.info.declaredGroups }
            val granted = entries.count { group in it.info.grantedGroups }
            declared to granted
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.perm_title)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.perm_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            items(PermissionGroup.ordered.size) { index ->
                val group = PermissionGroup.ordered[index]
                val (declared, granted) = counts[group] ?: (0 to 0)
                PermissionGroupRow(
                    group = group,
                    declared = declared,
                    granted = granted,
                    onClick = { onOpenGroup(group) }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.perm_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionGroupRow(
    group: PermissionGroup,
    declared: Int,
    granted: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        // 高さを固定してはいけない。タイトル+サブタイトルの2行が入るため、
        // height(32.dp) だと「付与済み n 件 / 宣言 m 件」が縦に見切れる（実機で確認済み）。
        Row(
            modifier = Modifier.padding(16.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(group.labelRes),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.perm_granted_count, granted, declared),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (granted > 0) {
                Badge { Text(granted.toString()) }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
