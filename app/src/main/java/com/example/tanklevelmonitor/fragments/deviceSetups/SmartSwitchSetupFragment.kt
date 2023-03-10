package com.example.tanklevelmonitor.fragments.deviceSetups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tanklevelmonitor.databinding.FragmentSmartSwitchSetupBinding

class SmartSwitchSetupFragment : Fragment() {
    private val TAG = "SmartSwitchSetupFragmen"
    lateinit var binding: FragmentSmartSwitchSetupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSmartSwitchSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}