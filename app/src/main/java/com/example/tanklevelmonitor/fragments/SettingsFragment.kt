package com.example.tanklevelmonitor.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentSettingsBinding
import com.example.tanklevelmonitor.utils.SharedPrefs
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SettingsFragment : Fragment() {

    lateinit var binding: FragmentSettingsBinding
    var userId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = SharedPrefs.getUserData(requireContext(), "userId")

        binding.submitLevelButton.setOnClickListener {
            setSubmitLevelButtonListener()
        }

        binding.submitRouterButton.setOnClickListener {
            setSubmitRouterButtonListener()
        }

        binding.submitHotspotButton.setOnClickListener {
            setSubmitHotspotButtonListener()
        }
    }

    private fun setSubmitHotspotButtonListener() {
        if (binding.ssidHotspotInput.text.isNotEmpty() and binding.passwordHotspotInput.text.isNotEmpty()) {
            val ssid = binding.ssidHotspotInput.text.toString()
            val password = binding.passwordHotspotInput.text.toString()

            val db = Firebase.database
            db.getReference("devices").child(userId).child("ap_ssid").setValue(ssid)
            db.getReference("devices").child(userId).child("ap_pass").setValue(password)
            db.getReference("devices").child(userId).child("pending").setValue(true)
        } else {
            Toast.makeText(requireContext(), "Please enter both fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSubmitRouterButtonListener() {
        if (binding.ssidInput.text.isNotEmpty() and binding.passwordInput.text.isNotEmpty()) {
            val ssid = binding.ssidInput.text.toString()
            val password = binding.passwordInput.text.toString()

            val db = Firebase.database
            db.getReference("devices").child(userId).child("ssid").setValue(ssid)
            db.getReference("devices").child(userId).child("pass").setValue(password)
            db.getReference("devices").child(userId).child("pending").setValue(true)
        } else {
            Toast.makeText(requireContext(), "Please enter both fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSubmitLevelButtonListener() {
        if (binding.minLevelInput.text.isNotEmpty() and binding.maxLevelInput.text.isNotEmpty()) {
            val max = binding.maxLevelInput.text.toString()
            val min = binding.minLevelInput.text.toString()

            val db = Firebase.database
            db.getReference("devices").child(userId).child("max").setValue(max.toLong())
            db.getReference("devices").child(userId).child("min").setValue(min.toLong())
            db.getReference("devices").child(userId).child("pending").setValue(true)
        } else {
            Toast.makeText(requireContext(), "Please enter both fields", Toast.LENGTH_SHORT).show()
        }
    }
}