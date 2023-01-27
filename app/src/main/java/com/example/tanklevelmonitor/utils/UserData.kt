package com.example.tanklevelmonitor.utils

import java.sql.Timestamp

data class UserData(
    val ap_pass: String? = "",
    val ap_ssid: String? = "",
    val level: Long? = 0L,
    val min: Long? = 0L,
    val max: Long? = 0L,
    val ssid: String? = "",
    val pass: String? = "",
    val time: Long? = 0L,
    val pending: Boolean? = false
)
