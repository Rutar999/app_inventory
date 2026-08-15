package io.github.rutar999.appshelf.model

import android.Manifest
import androidx.annotation.StringRes
import io.github.rutar999.appshelf.R

/**
 * 危険権限のグループ（要件定義書 §4 の「権限」タブのカテゴリ）。
 *
 * OS の PermissionInfo からグループを引くこともできるが、
 * - 端末やバージョンによってグループ名が揺れる
 * - 全アプリ分を PackageManager に問い合わせると遅い
 * という理由で、棚卸しに必要なぶんだけ静的な表として持っている。
 */
enum class PermissionGroup(
    @get:StringRes val labelRes: Int,
    /** 危険度の並び順（小さいほど上に出す） */
    val severity: Int
) {
    LOCATION(R.string.pg_location, 0),
    MICROPHONE(R.string.pg_microphone, 1),
    CAMERA(R.string.pg_camera, 2),
    CONTACTS(R.string.pg_contacts, 3),
    SMS(R.string.pg_sms, 4),
    CALL_LOG(R.string.pg_call_log, 5),
    PHONE(R.string.pg_phone, 6),
    CALENDAR(R.string.pg_calendar, 7),
    STORAGE(R.string.pg_storage, 8),
    SENSORS(R.string.pg_sensors, 9),
    ACTIVITY(R.string.pg_activity, 10),
    NEARBY(R.string.pg_nearby, 11),
    NOTIFICATIONS(R.string.pg_notifications, 12);

    companion object {
        /**
         * 権限名 → グループ の対応表。ここに無い権限は「通常権限」とみなして扱わない。
         *
         * PROCESS_OUTGOING_CALLS のように非推奨になった権限もあえて残している。
         * 古いアプリは今も宣言しており、棚卸しでは「宣言されている事実」を見せたいため。
         */
        @Suppress("DEPRECATION")
        private val MAP: Map<String, PermissionGroup> = buildMap {
            fun put(group: PermissionGroup, vararg permissions: String) {
                permissions.forEach { put(it, group) }
            }

            put(
                LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                "android.permission.ACCESS_BACKGROUND_LOCATION"
            )
            put(MICROPHONE, Manifest.permission.RECORD_AUDIO)
            put(CAMERA, Manifest.permission.CAMERA)
            put(
                CONTACTS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.GET_ACCOUNTS
            )
            put(
                SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.RECEIVE_WAP_PUSH
            )
            put(
                CALL_LOG,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.PROCESS_OUTGOING_CALLS
            )
            put(
                PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.USE_SIP,
                Manifest.permission.ADD_VOICEMAIL,
                "android.permission.READ_PHONE_NUMBERS",
                "android.permission.ANSWER_PHONE_CALLS",
                "android.permission.ACCEPT_HANDOVER"
            )
            put(
                CALENDAR,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
            put(
                STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            )
            put(
                SENSORS,
                Manifest.permission.BODY_SENSORS,
                "android.permission.BODY_SENSORS_BACKGROUND"
            )
            put(ACTIVITY, "android.permission.ACTIVITY_RECOGNITION")
            put(
                NEARBY,
                "android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH_ADVERTISE",
                "android.permission.NEARBY_WIFI_DEVICES",
                "android.permission.UWB_RANGING"
            )
            put(NOTIFICATIONS, "android.permission.POST_NOTIFICATIONS")
        }

        fun of(permissionName: String): PermissionGroup? = MAP[permissionName]

        /** 表示順に並べたグループ一覧 */
        val ordered: List<PermissionGroup> = entries.sortedBy { it.severity }
    }
}
