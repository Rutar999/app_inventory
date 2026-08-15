package io.github.rutar999.appshelf.util

import android.content.Context
import android.text.format.Formatter
import io.github.rutar999.appshelf.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

/** 画面表示用の書式変換。ロケール依存の処理は OS の API に任せる。 */
object Formatters {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

    /** 1.2 GB / 340 MB のような表記。ロケールに合わせて OS が整形してくれる。 */
    fun bytes(context: Context, value: Long?): String =
        if (value == null) context.getString(R.string.value_unknown)
        else Formatter.formatShortFileSize(context, value)

    fun date(context: Context, epochMillis: Long?): String =
        if (epochMillis == null || epochMillis <= 0L) context.getString(R.string.value_unknown)
        else dateFormatter.format(Instant.ofEpochMilli(epochMillis))

    /** 前面表示時間。0 のときも「0分」と出す（未取得の「—」と区別するため）。 */
    fun duration(context: Context, millis: Long?): String {
        if (millis == null) return context.getString(R.string.value_unknown)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        return when {
            totalMinutes <= 0L -> context.getString(R.string.duration_none)
            totalMinutes < 60L -> context.getString(R.string.duration_minutes, totalMinutes.toInt())
            else -> context.getString(
                R.string.duration_hours,
                (totalMinutes / 60).toInt(),
                (totalMinutes % 60).toInt()
            )
        }
    }

    /**
     * 「今日 / 昨日 / n日前 / 起動記録なし」。
     * @param lastTimeUsed 0 以下なら起動記録なし。null なら使用状況アクセスが未許可。
     */
    fun lastUsed(context: Context, lastTimeUsed: Long?, now: Long = System.currentTimeMillis()): String {
        if (lastTimeUsed == null) return context.getString(R.string.value_unknown)
        if (lastTimeUsed <= 0L) return context.getString(R.string.unused_never)
        val days = ((now - lastTimeUsed) / (24L * 60 * 60 * 1000)).toInt()
        return when {
            days <= 0 -> context.getString(R.string.unused_today)
            days == 1 -> context.getString(R.string.unused_yesterday)
            else -> context.getString(R.string.unused_days_ago, days)
        }
    }
}
