package com.example.tanklevelmonitor.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentConfigureDeviceBinding
import com.example.tanklevelmonitor.utils.Constants
import com.example.tanklevelmonitor.utils.SharedPrefs
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class ConfigureDeviceFragment : Fragment() {
    private val TAG = "ConfigureDeviceFragment"
    lateinit var binding: FragmentConfigureDeviceBinding

    private lateinit var wifiManager: WifiManager

    lateinit var statusTextView: TextView

    val okHttpClient = OkHttpClient()

    var permissionsGranted = 0

    var statusString: String = ""
    var requiredNetworkSSID = ""
    var requiredNetworkPass = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentConfigureDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusTextView = binding.statusTextView
        statusTextView.movementMethod = ScrollingMovementMethod()
        statusTextView.append(statusString)

        arguments?.getString("qrText").let {
            if (it != null) {
                decodeWifiQRText(it)
            }
        }

        checkIfLocationEnabled()
        binding.scanButton.setOnClickListener {
            checkIfLocationEnabled()
        }
    }

    private fun decodeWifiQRText(qrText: String) {
        val items = qrText.split(":", ";")

        var index = 0
        for (item in items) {
            if (item == "S") {
                requiredNetworkSSID = items[index + 1]
            }
            index += 1
        }

        index = 0
        for (item in items) {
            if (item == "P") {
                requiredNetworkPass = items[index + 1]
            }
            index += 1
        }

        Log.d(TAG, "onViewCreated: $requiredNetworkSSID, $requiredNetworkPass")
    }

    private fun checkIfLocationEnabled() {
        val locationStatus: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is a new method provided in API 28
            val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isLocationEnabled = lm.isLocationEnabled
            locationStatus = isLocationEnabled
        } else {
            // This was deprecated in API 28
            val mode = Settings.Secure.getInt(
                requireContext().contentResolver, Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            val enabled = mode != Settings.Secure.LOCATION_MODE_OFF
            locationStatus = enabled
        }

        if (locationStatus) {
//            statusString = "Location Enabled\n"
//            statusTextView.append(statusString)
            initWifi()
        }
        if (!locationStatus) {
            Toast.makeText(requireContext(), "Please Enable Location.", Toast.LENGTH_SHORT).show()
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(myIntent)
        }
    }

    private fun initWifi() {
        Log.d(TAG, "initWifi: ")
        wifiManager =
            (requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager)

        addWifiStateReceiver()
    }

    private fun addWifiStateReceiver() {
        Log.d(TAG, "addWifiStateReceiver: ")
        val intentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        requireContext().registerReceiver(wifiStateReceiver, intentFilter)
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
                    statusString = "Wifi is ON\n"
                    statusTextView.append(statusString)
                }

                WifiManager.WIFI_STATE_DISABLED -> {
                    statusString = "Wifi is OFF\n"
                    statusTextView.append(statusString)
                    Toast.makeText(requireContext(), "Please Enable Wifi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun askAndStartScanWifi() {
        Log.d(TAG, "askAndStartScanWifi: ")
        val permission1 =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        // Check for permissions
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting Permissions")

            // Request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.NEARBY_WIFI_DEVICES,
                    )
                )
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE,
                        Manifest.permission.INTERNET,
                    )
                )
            }
            return
        } else {
            startWifiScan()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { result ->
            Log.d(TAG, "requestPermissionLauncher: ${result.keys}")
            Log.d(TAG, "requestPermissionLauncher: ${result.values}")
            for (value in result.values) {
                if (value) {
                    permissionsGranted++
                }
            }
            if (permissionsGranted == result.values.size) {
//                Successful
                startWifiScan()
            }
        }

    private fun startWifiScan() {
        if (wifiManager.isWifiEnabled) {
            Log.d(TAG, "Permissions Already Granted")
            statusString = "Permissions Already Granted\n"
            statusTextView.append(statusString)

            statusString = "Starting Wifi Scan\n"
            statusTextView.append(statusString)

            val scanStarted = wifiManager.startScan()

            if (scanStarted) {
//                stringLong = "Scanning true\n"
//                statusTextView.append(stringLong)

                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                } else {
                    Log.d(TAG, "doStartScanWifi: scanResults: ${wifiManager.scanResults}")
                }

                // Register the receiver
                requireContext().registerReceiver(
                    wifiScanResultReceiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )
            } else {
//                stringLong = "Scanning false\n"
//                statusTextView.append(stringLong)
                Toast.makeText(requireContext(), "Wifi Scanning Failed.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Please Enable Wifi.", Toast.LENGTH_LONG).show()
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
                        requireActivity(),
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
            requireContext().unregisterReceiver(wifiScanResultReceiver)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "connectToRequiredNetwork: Connecting to: $requiredNetworkSSID")
                statusString = "Connecting to: $requiredNetworkSSID\n"
                statusTextView.append(statusString)

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
//                    stringLong = "Failed in adding network suggestion $status\n"
//                    statusTextView.append(stringLong)
                } else {
                    Log.d(TAG, "connectToRequiredNetwork: addNetworkSuggestions status $status")
//                    stringLong = "addNetworkSuggestions status $status\n"
//                    statusTextView.append(stringLong)
                }

                // Optional (Wait for post connection broadcast to one of your suggestions)
                val intentFilter =
                    IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)

                val suggestionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action != WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION) {
                            return
                        }
                        Log.d(
                            TAG,
                            "connectToRequiredNetwork: Network suggestion added successfully"
                        )
