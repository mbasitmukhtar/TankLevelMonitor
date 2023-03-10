package com.example.tanklevelmonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentSmartSwitchControlBinding

class SmartSwitchControlFragment : Fragment() {
    private val TAG = "SmartSwitchControlFragm"
    lateinit var binding: FragmentSmartSwitchControlBinding

    var deviceState = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSmartSwitchControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        TODO get device current state from database and set it below

        binding.linkedDeviceText.text = getString(R.string.switch_is_linked_to) + "Device Name"
        binding.smartSwitch.ivStateImage.setImageResource(R.mipmap.turn_on_icon_foreground)
        binding.smartSwitch.tvState.text = getString(R.string.click_to_turn_off)

        binding.smartSwitch.root.setOnClickListener {
//            Toggle this device's state
//            Send request to database to update this device's state
        }
    }

}