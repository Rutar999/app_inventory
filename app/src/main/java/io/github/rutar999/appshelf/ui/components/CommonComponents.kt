package io.github.rutar999.appshelf.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.rutar999.appshelf.util.IconLoader

/**
 * アプリアイコン。
 * PackageManager から Drawable を読むのは IO なので、
 * まずキャッシュを見て、無ければ非同期に読み込む（非機能要件: 遅延読み込み）。
 */
@Composable
fun AppIconImage(
    packageName: String,
    iconLoader: IconLoader,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    // produceState は使わないこと。
    // produceState の内部 remember は key1 では作り直されないため、LazyColumn が
    // 行を使い回したときに前の行のアイコンが value に残り、別アプリのアイコンが
    // 表示されたままになる（実機で「Fate/GO」に Brave のアイコンが出た）。
    // remember(packageName) なら package が変わった時点で必ず null に戻る。
    var icon by remember(packageName) { mutableStateOf(iconLoader.cached(packageName)) }
    LaunchedEffect(packageName) {
        if (icon == null) icon = iconLoader.load(packageName)
    }

    val shape = RoundedCornerShape(percent = 22)
    val current = icon
    if (current != null) {
        Image(
            bitmap = current,
            // アプリ名がすぐ隣に読み上げられるので、アイコン自体は読み上げ対象から外す
            contentDescription = null,
            modifier = modifier.size(size.dp).clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/** 見出し付きのカード。ダッシュボードや詳細画面の各セクションで使う。 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) { Text(actionLabel) }
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** 一覧が空のときの案内。ただ「空です」と出すより次の行動が分かるようにする。 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector = Icons.Default.Info
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 横棒グラフ 1 行ぶんのデータ */
data class BarItem(
    val key: String,
    val label: String,
    val valueLabel: String,
    /** 0f..1f */
    val ratio: Float,
    val segments: List<Pair<Float, Color>> = emptyList()
)

/**
 * 容量ランキング用の横棒グラフ（F-11）。
 * ライブラリを入れず Compose の Box だけで描いている（棒グラフ程度なら十分）。
 */
@Composable
fun HorizontalBarRow(
    item: BarItem,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Row(
        modifier = rowModifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.valueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.segments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.ratio.coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(item.ratio.coerceIn(0f, 1f))) {
                        item.segments.forEach { (weight, color) ->
                            if (weight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .height(8.dp)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
