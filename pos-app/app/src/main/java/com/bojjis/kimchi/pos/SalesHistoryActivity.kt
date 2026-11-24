package com.bojjis.kimchi.pos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bojjis.kimchi.pos.data.Sale
import com.bojjis.kimchi.pos.data.SaleRepository
import com.bojjis.kimchi.pos.databinding.ActivitySalesHistoryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SalesHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesHistoryBinding
    private lateinit var repo: SaleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = SaleRepository(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnClear.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.clear) { _, _ ->
                    repo.clearSales()
                    render()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun render() {
        val sales = repo.loadSales().sortedByDescending { it.timestamp }
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val today = sales.filter { it.timestamp >= todayStart }
        val todayTotal = today.sumOf { it.total }
        val todayCash = today.filter { it.paymentMethod.name == "CASH" }.sumOf { it.total }
        val todayGcash = today.filter { it.paymentMethod.name == "GCASH" }.sumOf { it.total }
        val allTime = sales.sumOf { it.total }

        binding.statTodayTotal.text = getString(R.string.total_fmt, todayTotal)
        binding.statTodayCash.text = getString(R.string.total_fmt, todayCash)
        binding.statTodayGcash.text = getString(R.string.total_fmt, todayGcash)
        binding.statAllTime.text = getString(R.string.total_fmt, allTime)
        binding.statCount.text = sales.size.toString()

        binding.emptyState.visibility = if (sales.isEmpty()) View.VISIBLE else View.GONE
        binding.salesList.visibility = if (sales.isEmpty()) View.GONE else View.VISIBLE
        binding.salesList.layoutManager = LinearLayoutManager(this)
        binding.salesList.adapter = SaleAdapter(sales)
    }

    class SaleAdapter(private val items: List<Sale>) :
        RecyclerView.Adapter<SaleAdapter.VH>() {

        private val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

        class VH(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sale, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val sale = items[position]
            val root = holder.view
            root.findViewById<TextView>(R.id.saleHeader).text =
                "${sale.paymentMethod.label} · ${fmt.format(Date(sale.timestamp))}"
            root.findViewById<TextView>(R.id.saleItems).text =
                sale.lines.joinToString("\n") { "- ${it.summary()} ×${it.qty}" }
            root.findViewById<TextView>(R.id.saleTotal).text =
                root.context.getString(R.string.total_fmt, sale.total)
            val extra = buildString {
                sale.cashTendered?.let {
                    append(root.context.getString(R.string.tendered_fmt, it))
                }
                sale.changeDue?.let {
                    if (isNotEmpty()) append("  ·  ")
                    append(root.context.getString(R.string.change_fmt, it))
                }
            }
            root.findViewById<TextView>(R.id.saleExtra).apply {
                text = extra
                visibility = if (extra.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
}
