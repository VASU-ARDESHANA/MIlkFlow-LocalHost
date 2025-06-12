package vasu.apps.milkflow.Fragment

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.Activity.LoginActivity
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var accountService = Appwrite.accountService

    private lateinit var rootView: View
    private lateinit var displayTime: TextView
    private lateinit var account: MaterialCardView
    private lateinit var customerList: MaterialCardView
    private lateinit var dailySell: MaterialCardView
    private lateinit var products: MaterialCardView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)
        val title = getString(R.string.app_name)
        (requireActivity() as? MainActivity)?.setMainToolbarTitle(title)

        displayTime = rootView.findViewById(R.id.home_timer)
        account = rootView.findViewById(R.id.home_mcv_account)
        customerList = rootView.findViewById(R.id.home_mcv_customer_list)
        dailySell = rootView.findViewById(R.id.home_mcv_daily_sell)
        products = rootView.findViewById(R.id.home_mcv_products)
        progressBar = rootView.findViewById(R.id.home_progressBar)

        products.setOnClickListener {
            navigateWithArgs(R.id.nav_supplier_products, "products" to "MainActivity")
        }

        customerList.setOnClickListener {
            navigateWithArgs(R.id.nav_customer_list)
        }

        dailySell.setOnClickListener {
            navigateWithArgs(R.id.nav_daily_sell)
        }

        account.setOnClickListener {
            navigateWithArgs(R.id.nav_supplier_account, "products" to "MainActivity")
        }


        return rootView
    }

    private fun startUpdatingTime() {
        val dateFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.format(Date())
        displayTime.text = date
    }

    private fun handleLogout(message: String) {
        toastMsg(message, "error")
        lifecycleScope.launch {
            accountService.logout()
        }
        requireContext().getSharedPreferences("MyPrefs", MODE_PRIVATE).edit { clear() }
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun navigateWithArgs(destinationId: Int, vararg args: Pair<String, Any?>) {
        findNavController().navigate(destinationId, bundleOf(*args))
    }

    private fun toastMsg(message: String, toastType: String) {
        when (toastType) {
            "warning" -> {
                DynamicToast.makeWarning(requireActivity(), message, Toast.LENGTH_SHORT).show()
            }

            "error" -> {
                DynamicToast.makeError(requireActivity(), message, Toast.LENGTH_SHORT).show()
            }

            else -> {
                DynamicToast.make(
                    requireActivity(),
                    message,
                    ContextCompat.getColor(requireContext(), R.color.white),
                    ContextCompat.getColor(requireContext(), R.color.success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onStart() {

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    accountService.getLoggedIn()
                }
                if (user == null) {
                    handleLogout("User Sessions not Found")
                }
            } catch (e: AppwriteException) {
                toastMsg(e.message ?: "An unknown error occurred", "error")
            }
        }
        super.onStart()
        startUpdatingTime()
    }

}