package vasu.apps.milkflow.Adapter

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import vasu.apps.milkflow.Model.DailySell
import vasu.apps.milkflow.R

class DailySellAdapter(
    private var originalItems: MutableList<DailySell>, private var time: String
) : RecyclerView.Adapter<DailySellAdapter.ViewHolder>(), Filterable {

    private var items: List<DailySell> = originalItems.toList()
    internal var onItemCheckedChange: (() -> Unit)? = null
    internal var onDataChange: (() -> Unit)? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.rv_daily_sell_check_box)
        val name: TextView = view.findViewById(R.id.rv_daily_sell_name)
        val milkType: TextView = view.findViewById(R.id.rv_daily_sell_milk_type)
        val cowMilkQuantity: EditText =
            (view.findViewById<TextInputLayout>(R.id.rv_daily_sell_cow_milk_litters)).editText!!
        val cowMilkPrice: TextView = view.findViewById(R.id.rv_daily_sell_cow_milk_price)
        val cowLayout: LinearLayout = view.findViewById(R.id.rv_daily_sell_cow_edit_ll)
        val buffaloMilkQuantity: EditText =
            (view.findViewById<TextInputLayout>(R.id.rv_daily_sell_buffalo_milk_litters)).editText!!
        val buffaloMilkPrice: TextView = view.findViewById(R.id.rv_daily_sell_buffalo_milk_price)

        var cowWatcher: TextWatcher? = null
        var buffaloWatcher: TextWatcher? = null
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_daily_sell, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    @SuppressLint("DefaultLocale", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name

        holder.cowMilkQuantity.removeTextChangedListener(holder.cowWatcher)
        holder.buffaloMilkQuantity.removeTextChangedListener(holder.buffaloWatcher)

        when (item.milkType) {
            "cow" -> {
                holder.cowLayout.visibility = View.VISIBLE
                (holder.cowLayout.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
                holder.cowMilkQuantity.visibility = View.VISIBLE
                holder.cowMilkPrice.visibility = View.VISIBLE
                holder.buffaloMilkQuantity.visibility = View.GONE
                holder.buffaloMilkPrice.visibility = View.GONE

                val cowQty =
                    if (time == "morning") item.morningCowMilkQty else item.eveningCowMilkQty
                holder.milkType.text = "Milk type : Cow"
                holder.cowMilkQuantity.setText(String.format("%.1f", cowQty))
                holder.cowMilkPrice.text = String.format("%.0f", cowQty!! * item.priceCowMilk!!)

                holder.cowWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val newQty = s?.toString()?.toDoubleOrNull() ?: 0.0
                        if (time == "morning") item.morningCowMilkQty = newQty
                        else item.eveningCowMilkQty = newQty
                        holder.cowMilkPrice.text = String.format("%.0f", newQty * item.priceCowMilk)
                        onDataChange?.invoke()
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }
                }
                holder.cowMilkQuantity.addTextChangedListener(holder.cowWatcher)
            }

            "buffalo" -> {
                holder.cowLayout.visibility = View.GONE
                holder.cowMilkQuantity.visibility = View.GONE
                holder.cowMilkPrice.visibility = View.GONE
                holder.buffaloMilkQuantity.visibility = View.VISIBLE
                holder.buffaloMilkPrice.visibility = View.VISIBLE

                val buffaloQty =
                    if (time == "morning") item.morningBuffaloMilkQty else item.eveningBuffaloMilkQty
                holder.milkType.text = "Milk type : Buffalo"
                holder.buffaloMilkQuantity.setText(String.format("%.1f", buffaloQty))
                holder.buffaloMilkPrice.text =
                    String.format("%.0f", buffaloQty!! * item.priceBuffaloMilk!!)

                holder.buffaloWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val newQty = s?.toString()?.toDoubleOrNull() ?: 0.0
                        if (time == "morning") item.morningBuffaloMilkQty = newQty
                        else item.eveningBuffaloMilkQty = newQty
                        holder.buffaloMilkPrice.text =
                            String.format("%.0f", newQty * item.priceBuffaloMilk)
                        onDataChange?.invoke()
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }
                }
                holder.buffaloMilkQuantity.addTextChangedListener(holder.buffaloWatcher)
            }

            "both" -> {
                val cowQty =
                    if (time == "morning") item.morningCowMilkQty else item.eveningCowMilkQty
                val buffaloQty =
                    if (time == "morning") item.morningBuffaloMilkQty else item.eveningBuffaloMilkQty

                if (cowQty != null && cowQty != 0.0) {
                    holder.cowMilkQuantity.visibility = View.VISIBLE
                    holder.cowLayout.visibility = View.VISIBLE
                    (holder.cowLayout.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
                    holder.cowMilkPrice.visibility = View.VISIBLE
                    holder.cowMilkQuantity.setText(String.format("%.1f", cowQty))
                    holder.cowMilkPrice.text = String.format("%.0f", cowQty * item.priceCowMilk!!)
                    holder.milkType.text = "Milk type : Cow"
                } else {
                    holder.cowMilkQuantity.visibility = View.GONE
                    holder.cowLayout.visibility = View.GONE
                    holder.cowMilkPrice.visibility = View.GONE
                    holder.cowMilkQuantity.setText("")
                    holder.cowMilkQuantity.clearFocus()
                    holder.cowMilkPrice.text = ""
                }

                if (buffaloQty != null && buffaloQty != 0.0) {
                    holder.buffaloMilkQuantity.visibility = View.VISIBLE
                    holder.buffaloMilkPrice.visibility = View.VISIBLE
                    holder.buffaloMilkQuantity.setText(String.format("%.1f", buffaloQty))
                    holder.buffaloMilkPrice.text =
                        String.format("%.0f", buffaloQty * item.priceBuffaloMilk!!)
                    holder.milkType.text = "Milk type : Buffalo"
                } else {
                    holder.buffaloMilkQuantity.visibility = View.GONE
                    holder.buffaloMilkPrice.visibility = View.GONE
                    holder.buffaloMilkQuantity.setText("")
                    holder.buffaloMilkQuantity.clearFocus()
                    holder.buffaloMilkPrice.text = ""
                }

                if (cowQty != null && cowQty != 0.0 && buffaloQty != null && buffaloQty != 0.0) {
                    (holder.cowLayout.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                        15
                    holder.milkType.text = "Milk type : ${item.milkType}"
                }

                holder.cowWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val newQty = s?.toString()?.toDoubleOrNull() ?: 0.0
                        if (time == "morning") item.morningCowMilkQty = newQty
                        else item.eveningCowMilkQty = newQty
                        holder.cowMilkPrice.text =
                            String.format("%.0f", newQty * (item.priceCowMilk ?: 0.0))
                        onDataChange?.invoke()
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }
                }

                holder.cowMilkQuantity.addTextChangedListener(holder.cowWatcher)

                holder.buffaloWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val newQty = s?.toString()?.toDoubleOrNull() ?: 0.0
                        if (time == "morning") item.morningBuffaloMilkQty = newQty
                        else item.eveningBuffaloMilkQty = newQty
                        holder.buffaloMilkPrice.text =
                            String.format("%.0f", newQty * (item.priceBuffaloMilk ?: 0.0))
                        onDataChange?.invoke()
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }
                }
                holder.buffaloMilkQuantity.addTextChangedListener(holder.buffaloWatcher)
            }
        }

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.isSelected

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            onItemCheckedChange?.invoke()
            onDataChange?.invoke()
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<DailySell>, time: String) {
        this.originalItems.clear()
        this.originalItems.addAll(newItems)
        this.items = originalItems.toList()
        this.time = time
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val filtered = if (query.isNullOrEmpty()) {
                    originalItems
                } else {
                    originalItems.filter {
                        it.name.contains(query, ignoreCase = true) || it.milkType.contains(
                            query, ignoreCase = true
                        )
                    }
                }

                return FilterResults().apply { values = filtered }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(query: CharSequence?, results: FilterResults?) {
                val filteredList = if (results?.values is List<*>) {
                    (results.values as List<*>).filterIsInstance<DailySell>()
                } else {
                    listOf()
                }
                items = filteredList
                notifyDataSetChanged()
            }
        }
    }

    fun selectAll() {
        originalItems.forEach { it.isSelected = true }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        originalItems.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    fun currentItems(): List<DailySell> = items

    fun allSelectedItems(): List<DailySell> {
        return originalItems.filter { it.isSelected }
    }

}
