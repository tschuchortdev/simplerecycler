package com.tschuchort.simplerecycler

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup

/**
 *  RecyclerItem base class for items with xml layout that creates the ViewHolder automatically
 */
abstract class XmlRecyclerItem : RecyclerItem() {
	abstract val layoutId: ResourceId

	final override val viewHolderFactory: ViewHolderFactory
		= { inflate -> AutoViewHolder(inflate(layoutId)) }

	abstract fun bindViewHolder(holder: AutoViewHolder)

	open fun unbindViewHolder(holder: AutoViewHolder) {
		// intentionally empty
	}

	final override fun bindViewHolder(holder: ViewHolder) = bindViewHolder(holder as AutoViewHolder)

	final override fun unbindViewHolder(holder: ViewHolder) {
		super.unbindViewHolder(holder)
		unbindViewHolder(holder as AutoViewHolder)
	}
}

/**
 * Automatic ViewHolder that indexes the view's children for faster retrieval with findViewById
 */
class AutoViewHolder(val view: View) : RecyclerItem.ViewHolder(view) {
	private var childViews = SparseArray<View>() //includes grand children

	init {
		holdViews(itemView)
	}

	private fun holdViews(itemView: View) {
		if (itemView is ViewGroup) {
			itemView.children
					.onEach { child -> childViews.put(child.id, child) }
					.filter { it is ViewGroup }
					.forEach { holdViews(it) }
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : View> findViewById(id: Int): T? = childViews.get(id) as? T
}