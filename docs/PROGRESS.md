# AppShelf 開発進捗メモ

> Claude Code の会話が圧縮されても文脈を失わないための作業メモ。
> 更新日: 2026-08-15

## 1. 何を作っているか

`C:\Users\mytho\Downloads\要件定義書_アプリ棚卸しアプリ.md` に基づく Android アプリ **AppShelf（アプリ棚卸し）**。
端末にインストール済みのアプリを 一覧 / 検索 / 分析 / 整理 するローカル完結型ユーティリティ。

- プロジェクトルート: `D:\dev_app`
- applicationId: `io.github.rutar999.appshelf`
- minSdk 26 / targetSdk 36 / compileSdk 36
- Kotlin + Jetpack Compose + Material 3 + Room + DataStore + Navigation Compose
- DI なし（手動 = `AppContainer`）、ネットワーク権限なし（完全オフライン）

## 2. 環境（2026-08-15 構築済み）

すべて管理者権限なしで `D:\dev_tools` に zip 展開してある。

| 項目 | 場所 |
|---|---|
| JDK 17.0.20 (Temurin) | `D:\dev_tools\jdk\jdk-17.0.20+8` |
| Android SDK | `D:\dev_tools\android-sdk`（Platform 36 / Build-Tools 36.0.0・35.0.0 / Platform-Tools） |
| Gradle 8.11.1 | `D:\dev_tools\gradle\gradle-8.11.1` |
| Android Studio 2026.1.3.7 | `D:\dev_tools\AndroidStudio` |

`D:\dev_app\local.properties` に SDK の場所を記録済み（`.gitignore` 対象）。
`gradlew.bat` と wrapper jar も生成済みなので、コマンドラインからビルドできる。

コマンドラインで叩くときは環境変数を渡すこと:

```bash
$env:JAVA_HOME='D:\dev_tools\jdk\jdk-17.0.20+8'; $env:ANDROID_HOME='D:\dev_tools\android-sdk'; .\gradlew.bat assembleDebug
```

### ビルド検証の結果（済）

| 項目 | 結果 |
|---|---|
| `assembleDebug` | ✅ 成功 → `app-debug.apk` 18.1 MB |
| `assembleRelease`（R8 有効） | ✅ 成功 → `app-release-unsigned.apk` **1.57 MB** |
| `bundleRelease` | ✅ 成功 → `app-release.aab` 3.59 MB |
| `testDebugUnitTest` | ✅ **18件すべて合格** |
| コンパイル警告 | ✅ 0件 |
| リリース版マージ済みマニフェスト | ✅ `<queries>` 維持 / `QUERY_ALL_PACKAGES` なし / INTERNET なし / minSdk 26・targetSdk 36 |

初回ビルドで出たエラーは1件のみで修正済み:
`PackageManager.PermissionInfoFlags` は**存在しないクラス**だった。
API 33 で `Flags` クラス化されたのは `getPackageInfo` / `getApplicationInfo` /
`queryIntentActivities` などで、`getPermissionInfo` は int 引数のまま（非推奨でもない）。

### 実機検証の結果（2026-08-15 実施）

検証機: **OPPO CPH2797 / Android 16 (API 36) / arm64-v8a / ja-JP**
（targetSdk と同じ API 36。インストール済みアプリ 235 本という実運用に近い条件）

| 項目 | 結果 |
|---|---|
| アプリ一覧の取得（`<queries>` 方式） | ✅ 235 本を取得。アイコン・名称すべて表示 |
| 容量取得（StorageStats） | ✅ 合計 80 GB、NIKKE 20 GB / ShadowverseWB 11 GB 等 |
| 使用状況（UsageStats） | ✅ 「338 MB · 45日前」のように最終起動日が出る |
| 未使用検出（90日しきい値） | ✅ 110〜113 本を検出、回収可能 32 GB |
| **F-15 未許可時の代替表示** | ✅ バナー表示 → 容量は「—」→ インストール日順にフォールバック |
| 日本語表示・ダークテーマ・Dynamic Color | ✅ 壁紙由来の配色が適用されている |
| ボトムナビ 4 タブ | ✅ 動作 |

**実機で見つけて直したバグ 3 件:**

1. **オンボーディングがナビゲーションバーに潜り込む**
   targetSdk 35 以降は edge-to-edge が強制されるため、`Scaffold` を使っていない
   `OnboardingScreen` では自前で `safeDrawingPadding()` が必要だった。
   「次へ」ボタンの下部が隠れていた。

2. **アイコンの取り違え**（「Fate/GO」に Brave のアイコンが出た）
   `produceState` の内部 `remember` は `key1` では作り直されないため、
   LazyColumn が行を使い回すと前の行のアイコンが `value` に残り、
   `if (value == null)` のガードで正しいアイコンを読み込まないまま固定されていた。
   `remember(packageName) { mutableStateOf(...) }` + `LaunchedEffect` に変更。
   **同じ罠があるので、リスト内の非同期読み込みに `produceState` を使わないこと。**

