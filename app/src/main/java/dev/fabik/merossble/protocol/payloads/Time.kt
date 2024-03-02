package dev.fabik.merossble.protocol.payloads

import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class Time(
    private val timezone: String,
    private val timestamp: Long,
) {
    private val timeRule: List<IntArray> = calculateTimeRule()

    fun toJSONObject() = JSONObject().apply {
        put("timeRule", JSONArray(timeRule.map {
            JSONArray().apply {
                put(it[0])
                put(it[1])
                put(it[2])
            }
        }))
        put("timezone", timezone)
        put("timestamp", timestamp)
    }

    private fun calculateTimeRule(): List<IntArray> {
        val timeRules = mutableListOf<IntArray>()

        val tz = TimeZone.getTimeZone(timezone)
        val calendar = Calendar.getInstance(tz)

        if (tz.useDaylightTime() || tz.inDaylightTime(Date(timestamp))) {
            repeat(20) {
                val transitionTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val rules = tz.toZoneId().rules
                    rules.nextTransition(calendar.toInstant()).instant.toEpochMilli()
                } else {
                    findNextDstTransition(calendar)
                }

                timeRules.add(
                    intArrayOf(
                        (transitionTime / 1000).toInt(),
                        tz.getOffset(transitionTime) / 1000,
                        if (tz.inDaylightTime(Date(transitionTime))) 1 else 0
                    )
                )

                calendar.timeInMillis = transitionTime
            }
        } else {
            timeRules.add(intArrayOf(0, tz.rawOffset / 1000, 0))
        }

        return timeRules
    }

    // Function for devices running Android lower than 8.0
    private fun findNextDstTransition(calendar: Calendar): Long {
        // Define the search range
        var start = calendar.timeInMillis
        var end = start + 365 * 24 * 60 * 60 * 1000L // One year in milliseconds
        val current = calendar.timeZone.inDaylightTime(Date(start))

        // Perform binary search
        while (start <= end) {
            val mid = start + (end - start) / 2
            val isDst = calendar.timeZone.inDaylightTime(Date(mid))
            if (isDst != current) {
                end = mid - 1
            } else {
                start = mid + 1
            }
        }

        return start
    }
}
