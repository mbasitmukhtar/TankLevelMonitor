package com.example.tanklevelmonitor.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentHomeBinding
import com.example.tanklevelmonitor.utils.Constants.PREFERENCES_FIRST_TIME
import com.example.tanklevelmonitor.utils.Constants.USERID
import com.example.tanklevelmonitor.utils.SharedPrefs
import com.example.tanklevelmonitor.utils.UserData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*


class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    lateinit var binding: FragmentHomeBinding
    var userId = ""
    val db = Firebase.database
    lateinit var userReference: DatabaseReference
    val calendar = Calendar.getInstance()

    var hasDeviceInternet: MutableLiveData<Boolean> = MutableLiveData(true)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = SharedPrefs.getUserData(requireContext(), "userId")

        checkScreenSize()
        firstTimeCheck()
        handleNavigation()

        hasDeviceInternet.observe(viewLifecycleOwner) {
            if (!it) {
                binding.connectionModeText.text = "Device has no internet connection."
//                showAlertForDirectConnection()
            }
        }
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
        val firstTime = SharedPrefs.getFirstTimeFlag(requireContext(), PREFERENCES_FIRST_TIME)
        if (firstTime) {
            findNavController().navigate(R.id.action_homeFragment_to_initFragment)
        } else {
            handleArguments()
        }
    }

    private fun handleNavigation() {
        binding.buttonScanQr.tvMenuText.text = getString(R.string.scan_qr)
        binding.buttonScanQr.ivMenuImg.setImageResource(R.drawable.img_qr_code)
        binding.buttonScanQr.root.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_QRCodeScannerFragment)
        }

        binding.buttonSettings.tvMenuText.text = getString(R.string.settings)
        binding.buttonSettings.ivMenuImg.setImageResource(R.drawable.img_settings)
        binding.buttonSettings.root.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }
    }

    private fun handleArguments() {
        val mac = SharedPrefs.getUserData(requireContext(), USERID)
        if (mac == "") {
            Toast.makeText(
                requireContext(), "Please connect to device using Wifi.", Toast.LENGTH_LONG
            ).show()
            binding.connectionModeText.text = "Please connect to device using Wifi/QR."

        } else {
            userId = mac

            showProgressBar()
            listenForLevelValue()
            checkForWifiAndLevelsDataToDisplay()
        }
//        arguments?.getCharSequence("qrText")?.let {
//            SharedPrefs.storeUserData(requireContext(), "userId", it.toString())
//            userId = it.toString()
//            SharedPrefs.storeFirstTimeFlag(requireContext(), PREFERENCES_FIRST_TIME, false)
//        }
    }

    private fun checkForWifiAndLevelsDataToDisplay() {
        if (userId == "")
            return

        db.getReference("devices").child(userId).get().addOnSuccessListener {
            if (it.exists()) {
                Log.d(TAG, "checkForWifiAndLevelsDataToDisplay: ${it.value}")
                val userData = it.getValue<UserData>()

                val minLevel = userData?.min
                val maxLevel = userData?.max

                if (minLevel == null || maxLevel == null) {
                    Toast.makeText(
                        requireContext(),
                        "Please enter max and min level values.",
                        Toast.LENGTH_LONG
                    ).show()

//                        TODO (handle case if min, max values null)
//                    findNavController().navigate(R.id.action_homeFragment_to_initialQuestionsFragment)
                }
            }
        }

    }

    private fun listenForLevelValue() {
        if (userId == "")
            return

        userReference = db.getReference("devices").child(userId)
        userReference.addValueEventListener(userRefListener)
    }

    private val userRefListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // Get User object and use the values to update the UI
            val userData = dataSnapshot.getValue<UserData>()

            val timestamp = userData?.time
            Log.d(TAG, "listenForLevelValue Value is: $timestamp")
            timestamp?.let {
                val dateEpochs =
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date(timestamp * 1000))
                Log.d(TAG, "checkForWifiAndLevelsDataToDisplay: dateEpochs: $dateEpochs")
                binding.timestampText.text = "Timestamp: $dateEpochs"

                compareDateTime(dateEpochs)
            }

            val levelValue = userData?.level
            Log.d(TAG, "listenForLevelValue Value is: $levelValue")

            binding.levelPercentageText.text = "$levelValue%"
            binding.waterLevelMeter.chargeLevel = levelValue?.toInt()
            hideProgressBar()

            val pending = userData?.pending
            if (pending == true) {
                binding.pendingSyncText.text = "Sync Pending by device."
                binding.pendingSyncText.visibility = View.VISIBLE
            } else {
                binding.pendingSyncText.visibility = View.GONE
            }

            val ip = userData?.ip
            if (ip == "" || ip == null) {
                hasDeviceInternet.postValue(false)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "listenForLevelValue onCancelled", databaseError.toException())
        }
    }

    private fun compareDateTime(dateFromFirebase: String) {
        val localTime = calendar.time
//        comparing dates
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val localdate = df.format(localTime)
        val firebaseDate = df.format(dateFromFirebase)

        if (localdate.compareTo(firebaseDate) > 0) {
            Log.d(TAG, "todayDate is after previousDate")
            hasDeviceInternet.postValue(false)
        } else if (localdate.compareTo(firebaseDate) < 0) {
            Log.d(TAG, "todayDate is before previousDate")
        } else if (localdate.compareTo(firebaseDate) == 0) {
            Log.d(TAG, "todayDate is equal to previousDate")
            hasDeviceInternet.postValue(true)
        }

    }

    private fun showAlertForDirectConnection() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Message").setNegativeButton(
            "No"
        ) { dialog, id ->
            run {
                dialog.cancel()
            }
        }.setPositiveButton(
            "Yes"
        ) { dialog, id ->
            run {
                connectToDeviceHotspot()
            }
        }
    }

    private fun connectToDeviceHotspot() {
        Log.d(TAG, "connectToDeviceHotspot: ")
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        try {
            userReference.removeEventListener(userRefListener)
        } catch (e: Exception) {
            Log.d(TAG, "onStop: Exception: ${e.message}")
        }
    }
}