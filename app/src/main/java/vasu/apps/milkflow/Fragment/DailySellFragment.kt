package vasu.apps.milkflow.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.datepickerwheel.DatePickerWheel
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.Adapter.DailySellAdapter
import vasu.apps.milkflow.Model.DailySell
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite
import java.util.Calendar

class DailySellFragment : Fragment() {

    private var accountService = Appwrite.accountService
    private var userService = Appwrite.userService
    private var customerService = Appwrite.customerService

    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: SearchView
    private lateinit var customerNotFound: TextView
    private lateinit var nameNotFound: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var datePicker: DatePickerWheel
    private lateinit var adapter: DailySellAdapter
    private lateinit var progressBar: ProgressBar

    private var time = "morning"
    private var allCustomersList: MutableList<DailySell> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_daily_sell, container, false)

        recyclerView = rootView.findViewById(R.id.daily_sell_recycler_view)
        searchInput = rootView.findViewById(R.id.daily_sell_search_input)
        customerNotFound = rootView.findViewById(R.id.daily_sell_not_found)
        nameNotFound = rootView.findViewById(R.id.daily_sell_search_not_found)
        radioGroup = rootView.findViewById(R.id.daily_sell_time)
        datePicker = rootView.findViewById(R.id.daily_sell_date_picker)
        progressBar = rootView.findViewById(R.id.daily_sell_progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DailySellAdapter(mutableListOf(), time)

        recyclerView.adapter = adapter

        val today = Calendar.getInstance()
        datePicker.setActiveDate(today)

        radioGroup.check(R.id.daily_sell_morning)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            time = when (checkedId) {
                R.id.daily_sell_morning -> "morning"
                R.id.daily_sell_evening -> "evening"
                else -> "evening"
            }
            filterAndDisplayData()
        }

        fetchData()

        return rootView
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            try {
                val supplierUser = withContext(Dispatchers.IO) {
                    accountService.getLoggedIn()
                }
                val supplierId = supplierUser?.id ?: return@launch

                val supplierData = userService.getUserDocument(supplierUser.id)

                val cowMilkPrice = (supplierData?.get("price_cow_milk") as? Number)?.toDouble()
                val buffaloMilkPrice =
                    (supplierData?.get("price_buffalo_milk") as? Number)?.toDouble()

                val allCustomers = customerService.getCustomers(supplierId)

                allCustomersList = allCustomers.mapNotNull { document ->
                    val data = document.data
                    val isOnVacation = data["is_on_vacation"] as? Boolean == true

                    if (isOnVacation) return@mapNotNull null

                    DailySell(
                        idSupplier = supplierId,
                        id = document.id,
                        name = data["name"].toString(),
                        vacationMode = data["is_on_vacation"] as? Boolean == true,
                        deliveryTime = data["delivery_time"].toString(),
                        milkType = data["milk_type"].toString(),
                        morningCowMilkQty = (data["morning_cow_milk_qty"] as? Number)?.toDouble(),
                        eveningCowMilkQty = (data["evening_cow_milk_qty"] as? Number)?.toDouble(),
                        morningBuffaloMilkQty = (data["morning_buffalo_milk_qty"] as? Number)?.toDouble(),
                        eveningBuffaloMilkQty = (data["evening_buffalo_milk_qty"] as? Number)?.toDouble(),
                        priceCowMilk = cowMilkPrice,
                        priceBuffaloMilk = buffaloMilkPrice
                    )
                }.toMutableList()

                filterAndDisplayData()

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

    private fun filterAndDisplayData() {

        searchInput.setQuery("", false)

        val filteredList = allCustomersList.filter {
            it.deliveryTime == time || it.deliveryTime == "both"
        }

        adapter.updateList(filteredList, time)
        updateToolbarTitle(filteredList.size.toString())
        updateListVisibility(filteredList.size, "main")
    }

    private fun updateToolbarTitle(count: String) {
        val title = getString(R.string.app_name) + " - Daily Sell ($count)"
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
