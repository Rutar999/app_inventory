package io.github.rutar999.appshelf

import android.app.Application
import io.github.rutar999.appshelf.di.AppContainer

/**
 * アプリのエントリポイント。
 * DI コンテナ（依存の入れ物）をここで 1 つだけ作る。
 */
class AppShelfApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
