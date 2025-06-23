package vasu.apps.milkflow.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SearchView
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
import vasu.apps.datepickerwheel.OnDateSelected
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
    private var dailySellService = Appwrite.dailySellService

    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: SearchView
    private lateinit var customerNotFound: TextView
    private lateinit var nameNotFound: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var selectAllCustomer: CheckBox
    private lateinit var totalPrice: TextView
    private lateinit var totalLitter: TextView
    private lateinit var addData: AppCompatButton
    private lateinit var totalLayout: LinearLayout
    private lateinit var datePicker: DatePickerWheel
    private lateinit var adapter: DailySellAdapter
    private lateinit var progressBar: ProgressBar

    private var time = "morning"
    private var allCustomersList: MutableList<DailySell> = mutableListOf()
    private var selectedDateTime: String? = null
    private var supplierId: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_daily_sell, container, false)

        recyclerView = rootView.findViewById(R.id.daily_sell_recycler_view)
        searchInput = rootView.findViewById(R.id.daily_sell_search_input)
        customerNotFound = rootView.findViewById(R.id.daily_sell_not_found)
        nameNotFound = rootView.findViewById(R.id.daily_sell_search_not_found)
        radioGroup = rootView.findViewById(R.id.daily_sell_time)
        selectAllCustomer = rootView.findViewById(R.id.daily_sell_select_all_customer)
        totalPrice = rootView.findViewById(R.id.daily_sell_total_price)
        totalLitter = rootView.findViewById(R.id.daily_sell_total_litters)
        addData = rootView.findViewById(R.id.daily_sell_add_data)
        totalLayout = rootView.findViewById(R.id.daily_sell_bottom_container)
        datePicker = rootView.findViewById(R.id.daily_sell_date_picker)
        progressBar = rootView.findViewById(R.id.daily_sell_progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DailySellAdapter(mutableListOf(), time)
        recyclerView.adapter = adapter

        adapter.onDataChange = { updateTotal() }

        totalLitter.text = getString(R.string._0_0_Ltr)
        totalPrice.text = getString(R.string._0)

        adapter.onItemCheckedChange = {
            val allChecked = adapter.currentItems().all { it.isSelected }
            if (selectAllCustomer.isChecked != allChecked) {
                selectAllCustomer.setOnCheckedChangeListener(null)
                selectAllCustomer.isChecked = allChecked
                restoreSelectAllListener()
            }
            updateTotal()
        }


        selectAllCustomer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) adapter.selectAll() else adapter.deselectAll()
            updateTotal()
        }

        val today = Calendar.getInstance()
        datePicker.setActiveDate(today)
        selectedDateTime = datePicker.getSelectedDateNumber()

        datePicker.setOnDateSelectedListener(object : OnDateSelected {
            override fun onDateSelected(year: Int, month: Int, day: Int, dayOfWeek: Int) {
                selectedDateTime = datePicker.getSelectedDateNumber()
            }

            override fun onDisabledDateSelected(
                year: Int, month: Int, day: Int, dayOfWeek: Int, todayDate: Boolean
            ) {
            }
        })

        radioGroup.check(R.id.daily_sell_morning)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            time = when (checkedId) {
                R.id.daily_sell_morning -> "morning"
                R.id.daily_sell_evening -> "evening"
                else -> "evening"
            }

            selectAllCustomer.setOnCheckedChangeListener(null)
            selectAllCustomer.isChecked = false
            restoreSelectAllListener()
            adapter.deselectAll()

            totalLitter.text = getString(R.string._0_0_Ltr)
            totalPrice.text = getString(R.string._0)

            filterAndDisplayData()
        }

        fetchData()

        addData.setOnClickListener { storeSelectedRecords() }

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
                supplierId = supplierUser?.id ?: return@launch

                val supplierData = userService.getUserDocument(supplierUser.id)

                val cowMilkPrice = (supplierData?.get("price_cow_milk") as? Number)?.toDouble()
                val buffaloMilkPrice =
                    (supplierData?.get("price_buffalo_milk") as? Number)?.toDouble()

                val allCustomers = customerService.getCustomers(supplierId.toString())

                allCustomersList = allCustomers.mapNotNull { document ->
                    val data = document.data
                    val isOnVacation = data["is_on_vacation"] as? Boolean == true
                    val isActive = data["is_active"] as? Boolean == false

                    if (isOnVacation) return@mapNotNull null
                    if (isActive) return@mapNotNull null

                    DailySell(
                        idSupplier = supplierId.toString(),
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

    private fun storeSelectedRecords() {
        progressBar.visibility = View.VISIBLE

        val selectedItems = adapter.allSelectedItems()
        val date = selectedDateTime ?: return

        if (selectedItems.isEmpty()) {
            toastMsg("No customers selected", "warning")
            progressBar.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            var count = 0
            try {
                for (item in selectedItems) {
                    val cowQty =
                        if (time == "morning") item.morningCowMilkQty else item.eveningCowMilkQty
                    val buffaloQty =
                        if (time == "morning") item.morningBuffaloMilkQty else item.eveningBuffaloMilkQty

                    val data = mutableMapOf<String, Any>(
                        "supplier_id" to supplierId!!,
                        "consumer_id" to item.id,
                        "time_slot" to time,
                        "date" to date,
                        "is_delivered" to true,
                    )

                    if (cowQty != null && cowQty >= 1.0) {
                        data["cow_milk_qty"] = cowQty
                    }
                    if (buffaloQty != null && buffaloQty >= 1.0) {
                        data["buffalo_milk_qty"] = buffaloQty
                    }

                    withContext(Dispatchers.IO) {
                        dailySellService.createDeliveryRecord(data)
                    }

                    count++
                }

                if (count > 0) {
                    toastMsg("Saved $count delivery record(s) successfully!", "success")
                } else {
                    toastMsg("No valid milk entries found to save.", "warning")
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                if (e.message?.contains("Document with the requested ID already exists") == true) {
                    toastMsg("Selected customer(s) already have today's data added.", "warning")
                } else {
                    toastMsg("Failed to save data: ${e.message}", "error")
                }
            } finally {
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

    private fun updateTotal() {
        val selectedItems = adapter.allSelectedItems()

        var totalLiters = 0.0
        var totalPrices = 0.0

        for (item in selectedItems) {
            val cowQty = if (time == "morning") item.morningCowMilkQty else item.eveningCowMilkQty
            val buffaloQty =
                if (time == "morning") item.morningBuffaloMilkQty else item.eveningBuffaloMilkQty

            totalLiters += (cowQty ?: 0.0) + (buffaloQty ?: 0.0)
            totalPrices += (cowQty ?: 0.0) * (item.priceCowMilk ?: 0.0)
            totalPrices += (buffaloQty ?: 0.0) * (item.priceBuffaloMilk ?: 0.0)
        }

        totalLitter.text = String.format("%.1f Ltr", totalLiters)
        totalPrice.text = String.format("%.0f", totalPrices)
    }

    private fun updateToolbarTitle(count: String) {
        val title = getString(R.string.app_name) + " - Daily Sell ($count)"
        (requireActivity() as? MainActivity)?.setMainToolbarTitle(title)
    }

    private fun restoreSelectAllListener() {
        selectAllCustomer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) adapter.selectAll() else adapter.deselectAll()
            updateTotal()
        }
    }

    private fun updateListVisibility(count: Int, head: String) {
        if (count == 0 && head == "main") {
            recyclerView.visibility = View.GONE
            customerNotFound.visibility = View.VISIBLE
            nameNotFound.visibility = View.GONE
            totalLayout.visibility = View.GONE
            addData.visibility = View.GONE
        } else if (count == 0 && head == "sub") {
            recyclerView.visibility = View.GONE
            nameNotFound.visibility = View.VISIBLE
            customerNotFound.visibility = View.GONE
            totalLayout.visibility = View.GONE
            addData.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            customerNotFound.visibility = View.GONE
            nameNotFound.visibility = View.GONE
            totalLayout.visibility = View.VISIBLE
            addData.visibility = View.VISIBLE
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
