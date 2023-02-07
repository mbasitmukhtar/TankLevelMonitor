package com.example.tanklevelmonitor.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentInitBinding

class InitFragment : Fragment() {
    private val TAG = "InitFragment"
    lateinit var binding: FragmentInitBinding
    var permissionsGranted = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentInitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpPermissionUI()
        checkForAllPermissions()
    }

    private fun checkForAllPermissions() {
        val permissions: Array<Any>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
            )
        } else {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
            )
        }

        var permissionCheck = 0
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionCheck += 1
            }
        }

        if (permissionCheck == 0) {
            initQRScanUI()
        }
    }

    private fun setUpPermissionUI() {
        binding.continueButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_DENIED
            ) {
                askForPermissions()
            }
        }
    }

    private fun askForPermissions() {
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
                    Manifest.permission.CAMERA,
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
                    Manifest.permission.CAMERA,
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
                initQRScanUI()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please grant all permissions.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun initQRScanUI() {
        binding.iconImage.setImageResource(R.drawable.img_qr_code)
        binding.infoText.text = "Please Scan the QR Code from your device."
        binding.continueButton.setOnClickListener {
            findNavController().navigate(R.id.action_initFragment_to_QRCodeScannerFragment)
        }
    }

}