package com.tschuchort.simplerecycler

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

/**
 * Base class for implementing a RecyclerView adapter decorator
 *
 * Unfortunately adaptors are not interfaces, so they can't be easily delegated.
 * This class provides the necessary boiler plate code
 */
open class AdapterDecorator<VH : RecyclerView.ViewHolder>(private val decoratedAdapter: RecyclerView.Adapter<VH>) : RecyclerView.Adapter<VH>() {

	init {
		setHasStableIds(decoratedAdapter.hasStableIds())
	}

	// make setHasStableIds final to prevent leaking uninitialized this to subclasses
	final override fun setHasStableIds(hasStableIds: Boolean) {
		super.setHasStableIds(hasStableIds)
	}

	override fun getItemCount() = decoratedAdapter.itemCount

	override fun getItemId(position: Int) = decoratedAdapter.getItemId(position)

	override fun getItemViewType(position: Int) = decoratedAdapter.getItemViewType(position)


	override fun onAttachedToRecyclerView(recycler: RecyclerView?) {
		super.onAttachedToRecyclerView(recycler)
		decoratedAdapter.onAttachedToRecyclerView(recycler)
	}

	override fun onBindViewHolder(holder: VH, position: Int)
			= decoratedAdapter.onBindViewHolder(holder, position)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH
			= decoratedAdapter.onCreateViewHolder(parent, viewType)

	override fun onDetachedFromRecyclerView(recycler: RecyclerView?) {
		super.onDetachedFromRecyclerView(recycler)
		decoratedAdapter.onDetachedFromRecyclerView(recycler)
	}

	override fun onFailedToRecycleView(holder: VH?)
			= decoratedAdapter.onFailedToRecycleView(holder)

	override fun onViewAttachedToWindow(holder: VH?)
			= decoratedAdapter.onViewAttachedToWindow(holder)

	override fun onViewDetachedFromWindow(holder: VH?)
			= decoratedAdapter.onViewDetachedFromWindow(holder)

	override fun onViewRecycled(holder: VH?)
			= decoratedAdapter.onViewRecycled(holder)

	override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
		super.registerAdapterDataObserver(observer)
		decoratedAdapter.registerAdapterDataObserver(observer)
	}

	override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
		super.unregisterAdapterDataObserver(observer)
		decoratedAdapter.unregisterAdapterDataObserver(observer)
	}
}