package com.example.tanklevelmonitor.fragments

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tanklevelmonitor.MainActivity
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentHomeBinding
import com.example.tanklevelmonitor.models.LevelResponse
import com.example.tanklevelmonitor.others.ConnectionLiveData
import com.example.tanklevelmonitor.receivers.SnoozeMessageReceiver
import com.example.tanklevelmonitor.utils.Constants.ACTION_SNOOZE
import com.example.tanklevelmonitor.utils.Constants.APPASS
import com.example.tanklevelmonitor.utils.Constants.APSSID
import com.example.tanklevelmonitor.utils.Constants.EXTRA_NOTIFICATION_ID
import com.example.tanklevelmonitor.utils.Constants.LEVEL_VALUE_CHANEL
import com.example.tanklevelmonitor.utils.Constants.PREFERENCES_FIRST_TIME
import com.example.tanklevelmonitor.utils.Constants.USERID
import com.example.tanklevelmonitor.utils.HelperClass
import com.example.tanklevelmonitor.utils.SharedPrefs
import com.example.tanklevelmonitor.utils.UserData
import com.example.tanklevelmonitor.utils.WifiConnectivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"

    companion object {
        var snoozeMessageLiveData = MutableLiveData(false)
    }

    val CHANNEL_ID = LEVEL_VALUE_CHANEL

    lateinit var binding: FragmentHomeBinding
    var userId = ""
    val db = Firebase.database
    lateinit var userReference: DatabaseReference

    var receivingUpdatesFromDeviceBoolean: MutableLiveData<Boolean> = MutableLiveData(true)
    lateinit var connectionLiveData: ConnectionLiveData

    var lastUpdatedInFirebase = ""
    private var connectedToHotspot = false
    var keepCheckingDateDiff = true
    var dataConnectionAvailable = false
    var isAlertDisplayedRecently = false
    var firstDatabaseRead = true

    var notificationAllowedByUser = true
    var notificationId = 101010

    lateinit var wifiConnectivity: WifiConnectivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkScreenSize()
        firstTimeCheck()
        handleNavigation()
    }

    private fun checkScreenSize() {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        if (height < 321) {
            binding.titleText.visibility = View.GONE
        }
        Log.d(TAG, "checkScreenSize: $width * $height")
    }

    private fun firstTimeCheck() {
        Log.d(TAG, "firstTimeCheck: ")
        SharedPrefs.storeFirstTimeFlag(requireContext(), PREFERENCES_FIRST_TIME, false)
        val firstTime = SharedPrefs.getFirstTimeFlag(requireContext(), PREFERENCES_FIRST_TIME)
        if (firstTime) {
            findNavController().navigate(R.id.action_homeFragment_to_initFragment)
        } else {
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions: ")
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initHomeScreen()
        } else {
            findNavController().navigate(R.id.action_homeFragment_to_initFragment)
        }
    }

    private fun handleNavigation() {
        binding.buttonScanQr.tvMenuText.text = getString(R.string.scan_qr)
        binding.buttonScanQr.ivMenuImg.setImageResource(R.drawable.img_qr_code)
        binding.buttonScanQr.root.setOnClickListener {
//            findNavController().navigate(R.id.action_homeFragment_to_QRCodeScannerFragment)
            findNavController().navigate(R.id.action_homeFragment_to_QReaderFragment)
        }

        binding.buttonSettings.tvMenuText.text = getString(R.string.settings)
        binding.buttonSettings.ivMenuImg.setImageResource(R.drawable.img_settings)
        binding.buttonSettings.root.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }
    }

    private fun initHomeScreen() {
        Log.d(TAG, "handleArguments: ")

//        TODO
//         check if any observer device is added then continue
//         if not then clear UI and show a button to add devices

//        above here in case when local db changes

        var mac = SharedPrefs.getUserData(requireContext(), USERID)
        mac = "abcd"
        Log.d(TAG, "handleArguments: mac: $mac")


        if (mac == "") {
            setUpNoDeviceCase()
        } else {
            setUpDeviceAddedCase(mac)
        }
    }

    private fun setUpNoDeviceCase() {
//            Toast.makeText(
//                requireContext(), "Please connect to device using Wifi.", Toast.LENGTH_LONG
//            ).show()
//            binding.connectionModeText.text = "Please connect to device using Wifi/QR."
        binding.noDevicesLayout.visibility = View.VISIBLE
        binding.bodyLayout.visibility = View.GONE

        binding.addDeviceButton.setOnClickListener {
            MainActivity.switchBottomTabsLiveData.postValue(1)
//            findNavController().navigate(R.id.action_homeFragment_to_addDeviceFragment)
        }
    }

    private fun setUpDeviceAddedCase(mac: String) {
        receivingUpdatesFromDeviceBoolean.observe(viewLifecycleOwner) {
            if (!it) {
                Log.d(TAG, "receivingUpdatesFromDeviceBoolean: false")
                binding.connectionModeText.text =
                    getString(R.string.no_updates_from_online_database)
                if (!isAlertDisplayedRecently) {
                    showAlertForDirectConnection()
                }
            }
        }

        snoozeMessageLiveData.observe(viewLifecycleOwner) {
            if (it) {
                notificationAllowedByUser = false
                val nMgr =
                    requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nMgr.cancel(notificationId)
                snoozeMessageLiveData.postValue(false)
            }
        }

        userId = mac
        wifiConnectivity = WifiConnectivity(requireContext())

        initNetworkStatus()
        createNotificationChannel()
        showProgressBar()
//            listenForLevelValue()
        listenForLevelValueFromSql()
        checkForWifiAndLevelsDataToDisplay()

        scheduleUpdateDifferenceCheck()
    }

    private fun initNetworkStatus() {
        Log.d(TAG, "initNetworkStatus: ")

        Executors.newSingleThreadScheduledExecutor().schedule({
            if (!HelperClass.hasInternetConnection(requireContext())) {
                Log.d(TAG, "initNetworkStatus: no active network")
                binding.connectionModeText.text = getString(R.string.no_active_network)

                Log.d(TAG, "initNetworkStatus: connectedToHotspot: $connectedToHotspot")
                if (!connectedToHotspot) {
                    Log.d(TAG, "initNetworkStatus: sending false")

                    receivingUpdatesFromDeviceBoolean.postValue(false)
                }

                hideProgressBar()
            }
        }, 5, TimeUnit.SECONDS)

        connectionLiveData = ConnectionLiveData(requireContext())
        connectionLiveData.observe(viewLifecycleOwner)
        { isNetworkAvailable ->
            Log.d(TAG, "initNetworkStatus: isNetworkAvailable: $isNetworkAvailable")
            if (isNetworkAvailable) {

                dataConnectionAvailable = true
                binding.connectionModeText.text = getString(R.string.internet_connected)

                binding.connectToDeviceLayout.visibility = View.GONE
            } else {
                dataConnectionAvailable = false
                binding.connectionModeText.text = getString(R.string.no_internet_connection)
                if (!connectedToHotspot) {
                    receivingUpdatesFromDeviceBoolean.postValue(false)
                } else {
                    Log.d(TAG, "initNetworkStatus: connectedToHotspot: $connectedToHotspot")
                }
            }
        }
    }

    private fun checkForWifiAndLevelsDataToDisplay() {
        Log.d(TAG, "checkForWifiAndLevelsDataToDisplay: ")
        if (userId == "") {
            Log.d(TAG, "checkForWifiAndLevelsDataToDisplay: userid null")
            return
        }

        db.getReference("devices").child(userId).get().addOnSuccessListener {
            if (it.exists()) {
                Log.d(TAG, "checkForWifiAndLevelsDataToDisplay: ${it.value}")
                val userData = it.getValue<UserData>()

                val minLevel = userData?.min
                val maxLevel = userData?.max

                if (minLevel == null || maxLevel == null) {
                    Toast.makeText(
                        requireContext(),
                        "Please enter max and min level values using settings.",
                        Toast.LENGTH_LONG
                    ).show()

//                        TODO (handle case if min, max values null)
//                    findNavController().navigate(R.id.action_homeFragment_to_initialQuestionsFragment)
                }
            }
        }

    }

    private fun listenForLevelValueFromSql() {
        val path = "level"
        Log.d(TAG, "listenForLevelValueFromSql: ")
        lifecycleScope.launch {
            val request = Request.Builder()
                .url("https://a314e16a-526a-4bea-b200-a5ef020e32f0.mock.pstmn.io/$path")
                .build()

            val okHttpClient = OkHttpClient()
            val gson = Gson()

            while (true) {

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
//                            statusInfo.postValue("")

                                Log.d(TAG, "makeGetRequest: response: $response")
                                Log.d(
                                    TAG,
                                    "makeGetRequest: response.body.string(): $returnResponse"
                                )
                                val json = gson.fromJson(returnResponse, LevelResponse::class.java)

                                binding.levelPercentageText.text = "${json.level}%"
                                binding.waterLevelMeter.chargeLevel = json.level.toInt()

                            }
                        }
                    })

                } catch (e: Exception) {
                    Log.d(TAG, "makeGetRequest: $path Exception: ${e.message}")
//                statusInfo.postValue("Cannot get data from Wifi network.")
                }
                delay(5000)
            }

        }
    }

    private fun listenForLevelValue() {
        Log.d(TAG, "listenForLevelValue: ")
        if (userId == "") {
            Log.d(TAG, "listenForLevelValue: userid null")
            return
        }

        firstDatabaseRead = true

        userReference = db.getReference("devices").child(userId)
        userReference.addValueEventListener(userRefListener)
    }

    private val userRefListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // Get User object and use the values to update the UI
            hideProgressBar()

            val userData = dataSnapshot.getValue<UserData>()

            val timestamp = userData?.time
            Log.d(TAG, "listenForLevelValue Value is: $timestamp")
            timestamp?.let {
                val dateEpochs =
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date(timestamp * 1000))
                Log.d(TAG, "checkForWifiAndLevelsDataToDisplay: dateEpochs: $dateEpochs")
                binding.timestampText.text =
                    getString(R.string.timestamp) + getString(R.string.space) + dateEpochs

                keepCheckingDateDiff = true
                lastUpdatedInFirebase = dateEpochs
            }

            val levelValue = userData?.level
            if (levelValue == null) {
                receivingUpdatesFromDeviceBoolean.postValue(false)
            }
            Log.d(TAG, "listenForLevelValue Value is: $levelValue")

            binding.levelPercentageText.text = "$levelValue%"
            binding.waterLevelMeter.chargeLevel = levelValue?.toInt()

            checkAndDisplayNotification(levelValue)

            val pending = userData?.pending
            if (pending == true) {
                binding.pendingSyncText.text = getString(R.string.sync_pending)
                binding.pendingSyncText.visibility = View.VISIBLE
            } else {
                binding.pendingSyncText.visibility = View.GONE
            }

            if (firstDatabaseRead) {
                firstDatabaseRead = false
            } else {
                binding.connectionModeText.text = getString(R.string.connected_to_database)
            }

            val ip = userData?.ip
