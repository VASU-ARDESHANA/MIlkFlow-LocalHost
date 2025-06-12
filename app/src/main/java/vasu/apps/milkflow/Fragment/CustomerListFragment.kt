package vasu.apps.milkflow.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.Adapter.CustomerAdapter
import vasu.apps.milkflow.Model.Customer
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite
import java.util.Date
import java.util.Locale

class CustomerListFragment : Fragment() {

    private var accountService = Appwrite.accountService
    private var userService = Appwrite.userService

    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: SearchView
    private lateinit var customerNotFound: TextView
    private lateinit var nameNotFound: TextView
    private lateinit var refresh: ImageButton
    private var customerList = listOf<Customer>()
    private lateinit var adapter: CustomerAdapter
    private lateinit var floatingActionButton: ExtendedFloatingActionButton
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_customer_list, container, false)

        recyclerView = rootView.findViewById(R.id.customer_list_recycler_view)
        searchInput = rootView.findViewById(R.id.customer_list_search_input)
        refresh = rootView.findViewById(R.id.customer_list_refresh)
        floatingActionButton = rootView.findViewById(R.id.customer_list_floating_action_button)
        customerNotFound = rootView.findViewById(R.id.customer_list_not_found)
        nameNotFound = rootView.findViewById(R.id.customer_list_search_not_found)
        progressBar = rootView.findViewById(R.id.customer_list_progressBar)

        floatingActionButton.setOnClickListener {
            navigateWithArgs(R.id.nav_register_customer)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        refresh.setOnClickListener {
            fetchData()
        }

        fetchData()

        return rootView
    }

    private fun fetchData() {
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val currentDate = startUpdatingTime()

            try {
                progressBar.visibility = View.VISIBLE

                val currentUser = accountService.getLoggedIn()
                val supplierId = currentUser?.id ?: return@launch

                val allCustomers = userService.getCustomers(supplierId)

                var requiresUpdate = false
                for (customerDoc in allCustomers) {
                    val data = customerDoc.data
                    val startDateStr = data["start_date"].toString()
                    val startDateOnly = startDateStr.substringBefore("T")
                    val isActive = data["is_active"] as? Boolean ?: false

                    if (startDateOnly == currentDate && !isActive) {
                        requiresUpdate = true
                        userService.updatedCustomerById(customerDoc.id, mapOf("is_active" to true))
                    }
                }

                val finalCustomerList = if (requiresUpdate) {
                    userService.getCustomers(supplierId)
                } else {
                    allCustomers
                }

                customerList = finalCustomerList.map {
                    val data = it.data
                    Customer(
                        id = it.id,
                        name = data["name"].toString(),
                        phone = data["phone_number"].toString(),
                        vacationMode = data["is_on_vacation"] as? Boolean ?: false,
                        deliveryTime = data["delivery_time"].toString(),
                        milkType = data["milk_type"].toString(),
                        isActive = data["is_active"] as? Boolean ?: false,
                        startDate = data["start_date"].toString(),
                        paddingAmount = data["padding_amount"].toString()
                    )
                }

                adapter = CustomerAdapter(customerList) { customerId, isOnVacation ->
                    progressBar.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val startTimeVaC = System.currentTimeMillis()
                        try {
                            userService.updatedCustomerById(customerId, mapOf("is_on_vacation" to isOnVacation))
                            if (isOnVacation) {
                                toastMsg("Customer marked as on vacation.", "warning")
                            } else {
                                toastMsg("Customer is now back to receiving deliveries.", "success")
                            }
                        } catch (e: Exception) {
                            toastMsg("Failed to update status: ${e.message ?: "Unknown error"}", "error")
                        } finally {
                            val elapsedTime = System.currentTimeMillis() - startTimeVaC
                            val delayTime = 1000 - elapsedTime
                            if (delayTime > 0) delay(delayTime)
                            progressBar.visibility = View.GONE
                        }
                    }
                }

                recyclerView.adapter = adapter

                updateToolbarTitle(customerList.size)

                updateListVisibility(customerList.size, "main")

                searchInput.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false
                    override fun onQueryTextChange(newText: String?): Boolean {
                        adapter.filter.filter(newText)

                        recyclerView.postDelayed({
                            val visibleCount = adapter.itemCount
                            updateListVisibility(visibleCount, "sub")
                        }, 100)

                        return true
                    }
                })

            } catch (e: Exception) {
                toastMsg(e.message ?: "An unknown error occurred", "error")
            } finally {
                val elapsedTime = System.currentTimeMillis() - startTime
                val delayTime = 1000 - elapsedTime
                if (delayTime > 0) delay(delayTime)
                progressBar.visibility = View.GONE
            }
        }

    }

    private fun updateToolbarTitle(count: Int) {
        val title = getString(R.string.app_name) + " - Customer List ($count)"
        (requireActivity() as? MainActivity)?.setMainToolbarTitle(title)
    }

    private fun updateListVisibility(count: Int, head: String) {
        if (count == 0 && head == "main") {
            recyclerView.visibility = View.GONE
            customerNotFound.visibility = View.VISIBLE
            nameNotFound.visibility = View.GONE
        } else if (count == 0 && head == "sub") {
            recyclerView.visibility = View.GONE
            nameNotFound.visibility = View.VISIBLE
            customerNotFound.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            customerNotFound.visibility = View.GONE
            nameNotFound.visibility = View.GONE

        }
    }

    private fun startUpdatingTime(): String {
        val dateFormat = android.icu.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
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
}
