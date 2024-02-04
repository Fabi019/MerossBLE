package dev.fabik.merossble.protocol.payloads

import org.json.JSONArray
import org.json.JSONObject

val RULES = listOf(
    intArrayOf(1698541200, 3600, 0),
    intArrayOf(1711846800, 7200, 1),
    intArrayOf(1729990800, 3600, 0),
    intArrayOf(1743296400, 7200, 1),
    intArrayOf(1761440400, 3600, 0),
    intArrayOf(1774746000, 7200, 1),
    intArrayOf(1792890000, 3600, 0),
    intArrayOf(1806195600, 7200, 1),
    intArrayOf(1824944400, 3600, 0),
    intArrayOf(1837645200, 7200, 1),
    intArrayOf(1856394000, 3600, 0),
    intArrayOf(1869094800, 7200, 1),
    intArrayOf(1887843600, 3600, 0),
    intArrayOf(1901149200, 7200, 1),
    intArrayOf(1919293200, 3600, 0),
    intArrayOf(1932598800, 7200, 1),
    intArrayOf(1950742800, 3600, 0),
    intArrayOf(1964048400, 7200, 1),
    intArrayOf(1982797200, 3600, 0),
    intArrayOf(1995498000, 7200, 1)
)

data class Time(
    val timezone: String,
    val timestamp: Long,
    val timeRule: List<IntArray> = RULES
)

fun Time.toJSONObject() = JSONObject().apply {
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