package io.github.rutar999.appshelf.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.components.AppIconImage
import io.github.rutar999.appshelf.ui.components.EmptyState

/** 非表示リスト（F-34）。ここから元に戻せる。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsScreen(
    viewModel: AppShelfViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val hidden = entries.filter { it.meta.isHidden }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hidden_title)) },
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
            Text(
                text = stringResource(R.string.hidden_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            if (hidden.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.hidden_empty),
                    icon = Icons.Default.VisibilityOff
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = hidden, key = { it.packageName }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(
                                packageName = entry.packageName,
                                iconLoader = viewModel.iconLoader,
                                size = 36
                            )
                            Text(
                                text = entry.info.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )
                            TextButton(onClick = { viewModel.setHidden(entry.packageName, false) }) {
                                Text(stringResource(R.string.hidden_restore))
                            }
                        }
                    }
                }
            }
        }
    }
}
