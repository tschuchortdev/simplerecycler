package com.tschuchort.simplerecycler

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.*

typealias ResourceId = Int
typealias ViewHolderFactory = ((ResourceId) -> View) -> RecyclerItem.ViewHolder

/**
 * generic RecyclerView adapter that can accept all types of Items and handle clicks
 */
open class GenericAdapter
	@JvmOverloads constructor(items: MutableList<RecyclerItem> = ArrayList()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val mViewHolderFactories = SparseArray<ViewHolderFactory>() //map factories for creating an appropriate viewholder to the item types

	private val mClickListeners = ArrayList<OnClickListener>()
	private val mLongClickListeners = ArrayList<OnLongClickListener>()

	//TODO this leak
	private val diffDataDelegate: RecyclerNotifyDataChangedDelegate = RecyclerAdapterDiffDelegate(this)

	var items = items
		set(value) {
			val oldItems = items
			field = value
			saveViewHolderFactoriesOf(items)
			diffDataDelegate.onDataChanged(oldItems, items)
		}

	init {
		setHasStableIds(true)
	}

	/**
	 * make function final to prevent this-leak in ctor
	 */
	final override fun setHasStableIds(hasStableIds: Boolean) = super.setHasStableIds(hasStableIds)

	fun saveViewHolderFactoriesOf(newItems: List<RecyclerItem>) {
		newItems.filter { !mViewHolderFactories.containsKey(it.viewTypeId) }
				.forEach { mViewHolderFactories.put(it.viewTypeId, it.viewHolderFactory) }
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val holderFactory = mViewHolderFactories[viewType]

		val holder = holderFactory { resId: ResourceId -> LayoutInflater.from(parent.context).inflate(resId, parent, false) }

		return holder
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		if (holder is RecyclerItem.ViewHolder) {
			holder.bind(items[position])
			onViewHolderBound(holder)

			holder.onClickListener = { mClickListeners.forEach { it.onRecyclerItemClick(holder.item!!) } }

			holder.onLongClickListener = {
				mLongClickListeners.fold(false) { acc, listener ->
					acc || listener.onRecyclerItemLongClick(holder.item!!)
				}
			}
		}
		else
			throw IllegalStateException("onBindViewHolder called with ViewHolder of unknown type")
	}

	/**
	 * override to be notified after a ViewHolder has been bound
	 */
	open protected fun onViewHolderBound(holder: RecyclerItem.ViewHolder) {
		// intentionally empty
	}

	override fun onViewRecycled(holder: RecyclerView.ViewHolder?) {
		if (holder is RecyclerItem.ViewHolder) {
			holder.unbind()
			onViewHolderUnbound(holder)
		}
		else {
			// somehow the ViewHolder is not from one of our items. Maybe a decorator was implemented wrongly
			// this should never happen, but we can be forgiving and handle it anyway
			Log.w("GenericAdapter", "unknown ViewHolder was detached")
			super.onViewDetachedFromWindow(holder)
		}

		// unbind must be called before super clears ViewHolder's internal data
		super.onViewRecycled(holder)
	}

	/**
	 * override to be notified after a ViewHolder has been unbound and is about to be recycled
	 */
	open protected fun onViewHolderUnbound(holder: RecyclerItem.ViewHolder) {
		// intentionally empty
	}

	override fun getItemCount() = items.size

	fun add(newItem: RecyclerItem) = insertAt(newItem, itemCount)

	fun add(newItems: List<RecyclerItem>) = insertAt(newItems, itemCount)

	fun insertAt(newItems: List<RecyclerItem>, position: Int) {
		for (i in newItems.indices)
			items.add(position + i, newItems[i])

		saveViewHolderFactoriesOf(newItems)
		notifyItemRangeInserted(position, position + newItems.size)
	}

	fun insertAt(newItem: RecyclerItem, position: Int) {
		items.add(position, newItem)
		saveViewHolderFactoriesOf(listOf(newItem))
		notifyItemInserted(position)
	}

	fun remove(oldItem: RecyclerItem) = removeAt(items.indexOf(oldItem))

	fun removeAt(position: Int) {
		items.removeAt(position)
		notifyItemRemoved(position)
	}

	/**
	 * @start: first element index to be removed
	 * @end: last element index to be removed
	 */
	fun removeRange(start: Int, end: Int) {
		for (i in start..end)
			items.removeAt(i)

		notifyItemRangeRemoved(start, end - start + 1)
	}

	override fun getItemViewType(position: Int) = items[position].viewTypeId

	// override getItemId to use stable ids
	override fun getItemId(position: Int): Long {
		try {
			return items[position].stableId
		}
		catch(e: IndexOutOfBoundsException) {
			throw IllegalArgumentException("could not get stable id for item: position out of bounds")
		}
	}

	fun addOnClickListener(listener: OnClickListener) = mClickListeners.add(listener)

	fun removeOnClickListener(listener: OnClickListener) = mClickListeners.remove(listener)

	fun addOnLongClickListener(listener: OnLongClickListener) = mLongClickListeners.add(listener)

	fun removeOnLongClickListener(listener: OnLongClickListener) = mLongClickListeners.remove(listener)

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		super.onDetachedFromRecyclerView(recyclerView)

		//stop all background tasks or they might try to access unavailable resources
		diffDataDelegate.stopBackgroundTasks()
	}

	@FunctionalInterface
	interface OnClickListener {
		fun onRecyclerItemClick(item: RecyclerItem)
	}

	@FunctionalInterface
	interface OnLongClickListener {
		fun onRecyclerItemLongClick(item: RecyclerItem): Boolean
	}
}

private fun <T> SparseArray<T>.containsKey(key: Int) = get(key) != null