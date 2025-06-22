package vasu.apps.milkflow.Adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vasu.apps.milkflow.Model.Customer
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite

class CustomerAdapter(
    private val customers: List<Customer>,
    private val onVacationSwitchChanged: (customerId: String, isOnVacation: Boolean) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>(), Filterable {

    private var filteredList = customers.toMutableList()

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.customer_name)
        val phone: TextView = itemView.findViewById(R.id.customer_phone)
        val startDate: TextView = itemView.findViewById(R.id.customer_starting_date)

        val moreOptionsButton: View = itemView.findViewById(R.id.more_options_button)

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val vacationSwitch: MaterialSwitch = itemView.findViewById(R.id.customer_vacation_switch)
        val vacationBadge: TextView = itemView.findViewById(R.id.vacation_badge)
        val activeBadge: TextView = itemView.findViewById(R.id.active_badge)
        val cardView: CardView = itemView.findViewById(R.id.customer_card)
        val paddingAmount: TextView = itemView.findViewById(R.id.customer_paddings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun getItemCount(): Int = filteredList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val context = holder.itemView.context
        val customer = filteredList[position]

        holder.name.text = customer.name
        holder.phone.text = customer.phone

        val startDateStr = customer.startDate
        val parts = startDateStr.substringBefore("T").split("-")
        val startDateOnly = "${parts[2]}-${parts[1]}-${parts[0]}"

        if (!customer.isActive) {
            holder.startDate.text = holder.itemView.context.getString(R.string.active_date) + startDateOnly
            holder.activeBadge.visibility = View.VISIBLE
            holder.vacationSwitch.visibility = View.GONE
        } else {
            holder.activeBadge.visibility = View.GONE
            holder.startDate.visibility = View.GONE
        }

        if (customer.paddingAmount == "null") {
            val paddingAmountText = "Padding : ₹ 0"
            holder.paddingAmount.text = paddingAmountText
        } else {
            val paddingAmountText = "Padding : ₹ " + customer.paddingAmount
            holder.paddingAmount.text = paddingAmountText
        }

        holder.vacationSwitch.setOnCheckedChangeListener(null)
        holder.vacationSwitch.isChecked = customer.vacationMode

        holder.name.setTextColor(
            context.getColor(if (customer.isActive) R.color.success else R.color.error)
        )

        holder.vacationSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d("Switch", "Toggled ${customer.name} to $isChecked")
            filteredList[position] = customer.copy(vacationMode = isChecked)
            onVacationSwitchChanged(customer.id, isChecked)
            if (isChecked) {
                holder.vacationBadge.visibility = View.VISIBLE
                holder.cardView.setCardBackgroundColor(context.getColor(R.color.vacation_bg))
            } else {
                holder.vacationBadge.visibility = View.GONE
                holder.cardView.setCardBackgroundColor(context.getColor(R.color.ToolBar))
            }
        }


        if (customer.vacationMode) {
            holder.vacationBadge.visibility = View.VISIBLE
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.vacation_bg))
        } else {
            holder.vacationBadge.visibility = View.GONE
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.ToolBar))
        }

        holder.phone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:${customer.phone}".toUri()
            }
            context.startActivity(intent)
        }

        holder.moreOptionsButton.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(context, holder.moreOptionsButton)
            popup.inflate(R.menu.customer_item_menu)

            try {
                val fields = popup.javaClass.declaredFields
                for (field in fields) {
                    if (field.name == "mPopup") {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(popup)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_edit -> {
                        Log.d("MenuAction", "Edit clicked for ${customer.name}")
                        true
                    }

                    R.id.menu_delete -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                Appwrite.customerService.deleteCustomer(customer.id)
                                DynamicToast.makeSuccess(context, "${customer.name.lowercase()} has been deleted successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                DynamicToast.makeError(context, e.message
                                    ?: "An unknown error occurred", Toast.LENGTH_LONG).show()
                            }
                        }
                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }


    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val filtered = if (query.isNullOrEmpty()) {
                    customers
                } else {
                    customers.filter {
                        it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
                    }
                }

                return FilterResults().apply { values = filtered }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(query: CharSequence?, results: FilterResults?) {
                val filtered = results?.values
                filteredList = if (filtered is List<*>) {
                    filtered.filterIsInstance<Customer>().toMutableList()
                } else {
                    mutableListOf()
                }
                notifyDataSetChanged()
            }

        }
    }
}
