package io.github.rutar999.appshelf.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.model.Tag
import io.github.rutar999.appshelf.util.IconLoader

/** タグの色付きチップ。 */
@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    val color = Color(tag.colorArgb)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 一覧の 1 行（F-01）。
 * 長押しで複数選択モードに入る（F-35 の入口）。
 * タップ領域は 48dp 以上を確保している（アクセシビリティ要件）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    entry: AppEntry,
    iconLoader: IconLoader,
    subtitle: String,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
            Spacer(Modifier.width(4.dp))
        }

        AppIconImage(packageName = entry.packageName, iconLoader = iconLoader)
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.info.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (entry.meta.isFavorite) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.apps_filter_favorite),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.size(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    entry.tags.take(3).forEach { TagChip(it) }
                }
            }
        }

        if (!selectionMode && selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .semantics { stateDescription = "selected" }
            )
        }
    }
}
