package io.github.rutar999.appshelf.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.Tag
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.components.EmptyState
import io.github.rutar999.appshelf.ui.theme.TagColors

/** タグ管理（F-30）。作成・名前と色の編集・削除。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    viewModel: AppShelfViewModel,
    onBack: () -> Unit
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Tag?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tags_new))
            }
        }
    ) { padding ->
        if (tags.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.tags_empty),
                icon = Icons.AutoMirrored.Filled.Label,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(items = tags, key = { it.id }) { tag ->
                    val count = entries.count { entry -> entry.tags.any { it.id == tag.id } }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = tag }
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(tag.colorArgb))
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tag.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = pluralStringResource(R.plurals.tags_count_apps, count, count),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { deleting = tag }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete)
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        TagEditDialog(
            initialName = "",
            initialColor = TagColors.first(),
            onDismiss = { creating = false },
            onConfirm = { name, color ->
                viewModel.createTag(name, color)
                creating = false
            }
        )
    }

    editing?.let { tag ->
        TagEditDialog(
            initialName = tag.name,
            initialColor = tag.colorArgb,
            onDismiss = { editing = null },
            onConfirm = { name, color ->
                viewModel.updateTag(tag.copy(name = name, colorArgb = color))
                editing = null
            }
        )
    }

    deleting?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.tags_delete_confirm, tag.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        deleting = null
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun TagEditDialog(
    initialName: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_new)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tags_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.tags_color),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagColors.take(5).forEach { candidate ->
                        ColorDot(candidate, candidate == color) { color = candidate }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagColors.drop(5).forEach { candidate ->
                        ColorDot(candidate, candidate == color) { color = candidate }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, color) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun ColorDot(colorArgb: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