//            if (ip == "" || ip == null) {
//                hasDeviceInternet.postValue(false)
//            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "listenForLevelValue onCancelled", databaseError.toException())
        }
    }

    private fun checkAndDisplayNotification(level: Long?) {
        if (level != null) {
            if (level < 20) {
                showNotification(level.toString())
            } else if (level > 90) {
                showNotification(level.toString())
            }
            if (level.toInt() < 90) {
                notificationAllowedByUser = true
            }
        }
    }

    private fun scheduleUpdateDifferenceCheck() {
        Log.d(TAG, "scheduleUpdateDifferenceCheck: ")
        lifecycleScope.launch {
            while (!connectedToHotspot and keepCheckingDateDiff) {
                Log.d(
                    TAG,
                    "scheduleUpdateDifferenceCheck: in loop $connectedToHotspot, $keepCheckingDateDiff"
                )
                if (lastUpdatedInFirebase != "") {
                    compareDateTime(lastUpdatedInFirebase)
                }
                delay(10000)
            }
            Log.d(
                TAG,
                "scheduleUpdateDifferenceCheck: out of loop $connectedToHotspot, $keepCheckingDateDiff"
            )
        }
    }

    private fun compareDateTime(dateFromFirebase: String) {
        val calendar = Calendar.getInstance()
        val localTime = calendar.time
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val localDate = df.format(localTime)

        Log.d(TAG, "compareDateTime: localDate: $localDate")
        Log.d(TAG, "compareDateTime: dateFromFirebase: $dateFromFirebase")

        var localDateSplit = localDate.split(" ")
        val localTimeSplit = localDateSplit[1].split(":")
        localDateSplit = localDateSplit[0].split("/")

        var firebaseDateSplit = dateFromFirebase.split(" ")
        val firebaseTimeSplit = firebaseDateSplit[1].split(":")
        firebaseDateSplit = dateFromFirebase.split("/")

        Log.d(TAG, "compareDateTime: localDate: $localTimeSplit")
        Log.d(TAG, "compareDateTime: dateFromFirebase: $firebaseTimeSplit")
//        comparing dates
        if (localDateSplit[0] > firebaseDateSplit[0]) {
            Log.d(TAG, "localDate is after dateFromFirebase")
            receivingUpdatesFromDeviceBoolean.postValue(false)

        } else if (localDateSplit[0] < firebaseDateSplit[0]) {
            Log.d(TAG, "localDate is before dateFromFirebase")

        } else if (localDateSplit[0] == (firebaseDateSplit[0])) {
            Log.d(TAG, "localDate is equal to dateFromFirebase")

            if (firebaseTimeSplit[0] == localTimeSplit[0]) {
                if (firebaseTimeSplit[1] == localTimeSplit[1]) {
                    val diff = localTimeSplit[2].toInt() - firebaseTimeSplit[2].toInt()
                    Log.d(TAG, "compareDateTime: diff: $diff")
                    if (diff > 10) {
                        Log.d(TAG, "compareDateTime: diff greater than 10")
                        receivingUpdatesFromDeviceBoolean.postValue(true)
                    }
                } else {
                    Log.d(TAG, "compareDateTime: minute not same")
                    receivingUpdatesFromDeviceBoolean.postValue(false)
                }
            } else {
                Log.d(TAG, "compareDateTime: hour not same")
                receivingUpdatesFromDeviceBoolean.postValue(false)
            }
        }

    }

    private fun showAlertForDirectConnection() {
        Log.d(TAG, "showAlertForDirectConnection: ")
        keepCheckingDateDiff = false

        binding.connectToDeviceLayout.visibility = View.VISIBLE
        binding.connectToDeviceButton.setOnClickListener {
            connectToDeviceHotspot()
        }

        isAlertDisplayedRecently = true
        Executors.newSingleThreadScheduledExecutor().schedule({
            isAlertDisplayedRecently = false
        }, 1, TimeUnit.MINUTES)


//        val builder = AlertDialog.Builder(requireContext())
//        builder
//            .setTitle("Message")
//            .setMessage("Do you want to connect to smart device directly?")
//            .setCancelable(false)
//            .setNegativeButton(
//                "No"
//            ) { dialog, id ->
//                run {
//                    dialog.cancel()
//                }
//            }.setPositiveButton(
//                "Yes"
//            ) { dialog, id ->
//                run {
//                    connectToDeviceHotspot()
//                }
//            }
//
//        val dialog: AlertDialog = builder.create()
//        dialog.show()
    }

    private fun connectToDeviceHotspot() {
        Log.d(TAG, "connectToDeviceHotspot: ")
        showProgressBar()

        val requiredNetworkSSID = SharedPrefs.getUserData(requireContext(), APSSID)
        val requiredNetworkPass = SharedPrefs.getUserData(requireContext(), APPASS)

        wifiConnectivity.requiredNetworkSSID = requiredNetworkSSID
        wifiConnectivity.requiredNetworkPass = requiredNetworkPass

        wifiConnectivity.checkIfLocationEnabled()

        wifiConnectivity.wifiConnected.observe(viewLifecycleOwner) {
            if (it) {
                connectedToHotspot = true
                binding.connectToDeviceLayout.visibility = View.GONE
                Log.d(TAG, "connectToDeviceHotspot: $connectedToHotspot")
                connectionLiveData.removeObservers(viewLifecycleOwner)
                getLevelsFromDevice()
                hideProgressBar()
            } else {
                connectedToHotspot = false
                Log.d(TAG, "connectToDeviceHotspot: $connectedToHotspot")
            }
        }

        wifiConnectivity.getRequestResponse.observe(viewLifecycleOwner) {
            if (it != "") {
                val items = it.split(",")
                if (items[0] == "level") {
                    val level = items[1]

                    binding.connectionModeText.text =
                        getString(R.string.getting_updates_from_device)
                    binding.timestampText.text = ""
                    binding.levelPercentageText.text = "$level%"
                    binding.waterLevelMeter.chargeLevel = level.toInt()
                    checkAndDisplayNotification(level.toLong())
                }
            }
        }
    }

    private fun getLevelsFromDevice() {
        lifecycleScope.launch {
            while (connectedToHotspot) {
                wifiConnectivity.makeGetRequest("level")
                delay(5000)
            }
            Log.d(TAG, "getLevelsFromDevice: stopping get calls")
            wifiConnectivity.removeNetworkCallback()

            keepCheckingDateDiff = true
            initNetworkStatus()
//            listenForLevelValue()
            listenForLevelValueFromSql()
            scheduleUpdateDifferenceCheck()
            binding.connectionModeText.text = getString(R.string.waiting_for_connection)
        }
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel: ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "LEVEL NOTIFICATION NAME",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = requireActivity().getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun showNotification(level: String) {
        if (notificationAllowedByUser) {
            val notificationIntent = Intent(requireContext(), MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                requireContext(),
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(requireContext(), SnoozeMessageReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }

            val snoozePendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    requireContext(),
                    0,
                    snoozeIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification: Notification =
                NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setContentTitle("Notification")
                    .setContentText("Level: $level")
                    .setSmallIcon(R.mipmap.water_level_icon_foreground)
                    .setContentIntent(pendingIntent)
                    .setSound(alarmSound)
                    .setAutoCancel(true)
                    .addAction(R.mipmap.water_level_icon_foreground, "Snooze", snoozePendingIntent)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            val notificationManager = NotificationManagerCompat.from(requireContext())

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notificationManager.notify(notificationId, notification)
        }
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
            Log.d(TAG, "onDestroy: trying to remove userRefListener")
            userReference.removeEventListener(userRefListener)
        } catch (e: Exception) {
            Log.d(TAG, "onStop: Exception: ${e.message}")
        }
    }
}

