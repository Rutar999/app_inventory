# アプリ棚卸し / AppShelf

端末にインストール済みのアプリを **一覧 → 検索 → 分析 → 整理** するための、
ローカル完結型 Android ユーティリティ。

> 入れっぱなしのアプリを、消す判断ができる形で見せる。

## 特徴

- **完全オフライン** — INTERNET 権限を宣言していないので、データが端末外に出ることが技術的にない
- **権限の逆引き** — 「マイクを使えるアプリ 8件」のようにカテゴリから引ける
- **未使用検出** — 最終起動日から棚卸し候補を提示。使用状況が未許可でもインストール日で代替表示
- **自分なりの分類** — 色付きタグ、お気に入り、非表示（すべてこのアプリ内の表示にのみ影響）

## 技術構成

| 領域 | 採用 |
|---|---|
| 言語 | Kotlin |
| UI | Jetpack Compose + Material 3（Dynamic Color 対応） |
| アーキテクチャ | MVVM（Composable / ViewModel / Repository） |
| 非同期 | Coroutines + Flow |
| DB | Room |
| 設定 | DataStore (Preferences) |
| DI | 手動（`di/AppContainer.kt`） |
| ナビゲーション | Navigation Compose |
| 対応 | minSdk 26 / targetSdk 36 |

外部の UI ライブラリ・画像ライブラリ・グラフライブラリは使っていません（棒グラフは Compose で自作）。

## ドキュメント

| ファイル | 内容 |
|---|---|
| [docs/SETUP.md](docs/SETUP.md) | 環境構築とビルド手順（Android 初学者向け） |
| [docs/PLAY_RELEASE.md](docs/PLAY_RELEASE.md) | Google Play 公開手順、ストア掲載文の案 |
| [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) | プライバシーポリシー（公開時に URL 化が必要） |
| [docs/PROGRESS.md](docs/PROGRESS.md) | 実装状況・未実装項目・設計上の判断 |

## Android の制約について

このアプリの設計は、Android のプライバシー保護による制約が前提になっています。

| やりたいこと | 可否 |
|---|---|
| インストール済みアプリの一覧・検索 | ✅ |
| 最終起動日・使用時間・容量 | ✅（「使用状況へのアクセス」の許可が必要） |
| 宣言権限・付与済み権限の一覧と逆引き | ✅ |
| 1本ずつのアンインストール（確認ダイアログ経由） | ✅ |
| 一括・無確認アンインストール | ❌ |
| 他アプリの権限剥奪・キャッシュ削除・強制停止 | ❌（設定画面へ誘導のみ） |
| システムアプリの削除・無効化 | ❌ |

アプリ一覧の取得は `<queries>`（ランチャー Intent 宣言）方式です。
`QUERY_ALL_PACKAGES` は Play の権限宣言フォームが必要になり公開遅延の主因になるため、使っていません。

## ライセンス

このプロジェクトは [Apache License 2.0](LICENSE) のもとで公開しています。

```
Copyright 2026 rutar999

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

### 利用しているオープンソースソフトウェア

リリース版に同梱されるライブラリは**すべて Apache License 2.0** です。

- Jetpack Compose / AndroidX Core・Activity・Lifecycle・Navigation・Room・DataStore ほか — The Android Open Source Project
- Kotlin Standard Library / kotlinx.coroutines — JetBrains s.r.o.
- Okio — Square, Inc.
- Guava (ListenableFuture) — The Guava Authors
- JSpecify Annotations — The JSpecify Authors

Apache 2.0 はバイナリ配布時にもライセンス全文と帰属表示の同梱を求めるため、
アプリ内に **設定 → オープンソースライセンス** の画面を用意しています
（`ui/settings/LicensesScreen.kt` と `res/raw/apache_2_0.txt`）。

ライブラリを追加・変更したときは、次のコマンドで実際の同梱物を確認し、
`LicensesScreen.kt` の `LIBRARIES` を更新してください。

```bash
.\gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath
```

> なお、アプリアイコンは生成 AI で作成した画像を加工したものです。
> ソースコードの Apache 2.0 とは別に、画像の利用条件は各自でご確認ください。
