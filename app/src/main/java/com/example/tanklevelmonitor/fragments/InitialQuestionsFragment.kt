package com.example.tanklevelmonitor.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.tanklevelmonitor.R
import com.example.tanklevelmonitor.databinding.FragmentInitialQuestionsBinding
import com.example.tanklevelmonitor.utils.SharedPrefs
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class InitialQuestionsFragment : Fragment() {
    lateinit var binding: FragmentInitialQuestionsBinding
    var userId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentInitialQuestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = SharedPrefs.getUserData(requireContext(), "userId")
        binding.submitButton.setOnClickListener {
            setSubmitButtonListener()
        }
    }

    private fun setSubmitButtonListener() {
        if (binding.minLevelInput.text.isNotEmpty() and binding.maxLevelInput.text.isNotEmpty()) {
            val max = binding.maxLevelInput.text.toString()
            val min = binding.minLevelInput.text.toString()

            val db = Firebase.database
            db.getReference("devices").child(userId).child("max").setValue(max.toLong())
            db.getReference("devices").child(userId).child("min").setValue(min.toLong())
            db.getReference("devices").child(userId).child("pending").setValue(true)

            findNavController().navigate(R.id.action_initialQuestionsFragment_to_homeFragment)
        }
    }

}