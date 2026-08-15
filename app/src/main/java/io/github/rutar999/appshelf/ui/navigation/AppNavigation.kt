package io.github.rutar999.appshelf.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.model.PermissionGroup
import io.github.rutar999.appshelf.ui.AppShelfViewModel
import io.github.rutar999.appshelf.ui.applist.AppListScreen
import io.github.rutar999.appshelf.ui.detail.AppDetailScreen
import io.github.rutar999.appshelf.ui.home.HomeScreen
import io.github.rutar999.appshelf.ui.permissions.PermissionAppsScreen
import io.github.rutar999.appshelf.ui.permissions.PermissionsScreen
import io.github.rutar999.appshelf.ui.settings.HiddenAppsScreen
import io.github.rutar999.appshelf.ui.settings.LicensesScreen
import io.github.rutar999.appshelf.ui.settings.SettingsScreen
import io.github.rutar999.appshelf.ui.settings.TagManagerScreen
import io.github.rutar999.appshelf.ui.usage.UsageAccessScreen
import io.github.rutar999.appshelf.util.AppIntents

/** 画面の識別子。文字列ルートで素直に組む。 */
object Routes {
    const val HOME = "home"
    const val APPS = "apps"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings"

    const val DETAIL = "detail/{packageName}"
    fun detail(packageName: String) = "detail/$packageName"

    const val PERMISSION_APPS = "permission_apps/{group}"
    fun permissionApps(group: PermissionGroup) = "permission_apps/${group.name}"

    const val TAGS = "tags"
    const val HIDDEN = "hidden"
    const val USAGE_ACCESS = "usage_access"
    const val LICENSES = "licenses"
}

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.nav_home, Icons.Default.Home),
    BottomTab(Routes.APPS, R.string.nav_apps, Icons.Default.Apps),
    BottomTab(Routes.PERMISSIONS, R.string.nav_permissions, Icons.Default.Shield),
    BottomTab(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
)

@Composable
fun AppNavigation(
    viewModel: AppShelfViewModel,
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // LocalContext.current.getString() は使わないこと。
    // Configuration の変更（Android 13 のアプリごと言語切替など）で読み直されず、
    // 古い言語の文字列が出てしまう。LocalResources なら再構成時に更新される。
    // さらに LaunchedEffect(Unit) は再起動しないので、rememberUpdatedState で
    // コルーチンが常に最新の Resources を見るようにする。
    val resources by rememberUpdatedState(LocalResources.current)

    // ViewModel からの一言メッセージ（タグ名の重複など）
    LaunchedEffect(Unit) {
        viewModel.messages.collect { messageRes ->
            snackbarHostState.showSnackbar(resources.getString(messageRes))
        }
    }

    UninstallHost(viewModel = viewModel, snackbarHostState = snackbarHostState)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showBottomBar = bottomTabs.any { tab ->
        currentRoute?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenApp = { navController.navigate(Routes.detail(it)) },
                    onOpenAppList = { navController.navigateToTab(Routes.APPS) },
                    onOpenUsageAccess = { navController.navigate(Routes.USAGE_ACCESS) }
                )
            }
            composable(Routes.APPS) {
                AppListScreen(
                    viewModel = viewModel,
                    onOpenApp = { navController.navigate(Routes.detail(it)) }
                )
            }
            composable(Routes.PERMISSIONS) {
                PermissionsScreen(
                    viewModel = viewModel,
                    onOpenGroup = { navController.navigate(Routes.permissionApps(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenTags = { navController.navigate(Routes.TAGS) },
                    onOpenHidden = { navController.navigate(Routes.HIDDEN) },
                    onOpenUsageAccess = { navController.navigate(Routes.USAGE_ACCESS) },
                    onOpenLicenses = { navController.navigate(Routes.LICENSES) }
                )
            }

            composable(Routes.DETAIL) { entry ->
                val packageName = entry.arguments?.getString("packageName").orEmpty()
                AppDetailScreen(
                    viewModel = viewModel,
                    packageName = packageName,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PERMISSION_APPS) { entry ->
                val groupName = entry.arguments?.getString("group")
                val group = PermissionGroup.entries.firstOrNull { it.name == groupName }
                if (group == null) {
                    navController.popBackStack()
                } else {
                    PermissionAppsScreen(
                        viewModel = viewModel,
                        group = group,
                        onBack = { navController.popBackStack() },
                        onOpenApp = { navController.navigate(Routes.detail(it)) }
                    )
                }
            }
            composable(Routes.TAGS) {
                TagManagerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.HIDDEN) {
                HiddenAppsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.USAGE_ACCESS) {
                UsageAccessScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.LICENSES) {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/**
 * 順次アンインストールの実行役（F-35）。
 *
 * Android では「確認なしの一括削除」ができないため、
 * 待ち行列の先頭から 1 件ずつ OS のダイアログを開き、戻ってきたら次へ進める。
 * 画面遷移で消えないよう、NavHost の外側に置いている。
 */
@Composable
private fun UninstallHost(
    viewModel: AppShelfViewModel,
    snackbarHostState: SnackbarHostState
) {
    // LocalContext.current.getString() ではなく LocalResources を使う理由は
    // AppNavigation 側のコメントを参照
    val resources by rememberUpdatedState(LocalResources.current)
    val session by viewModel.uninstallSession.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onUninstallResult(removed = result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(session?.queue, session?.index) {
        val current = session ?: return@LaunchedEffect
        val target = current.current
        if (target != null) {
            launcher.launch(AppIntents.uninstall(target))
        } else {
            // 全件終わった
            snackbarHostState.showSnackbar(
                resources.getString(R.string.uninstall_done, current.removed)
            )
            viewModel.finishUninstall()
        }
    }
}

/** ボトムナビのタブ切替。同じタブを再度押しても積み上がらないようにする。 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
