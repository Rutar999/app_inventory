package io.github.rutar999.appshelf.util

import java.text.Normalizer

/**
 * 検索文字列の正規化（F-02）。
 *
 * やっていること:
 * - NFKC 正規化（全角英数 → 半角、半角カナ → 全角カナ）
 * - カタカナ → ひらがな
 * - 小文字化
 *
 * やっていないこと:
 * - 漢字 → よみがな の変換。これは辞書（形態素解析）が必要で、
 *   端末内完結・依存最小という方針に合わないため v1 では対象外。
 */
object SearchText {

    fun normalize(raw: String): String {
        val nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        val sb = StringBuilder(nfkc.length)
        for (ch in nfkc) {
            sb.append(
                when (ch) {
                    in 'ァ'..'ヶ' -> ch - 0x60 // ァ..ヶ → ぁ..ゖ
                    'ヽ' -> 'ゝ'              // ヽ → ゝ
                    'ヾ' -> 'ゞ'              // ヾ → ゞ
                    else -> ch
                }
            )
        }
        return sb.toString().lowercase()
    }

    /**
     * 空白区切りの検索語をすべて含むか（AND 検索）。
     * 検索語が空なら常に true。
     */
    fun matches(normalizedTarget: String, rawQuery: String): Boolean {
        val query = normalize(rawQuery).trim()
        if (query.isEmpty()) return true
        return query.split(' ', '　')
            .filter { it.isNotEmpty() }
            .all { normalizedTarget.contains(it) }
    }
}
