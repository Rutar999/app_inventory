# F-Droid 公開 手順書

最終更新: 2026-08-15

---

## なぜ F-Droid を併用するのか

| | Google Play | F-Droid |
|---|---|---|
| 費用 | $25 | **無料** |
| テスター12人×14日 | **必須** | **不要** |
| 審査 | 自動＋人力 | 人力レビュー（時間はかかる） |
| リーチ | 圧倒的に大きい | 小さい |
| 利用者層 | 一般 | **プライバシー意識が高い層** |

**排他ではなく併用できます。** F-Droid は今すぐ着手でき、Play の本人確認待ちや
テスター集めと完全に並行できるため、待ち時間が実質ゼロになります。

そして F-Droid の利用者層は、このアプリの想定ユーザーとほぼ一致します。
「広告なし・トラッキングなし・INTERNET 権限なし」は、Play では埋もれますが
F-Droid では最大の訴求点になります。

---

## 適合状況（2026-08-15 時点で確認済み）

| F-Droid の要件 | 本アプリ |
|---|---|
| OSI 承認のオープンソースライセンス | ✅ Apache License 2.0（`LICENSE`） |
| ソースからビルド可能 | ✅ Gradle 標準構成 |
| プロプライエタリ依存がないこと | ✅ **GMS / Firebase / 課金 / 広告 SDK いずれも不使用** |
| トラッキングがないこと | ✅ INTERNET 権限すら宣言していない |
| 署名鍵なしでビルドが通ること | ✅ `keystore.properties` が無ければ署名なしでビルドする設計 |
| Gradle wrapper が含まれること | ✅ `gradle/wrapper/gradle-wrapper.jar` |

**技術的な障害は見当たりません。**

---

## Step 1. GitHub の公開リポジトリを作る

### Git のコミット識別情報（✅ 設定済み）

**本名とメールアドレスが公開リポジトリに残らないよう設定してあります。**

```
user.name  = Rutar
user.email = 40605258+Rutar999@users.noreply.github.com
```

この PC のグローバル設定は `Yudai Furuta <furuta.yudai123@gmail.com>` ですが、
**このリポジトリのローカル設定で上書きしている**ため、コミットに本名は残りません。
`--global` を付けずに `git config` したためです。

> noreply アドレスの数字（40605258）は GitHub アカウント固有の ID です。
> これが正確でないと、コミットが GitHub プロフィールに紐付かず
> Contributions（草）に反映されません。GitHub の Settings > Emails で
> **Keep my email addresses private** を有効にしておくこと。

### ✅ 初期化とコミットは完了しています

```
commit  アプリ棚卸し v1.0.0 初回リリース
tag     v1.0.0
```

### リポジトリとプッシュ

公開リポジトリ **https://github.com/Rutar999/app_inventory** を作成済み。

```bash
git remote add origin https://github.com/Rutar999/app_inventory.git
```

```bash
git push -u origin main --tags
```

> **`.gitignore` を必ず確認すること。** `keystore.properties` と `*.jks` が
> 除外されていることは確認済みですが、プッシュ前に `git status` で
> 署名鍵が含まれていないことを目視してください。

---

## Step 2. メタデータの確認

F-Droid は `fastlane/metadata/android/<locale>/` を自動で読み込みます。作成済みです。

```
fastlane/metadata/android/
├ ja-JP/
│  ├ title.txt               … アプリ棚卸し
│  ├ short_description.txt   … 80文字以内
│  ├ full_description.txt    … 詳細説明
│  ├ changelogs/1.txt        … versionCode に対応
│  └ images/
│     ├ icon.png             … 512×512
│     ├ featureGraphic.png   … 1024×500
│     └ phoneScreenshots/    … 1〜6.png
└ en-US/  （同じ構成）
```

**バージョンを上げたら `changelogs/<versionCode>.txt` を追加すること。**
ファイル名は versionName ではなく **versionCode** です（v1.0.0 なら `1.txt`）。

---

## Step 3. F-Droid に申請する

F-Droid はアプリのメタデータを `fdroiddata` リポジトリで管理しています。
申請は GitLab 上でのマージリクエストです。

1. https://gitlab.com/fdroid/fdroiddata をフォーク
2. `metadata/io.github.rutar999.appshelf.yml` を作成
3. マージリクエストを送る
4. F-Droid のメンテナがレビュー（**数週間かかることがあります**）

### メタデータファイルの雛形

`metadata/io.github.rutar999.appshelf.yml`

```yaml
Categories:
  - System
License: Apache-2.0
SourceCode: https://github.com/Rutar999/app_inventory
IssueTracker: https://github.com/Rutar999/app_inventory/issues

AutoName: アプリ棚卸し

RepoType: git
Repo: https://github.com/Rutar999/app_inventory.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

> `commit:` には Step 1 で打った **タグ名**を書きます。
> ブランチ名ではなくタグを指すのが F-Droid の作法です。

### 簡易な申請方法

雛形を自分で書くのが不安な場合、F-Droid には
**「Requests for Packaging」** というイシューテンプレートがあります。
リポジトリ URL を添えて依頼を出すと、メンテナ側で用意してくれることがあります。

https://gitlab.com/fdroid/rfp/-/issues

---

## Step 4. 署名について（重要）

F-Droid は **F-Droid 自身の鍵でアプリに署名します。**
これは Google Play の署名鍵とは別物です。

そのため、**同じアプリでも Play 版と F-Droid 版は署名が異なり、相互に上書き更新できません。**
ユーザーはどちらか一方からインストールすることになります。これは仕様であり、問題ありません。

> なお、Play 版の署名鍵を F-Droid に渡す必要はありません（渡してはいけません）。

---

## Step 5. 公開後の更新フロー

1. `app/build.gradle.kts` の `versionCode` を +1、`versionName` を更新
2. `fastlane/metadata/android/<locale>/changelogs/<新versionCode>.txt` を追加
3. コミットして**新しいタグを打つ**（例: `v1.1.0`）
4. GitHub にプッシュ

`AutoUpdateMode: Version` と `UpdateCheckMode: Tags` を設定してあるので、
**F-Droid が新しいタグを検出して自動でビルド・公開します。** 毎回申請する必要はありません。

---

## 併用時の注意

- **applicationId は Play 版と同じ `io.github.rutar999.appshelf` で構いません。**
  ストアが違えば衝突しません
- プライバシーポリシーの URL は Play と共用できます
- **Play で「クローズドテスト中」でも F-Droid には出せます。** 両者は独立しています

---

## 他ストアについて

F-Droid 以外にも、費用ゼロ・テスター要件なしのストアがあります。
APK を上げるだけなので、追加コストはほぼありません。

| ストア | 費用 | 備考 |
|---|---|---|
| Amazon Appstore | 無料 | Fire タブレット含む。審査あり |
| Samsung Galaxy Store | 無料 | Samsung 端末。日本ではシェア小 |
| Huawei AppGallery | 無料 | **本アプリは GMS 非依存なのでそのまま動く** |

> **APK の直接配布について**
>
> 2026年9月30日から **Android Developer Verification** が始まり、
> 認定 Android 端末では Play 以外（他ストア・APK 直配布を含む）でも
> 開発者の身元登録（政府発行ID・$25・署名鍵の登録）が必要になります。
> 適用はブラジル・インドネシア・シンガポール・タイから始まり、2027年に世界展開予定です。
>
> **「Play を避けて費用と本人確認を回避する」という道は今後塞がります。**
> F-Droid 経由の配布がこの規制でどう扱われるかは流動的なので、
> 実際に申請される時点で最新情報をご確認ください。
