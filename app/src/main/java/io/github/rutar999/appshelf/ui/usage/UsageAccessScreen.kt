package io.github.rutar999.appshelf.ui.usage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.ui.AppShelfViewModel

/**
 * 使用状況アクセスの説明画面（要件定義書 §8）。
 *
 * Play では機微な権限について「アプリ内で用途を説明する画面を挟んでから設定へ誘導する」ことが求められる。
 * いきなり設定画面へ飛ばさず、必ずこの画面を経由させる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageAccessScreen(
    viewModel: AppShelfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val granted by viewModel.usageAccessGranted.collectAsStateWithLifecycle()
    val appName = stringResource(R.string.app_name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.usage_access_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(
                            if (granted) R.string.usage_access_granted
                            else R.string.usage_access_not_granted
                        )
                    )
                }
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.usage_access_why_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.usage_access_why_body),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.usage_access_howto, appName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { runCatching { context.startActivity(viewModel.usageAccessIntent()) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.usage_access_open_settings))
            }
        }
    }
}
