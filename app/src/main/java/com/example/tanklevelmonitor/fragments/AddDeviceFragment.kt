package com.example.tanklevelmonitor.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentAddDeviceBinding
import com.example.tanklevelmonitor.utils.Constants.DEVICE_TYPE
import com.example.tanklevelmonitor.utils.Constants.SMART_SWITCH
import com.example.tanklevelmonitor.utils.Constants.TANK_MONITOR

class AddDeviceFragment : Fragment() {
    private val TAG = "AddDeviceFragment"
    lateinit var binding: FragmentAddDeviceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: ")
        val deviceTankMonitor = binding.deviceTankMonitor
        val deviceSmartSwitch = binding.deviceSmartSwitch

        deviceTankMonitor.tvDeviceName.text = getString(R.string.tank_monitor)
        deviceSmartSwitch.tvDeviceName.text = getString(R.string.smart_switch)

        deviceTankMonitor.ivDeviceImage.setImageResource(R.mipmap.water_tank_icon_foreground)
        deviceSmartSwitch.ivDeviceImage.setImageResource(R.mipmap.socket_icon_foreground)

        deviceTankMonitor.root.setOnClickListener {
//            Send device type to scanner and
//            Scan QR Code and retrieve device id
//            Show settings options
//            switch to internet for updates
            val bundle = bundleOf(DEVICE_TYPE to TANK_MONITOR)
            findNavController().navigate(
                R.id.action_addDeviceFragment_to_QReaderFragment,
                bundle
            )
        }

        deviceSmartSwitch.root.setOnClickListener {
//            Scan QR Code / Scan Wifi Network / Add some number
//            Retrieve device id
//            Save info in local db and in online db
//            ask to link it to another device (type observer device)
//            Send device id to database with its observer id
            val bundle = bundleOf(DEVICE_TYPE to SMART_SWITCH)
            findNavController().navigate(
                R.id.action_addDeviceFragment_to_QReaderFragment,
                bundle
            )
        }
    }


}