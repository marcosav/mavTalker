package com.gmail.marcosav2010.common

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Logger
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object PublicIPResolver {

    private val IP_PROVIDERS = arrayOf("http://checkip.amazonaws.com",
            "http://bot.whatismyipaddress.com/", "https://ident.me/", "https://ip.seeip.org/",
            "https://api.ipify.org")
    private const val IP_TIMEOUT = 5L
    private val EXECUTOR = Executors.newFixedThreadPool(1)

    private val log: ILog = Logger.global

    var publicAddress: InetAddress? = null
        private set

    fun obtainPublicAddress() {
        log.log("Obtaining public address...", VerboseLevel.MEDIUM)
        try {
            val m = System.currentTimeMillis()
            publicAddress = obtainExternalAddress()
            log.log("Public address got in " + (System.currentTimeMillis() - m) + "ms: " + publicAddress!!.hostName,
                    VerboseLevel.MEDIUM)
        } catch (e: IOException) {
            log.log(e, "There was an error while obtaining public address, shutting down...")
        }
    }

    private fun obtainExternalAddress(): InetAddress {
        val r = try {
            EXECUTOR.invokeAny(IP_PROVIDERS.map { str -> readRawWebsite(str) }.toList(), IP_TIMEOUT, TimeUnit.SECONDS)
        } catch (e: Exception) {
            return InetAddress.getLocalHost()
        }

        EXECUTOR.shutdownNow()

        return InetAddress.getByName(r)
    }

    private fun readRawWebsite(str: String): Callable<String> = Callable {
        BufferedReader(InputStreamReader(URL(str).openStream())).use { it.readLine() }
    }
}