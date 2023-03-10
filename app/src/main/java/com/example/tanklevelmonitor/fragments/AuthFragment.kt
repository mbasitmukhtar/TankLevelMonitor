package com.example.tanklevelmonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tanklevelmonitor.databinding.FragmentAuthBinding

class AuthFragment : Fragment() {
    private val TAG = "AuthFragment"
    lateinit var binding: FragmentAuthBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLogIn()
        initSignUp()
    }

    private fun initLogIn() {
        binding.buttonSignIn.setOnClickListener {
            val email = binding.inputEmail.text.toString()
            val password = binding.inputPassword.text.toString()

            if (email != "" && password != "") {

//                Make API call for sign in and change fragment on success

            }

        }

        binding.goToSignUpText.setOnClickListener {
            binding.signInLayout.visibility = View.GONE
            binding.signUpLayout.visibility = View.INVISIBLE
        }
    }

    private fun initSignUp() {
        binding.buttonSignUp.setOnClickListener {
            val name = binding.inputUsernameSignUp.text.toString()
            val email = binding.inputEmailSignUp.text.toString()
            val password = binding.inputPasswordSignUp.text.toString()

            if (name != "" && email != "" && password != "") {

//                Make API call for sign up and store user info in phone, change fragment on success

            }
        }

        binding.goToSignUpText.setOnClickListener {
            binding.signInLayout.visibility = View.VISIBLE
            binding.signUpLayout.visibility = View.GONE
        }
    }

}