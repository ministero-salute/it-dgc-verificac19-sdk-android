package it.ministerodellasalute.verificaC19sdk.util

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Locale
import okhttp3.Response
import kotlin.math.abs

object TimeCheckManager {

    fun isTimeAlignedWithServer(response: Response): Boolean? {
        return response.header("Date")?.let { serverDateString ->
            val serverDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).parse(serverDateString)!!
			val MAX_GAP_DEVICE_SERVER: Long = TimeUnit.MINUTES.toMillis(2L)
            abs(serverDate.time - System.currentTimeMillis()) < MAX_GAP_DEVICE_SERVER
        }
    }
}
