package vasu.apps.milkflow.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.Activity.LoginActivity
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.Activity.ProfileUpdateActivity
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite

class UpdateProductsFragment : Fragment() {

    private var accountService = Appwrite.accountService
    private var userService = Appwrite.userService

    private lateinit var rootView: View
    private lateinit var businessName: TextInputLayout
    private lateinit var cowPriceLL: LinearLayout
    private lateinit var cowPrice: TextInputLayout
    private lateinit var buffaloPriceLL: LinearLayout
    private lateinit var buffaloPrice: TextInputLayout
    private lateinit var cowEnabledMACT: MaterialAutoCompleteTextView
    private lateinit var buffaloEnabledMACT: MaterialAutoCompleteTextView
    private lateinit var progressBar: ProgressBar
    private lateinit var updateProducts: AppCompatButton

    private var cachedUserData: Map<String, Any>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_update_products, container, false)

        val productId = arguments?.getString("products")

        if (productId == "MainActivity") {
            val title = getString(R.string.app_name) + " - Update Products"
            (requireActivity() as? MainActivity)?.setMainToolbarTitle(title)
        } else {
            val title = getString(R.string.app_name) + " - Update Products"
            (requireActivity() as? ProfileUpdateActivity)?.setToolbarTitle(title)
        }

        businessName = rootView.findViewById(R.id.update_product_business_name_til)
        cowPriceLL = rootView.findViewById(R.id.set_cow_milk_price_ll)
        cowPrice = rootView.findViewById(R.id.update_product_cow_price_til)
        buffaloPriceLL = rootView.findViewById(R.id.set_buffalo_milk_price_ll)
        buffaloPrice = rootView.findViewById(R.id.update_product_buffalo_price_til)
        cowEnabledMACT = rootView.findViewById(R.id.update_product_cow_enable_mact)
        buffaloEnabledMACT = rootView.findViewById(R.id.update_product_buffalo_enable_mact)
        progressBar = rootView.findViewById(R.id.update_product_progressBar)
        updateProducts = rootView.findViewById(R.id.set_products)

        fetchProducts()

        val cowOptions = listOf("Yes", "No")
        val cowAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cowOptions)
        cowEnabledMACT.setAdapter(cowAdapter)

        val buffaloOptions = listOf("Yes", "No")
        val buffaloAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, buffaloOptions)
        buffaloEnabledMACT.setAdapter(buffaloAdapter)

        buffaloEnabledMACT.setOnItemClickListener { _, _, position, _ ->
            val selected = buffaloEnabledMACT.adapter.getItem(position).toString()
            buffaloPriceLL.visibility = if (selected == "Yes") View.VISIBLE else View.GONE
        }

        cowEnabledMACT.setOnItemClickListener { _, _, position, _ ->
            val selected = cowEnabledMACT.adapter.getItem(position).toString()
            cowPriceLL.visibility = if (selected == "Yes") View.VISIBLE else View.GONE
        }

        updateProducts.setOnClickListener {
            productUpdate(productId)
        }

        return rootView
    }

    private fun fetchProducts() {
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
                    businessName.editText?.setText(userData["business_name"]?.toString() ?: "")

                    val cowEnabled = (userData["is_cow_milk_enabled"] as? Boolean) == true
                    val buffaloEnabled = (userData["is_buffalo_milk_enabled"] as? Boolean) == true

                    cowEnabledMACT.setText(if (cowEnabled) "Yes" else "No", false)
                    buffaloEnabledMACT.setText(if (buffaloEnabled) "Yes" else "No", false)

                    cowPrice.editText?.setText(userData["price_cow_milk"]?.toString() ?: "")
                    buffaloPrice.editText?.setText(userData["price_buffalo_milk"]?.toString() ?: "")

                    cowPriceLL.visibility = if (cowEnabled) View.VISIBLE else View.GONE
                    buffaloPriceLL.visibility = if (buffaloEnabled) View.VISIBLE else View.GONE
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

    private fun productUpdate(productId: String?) {
        val cowEnabledText = cowEnabledMACT.text.toString().trim()
        val buffaloEnabledText = buffaloEnabledMACT.text.toString().trim()
        val cowPriceText = cowPrice.editText?.text.toString().trim()
        val buffaloPriceText = buffaloPrice.editText?.text.toString().trim()
        val cowEnabled = cowEnabledText == "Yes"
        val buffaloEnabled = buffaloEnabledText == "Yes"
        val cowPriceFloat = cowPriceText.toDouble()
        val buffaloPriceFloat = buffaloPriceText.toDouble()

        if (cowPriceText.isEmpty() || buffaloPriceText.isEmpty() || !cowEnabled && !buffaloEnabled) {
            toastMsg("Please Set the prices and enable milk types.", "error")
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

                val currentUserData = cachedUserData ?: emptyMap()

                val updateData = mapOf(
                    "is_cow_milk_enabled" to cowEnabled,
                    "is_buffalo_milk_enabled" to buffaloEnabled,
                    "price_cow_milk" to cowPriceFloat,
                    "price_buffalo_milk" to buffaloPriceFloat
                )

                withContext(Dispatchers.IO) {
                    userService.updateProductsDocument(userId, updateData, currentUserData)

                    if (productId != "MainActivity") {
                        accountService.setUserPrefs(productSet = "yes")
                        accountService.setUserPrefs(profileCreated = "yes")
                    }
                }

                when (productId) {
                    "MainActivity" -> {
                        toastMsg("Prices set successfully!", "success")
                    }
                    "AddCustomer" -> {
                        toastMsg("Prices set successfully!", "success")
                        navigateWithArgs(R.id.nav_register_customer)
                    }
                    else -> {
                        toastMsg("Prices set successfully!", "success")
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
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

    private fun navigateWithArgs(destinationId: Int, vararg args: Pair<String, Any?>) {
        findNavController().navigate(destinationId, bundleOf(*args))
    }

    private fun toastMsg(message: String, toastType: String) {
        if (toastType == "warning") {
            DynamicToast.makeWarning(requireActivity(), message, Toast.LENGTH_SHORT).show()
        } else if (toastType == "error") {
            DynamicToast.makeError(requireActivity(), message, Toast.LENGTH_SHORT).show()
        } else if (toastType == "success") {
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


