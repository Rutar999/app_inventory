# 環境構築とビルド手順

Android 開発が初めての人向けに、最短で「アプリが実機で動く」ところまでの手順を書いています。

---

## 0. いまの状態

**環境構築とビルド検証は完了しています。** この PC には管理者権限が無かったため、
インストーラを使わず **zip 展開方式** ですべて `D:\dev_tools` に配置してあります。

| 道具 | 場所 |
|---|---|
| JDK 17.0.20 (Temurin) | `D:\dev_tools\jdk\jdk-17.0.20+8` |
| Android SDK | `D:\dev_tools\android-sdk` |
| Gradle 8.11.1 | `D:\dev_tools\gradle\gradle-8.11.1` |
| Android Studio 2026.1.3.7 | `D:\dev_tools\AndroidStudio` |

ビルド済みの成果物:

- `app-debug.apk` 18.1 MB
- `app-release-unsigned.apk` 1.57 MB（R8 で縮小済み）
- `app-release.aab` 3.59 MB（Play 提出形式）
- ユニットテスト 18件すべて合格 / コンパイル警告 0件

以降は「Android Studio を起動して開発を続ける」ところからです。

---

## 1. Android Studio を起動する

インストーラを通していないので、スタートメニューには登録されていません。
次の実行ファイルを直接起動します。

```
D:\dev_tools\AndroidStudio\android-studio\bin\studio64.exe
```

よく使うので、右クリック →「スタートメニューにピン留めする」かデスクトップにショートカットを作っておくと楽です。

### 初回起動時の設定

1. 「Do not import settings」を選ぶ
2. セットアップウィザードが SDK を入れようとしたら **Custom** を選び、
   SDK の場所に **`D:\dev_tools\android-sdk`** を指定する
   （すでに Platform 36 と Build-Tools が入っているので、再ダウンロードは不要）
3. Gradle JDK の確認: File > Settings > Build, Execution, Deployment > Build Tools > Gradle
   → **Gradle JDK** に `D:\dev_tools\jdk\jdk-17.0.20+8` を指定（または Studio 同梱の JBR でも可）

---

## 2. プロジェクトを開く

Android Studio の最初の画面で **Open**（New Project ではない）を選び、`D:\dev_app` を指定します。

`local.properties` に SDK の場所が書き込み済みなので、そのまま Gradle Sync が通るはずです。

### もし Sync でエラーが出たら

| エラーの内容 | 対処 |
|---|---|
| `SDK location not found` | `local.properties` の `sdk.dir` が正しいか確認 |
| `Plugin ... not found` | `gradle/libs.versions.toml` の `[versions]` を Studio の提案に合わせる |
| `Unsupported Java` | 上記の Gradle JDK 設定を確認 |

> バージョン指定（AGP 8.10.1 / Kotlin 2.1.20 / KSP 2.1.20-2.0.1 / Compose BOM 2025.05.00 /
> Room 2.7.1）は **実際にビルドが通ることを確認済み**です。
> Studio が「新しい版があります」と提案してきても、急いで上げる必要はありません。

---

## 4. 実機で動かす

**エミュレータではなく実機を強く推奨します。** エミュレータにはアプリが十数本しか入っておらず、
このアプリの価値（大量のアプリを棚卸しする）が確認できません。

1. Android 端末の「設定 > デバイス情報 > ビルド番号」を7回タップ → 開発者モードが有効になる
2. 「設定 > システム > 開発者向けオプション」→ **USB デバッグ** を ON
3. USB で PC に接続 → 端末側に出るダイアログで「このパソコンを許可」
4. Android Studio の上部で端末を選び、緑の ▶ (Run) を押す

### 最初に確認すること

- アプリ一覧が出るか（これが出れば、このアプリは成立している）
- 設定 > 使用状況へのアクセス を ON にすると、容量と最終起動日が出るか
- 権限タブで「カメラを使えるアプリ」が正しく出るか

---

## 5. コマンドラインからビルドする

`gradlew.bat` と wrapper jar は生成済みです。Android Studio のターミナルからならそのまま動きます。

外部の PowerShell から叩く場合は、環境変数を先に渡してください（システム環境変数には登録していません）。

```bash
$env:JAVA_HOME='D:\dev_tools\jdk\jdk-17.0.20+8'; $env:ANDROID_HOME='D:\dev_tools\android-sdk'; .\gradlew.bat assembleDebug
```

```bash
$env:JAVA_HOME='D:\dev_tools\jdk\jdk-17.0.20+8'; $env:ANDROID_HOME='D:\dev_tools\android-sdk'; .\gradlew.bat testDebugUnitTest
```

```bash
$env:JAVA_HOME='D:\dev_tools\jdk\jdk-17.0.20+8'; $env:ANDROID_HOME='D:\dev_tools\android-sdk'; .\gradlew.bat bundleRelease
```

毎回書くのが面倒なら、システム環境変数に `JAVA_HOME` と `ANDROID_HOME` を登録してしまってください
（設定 > システム > バージョン情報 > システムの詳細設定 > 環境変数）。

---

## 6. テストを走らせる

ロジック部分（検索・並び替え・集計）には JUnit のテストが入っています。

- Android Studio のプロジェクトツリーで `app/src/test` を右クリック → **Run 'Tests in ...'**
- またはコマンドラインで `.\gradlew.bat test`

Android の API に依存しない純粋な関数だけをテスト対象にしているので、実機なしで数秒で終わります。

---

## 7. プロジェクトの読み方（どこに何があるか）

```
app/src/main/java/com/appshelf/inventory/
├ MainActivity.kt          … 画面の入口。テーマ適用とアプリ追加/削除の検知
├ AppShelfApp.kt           … Application。依存の入れ物を1つ作る
├ di/AppContainer.kt       … 手動DI。何がどこで作られるかはここを見る
├ model/                   … データの形（AppInfo / AppEntry / PermissionGroup など）
├ data/
│  ├ system/               … Android API との境界（★ここが本アプリの核心）
│  │  ├ PackageScanner     … アプリ一覧と権限の取得
│  │  ├ UsageStatsSource   … 最終起動日・使用時間
│  │  └ StorageStatsSource … 容量
│  ├ db/                   … Room（タグ・お気に入り・非表示・日次スナップショット）
│  ├ prefs/                … DataStore（設定）
│  └ repo/AppRepository    … 上記を結合して画面に流す
├ ui/
│  ├ AppShelfViewModel.kt  … 画面をまたいで共有する状態
│  ├ navigation/           … 画面遷移とボトムナビ
│  ├ home / applist / detail / permissions / settings / onboarding / usage
│  └ components/           … 共通の部品
└ util/                    … 書式変換・検索正規化・アイコン読み込み・Intent
```

**最初に読むなら** `data/system/PackageScanner.kt` → `data/repo/AppRepository.kt` →
`ui/applist/AppListScreen.kt` の順がおすすめです。
「OSから取る → 加工する → 画面に出す」の流れがそのまま追えます。
