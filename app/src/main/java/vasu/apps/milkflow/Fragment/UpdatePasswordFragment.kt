package vasu.apps.milkflow.Fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.Activity.ProfileUpdateActivity
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite

class UpdatePasswordFragment : Fragment() {

    private var accountService = Appwrite.accountService

    private lateinit var rootView: View
    private lateinit var oldPassword: TextInputLayout
    private lateinit var newPassword: TextInputLayout
    private lateinit var confirmPassword: TextInputLayout
    private lateinit var submit: AppCompatButton
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_update_password, container, false)

        oldPassword = rootView.findViewById(R.id.old_password)
        newPassword = rootView.findViewById(R.id.new_password)
        confirmPassword = rootView.findViewById(R.id.confirm_password)
        submit = rootView.findViewById(R.id.update_password)
        progressBar = rootView.findViewById(R.id.update_password_progressBar)
        val title = getString(R.string.app_name) + " - Change Password"
        (requireActivity() as? ProfileUpdateActivity)?.setToolbarTitle(title)

        oldPassword.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                oldPassword.error = null
                oldPassword.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        newPassword.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                newPassword.isCounterEnabled = true
                val currentPassword = s.toString().trim()
                if (currentPassword.isNotEmpty()) {
                    if (isValidPassword(currentPassword)) {
                        newPassword.setStartIconDrawable(R.drawable.password_lock)
                        newPassword.error = null
                    } else {
                        newPassword.setStartIconDrawable(R.drawable.password_unlocked)
                        newPassword.error =
                            "Password must have 8+ chars with 1 uppercase, 1 number, and 1 symbol (@,!,#,$)"
                        newPassword.requestFocus()
                    }
                } else {
                    newPassword.setStartIconDrawable(R.drawable.password_unlocked)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        confirmPassword.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                confirmPassword.isCounterEnabled = true
                val currentPassword = s.toString().trim()
                if (currentPassword.isNotEmpty()) {
                    if (isValidOldPassword(currentPassword)) {
                        confirmPassword.setStartIconDrawable(R.drawable.password_lock)
                        confirmPassword.error = null
                        confirmPassword.isErrorEnabled = false
                    } else {
                        confirmPassword.setStartIconDrawable(R.drawable.password_unlocked)
                    }
                } else {
                    confirmPassword.setStartIconDrawable(R.drawable.password_unlocked)
                    confirmPassword.error = null
                    confirmPassword.isErrorEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        submit.setOnClickListener {
            val oldPasswordText = oldPassword.editText?.text.toString().trim()
            val newPasswordText = newPassword.editText?.text.toString().trim()
            val confirmPasswordText = confirmPassword.editText?.text.toString().trim()

            if (oldPasswordText.isEmpty()) {
                oldPassword.error = "Old password is required"
                oldPassword.requestFocus()
                return@setOnClickListener
            } else if (newPasswordText.isEmpty()) {
                newPassword.error = "New password is required"
                newPassword.requestFocus()
                return@setOnClickListener
            } else if (confirmPasswordText.isEmpty()) {
                confirmPassword.error = "Confirm password is required"
                confirmPassword.requestFocus()
                return@setOnClickListener
            } else if (newPasswordText != confirmPasswordText) {
                confirmPassword.editText?.setText("")
                toastMsg("Passwords do not match", "warning")
                confirmPassword.requestFocus()
            } else if (oldPasswordText == newPasswordText) {
                toastMsg("New password cannot be the same as the old password", "warning")
                oldPassword.requestFocus()
            } else {
                progressBar.visibility = View.VISIBLE
                disable()
                updatePassword(oldPasswordText, newPasswordText)
            }
        }

        return rootView
    }

    private fun updatePassword(oldPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    accountService.passwordUpdate(oldPassword, newPassword)
                }

                progressBar.visibility = View.GONE
                enable()
                reset()
                toastMsg("Password updated successfully", "")

                val navOptions =
                    NavOptions.Builder().setPopUpTo(R.id.password_navigation, true).build()

                findNavController().navigate(R.id.action_to_update_profile, null, navOptions)

            } catch (e: AppwriteException) {
                toastMsg(e.message ?: "An unknown error occurred", "error")
                progressBar.visibility = View.GONE
                enable()
            }
        }
    }

    private fun toastMsg(message: String, toastType: String) {
        if (toastType == "warning") {
            DynamicToast.makeWarning(requireActivity(), message, Toast.LENGTH_SHORT).show()
        } else if (toastType == "error") {
            DynamicToast.makeError(requireActivity(), message, Toast.LENGTH_SHORT).show()
        } else {
            DynamicToast.make(
                requireActivity(),
                message,
                ContextCompat.getColor(requireContext(), R.color.white),
                ContextCompat.getColor(requireContext(), R.color.success),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isValidOldPassword(password: String): Boolean {
        val minLength = 8
        return password.length >= minLength
    }

    private fun isValidPassword(password: String): Boolean {
        val minLength = 8
        val upperCasePattern = Regex("[A-Z]")
        val digitPattern = Regex("[0-9]")
        val specialCharPattern = Regex("[^A-Za-z0-9]")
        val specificSpecialCharPattern = Regex("[@!#$]")

        return password.length >= minLength && upperCasePattern.containsMatchIn(password) && digitPattern.containsMatchIn(
            password
        ) && specialCharPattern.containsMatchIn(password) && specificSpecialCharPattern.containsMatchIn(
            password
        )
    }

    private fun disable() {
        oldPassword.isEnabled = false
        newPassword.isEnabled = false
        confirmPassword.isEnabled = false
        submit.isEnabled = false
    }

    private fun enable() {
        oldPassword.isEnabled = true
        newPassword.isEnabled = true
        confirmPassword.isEnabled = true
        submit.isEnabled = true
    }

    private fun reset() {
        oldPassword.editText?.setText("")
        newPassword.editText?.setText("")
        confirmPassword.editText?.setText("")
        oldPassword.error = null
        newPassword.error = null
        confirmPassword.error = null
        oldPassword.isErrorEnabled = false
        newPassword.isErrorEnabled = false
        confirmPassword.isErrorEnabled = false
    }
}