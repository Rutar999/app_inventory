package io.github.rutar999.appshelf.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector
)

private val pages = listOf(
    OnboardingPage(R.string.onb_p1_title, R.string.onb_p1_body, Icons.Default.Inventory2),
    OnboardingPage(R.string.onb_p2_title, R.string.onb_p2_body, Icons.Default.Style),
    OnboardingPage(R.string.onb_p3_title, R.string.onb_p3_body, Icons.Default.CloudOff),
    OnboardingPage(R.string.onb_p4_title, R.string.onb_p4_body, Icons.Default.QueryStats)
)

/**
 * 初回オンボーディング（F-40）。
 *
 * 4 ページ目で使用状況アクセスを説明してから設定へ誘導する。
 * Play の審査でも「機微な権限は用途を説明してから要求する」ことが求められている（要件定義書 §8）。
 * ここで許可しなくてもアプリは成立する、と明記するのが大事。
 */
@Composable
fun OnboardingScreen(
    viewModel: AppShelfViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // safeDrawingPadding が必須。targetSdk 35 以降は edge-to-edge が強制されるため、
    // Scaffold を使っていないこの画面では自前でシステムバーぶんを避けないと
    // 「次へ」ボタンがナビゲーションバーに隠れる（実機で確認済み）。
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.onb_skip)) }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(page.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (index == pages.lastIndex) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching { context.startActivity(viewModel.usageAccessIntent()) }
                        }
                    ) {
                        Text(stringResource(R.string.usage_access_grant))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                stringResource(
                    if (pagerState.currentPage == pages.lastIndex) R.string.onb_start else R.string.onb_next
                )
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}
