# R8（コード縮小・難読化）の設定。
# Compose / Room / DataStore は各ライブラリが consumer rules を同梱しているため、
# 基本的に追加設定は不要。ここは「念のため」の最小限。

# クラッシュ解析のために行番号を残す
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room が生成する実装クラス
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Kotlin のリフレクション用メタデータ（Room / Compose が一部利用）
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