3. **検索欄のプレースホルダが 2 行に折り返して入力欄が縦に伸びる**
   文言を短縮し `maxLines = 1` を指定。

### ⚠️ 未解決の設計課題: `firstInstallTime` が信用できない端末がある

検証機（ColorOS）では OS が多くのアプリに **固定のダミー値**を返す:

```
Instagram  firstInstallTime = 2010-01-01 09:00:27
Chrome     firstInstallTime = 2010-01-01 09:00:26
```

そのため「インストールから6070日」のような無意味な表示になり、
**F-15 のフォールバック（インストール日順で古いアプリを出す）が成立しない。**
`lastUpdateTime` は正しい値（2026-08-12）が返っている。

対応方針は未決。要検討の選択肢:
- ダミー値らしき `firstInstallTime` を検出して「不明」と表示する
- フォールバックの並び順を `lastUpdateTime` に変える
- そもそも代替表示をやめて「許可してください」だけにする

### 実機検証 第2弾（2026-08-15 実施・全機能）

| 機能 | 結果 |
|---|---|
| F-21 権限の逆引き | ✅ 「マイクを使えるアプリ 6本」を正しく列挙 |
| F-20 詳細画面の権限表示 | ✅ 付与済み／未付与を区別、全32件へのリンク |
| F-02 インクリメンタル検索 | ✅ 235件 → 11件。パッケージ名にもマッチ |
| F-30 タグ作成 | ✅ Room に永続化、色選択可 |
| F-31 タグ付与 | ✅ 一覧にタグチップが出る |
| F-33 お気に入り | ✅ ★表示、フィルタで1件に絞れる |
| **F-35 順次アンインストール** | ✅ 長押し→選択→確認→OS ダイアログ→**実際に削除完了** |
| F-41 アンインストール検知 | ✅ 削除後に一覧が自動更新された |
| F-37 CSV エクスポート | ✅ SAF 経由で保存、270行×19列、書式正常 |
| アクセシビリティ | ⚠️ ノードツリーに全テキストが露出、アイコンには content-desc あり、タップ領域 48dp 以上。**ただし TalkBack 実起動での読み上げ順序は未確認** |

**追加で見つけて直したバグ:**

4. **権限画面のサブタイトルが縦に見切れる**
   `PermissionGroupRow` の `Row` に `height(32.dp)` を固定していたため、
   「付与済み n 件 / 宣言 m 件」が切れていた。`heightIn(min = 48.dp)` に変更。
   **リストの行に固定高を与えないこと。**

5. 「マイク **を**使えるアプリ」の不自然な空白（文字列リソースを修正）

### 設計上の申し送り

- **エクスポートは表示中の一覧ではなく全アプリを出力する**（269行 vs 画面表示235件）。
  システムアプリ非表示の設定を無視するため。棚卸し記録としては全件が妥当だが、
  ユーザーが混乱する可能性はある。仕様として要判断。
- `firstInstallTime` のダミー値問題は**一部のアプリに限られる**ことが判明。
  Box は 2025/12/27 と正常値。Instagram / Chrome は 2010-01-01。
  プリインストールや復元されたアプリだけが影響を受けるとみられる。

**残る未検証:** TalkBack 実起動での読み上げ、JSON エクスポート、非表示リストからの復帰、
テーマ切替、グリッド表示、折りたたみ端末・タブレット。

### ハマったところ（再発防止メモ）

**1. `mipmap-anydpi-v26` の `-v26` は外してはいけない**

Lint が `ObsoleteSdkInt`（minSdk 26 だから `-v26` は不要）と言ってくるが、
実際に `mipmap-anydpi` へ変えると AAPT2 がアダプティブアイコンを解決できず
`resource mipmap/ic_launcher not found` でビルドが落ちる。
Android Studio の新規プロジェクトテンプレートも `-v26` 付きのまま。
`app/build.gradle.kts` の `lint { disable += "ObsoleteSdkInt" }` で警告を止めてある。

**2. Windows で `gradlew clean` が失敗することがある**

Lint が解析用 jar を掴んだままだと `app\build` を削除できず、
`Unable to delete directory` で clean が落ちる。**この状態で「クリーンビルドした」と誤認しやすい。**
先に `gradlew --stop` でデーモンを止めてから clean すること。

**3. PowerShell 5.1 でソースを一括置換しない**

`Get-Content -Raw` は UTF-8 を CP932 として読むため、日本語コメントが文字化けする。
置換は Android Studio か、`[System.IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)` で
エンコーディングを明示して行うこと。

**4. マニフェストの検証は文字列一致でやらない**

