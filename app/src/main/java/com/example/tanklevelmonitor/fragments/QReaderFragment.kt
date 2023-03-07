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


class QReaderFragment : Fragment(), QRCodeReaderView.OnQRCodeReadListener {
    private val TAG = "QReaderFragment"
    lateinit var binding: FragmentQReaderBinding

    lateinit var qrCodeReaderView: QRCodeReaderView
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

        qrCodeReaderView = binding.qrdecoderview
        qrCodeReaderView.setOnQRCodeReadListener(this);

        // Use this function to enable/disable decoding
        qrCodeReaderView.setQRDecodingEnabled(true);

        // Use this function to change the autofocus interval (default is 5 secs)
        qrCodeReaderView.setAutofocusInterval(2000L);
        qrCodeReaderView.forceAutoFocus()

        // Use this function to enable/disable Torch
        qrCodeReaderView.setTorchEnabled(true);

        // Use this function to set front camera preview
//        qrCodeReaderView.setFrontCamera();

        // Use this function to set back camera preview
        qrCodeReaderView.setBackCamera();
    }


    override fun onResume() {
        super.onResume()
        qrCodeReaderView.startCamera()
    }


    override fun onPause() {
        super.onPause()
        qrCodeReaderView.stopCamera()
    }

    override fun onQRCodeRead(text: String?, points: Array<out PointF>?) {
        Log.d(TAG, "onQRCodeRead: $text")
        qrCodeReaderView.stopCamera()
        val bundle = bundleOf("qrText" to text)
        findNavController().navigate(
            R.id.action_QReaderFragment_to_configureDeviceFragment,
            bundle
        )
    }

}