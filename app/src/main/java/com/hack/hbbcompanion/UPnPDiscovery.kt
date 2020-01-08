package com.hack.hbbcompanion

import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.util.Log
import okhttp3.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

class UPnPDiscovery(
    private val wifiManager: WifiManager
) : AsyncTask<Any?, Any?, Any?>() {
    private val dialDevices = HashSet<DialDevice>()
    override fun doInBackground(params: Array<Any?>): Any? {
        val lock = wifiManager.createMulticastLock("The Lock")
        lock.acquire()
        var socket: DatagramSocket? = null
        try {
            val group = InetAddress.getByName("239.255.255.250")
            val port = 1900
            val query = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 10\r\n" +
                    "ST: urn:dial-multiscreen-org:service:dial:1\r\n" +  // Use for Sonos
                    "ST: ssdp:all\r\n"+  // Use this for all UPnP Devices
                    "\r\n"
            socket = DatagramSocket(port)
            socket.reuseAddress = true
            val dgram = DatagramPacket(
                query.toByteArray(), query.length,
                group, port
            )
            socket.send(dgram)
            val time = System.currentTimeMillis()
            var curTime = System.currentTimeMillis()
            // Let's consider all the responses we can get in 1 second
            while (curTime - time < 1000) {
                val p = DatagramPacket(ByteArray(1024), 1024)
                socket.receive(p)
                val s = String(p.data, 0, p.length)
                print(s)

                val responseCode = s.substring(0, 12)
                if (responseCode.toUpperCase(Locale.ROOT) == "HTTP/1.1 200") {
                    dialDevices.add(DialDevice(
                        s.substringAfter("LOCATION: ").substringBefore("\r\n"),
                        p.address.hostAddress
                    ))
                }
                val request = Request.Builder()
                    .get()
                    .url(s.substringAfter("LOCATION: ").substringBefore("\r\n"))
                    .build()

                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        print(e?.stackTrace)
                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        Log.d("HbbTv", response?.message() ?: "No message")

                        val appName = "se.svt.smarttv"
                        // val appName = "YouTube"

                        val applicationUrl = response?.header("Application-URL") + appName

                        client.newCall(Request.Builder()
                            .url(applicationUrl)
                            .post(RequestBody.create(null, ByteArray(0)))
                            .header("Content-Length", "0")
                            .build())
                            .enqueue(object : Callback {
                                override fun onFailure(call: Call?, e: IOException?) {
                                    e?.printStackTrace()
                                }

                                override fun onResponse(call: Call?, response: Response?) {
                                    Log.d(TAG, "MJAU MJAU MJAU: ${response?.code()}")
                                }

                            })
                    }

                })
                curTime = System.currentTimeMillis()

            }



        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        lock.release()
        return null
    }

    companion object {
        val TAG = "MJAU"

        fun discoverDevices(wifiManager: WifiManager): List<DialDevice> {
            val discover = UPnPDiscovery(wifiManager)
            discover.execute()
            return try {
                Thread.sleep(1500)
                discover.dialDevices.toList()
            } catch (e: InterruptedException) {
                listOf()
            }
        }
    }

}