マージ済みマニフェストはソースのコメントを保持するため、
「`QUERY_ALL_PACKAGES` を使わない」というコメント本文が grep に引っかかる。
実際の宣言を確認するには次を使う（コンパイル済みバイナリを読む）:

```bash
D:\dev_tools\android-sdk\build-tools\36.0.0\aapt2.exe dump permissions app\build\outputs\apk\debug\app-debug.apk
```

現時点の出力は `PACKAGE_USAGE_STATS` / `REQUEST_DELETE_PACKAGES` /
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（androidx が自動付与する署名レベル権限）の3つのみ。

## 3. 要件定義書の「未決事項」に対して置いた既定値

書類 §10 の未決事項は、以下のように仮決めして実装済み。変更は容易。

| # | 項目 | 決めた値 | 変更箇所 |
|---|---|---|---|
| 1 | アプリ名 | `AppShelf`（表示名「アプリ棚卸し」） | `res/values*/strings.xml` の `app_name` |
| 2 | 未使用の既定しきい値 | **90日** | `SettingsRepository.DEFAULT_UNUSED_DAYS` |
| 3 | 配色 | Material 3 Dynamic Color 既定 ON、独自カラーはフォールバック | `ui/theme/Color.kt`, 設定画面で切替可 |
| 4 | 権限タブ | **v1 に入れる**（F-20/21/23 が P0 のため） | — |
| 5 | エクスポート形式 | **CSV と JSON の両方** | `util/Exporter.kt` |
| 6 | 英語対応 | **v1 に含める**（既定=日本語、`values-en` に英語） | `res/values-en/strings.xml` |

※ `applicationId` は公開後に変更不可。独自ドメインを持っているなら初回アップロード前に見直すこと。

## 4. 実装状況（要件ID対応）

### 完了（P0 + 主要な P1）
- F-01 一覧表示 / F-02 インクリメンタル検索（かな・全半角ゆらぎ吸収）/ F-03 並び替え
- F-04 リスト・グリッド切替 / F-05 絞り込みチップ
- F-06 詳細画面 / F-07 起動・システム設定・Play で開く
- F-10 ダッシュボード / F-11 容量ランキング / F-12 使用時間ランキング / F-13 未使用検出
- F-15 使用状況アクセス未許可時の代替表示（インストール日ベース）
- F-20 権限一覧 / F-21 権限からの逆引き / F-22 権限サマリー / F-23 権限設定へのショートカット
- F-30 タグ CRUD / F-31 タグ付け（単体・一括）/ F-33 お気に入り / F-34 非表示
- F-35 複数選択 → 順次アンインストール
- F-37 CSV / JSON エクスポート
- F-40 オンボーディング / F-41 インストール検知で自動更新 / F-42 ダーク＋Dynamic Color / F-43 日英対応

### 未実装（P2 / 意図的に v1 スコープ外）
- F-14 棚卸しスコア
- F-16 長期トレンドの可視化 … **日次スナップショットの蓄積自体は実装済み**（`UsageSnapshotEntity`、
  1日1回リフレッシュ時に自動保存）。グラフ表示のみ未実装。
- F-24 「権限は多いのに使っていない」ハイライト
- F-32 自動タグ提案
- F-36 「あとで判断」フラグと再通知
- F-44 リマインダー通知 / F-45 バックアップ・復元
- 折りたたみ端末の2ペイン表示

### 既知の制約（実装上の割り切り）
- 検索の「かな読み対応」は **ひらがな/カタカナ・全角/半角のゆらぎ吸収まで**。
  漢字→よみがな変換は辞書が必要なため未対応（要件定義書の想定範囲を明示的に狭めている）。
- 日次スナップショットは WorkManager を使わず「アプリ起動時に1日1回」保存。
  依存を増やさない代わりに、アプリを開かない日は記録が飛ぶ。

## 5. テスト

`app/src/test` に JUnit のテストが入っている（Android 非依存の純粋ロジックのみ）。

- `SearchTextTest` … ひらがな/カタカナ・全半角の正規化、AND 検索
- `AppListLogicTest` … お気に入り固定、未使用フィルタ、並び替え、タグ検索
- `DashboardLogicTest` … 合計容量（cache 二重計上しないこと）、回収可能容量、代替表示

`.\gradlew.bat test` または Android Studio から実行。

## 6. 次にやること

1. `docs/SETUP.md` に従い Android Studio + JDK + SDK を導入
2. `D:\dev_app` を Android Studio で **Open**（New Project ではない）→ Gradle Sync
3. バージョン不一致が出たら `gradle/libs.versions.toml` を Studio の提案に合わせて更新
4. 実機で動作確認（エミュレータだとインストール済みアプリが少なく検証にならない）
5. `docs/PLAY_RELEASE.md` に従って公開準備（クローズドテスト 12人×14日が最長ボトルネック）