//                        stringLong = "Network suggestion added successfully\n"
//                        statusTextView.append(stringLong)
                    }
                }

                requireContext().registerReceiver(suggestionReceiver, intentFilter)
                makeNetworkRequest(requiredNetworkSSID, requiredNetworkPass)

            } else {
                val wifiConfig = WifiConfiguration()
                wifiConfig.SSID = "\"" + requiredNetworkSSID + "\""

                if (networkCapabilities.uppercase().contains("WEP")) { // WEP Network.
                    statusString = "WEP Network\n"
                    statusTextView.append(statusString)

                    wifiConfig.wepKeys[0] = "\"" + requiredNetworkPass + "\"";
                    wifiConfig.wepTxKeyIndex = 0;
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                } else if (networkCapabilities.uppercase().contains("WPA")) { // WPA Network
                    statusString = "WPA Network\n"
                    statusTextView.append(statusString)
                    wifiConfig.preSharedKey = "\"" + requiredNetworkPass + "\"";
                } else { // OPEN Network.
                    statusString = "OPEN Network\n"
                    statusTextView.append(statusString)
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }

                wifiManager.addNetwork(wifiConfig)

                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
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
            statusString = "Not Found: $requiredNetworkSSID\n"
            statusTextView.append(statusString)
        }
    }

    private fun makeNetworkRequest(requiredNetworkSSID: String, requiredNetworkPass: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val builder = WifiNetworkSpecifier.Builder()
            builder.setSsid(requiredNetworkSSID)
            builder.setWpa2Passphrase(requiredNetworkPass)
            val wifiNetworkSpecifier = builder.build()

            val networkRequestBuilder1 = NetworkRequest.Builder()
            networkRequestBuilder1.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            networkRequestBuilder1.setNetworkSpecifier(wifiNetworkSpecifier)

            val nr = networkRequestBuilder1.build()
            val cm: ConnectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    cm.bindProcessToNetwork(network)
                    changeUIAfterWifiConnection()
                    //            cm.unregisterNetworkCallback(networkCallback)
                }
            }

            cm.requestNetwork(nr, networkCallback)
        }
    }

    private fun changeUIAfterWifiConnection() {
        requireActivity().runOnUiThread {
            binding.routerSettingsCard.visibility = View.VISIBLE
            binding.hotspotSettingsCard.visibility = View.VISIBLE
            binding.levelSettingsCard.visibility = View.VISIBLE
            binding.scanInfoLayout.visibility = View.GONE

            val mac = makeGetRequest("mac")
            saveMacAndInit(mac)

            binding.submitLevelButton.setOnClickListener {
                val min = binding.minLevelInput.text.toString()
                val max = binding.maxLevelInput.text.toString()

                makePOSTRequestToDevice("level-sett", "min", "max", min, max)
            }

            binding.submitRouterButton.setOnClickListener {
                val ssid = binding.ssidInput.text.toString()
                val pass = binding.passwordInput.text.toString()

                makePOSTRequestToDevice("sta-sett", "ssid", "pass", ssid, pass)
                Executors.newSingleThreadScheduledExecutor().schedule({
                    getIpFromDevice()
                }, 20, TimeUnit.SECONDS)
            }

            binding.submitHotspotButton.setOnClickListener {
                val apSsid = binding.ssidHotspotInput.text.toString()
                val apPass = binding.passwordHotspotInput.text.toString()

                makePOSTRequestToDevice("ap-sett", "ap_ssid", "ap_pass", apSsid, apPass)
            }

            binding.skipButton.setOnClickListener {
                findNavController().navigate(R.id.action_configureDeviceFragment_to_homeFragment)
            }
        }
    }

    private fun makePOSTRequestToDevice(
        path: String,
        title1: String,
        title2: String,
        value1: String,
        value2: String
    ): Boolean {
        showProgressBar()
        var isSuccessful = false

        val formBody = FormBody.Builder()
            .add(title1, value1)
            .add(title2, value2)
            .build()

        val request = Request.Builder()
            .url("http://192.168.4.1/$path")
            .post(formBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            isSuccessful = response.isSuccessful
            Log.d(TAG, "makeHttpRequestToDevice: response ${response.body.toString()}")
        } catch (e: Exception) {
            Log.d(TAG, "makeHttpRequestToDevice: exception ${e.message}")
        }

        hideProgressBar()
        return isSuccessful
    }

    private fun saveMacAndInit(mac: String) {
        SharedPrefs.storeFirstTimeFlag(
            requireContext(),
            Constants.PREFERENCES_FIRST_TIME, false
        )
        SharedPrefs.storeUserData(
            requireContext(),
            Constants.USERID, mac
        )
    }

    private fun getIpFromDevice() {
        val ipRequestResponse = makeGetRequest("ip")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Message")
            .setPositiveButton("Ok") { dialog, id -> dialog.dismiss() }

        if (ipRequestResponse == "") {
            val message = "Device has no internet connection, switching to direct connection."
            builder.setMessage(message)
        } else {
            val message =
                "Device has internet connection, you may disconnect from device's hotspot."
            builder.setMessage(message)
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun makeGetRequest(path: String): String {
        val request = Request.Builder()
            .url("http://192.168.4.1/$path")
            .build()

        var returnResponse = ""
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            for ((name, value) in response.headers) {
                Log.d(TAG, "makeGetRequest: $name: $value")
            }

            returnResponse = response.body.toString()
        }
        return returnResponse
    }

    override fun onResume() {
        super.onResume()
//        checkIfLocationEnabled()
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(wifiScanResultReceiver)
//            cm.unregisterNetworkCallback(networkCallback);
        } catch (e: Exception) {
            Log.d(TAG, "onDestroy: exception: ${e.message}")
        }
    }

}