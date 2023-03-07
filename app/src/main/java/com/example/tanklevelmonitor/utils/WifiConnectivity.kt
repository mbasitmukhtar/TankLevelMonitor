package com.example.tanklevelmonitor.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.*
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import okhttp3.*
import java.io.IOException

class WifiConnectivity(context: Context) {

    private val TAG = "WifiConnectivity"
    private lateinit var wifiManager: WifiManager
    private val okHttpClient = OkHttpClient()

    var requiredNetworkSSID = ""
    var requiredNetworkPass = ""

    val localContext = context
    var status = ""
    var wifiConnected: MutableLiveData<Boolean> = MutableLiveData()
    var statusInfo: MutableLiveData<String> = MutableLiveData("")
    var getRequestResponse: MutableLiveData<String> = MutableLiveData("")
    var postRequestResponse: MutableLiveData<String> = MutableLiveData("")
    val cm: ConnectivityManager =
        localContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        wifiConnected.postValue(false)
        checkIfLocationEnabled()
    }

    fun checkIfLocationEnabled() {
        val locationStatus: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is a new method provided in API 28
            val lm = localContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isLocationEnabled = lm.isLocationEnabled
            locationStatus = isLocationEnabled
        } else {
            // This was deprecated in API 28
            val mode = Settings.Secure.getInt(
                localContext.contentResolver, Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            val enabled = mode != Settings.Secure.LOCATION_MODE_OFF
            locationStatus = enabled
        }

        if (locationStatus) {
            initWifi()
        }
        if (!locationStatus) {
            Toast.makeText(localContext, "Please Enable Location.", Toast.LENGTH_SHORT).show()
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            localContext.startActivity(myIntent)
        }
    }

    private fun initWifi() {
        Log.d(TAG, "initWifi: ")
        wifiManager =
            (localContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)

        Log.d(TAG, "addWifiStateReceiver: ")
        val intentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        localContext.registerReceiver(wifiStateReceiver, intentFilter)
    }

    private val wifiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val wifiStateExtra = intent.getIntExtra(
                WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN
            )
            when (wifiStateExtra) {
                WifiManager.WIFI_STATE_ENABLED -> {
                    askAndStartScanWifi()
//                    statusString = "Wifi is ON\n"
//                    statusTextView.append(statusString)
                }

                WifiManager.WIFI_STATE_DISABLED -> {
                    status += "Wifi is OFF\n"
//                    statusString = "Wifi is OFF\n"
//                    statusTextView.append(statusString)
                    Toast.makeText(localContext, "Please Enable Wifi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun askAndStartScanWifi() {
        Log.d(TAG, "askAndStartScanWifi: ")
        val permission1 =
            ContextCompat.checkSelfPermission(
                localContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        // Check for permissions
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "askAndStartScanWifi: Permission not granted")
            return
        } else {
            startWifiScan()
        }
    }

    private fun startWifiScan() {
        if (wifiManager.isWifiEnabled) {
            Log.d(TAG, "startWifiScan: Permissions Already Granted")

            val scanStarted = wifiManager.startScan()

            if (scanStarted) {
                if (ActivityCompat.checkSelfPermission(
                        localContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                } else {
                    Log.d(TAG, "doStartScanWifi: scanResults: ${wifiManager.scanResults}")
                }

                // Register the receiver
                localContext.registerReceiver(
                    wifiScanResultReceiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )
                status += "Scan Started.\n"
            } else {
                Toast.makeText(localContext, "Wifi Scanning Failed.", Toast.LENGTH_LONG).show()
                status += "Scan Failed.\n"
            }
        } else {
            Toast.makeText(localContext, "Please Enable Wifi.", Toast.LENGTH_LONG).show()
            status += "Please Enable Wifi.\n"
        }
    }

    private val wifiScanResultReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive()")
            val ok = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)

            if (ok) {
                Log.d(TAG, "Scan Successful")
//                stringLong = "Scan Successful\n"
//                statusTextView.append(stringLong)

                if (ActivityCompat.checkSelfPermission(
                        localContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                } else {
                    val list: List<ScanResult> = wifiManager.scanResults
                    Log.d(TAG, "Scan Results: $list")
                    connectToRequiredNetwork(list, requiredNetworkSSID, requiredNetworkPass)
//                requireContext().unregisterReceiver(wifiReceiver)
                }
            } else {
                Log.d(TAG, "Error in scanning wifi devices.")
            }
        }
    }

    private fun connectToRequiredNetwork(
        list: List<ScanResult>,
        requiredNetworkSSID: String,
        requiredNetworkPass: String
    ) {
        var networkFound = false
        var networkCapabilities = ""
        for (item in list) {
            Log.d(TAG, "List of SSID: ${item.SSID}")
            if (item.SSID == requiredNetworkSSID) {
                networkFound = true
                networkCapabilities = item.capabilities
            }
        }

        if (networkFound) {
            localContext.unregisterReceiver(wifiStateReceiver)
            localContext.unregisterReceiver(wifiScanResultReceiver)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "connectToRequiredNetwork: Connecting to: $requiredNetworkSSID")
                status += "Connecting to: $requiredNetworkSSID\n"

                val suggestion1 = WifiNetworkSuggestion.Builder()
                    .setSsid(requiredNetworkSSID)
                    .setWpa2Passphrase(requiredNetworkPass)
                    .setIsAppInteractionRequired(true) // Optional (Needs location permission)
                    .build()

                val suggestionList = listOf(suggestion1)

//                wifiManager.removeNetworkSuggestions(suggestionList)
                val status = wifiManager.addNetworkSuggestions(suggestionList)
                if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    Log.d(
                        TAG,
                        "connectToRequiredNetwork: Failed in adding network suggestion $status"
                    )
                } else {
                    Log.d(TAG, "connectToRequiredNetwork: addNetworkSuggestions status $status")
                }

                makeNetworkRequest(requiredNetworkSSID, requiredNetworkPass)

            } else {
                val wifiConfig = WifiConfiguration()
                wifiConfig.SSID = "\"" + requiredNetworkSSID + "\""

                if (networkCapabilities.uppercase().contains("WEP")) {
                    // WEP Network.
                    wifiConfig.wepKeys[0] = "\"" + requiredNetworkPass + "\"";
                    wifiConfig.wepTxKeyIndex = 0;
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                } else if (networkCapabilities.uppercase().contains("WPA")) {
                    // WPA Network
                    wifiConfig.preSharedKey = "\"" + requiredNetworkPass + "\"";
                } else {
                    // OPEN Network.
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }

                wifiManager.addNetwork(wifiConfig)

                if (ActivityCompat.checkSelfPermission(
                        localContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                } else {
                    val configuredNetworksList = wifiManager.configuredNetworks
                    for (config in configuredNetworksList) {
                        if (config.SSID != null && config.SSID == "\"" + requiredNetworkSSID + "\"") {
                            wifiManager.disconnect()
                            wifiManager.enableNetwork(config.networkId, true)
                            wifiManager.reconnect()
                            changeUIAfterWifiConnection()
//                            requireContext().unregisterReceiver(wifiScanResultReceiver)
                            break
                        }
                    }
                }
            }

        } else {
//            statusString = "Not Found: $requiredNetworkSSID\n"
//            statusTextView.append(statusString)
        }
    }

    private fun makeNetworkRequest(requiredNetworkSSID: String, requiredNetworkPass: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val builder = WifiNetworkSpecifier.Builder()
            builder.setSsid(requiredNetworkSSID).setWpa2Passphrase(requiredNetworkPass)

            val wifiNetworkSpecifier = builder.build()

            val networkRequestBuilder1 = NetworkRequest.Builder()
            networkRequestBuilder1.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)

            val nr = networkRequestBuilder1.build()

            cm.requestNetwork(nr, networkCallback)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "onAvailable: ")
            cm.bindProcessToNetwork(network)
            changeUIAfterWifiConnection()
        }

        override fun onUnavailable() {
            super.onUnavailable()
            Log.d(TAG, "onUnavailable: ")
            wifiConnected.postValue(false)
//            removeNetworkCallback()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "onLost: ")
            wifiConnected.postValue(false)
//            removeNetworkCallback()
        }
    }


    private fun changeUIAfterWifiConnection() {
        wifiConnected.postValue(true)
    }

    fun removeNetworkCallback() {
        Log.d(TAG, "removeNetworkCallback: ${cm.boundNetworkForProcess}")
        try {
            cm.unregisterNetworkCallback(networkCallback)
            cm.bindProcessToNetwork(null)
            Log.d(TAG, "removeNetworkCallback: ${cm.boundNetworkForProcess}")
        } catch (e: Exception) {
            Log.d(TAG, "removeNetworkCallback: exception: ${e.message}")
        }
    }

    fun makePOSTRequestToDevice(
        path: String,
        title1: String,
        title2: String,
        value1: String,
        value2: String
    ) {
        val formBody = FormBody.Builder()
            .add(title1, value1)
            .add(title2, value2)
            .build()

        val request = Request.Builder()
            .url("http://192.168.4.1/$path")
            .post(formBody)
            .build()

        try {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "makePOSTRequestToDevice: onFailure ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, "makePOSTRequestToDevice: response: $response")

                    statusInfo.postValue("")

                    postRequestResponse.postValue("$path,${response.code}")
                }
            })

