package io.github.rutar999.appshelf.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rutar999.appshelf.R

/**
 * オープンソースライセンス表示（Apache License 2.0 の義務対応）。
 *
 * Apache 2.0 はバイナリ配布時にもライセンス全文と帰属表示の同梱を要求する。
 * APK / AAB で配布する以上これは必須。
 *
 * play-services-oss-licenses を使えば自動生成できるが、Play Services への依存が増える。
 * 本アプリは「依存最小・完全オフライン」が売りなので、静的な画面を自作している。
 * 同梱ライブラリがすべて Apache 2.0 なので、全文は 1 つで足りる。
 *
 * ライブラリを追加したら [LIBRARIES] を更新すること。
 * 実際に同梱されるものは次のコマンドで確認できる:
 *   gradlew :app:dependencies --configuration releaseRuntimeClasspath
 */
private val LIBRARIES = listOf(
    "Jetpack Compose (androidx.compose.*)" to "The Android Open Source Project",
    "AndroidX Core / Activity / Lifecycle" to "The Android Open Source Project",
    "AndroidX Navigation" to "The Android Open Source Project",
    "AndroidX Room / SQLite" to "The Android Open Source Project",
    "AndroidX DataStore" to "The Android Open Source Project",
    "AndroidX Annotation / Collection / Tracing ほか" to "The Android Open Source Project",
    "Kotlin Standard Library" to "JetBrains s.r.o. and Kotlin Programming Language contributors",
    "kotlinx.coroutines" to "JetBrains s.r.o.",
    "Okio" to "Square, Inc.",
    "Guava (ListenableFuture)" to "The Guava Authors",
    "JSpecify Annotations" to "The JSpecify Authors"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // 11KB のテキストを読むだけなので、画面を開いたときに 1 回読めば十分
    val licenseText = remember {
        runCatching {
            context.resources.openRawResource(R.raw.apache_2_0)
                .bufferedReader()
                .use { it.readText() }
        }.getOrElse { "" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_licenses)) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
            }

            items(LIBRARIES.size) { index ->
                val (name, holder) = LIBRARIES[index]
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Copyright © $holder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Apache License 2.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.licenses_full_text),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                // 原文は 72 桁で改行された固定幅テキストだが、スマホの幅では 40 桁程度しか入らない。
                // 横スクロールにすると 1 行ごとに左右へ動かす必要があり実用にならないため、
                // 折り返して表示する。ライセンス原文の改変は許されないので、
                // テキスト自体はダウンロードした正典のまま一切加工していない。
                Text(
                    text = licenseText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
