package com.tschuchort.simplerecycler

import android.animation.Animator
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View


abstract class RecyclerItem {
	protected var holder: ViewHolder? = null

	val layoutPosition: Int?
		get() = holder?.layoutPosition

	val adapterPosition: Int?
		get() = holder?.adapterPosition

	// unique id to identify this item and be able use stable ids in the adapter
	val stableId = --stableIdCounter

	companion object {
		// we assign each item a unique negative id so the adapter can identify it and use stable ids
		// Long is more than big enough so we don't have to worry about unused ids when an item is removed from
		// the adapter
		var stableIdCounter: Long = 0
	}

	/**
	 * return a function that creates your view but doesn't initialize it. Do initialization
	 * in bindViewHolder
	 */
	abstract val viewHolderFactory: ViewHolderFactory

	/**
	 * initialize the view to an item specific state (icon, title etc.)
	 */
	abstract protected fun bindViewHolder(holder: ViewHolder)

	/**
	 *  do any deinitialization before the holder gets unbound
	 */
	open protected fun unbindViewHolder(holder: ViewHolder) {
		//intentionally empty
	}

	/**
	 * called when the viewholder could not be recycled because of transient state, for example some animation
	 * that is still running.
	 *
	 * @return: true if you have removed the transient state and the holder can now be recycled
	 */
	open fun onFailedToRecycleView(holder: ViewHolder): Boolean {
		Log.e("RecyclerItem", "did not handle onFailedToRecycleView")
		return false
	}

	/**
	 * IMPORTANT: implement this method or the diffing algorithm won't work
	 */
	abstract override fun equals(other: Any?): Boolean


	/**
	 * IMPORTANT: implement this method or the diffing algorithm won't work
	 */
	abstract override fun hashCode(): Int

	/**
	 * override this method to return true if you want to animate changes inside the ViewHolder
	 * with animateChange()
	 */
	open fun canAnimateChanges(other: RecyclerItem) = false

	/**
	 *  this method gets called when we want to assign new data to this ViewHolder
	 */
	open fun animateChange(other: RecyclerItem): Animator = throw NotImplementedError(
			"You must override RecyclerItem.animateChange() if you want to use custom change " +
			"animations and return true in RecyclerItem.canAnimateChanges()")

	open fun animateChange(other: RecyclerItem, interruptedAnimation: Animator? = null): Animator
			= throw NotImplementedError("implement animateChange(RecyclerItem, Animator) to deal with interrupted animations")

	/**
	 * get a unique identifier for the item type/class that can be used by the decoratedAdapter
	 * @return unique identifier of the item type/class
	 */
	val viewTypeId = javaClass.hashCode() //hashCode isn't guaranteed to be unique but with only so few item types, collision is very unlikely (1% for 10k objects)

	abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		var item: RecyclerItem? = null
			protected set

		val isBound get() = item != null

		var onClickListener: ((View) -> Unit)? = null
			set(value) {
				field = value
				itemView.setOnClickListener(value)
			}

		var onLongClickListener: ((View) -> Boolean)? = null
			set(value) {
				field = value
				itemView.setOnLongClickListener(value)
			}

		fun bind(item: RecyclerItem) {
			//if(this.item == null) {
				this.item = item
				item.holder = this
				item.bindViewHolder(this)
			//}
			//else {
			//	throw IllegalStateException("can not bind already bound ViewHolder")
			//}
		}

		fun unbind() {
			if(item != null) {
				item?.unbindViewHolder(this)
				onClickListener = null
				onLongClickListener = null
				item?.holder = null
				item = null
			}
			else {
				throw IllegalStateException("can not unbind already unbound ViewHolder")
			}
		}
	}
}




