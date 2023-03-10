package com.example.tanklevelmonitor.fragments

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentConfigureDeviceBinding
import com.example.tanklevelmonitor.utils.Constants
import com.example.tanklevelmonitor.utils.Constants.QR_TEXT
import com.example.tanklevelmonitor.utils.SharedPrefs
import com.example.tanklevelmonitor.utils.WifiConnectivity
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class ConfigureDeviceFragment : Fragment() {
    private val TAG = "ConfigureDeviceFragment"
    lateinit var binding: FragmentConfigureDeviceBinding

    private lateinit var statusTextView: TextView

    private var requiredNetworkSSID = ""
    private var requiredNetworkPass = ""

    private lateinit var wifiConnectivity: WifiConnectivity

    private var deviceHasInternet = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentConfigureDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wifiConnectivity = WifiConnectivity(requireContext())

        statusTextView = binding.statusTextView
        statusTextView.movementMethod = ScrollingMovementMethod()
        statusTextView.append(wifiConnectivity.status)

        arguments?.getString(QR_TEXT).let {
            if (it != null) {
                decodeWifiQRText(it)
            }
        }
    }

    private fun decodeWifiQRText(qrText: String) {
        val items = qrText.split(":", ";")

        var index = 0
        for (item in items) {
            if (item == "S") {
                requiredNetworkSSID = items[index + 1]
                break
            }
            index += 1
        }

        index = 0
        for (item in items) {
            if (item == "P") {
                requiredNetworkPass = items[index + 1]
                break
            }
            index += 1
        }

        SharedPrefs.storeUserData(requireContext(), Constants.APSSID, requiredNetworkSSID)
        SharedPrefs.storeUserData(requireContext(), Constants.APPASS, requiredNetworkPass)

        initWifiConnectivity(requiredNetworkSSID, requiredNetworkPass)

        Log.d(TAG, "onViewCreated: $requiredNetworkSSID, $requiredNetworkPass")
    }

    private fun initWifiConnectivity(requiredNetworkSSID: String, requiredNetworkPass: String) {
        wifiConnectivity.requiredNetworkSSID = requiredNetworkSSID
        wifiConnectivity.requiredNetworkPass = requiredNetworkPass

        wifiConnectivity.checkIfLocationEnabled()
        binding.scanButton.setOnClickListener {
            wifiConnectivity.checkIfLocationEnabled()
        }

        listenForWifiConnected()
    }

    private fun listenForWifiConnected() {
        wifiConnectivity.wifiConnected.observe(viewLifecycleOwner) {
            if (it) {
                hideProgressBar()
                changeUIAfterWifiConnection()
            } else {
                showProgressBar()
            }
        }

        wifiConnectivity.statusInfo.observe(viewLifecycleOwner) {
            if (it != "") {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = it
            } else {
                binding.errorTextView.visibility = View.GONE
            }
        }
    }

    private fun changeUIAfterWifiConnection() {
        requireActivity().runOnUiThread {
            binding.routerSettingsCard.visibility = View.VISIBLE
            binding.hotspotSettingsCard.visibility = View.VISIBLE
            binding.levelSettingsCard.visibility = View.VISIBLE
            binding.skipButton.visibility = View.VISIBLE
            binding.scanInfoLayout.visibility = View.GONE

            wifiConnectivity.makeGetRequest("mac")

            handleGetPostResponses()

            binding.submitLevelButton.setOnClickListener {
                showProgressBar()
                val min = binding.minLevelInput.text.toString()
                val max = binding.maxLevelInput.text.toString()

                wifiConnectivity.makePOSTRequestToDevice("level-sett", "min", "max", min, max)
            }

            binding.submitRouterButton.setOnClickListener {
                showProgressBar()
                val ssid = binding.ssidInput.text.toString()
                val pass = binding.passwordInput.text.toString()

                wifiConnectivity.makePOSTRequestToDevice("sta-sett", "ssid", "pass", ssid, pass)

                scheduleIpCallFromWifi()
            }

            binding.submitHotspotButton.setOnClickListener {
                showProgressBar()
                val apSsid = binding.ssidHotspotInput.text.toString()
                val apPass = binding.passwordHotspotInput.text.toString()

                wifiConnectivity.makePOSTRequestToDevice(
                    "ap-sett",
                    "ap_ssid",
                    "ap_pass",
                    apSsid,
                    apPass
                )
            }

            binding.skipButton.setOnClickListener {
//                requireActivity().finish()
//                startActivity(requireActivity().intent)

//                requireActivity().finishAffinity()
//                val myIntent = Intent(requireActivity(), MainActivity::class.java)
//                myIntent.flags =
//                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//                requireContext().startActivity(myIntent)

                val packageManager: PackageManager = requireContext().packageManager
                val intent: Intent? =
                    packageManager.getLaunchIntentForPackage(requireContext().packageName)
                val componentName: ComponentName? = intent?.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                requireContext().startActivity(mainIntent)
                Runtime.getRuntime().exit(0)

//                wifiConnectivity.removeNetworkCallback()
//                findNavController().navigate(R.id.action_configureDeviceFragment_to_homeFragment)
            }
        }
    }

    private fun handleGetPostResponses() {
        wifiConnectivity.getRequestResponse.observe(viewLifecycleOwner) {
            if (it != "") {
                val items = it.split(",")
                if (items[0] == "mac") {
                    saveMacAndInit(items[1])
                } else if (items[0] == "ip") {
                    handleIpResponse(items[1])
                }
            }
        }

        wifiConnectivity.postRequestResponse.observe(viewLifecycleOwner) {
            if (it != "") {
                hideProgressBar()
                val items = it.split(",")
                if (items[0] == "level-sett") {
                    if (items[1] == "200") {
                        binding.minLevelInput.text.clear()
                        binding.maxLevelInput.text.clear()
                    } else {
                        binding.errorTextView.text = getString(R.string.error_in_sending_data)
                    }
                } else if (items[0] == "sta-sett") {
                    if (items[1] == "200") {
                        binding.ssidInput.text.clear()
                        binding.passwordInput.text.clear()
                    } else {
                        binding.errorTextView.text = getString(R.string.error_in_sending_data)
                    }
                } else if (items[0] == "ap-sett") {
                    if (items[1] == "200") {
                        binding.ssidHotspotInput.text.clear()
                        binding.passwordHotspotInput.text.clear()
                    } else {
                        binding.errorTextView.text = getString(R.string.error_in_sending_data)
                    }
                }
            }
        }
    }

    private fun saveMacAndInit(mac: String) {
        Log.d(TAG, "saveMacAndInit: $mac")
        SharedPrefs.storeFirstTimeFlag(
            requireContext(),
            Constants.PREFERENCES_FIRST_TIME, false
        )
        SharedPrefs.storeUserData(
            requireContext(),
            Constants.USERID, mac
        )
    }

    private fun scheduleIpCallFromWifi() {
        Executors.newSingleThreadScheduledExecutor().schedule({
            getIpFromDevice()
        }, 15, TimeUnit.SECONDS)
    }

    private fun getIpFromDevice() {
        wifiConnectivity.makeGetRequest("ip")
    }

    private fun handleIpResponse(ipRequestResponse: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Message")
            .setPositiveButton("Ok") { dialog, id -> dialog.dismiss() }

        if (ipRequestResponse == "" || ipRequestResponse == "(IP unset)") {
            scheduleIpCallFromWifi()
            val message = "Device has no internet connection.."
            builder.setMessage(message)
            deviceHasInternet = false
        } else {
            deviceHasInternet = true
            val message =
                "Device has internet connection, you may disconnect from device's hotspot."
            builder.setMessage(message)
        }

//        val dialog: AlertDialog = builder.create()
//        dialog.show()
    }

    override fun onResume() {
        super.onResume()
//        checkIfLocationEnabled()
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}