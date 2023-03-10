package com.example.tanklevelmonitor.fragments

import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dlazaro66.qrcodereaderview.QRCodeReaderView
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentQReaderBinding
import com.example.tanklevelmonitor.utils.Constants.DEVICE_TYPE
import com.example.tanklevelmonitor.utils.Constants.QR_TEXT
import com.example.tanklevelmonitor.utils.Constants.SMART_SWITCH
import com.example.tanklevelmonitor.utils.Constants.TANK_MONITOR


class QReaderFragment : Fragment(), QRCodeReaderView.OnQRCodeReadListener {
    private val TAG = "QReaderFragment"
    lateinit var binding: FragmentQReaderBinding

    lateinit var qrCodeReaderView: QRCodeReaderView
    var deviceType = ""
    var cameraStarted = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentQReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(DEVICE_TYPE)?.let {
            deviceType = it
        }

        binding.continueButton.setOnClickListener {
            initQRScanner()

            binding.questionLayout.visibility = View.GONE
            binding.qrdecoderview.visibility = View.VISIBLE
        }
    }

    private fun initQRScanner() {
        qrCodeReaderView = binding.qrdecoderview
        qrCodeReaderView.setOnQRCodeReadListener(this)

        // Use this function to enable/disable decoding
        qrCodeReaderView.setQRDecodingEnabled(true)

        // Use this function to change the autofocus interval (default is 5 secs)
        qrCodeReaderView.setAutofocusInterval(2000L)
        qrCodeReaderView.forceAutoFocus()

        // Use this function to enable/disable Torch
        qrCodeReaderView.setTorchEnabled(true)

        // Use this function to set back camera preview
        qrCodeReaderView.setBackCamera()

        qrCodeReaderView.startCamera()
        cameraStarted = true
    }


    override fun onResume() {
        super.onResume()
    }


    override fun onPause() {
        super.onPause()
        if (cameraStarted) {
            qrCodeReaderView.stopCamera()
        }
    }

    override fun onQRCodeRead(text: String?, points: Array<out PointF>?) {
        Log.d(TAG, "onQRCodeRead: $text")
        qrCodeReaderView.stopCamera()

        if (deviceType == TANK_MONITOR) {
            val bundle = bundleOf(QR_TEXT to text, DEVICE_TYPE to deviceType)
            findNavController().navigate(
                R.id.action_QReaderFragment_to_configureDeviceFragment,
                bundle
            )
        } else if (deviceType == SMART_SWITCH) {
//            TODO ADD Smart switch info to online and Room db and ask to link to another device

            val bundle = bundleOf(QR_TEXT to text, DEVICE_TYPE to deviceType)
            findNavController().navigate(
                R.id.action_addDeviceFragment_to_homeFragment,
                bundle
            )
        }
    }

}