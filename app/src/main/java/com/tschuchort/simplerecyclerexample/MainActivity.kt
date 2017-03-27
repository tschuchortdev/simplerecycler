package com.tschuchort.simplerecyclerexample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import butterknife.bindView
import com.tschuchort.simplerecycler.GenericAdapter
import com.tschuchort.simplerecycler.RecyclerItem
import com.tschuchort.simplerecycler.SimpleRecyclerView

class MainActivity : AppCompatActivity(), GenericAdapter.OnClickListener, GenericAdapter.OnLongClickListener {

	val recycler: SimpleRecyclerView by bindView(R.id.recycler)
	lateinit var adapter: GenericAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		recycler.layoutManager = LinearLayoutManager(this)

		adapter = GenericAdapter()
		adapter.items = arrayListOf(
				TestItem("1"),
				TestItem("2"),
				TestItem("3"),
				TestItem("4"),
				TestItem("5"),
				TestItem("6"),
				TestItem("7"),
				TestItem("8"),
				TestItem("9"),
				TestItem("10"),
				TestItem("11"),
				TestItem("12"),
				TestItem("13"),
				TestItem("14"),
				TestItem("15"),
				TestItem("16"),
				TestItem("17"),
				TestItem("18"),
				TestItem("19"),
				TestItem("20")
		)

		adapter.addOnClickListener(this)
		adapter.addOnLongClickListener(this)

		recycler.adapter = adapter
	}

	override fun onRecyclerItemClick(item: RecyclerItem) {
		(item as TestItem).text = (item.text.toInt() + 1).toString()
		adapter.notifyItemChanged(item.adapterPosition!!)
	}

	override fun onRecyclerItemLongClick(item: RecyclerItem): Boolean {
		adapter.items.remove(item)
		adapter.notifyItemRemoved(item.adapterPosition!!)
		return true
	}
}
