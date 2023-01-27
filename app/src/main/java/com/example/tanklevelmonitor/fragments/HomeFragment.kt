package com.example.tanklevelmonitor.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentHomeBinding
import com.example.tanklevelmonitor.utils.Constants.PREFERENCES_FIRST_TIME
import com.example.tanklevelmonitor.utils.SharedPrefs
import com.example.tanklevelmonitor.utils.UserData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
    var permissionsGranted = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = SharedPrefs.getUserData(requireContext(), "userId")

        checkScreenSize()
        showProgressBar()
        firstTimeCheck()
        handleArguments()
        listenForLevelValue()
        checkForWifiAndLevelsDataToDisplay()
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
        val firstTime = SharedPrefs.getFirstTimeFlag(requireContext(), PREFERENCES_FIRST_TIME)
        if (firstTime) {
            checkPermissions()
        } else {
            if (userId == "") {
                checkPermissions()
            }
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            permissionsGranted = 0
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA
                )
            )
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
                askToScanFunction()
            }
        }


    private fun askToScanFunction() {
        Toast.makeText(
            requireContext(),
            "Please scan your device's QR Code",
            Toast.LENGTH_SHORT
        ).show()
        findNavController().navigate(R.id.action_homeFragment_to_QRCodeScannerFragment)
    }

    private fun handleNavigation() {
        binding.buttonScanQr.tvMenuText.text = "Scan QR"
        binding.buttonScanQr.ivMenuImg.setImageResource(R.drawable.img_qr_code)
        binding.buttonScanQr.root.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_QRCodeScannerFragment)
        }

        binding.buttonSettings.tvMenuText.text = "Settings"
        binding.buttonSettings.ivMenuImg.setImageResource(R.drawable.img_settings)
        binding.buttonSettings.root.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }
    }

    private fun handleArguments() {
        arguments?.getCharSequence("qrText")?.let {
            SharedPrefs.storeUserData(requireContext(), "userId", it.toString())
            userId = it.toString()
            SharedPrefs.storeFirstTimeFlag(requireContext(), PREFERENCES_FIRST_TIME, false)
        }
    }

    private fun checkForWifiAndLevelsDataToDisplay() {
        db.getReference("devices").child(userId).get().addOnSuccessListener {
            if (it.exists()) {
                Log.d(TAG, "checkForWifiAndLevelsDataToDisplay: ${it.value}")
                val userData = it.getValue<UserData>()

                val minLevel = userData?.min
                val maxLevel = userData?.max

                if (minLevel == null || maxLevel == null) {
                    Toast.makeText(
                        requireContext(),
                        "Please enter level values.",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_homeFragment_to_initialQuestionsFragment)
                }
            }
        }

    }

    private fun listenForLevelValue() {
        val userRef = db.getReference("devices").child(userId)
        val userRefListener = object : ValueEventListener {
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
                }

                val levelValue = userData?.level
                Log.d(TAG, "listenForLevelValue Value is: $levelValue")

                binding.levelPercentageText.text = "$levelValue%"
                binding.waterLevelMeter.chargeLevel = levelValue?.toInt()
                hideProgressBar()

                val pending = userData?.pending
                if (pending == true) {
                    binding.pendingSyncText.visibility = View.VISIBLE
                } else {
                    binding.pendingSyncText.visibility = View.GONE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "listenForLevelValue onCancelled", databaseError.toException())
            }
        }

        userRef.addValueEventListener(userRefListener)
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }
}