//            val response = okHttpClient.newCall(request).execute()
//            isSuccessful = response.isSuccessful
//            Log.d(TAG, "makeHttpRequestToDevice: response ${response.body.toString()}")
//            statusInfo.postValue("")
        } catch (e: Exception) {
            Log.d(TAG, "makeHttpRequestToDevice: exception ${e.message}")
            statusInfo.postValue("Cannot send data to Wifi network.")
        }
    }

    fun makeGetRequest(path: String) {
        Log.d(TAG, "makeGetRequest: path: $path")

        val request = Request.Builder()
            .url("http://192.168.4.1/$path")
            .build()

        try {

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "makeGetRequest: onFailure: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        for ((name, value) in response.headers) {
                            Log.d(TAG, "onResponse: headers: $name: $value")
                        }

                        val returnResponse = response.body?.string() ?: "No response"
                        statusInfo.postValue("")

                        Log.d(TAG, "makeGetRequest: response: $response")
                        Log.d(TAG, "makeGetRequest: response.body.string(): $returnResponse")

                        getRequestResponse.postValue("$path,$returnResponse")
                    }
                }
            })

        } catch (e: Exception) {
            Log.d(TAG, "makeGetRequest: $path Exception: ${e.message}")
            statusInfo.postValue("Cannot get data from Wifi network.")
        }
    }

}