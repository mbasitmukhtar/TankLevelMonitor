package com.example.tanklevelmonitor.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.budiyev.android.codescanner.*
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentQRCodeScannerBinding
import com.google.zxing.BarcodeFormat

class QRCodeScannerFragment : Fragment() {
    private val TAG = "QRCodeScannerFragment"
    lateinit var binding: FragmentQRCodeScannerBinding
    private lateinit var codeScanner: CodeScanner
    var codeScannerInit = false
    private val CAMERA_REQUEST_CODE = 10001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentQRCodeScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpPermissions()
    }

    private fun setUpScanner() {
        val scannerView = binding.scannerView

        codeScanner = CodeScanner(requireContext(), scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = listOf(BarcodeFormat.QR_CODE)
//            CodeScanner.ALL_FORMATS // list of type BarcodeFormat,

        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.CONTINUOUS // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            requireActivity().runOnUiThread {
//                SharedPrefs.storeUserData(requireContext(), "userId", it.toString())
                Log.d(TAG, "setUpScanner: ${it.text}")
                val bundle = bundleOf("qrText" to it.text)
                findNavController().navigate(
                    R.id.action_QRCodeScannerFragment_to_configureDeviceFragment,
                    bundle
                )
            }
        }

        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(), "Camera initialization error: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
        codeScannerInit = true
    }

    private fun setUpPermissions() {
        val permission =
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makePermissionRequest()
        } else {
            setUpScanner()
        }
    }

    private fun makePermissionRequest() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Camera Permission Needed", Toast.LENGTH_SHORT)
                        .show()
                } else {
//                    Successful
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (codeScannerInit) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        if (codeScannerInit) {
            codeScanner.releaseResources()
        }
        super.onPause()
    }
}