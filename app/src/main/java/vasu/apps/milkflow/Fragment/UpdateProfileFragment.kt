package vasu.apps.milkflow.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.Activity.ProfileUpdateActivity
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class UpdateProfileFragment : Fragment() {

    private var accountService = Appwrite.accountService
    private var userService = Appwrite.userService

    private lateinit var rootView: View
    private lateinit var titleHead: TextView
    private lateinit var name: TextInputLayout
    private lateinit var businessName: TextInputLayout
    private lateinit var email: TextInputLayout
    private lateinit var phone: TextInputLayout
    private lateinit var address: TextInputLayout
    private lateinit var city: TextInputLayout
    private lateinit var state: TextInputLayout
    private lateinit var pinCode: TextInputLayout
    private lateinit var upi: TextInputLayout
    private lateinit var notificationMACT: MaterialAutoCompleteTextView
    private lateinit var registeredDate: TextInputLayout
    private lateinit var updateProfile: AppCompatButton
    private lateinit var progressBar: ProgressBar

    private var isEmailVerified = false

    private var cachedUserData: Map<String, Any>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_update_profile, container, false)

        val productId = arguments?.getString("products")

        titleHead = rootView.findViewById(R.id.profile_update_title)

        if (productId == "MainActivity") {
            val title = getString(R.string.app_name) + " - Update Profile"
            (requireActivity() as? MainActivity)?.setMainToolbarTitle(title)
            titleHead.visibility = View.GONE
        } else {
            val title = getString(R.string.app_name) + " - Update Profile"
            (requireActivity() as? ProfileUpdateActivity)?.setToolbarTitle(title)
        }

        name = rootView.findViewById(R.id.profile_update_name_til)
        businessName = rootView.findViewById(R.id.profile_update_business_name_til)
        email = rootView.findViewById(R.id.profile_update_email_til)
        phone = rootView.findViewById(R.id.profile_update_phone_til)
        address = rootView.findViewById(R.id.profile_update_address_til)
        city = rootView.findViewById(R.id.profile_update_city_til)
        state = rootView.findViewById(R.id.profile_update_state_til)
        pinCode = rootView.findViewById(R.id.profile_update_pinCode_til)
        upi = rootView.findViewById(R.id.profile_update_upi_til)
        notificationMACT = rootView.findViewById(R.id.profile_update_notification_mact)
        registeredDate = rootView.findViewById(R.id.profile_update_registered_date_til)
        progressBar = rootView.findViewById(R.id.profile_update_progressBar)
        updateProfile = rootView.findViewById(R.id.update_profile)

        val notificationOptions = listOf("Yes", "No")
        val notificationAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, notificationOptions)
        notificationMACT.setAdapter(notificationAdapter)

        email.setEndIconOnClickListener {
            if (isEmailVerified) {
                toastMsg("Email already verified!", "emailSuccess")
            } else {
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            accountService.sendEmailVerification()
                        }
                        toastMsg("Email verification sent!", "success")
                    } catch (e: AppwriteException) {
                        toastMsg(e.message ?: "An unknown error occurred", "error")
                    }
                }
            }
        }

        updateProfile.setOnClickListener {
            profileUpdate(productId)
        }

        fetchData()

        return rootView
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        disable()

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            try {
                val user = withContext(Dispatchers.IO) {
                    accountService.getLoggedIn()
                }

                cachedUserData = withContext(Dispatchers.IO) {
                    user?.let { userService.getUserDocument(it.id) }
                }

                if (user != null && cachedUserData != null) {
                    name.editText?.setText(user.name)
                    email.editText?.setText(user.email)
                    val regDate = user.createdAt

                    val formattedTime = regDate.let {
                        val utcTime = OffsetDateTime.parse(it)
                        val localTime = utcTime.atZoneSameInstant(ZoneId.systemDefault())
                        val formatter = DateTimeFormatter.ofPattern(
                            "MMM d, yyyy 'at' h:mm a",
                            Locale.getDefault()
                        )
                        localTime.format(formatter)
                    }

                    isEmailVerified = user.emailVerification
                    val color = if (isEmailVerified) R.color.success else R.color.error
                    email.setEndIconTintList(
                        ContextCompat.getColorStateList(
                            requireContext(),
                            color
                        )
                    )

                    val userData = cachedUserData

                    phone.editText?.setText(userData?.get("phone_number")?.toString() ?: "")
                    businessName.editText?.setText(userData?.get("business_name")?.toString() ?: "")
                    address.editText?.setText(userData?.get("address")?.toString() ?: "")
                    city.editText?.setText(userData?.get("city")?.toString() ?: "")
                    state.editText?.setText(userData?.get("state")?.toString() ?: "")
                    pinCode.editText?.setText(userData?.get("pin_code")?.toString() ?: "")
                    upi.editText?.setText(userData?.get("upi_id")?.toString() ?: "")
                    notificationMACT.setText(
                        if ((userData?.get("notification_enabled") as? Boolean) == true) "Yes" else "No",
                        false
                    )
                    registeredDate.editText?.setText(formattedTime)
                }
            } catch (e: AppwriteException) {
                toastMsg(e.message ?: "An unknown error occurred", "error")
            } finally {
                val elapsedTime = System.currentTimeMillis() - startTime
                val delayTime = 1000 - elapsedTime
                if (delayTime > 0) delay(delayTime)
                progressBar.visibility = View.GONE
                enable()
            }
        }
    }

    private fun profileUpdate(productId: String?) {
        val nameText = name.editText?.text.toString().trim()
        val businessNameText = businessName.editText?.text.toString().trim()
        val phoneText = phone.editText?.text.toString().trim()
        val addressText = address.editText?.text.toString().trim()
        val cityText = city.editText?.text.toString().trim()
        val stateText = state.editText?.text.toString().trim()
        val pinCodeText = pinCode.editText?.text.toString().trim()
        val upiText = upi.editText?.text.toString().trim()
        val notificationText = notificationMACT.text.toString().trim()
        val notificationEnabled = notificationText == "Yes"

        if (nameText.isEmpty() || businessNameText.isEmpty() || phoneText.isEmpty() || addressText.isEmpty() || cityText.isEmpty() || stateText.isEmpty() || pinCodeText.isEmpty() || upiText.isEmpty() || notificationText.isEmpty()) {
            toastMsg("All fields are required!", "warning")
            return
        }

        if (!isEmailVerified) {
            toastMsg("Please verify your email before updating profile.", "warning")
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    accountService.getLoggedIn()
                }

                val userId = user?.id ?: run {
                    toastMsg("User not found.", "error")
                    progressBar.visibility = View.GONE
                    return@launch
                }

                val updateData = mapOf(
                    "name" to nameText,
                    "business_name" to businessNameText,
                    "address" to addressText,
                    "city" to cityText,
                    "state" to stateText,
                    "pin_code" to pinCodeText,
                    "upi_id" to upiText,
                    "notification_enabled" to notificationEnabled
                )

                withContext(Dispatchers.IO) {
                    cachedUserData?.let {
                        userService.updateUserDocument(
                            userId,
                            updateData,
                            it,
                        )
                    }

                    val currentNotificationEnabled = cachedUserData?.get("notification_enabled") as? Boolean
                    if (notificationEnabled != currentNotificationEnabled) {
                        try {
                            if (notificationEnabled) {
                                accountService.registerPushTarget()
                            } else {
                                accountService.removePushTargetIfExists()
                            }
                        } catch (e: Exception) {
                            Log.e("AccountService", "Push target update failed: ${e.message}")
                            toastMsg(e.message ?: "Unexpected error", "error")
                        }
                    }


                    if (productId != "MainActivity") {
                        accountService.setUserPrefs(profileUpdated = "yes")
                    }
                }

                if (productId == "MainActivity") {
                    toastMsg("Profile updated successfully!", "success")
                    fetchData()
                } else {
                    toastMsg("Profile updated successfully!", "success")

                    val navOptions =
                        NavOptions.Builder().setPopUpTo(R.id.update_profile_navigation, true).build()

                    findNavController().navigate(R.id.action_to_update_product, null, navOptions)
                }

            } catch (e: AppwriteException) {
                toastMsg(e.message ?: "Update failed!", "error")
            } catch (e: Exception) {
                toastMsg(e.message ?: "Unexpected error", "error")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun toastMsg(message: String, toastType: String) {
        when (toastType) {
            "warning" -> {
                DynamicToast.makeWarning(requireActivity(), message, Toast.LENGTH_SHORT).show()
            }

            "error" -> {
                DynamicToast.makeError(requireActivity(), message, Toast.LENGTH_SHORT).show()
            }

            "emailSuccess" -> {
                DynamicToast.make(
                    requireActivity(),
                    message,
                    ContextCompat.getDrawable(requireContext(), R.drawable.verified),
                    requireContext().getColor(R.color.white),
                    requireContext().getColor(R.color.success),
                    Toast.LENGTH_SHORT
                ).show()
            }

            "success" -> {
                DynamicToast.make(
                    requireContext(),
                    message,
                    requireContext().getColor(R.color.white),
                    requireContext().getColor(R.color.success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun disable() {
        name.isEnabled = false
        businessName.isEnabled = false
        phone.isEnabled = false
        address.isEnabled = false
        city.isEnabled = false
        state.isEnabled = false
        pinCode.isEnabled = false
        upi.isEnabled = false
        notificationMACT.isEnabled = false
        updateProfile.isEnabled = false
    }

    private fun enable() {
        name.isEnabled = true
        businessName.isEnabled = true
        phone.isEnabled = true
        address.isEnabled = true
        city.isEnabled = true
        state.isEnabled = true
        pinCode.isEnabled = true
        upi.isEnabled = true
        notificationMACT.isEnabled = true
        updateProfile.isEnabled = true
    }
}

// Show a test toast
//        DynamicToast.make(
//                requireContext(),
//                "Profile updated successfully!",
//                ContextCompat.getDrawable(requireContext(), R.drawable.email),
//                requireContext().getColor(R.color.white),
//                requireContext().getColor(R.color.success),
//                Toast.LENGTH_SHORT
//        ).show()