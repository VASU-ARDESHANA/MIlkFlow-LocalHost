package vasu.apps.milkflow.Fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterCustomerFragment : Fragment() {

    private var accountService = Appwrite.accountService
    private var userService = Appwrite.userService
    private var customerService = Appwrite.customerService

    private lateinit var rootView: View
    private lateinit var consumerName: TextInputLayout
    private lateinit var consumerNumber: TextInputLayout
    private lateinit var consumerAddress: TextInputLayout
    private lateinit var consumerDeliveryTimeTIL: TextInputLayout
    private lateinit var consumerDeliveryTime: MaterialAutoCompleteTextView
    private lateinit var consumerMilkTypeTIL: TextInputLayout
    private lateinit var consumerMilkType: MaterialAutoCompleteTextView
    private lateinit var consumerStartDate: TextInputLayout
    private lateinit var consumerCowMilkPrice: TextInputLayout
    private lateinit var cowText: TextView
    private lateinit var buffaloText: TextView
    private lateinit var consumerMorningCowLitter: TextInputLayout
    private lateinit var consumerEveningCowLitter: TextInputLayout
    private lateinit var consumerBuffaloMilkPrice: TextInputLayout
    private lateinit var consumerMorningBuffaloLitter: TextInputLayout
    private lateinit var consumerEveningBuffaloLitter: TextInputLayout
    private lateinit var consumerPaymentModeTIL: TextInputLayout
    private lateinit var consumerPaymentMode: MaterialAutoCompleteTextView
    private lateinit var addCustomerButton: AppCompatButton
    private lateinit var consumerStartDateTiel: TextInputEditText
    private lateinit var progressBar: ProgressBar

    private lateinit var morningCowGroup: List<View>
    private lateinit var eveningCowGroup: List<View>
    private lateinit var morningBuffaloGroup: List<View>
    private lateinit var eveningBuffaloGroup: List<View>
    private lateinit var cowMilkGroup: List<View>
    private lateinit var buffaloMilkGroup: List<View>
    private var selectedMilkType: Int = -1

    private var cachedUserData: Map<String, Any>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_register_customer, container, false)

        val title = getString(R.string.app_name) + " - Add Customer"
        (requireActivity() as? MainActivity)?.setMainToolbarTitle(title)

        consumerName = rootView.findViewById(R.id.customer_register_name_til)
        consumerNumber = rootView.findViewById(R.id.customer_register_number_til)
        consumerAddress = rootView.findViewById(R.id.customer_register_address_til)
        consumerDeliveryTimeTIL = rootView.findViewById(R.id.customer_register_delivery_time_til)
        consumerDeliveryTime = rootView.findViewById(R.id.customer_register_delivery_time_mact)
        consumerMilkTypeTIL = rootView.findViewById(R.id.customer_register_milk_type_til)
        consumerMilkType = rootView.findViewById(R.id.customer_register_milk_type_mact)
        consumerStartDate = rootView.findViewById(R.id.customer_register_start_date_til)
        consumerCowMilkPrice = rootView.findViewById(R.id.customer_register_cow_milk_price_til)
        cowText = rootView.findViewById(R.id.customer_register_cow_text)
        buffaloText = rootView.findViewById(R.id.customer_register_buffalo_text)
        consumerMorningCowLitter =
            rootView.findViewById(R.id.customer_register_morning_cow_litter_til)
        consumerEveningCowLitter =
            rootView.findViewById(R.id.customer_register_evening_cow_litter_til)
        consumerBuffaloMilkPrice =
            rootView.findViewById(R.id.customer_register_buffalo_milk_price_til)
        consumerMorningBuffaloLitter =
            rootView.findViewById(R.id.customer_register_morning_buffalo_litter_til)
        consumerEveningBuffaloLitter =
            rootView.findViewById(R.id.customer_register_evening_buffalo_litter_til)
        consumerPaymentModeTIL = rootView.findViewById(R.id.customer_register_payment_mode_til)
        consumerPaymentMode = rootView.findViewById(R.id.customer_register_payment_mode_mact)
        addCustomerButton = rootView.findViewById(R.id.add_customer_button)
        progressBar = rootView.findViewById(R.id.customer_register_progress_bar)

        fetchData()

        clearErrorOnTextChange(
            consumerName,
            consumerNumber,
            consumerAddress,
            consumerStartDate,
            consumerCowMilkPrice,
            consumerMorningCowLitter,
            consumerEveningCowLitter,
            consumerBuffaloMilkPrice,
            consumerMorningBuffaloLitter,
            consumerEveningBuffaloLitter,
            consumerDeliveryTimeTIL,
            consumerMilkTypeTIL
        )

        val deliveryTime = listOf(
            getString(R.string.morning), getString(R.string.evening), getString(R.string.both)
        )
        val deliveryTimeAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deliveryTime)
        consumerDeliveryTime.setAdapter(deliveryTimeAdapter)

        val milkType =
            listOf(getString(R.string.cow), getString(R.string.buffalo), getString(R.string.both))
        val milkTypeAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, milkType)
        consumerMilkType.setAdapter(milkTypeAdapter)

        val paymentMode = listOf(
            getString(R.string.cash),
            getString(R.string.upi),
            getString(R.string.online),
            getString(R.string.wallet)
        )
        val paymentModeAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, paymentMode)
        consumerPaymentMode.setAdapter(paymentModeAdapter)

        consumerStartDateTiel = rootView.findViewById(R.id.customer_register_start_date_et)
        setupDatePicker(consumerStartDateTiel)

        morningCowGroup = listOf(consumerMorningCowLitter)
        eveningCowGroup = listOf(consumerEveningCowLitter)
        morningBuffaloGroup = listOf(consumerMorningBuffaloLitter)
        eveningBuffaloGroup = listOf(consumerEveningBuffaloLitter)
        cowMilkGroup = listOf(
            cowText, consumerCowMilkPrice, consumerMorningCowLitter, consumerEveningCowLitter
        )

        buffaloMilkGroup = listOf(
            buffaloText,
            consumerBuffaloMilkPrice,
            consumerMorningBuffaloLitter,
            consumerEveningBuffaloLitter
        )


        consumerMilkType.setOnItemClickListener { _, _, position, _ ->
            selectedMilkType = position
            updateMilkTypeVisibility()
            updateDeliveryTimeVisibility()
            consumerMilkTypeTIL.error = null
            consumerMilkTypeTIL.isErrorEnabled = false
        }

        consumerDeliveryTime.setOnItemClickListener { _, _, _, _ ->
            updateDeliveryTimeVisibility()
            consumerDeliveryTimeTIL.error = null
            consumerDeliveryTimeTIL.isErrorEnabled = false
        }

        consumerPaymentMode.setOnItemClickListener { _, _, _, _ ->
            consumerPaymentModeTIL.error = null
            consumerPaymentModeTIL.isErrorEnabled = false
            closeKeyboard()
        }

        hideViews(cowMilkGroup)
        hideViews(buffaloMilkGroup)

        addCustomerButton.setOnClickListener {
            rootView.requestFocus()
            closeKeyboard()
            getInputs()
        }

        reset()

        return rootView
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            try {
                val user = withContext(Dispatchers.IO) {
                    accountService.getLoggedIn()
                }

                if (user == null) {
                    toastMsg("User not found.", "error")
                    progressBar.visibility = View.GONE
                    return@launch
                }

                cachedUserData = withContext(Dispatchers.IO) {
                    userService.getUserDocument(user.id)
                }

                val userData = cachedUserData
                if (userData != null) {
                    val cowEnabled = (userData["is_cow_milk_enabled"] as? Boolean) == true
                    val buffaloEnabled = (userData["is_buffalo_milk_enabled"] as? Boolean) == true

                    consumerCowMilkPrice.editText?.setText(
                        userData["price_cow_milk"]?.toString() ?: ""
                    )
                    consumerBuffaloMilkPrice.editText?.setText(
                        userData["price_buffalo_milk"]?.toString() ?: ""
                    )

                    val milkType = mutableListOf<String>()
                    if (cowEnabled) milkType.add(getString(R.string.cow))
                    if (buffaloEnabled) milkType.add(getString(R.string.buffalo))
                    if (cowEnabled && buffaloEnabled) milkType.add(getString(R.string.both))
                    if (!cowEnabled && !buffaloEnabled) {
                        toastMsg("No milk types enabled for this user", "error")
                        navigateWithArgs(R.id.nav_supplier_products, "products" to "AddCustomer")
                        consumerMilkTypeTIL.isEnabled = false
                    }

                    val milkTypeAdapter = ArrayAdapter(
                        requireContext(), android.R.layout.simple_list_item_1, milkType
                    )
                    consumerMilkType.setAdapter(milkTypeAdapter)

                } else {
                    toastMsg("Failed to load user data.", "error")
                }

            } catch (e: AppwriteException) {
                toastMsg(e.message ?: "An unknown error occurred", "error")
            } finally {
                val elapsedTime = System.currentTimeMillis() - startTime
                val delayTime = 1000 - elapsedTime
                if (delayTime > 0) delay(delayTime)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun validateVisibleFields(): Boolean {
        if (consumerName.editText?.text.isNullOrEmpty()) {
            consumerName.error = "Enter name"
            openKeyboard(consumerName.editText!!)
            return false
        }

        val numberText = consumerNumber.editText?.text.toString()
        if (numberText.isEmpty() || numberText.length != 10) {
            consumerNumber.error = "Please enter a valid number"
            openKeyboard(consumerNumber.editText!!)
            return false
        }

        if (consumerAddress.editText?.text.isNullOrEmpty()) {
            consumerAddress.error = "Enter address"
            openKeyboard(consumerAddress.editText!!)
            return false
        }

        if (consumerDeliveryTime.text.isNullOrEmpty()) {
            consumerDeliveryTimeTIL.error = "Select delivery time"
            consumerDeliveryTimeTIL.requestFocus()
            return false
        }

        if (consumerMilkType.text.isNullOrEmpty()) {
            consumerMilkTypeTIL.error = "Select milk type"
            consumerMilkTypeTIL.requestFocus()
            return false
        }

        if (consumerStartDate.editText?.text.isNullOrEmpty()) {
            consumerStartDate.error = "Select start date"
            consumerStartDate.requestFocus()
            return false
        }

        if (consumerPaymentMode.text.isNullOrEmpty()) {
            consumerPaymentModeTIL.error = "Select payment mode"
            consumerPaymentModeTIL.requestFocus()
            return false
        }

        return true
    }

    private fun getInputs() {
        clearAllErrors()
        if (validateVisibleFields()) {
            createUserOnServer()
        }
    }

    private fun createUserOnServer() {
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            progressBar.visibility = View.VISIBLE

            val user = withContext(Dispatchers.IO) {
                accountService.getLoggedIn()
            }

            if (user == null) {
                progressBar.visibility = View.GONE
                toastMsg("User not found.", "error")
                return@launch
            }

            val supplierData = cachedUserData

            Log.e("LogUserServices", supplierData.toString())

            if (supplierData == null) {
                progressBar.visibility = View.GONE
                toastMsg("Supplier document not found.", "error")
                return@launch
            }

            val maxConsumer = supplierData["max_consumer"].toString().toIntOrNull() ?: 0
            val createdConsumer = supplierData["created_consumer"].toString().toIntOrNull() ?: 0

            if (createdConsumer >= maxConsumer) {
                progressBar.visibility = View.GONE
                toastMsg("You have reached the maximum number of consumers allowed.", "error")
                toastMsg("Contact The Developer", "warning")
                fetchData()
                return@launch
            }

            val rawStartDate = consumerStartDate.editText?.text.toString()
            val formattedStartDate = convertToRFC3339(rawStartDate)

            val data = mutableMapOf<String, Any>(
                "supplier_id" to user.id,
                "name" to consumerName.editText?.text.toString(),
                "phone_number" to consumerNumber.editText?.text.toString(),
                "address" to consumerAddress.editText?.text.toString(),
                "delivery_time" to consumerDeliveryTime.text.toString().lowercase(),
                "milk_type" to consumerMilkType.text.toString().lowercase(),
                "start_date" to formattedStartDate,
                "payment_mode" to consumerPaymentMode.text.toString().lowercase(),
                "is_active" to (rawStartDate == startUpdatingTime()),
                "notification_enabled" to true,
                "is_on_vacation" to false,
            )

            val cowMorning = consumerMorningCowLitter.editText?.text.toString().toDoubleOrNull()
            val cowEvening = consumerEveningCowLitter.editText?.text.toString().toDoubleOrNull()
            if (cowMorning != null && cowMorning > 0) data["morning_cow_milk_qty"] = cowMorning
            if (cowEvening != null && cowEvening > 0) data["evening_cow_milk_qty"] = cowEvening

            val milkType = consumerMilkType.text.toString().lowercase()
            if (milkType.contains("buffalo") || milkType == "both") {
                val buffMorning =
                    consumerMorningBuffaloLitter.editText?.text.toString().toDoubleOrNull()
                val buffEvening =
                    consumerEveningBuffaloLitter.editText?.text.toString().toDoubleOrNull()
                if (buffMorning != null && buffMorning > 0) data["morning_buffalo_milk_qty"] =
                    buffMorning
                if (buffEvening != null && buffEvening > 0) data["evening_buffalo_milk_qty"] =
                    buffEvening
            }

            try {
                withContext(Dispatchers.IO) {
                    customerService.createNewUserDocument(data)
                    val updatedCount = createdConsumer + 1
                    val updateData = mapOf("created_consumer" to updatedCount)
                    userService.updateUserDocument(user.id, updateData, supplierData)
                }

                toastMsg("User added successfully", "success")
                fetchData()
                reset()
            } catch (e: AppwriteException) {
                if (e.code == 409) {
                    consumerNumber.error = "This phone number is already registered."
                    openKeyboard(consumerNumber.editText!!)
                } else {
                    toastMsg(e.message ?: "An unknown error occurred", "error")
                }
            } finally {
                val elapsedTime = System.currentTimeMillis() - startTime
                val delayTime = 1000 - elapsedTime
                if (delayTime > 0) delay(delayTime)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupDatePicker(editText: TextInputEditText) {
        editText.setOnClickListener {
            val constraintsBuilder =
                CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now())

            val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select a date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build()).build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = Date(selection)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                editText.setText(sdf.format(selectedDate))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }


    private fun updateMilkTypeVisibility() {
        when (selectedMilkType) {
            0 -> {
                showViews(cowMilkGroup)
                hideViews(buffaloMilkGroup)
            }

            1 -> {
                hideViews(cowMilkGroup)
                showViews(buffaloMilkGroup)
            }

            2 -> {
                showViews(cowMilkGroup)
                showViews(buffaloMilkGroup)
            }
        }
    }

    private fun updateDeliveryTimeVisibility() {
        val deliveryTimeText = consumerDeliveryTime.text.toString().lowercase(Locale.getDefault())

        hideViews(morningCowGroup)
        hideViews(eveningCowGroup)
        hideViews(morningBuffaloGroup)
        hideViews(eveningBuffaloGroup)

        if (deliveryTimeText.contains("morning") || deliveryTimeText.contains("both")) {
            if (selectedMilkType == 0 || selectedMilkType == 2) showViews(morningCowGroup)
            if (selectedMilkType == 1 || selectedMilkType == 2) showViews(morningBuffaloGroup)
        }
        if (deliveryTimeText.contains("evening") || deliveryTimeText.contains("both")) {
            if (selectedMilkType == 0 || selectedMilkType == 2) showViews(eveningCowGroup)
            if (selectedMilkType == 1 || selectedMilkType == 2) showViews(eveningBuffaloGroup)
        }
    }

    private fun showViews(views: List<View>) {
        views.forEach { it.visibility = View.VISIBLE }
    }

    private fun hideViews(views: List<View>) {
        views.forEach { it.visibility = View.GONE }
    }

    private fun clearErrorOnTextChange(vararg textInputLayouts: TextInputLayout) {
        for (til in textInputLayouts) {
            til.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    til.error = null
                    til.isErrorEnabled = false
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun navigateWithArgs(destinationId: Int, vararg args: Pair<String, Any?>) {
        findNavController().navigate(destinationId, bundleOf(*args))
    }

    private fun convertToRFC3339(input: String): String {
        val inputFormat = SimpleDateFormat(
            "dd/MM/yyyy", Locale.getDefault()
        )
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = inputFormat.parse(input)
        return outputFormat.format(date!!)
    }

    private fun startUpdatingTime(): String {
        val dateFormat = android.icu.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun closeKeyboard() {
        val activity = requireActivity()
        val view = activity.currentFocus ?: View(activity)
        val imm =
            activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun openKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm =
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
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

    private fun reset() {
        consumerName.editText?.setText("")
        consumerNumber.editText?.setText("")
        consumerAddress.editText?.setText("")
        consumerDeliveryTime.setText("")
        consumerDeliveryTimeTIL.editText?.setText("")
        consumerMilkType.setText("")
        consumerMilkTypeTIL.editText?.setText("")
        consumerStartDateTiel.setText("")
        consumerStartDate.editText?.setText("")
        consumerCowMilkPrice.editText?.setText("")
        consumerMorningCowLitter.editText?.setText("")
        consumerEveningCowLitter.editText?.setText("")
        consumerBuffaloMilkPrice.editText?.setText("")
        consumerMorningBuffaloLitter.editText?.setText("")
        consumerEveningBuffaloLitter.editText?.setText("")
        consumerPaymentMode.setText("")
        hideViews(cowMilkGroup)
        hideViews(buffaloMilkGroup)
    }

    private fun clearAllErrors() {
        consumerName.error = null
        consumerNumber.error = null
        consumerAddress.error = null
        consumerDeliveryTimeTIL.error = null
        consumerMilkTypeTIL.error = null
        consumerStartDate.error = null
        consumerCowMilkPrice.error = null
        consumerMorningCowLitter.error = null
        consumerEveningCowLitter.error = null
        consumerBuffaloMilkPrice.error = null
        consumerMorningBuffaloLitter.error = null
        consumerEveningBuffaloLitter.error = null
        consumerPaymentModeTIL.error = null
    }

}