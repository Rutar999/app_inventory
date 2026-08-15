package io.github.rutar999.appshelf

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.navigation.AppNavigation
import io.github.rutar999.appshelf.ui.onboarding.OnboardingScreen
import io.github.rutar999.appshelf.ui.theme.AppShelfTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppShelfViewModel by viewModels { AppShelfViewModel.Factory }

    /**
     * アプリのインストール／アンインストールを検知して一覧を更新する（F-41）。
     * マニフェスト宣言ではなく画面が表示されている間だけ動的に登録する。
     * 常駐する必要はないし、バックグラウンド制限も避けられる。
     */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()

            AppShelfTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // DataStore の読み込みが終わるまでは何も出さない（一瞬）。
                        // ここで判定しないと、既存ユーザーにオンボーディングが一瞬見えてしまう。
                        !settingsLoaded -> Unit

                        !settings.onboardingDone -> OnboardingScreen(
                            viewModel = viewModel,
                            onFinish = { viewModel.completeOnboarding() }
                        )

                        else -> AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }

        viewModel.refresh()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(packageReceiver) }
    }

    override fun onResume() {
        super.onResume()
        // 設定画面で「使用状況へのアクセス」を ON にして戻ってきたケースを拾う
        viewModel.onAppResumed()
    }
}
