// ルートの build.gradle.kts。
// ここではプラグインを「宣言だけ」して、実際に適用するのは app/build.gradle.kts 側。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
