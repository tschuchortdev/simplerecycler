package com.tschuchort.simplerecycler

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.RecyclerView.ItemAnimator
import java.util.*

class ItemChangeAnimationDelegator(private val adapter : GenericAdapter, animator: ItemAnimator) : ItemAnimatorDecorator(animator) {
	// contains only animations that haven't ended yet
	// they might be running, paused or pending/not started yet
	private var changeAnimations = HashMap<RecyclerItem.ViewHolder, Animator>()

	override fun getChangeDuration()
			= changeAnimations.map { it.value.duration }.max() ?: super.getChangeDuration()

	// called when one of our custom change animations stopped (ended or cancelled)
	fun onChangeAnimationStopped(holder: ViewHolder) {
		changeAnimations.remove(holder)
		dispatchAnimationFinished(holder)

		if(changeAnimations.isEmpty())
			dispatchChangeAnimationsFinished()
	}

	override fun animateChange(oldHolder: ViewHolder, newHolder: ViewHolder,
							   preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo): Boolean {
		if (oldHolder == newHolder
			&& oldHolder is RecyclerItem.ViewHolder
			&& canHolderAnimateChanges(oldHolder)) {

			val oldItem = oldHolder.item ?: throw IllegalStateException("can't animate change for view holder that wasn't bound")
			val newItem = adapter.items[newHolder.adapterPosition]
			var currentAnim: Animator

			// if there is already an animation running, ask Item to handle it
			// or schedule the new one to run afterwards
			// if there is an animation pending, replace it with the new one
			val oldAnim = changeAnimations[oldHolder]
			oldAnim?.removeAllListeners()

			if(oldAnim?.isRunning == true) {
				try {
					currentAnim = oldItem.animateChange(newItem, oldAnim)
					oldAnim.cancel()
					changeAnimations[oldHolder] = currentAnim
				}
				catch(e: NotImplementedError) {
					currentAnim = oldItem.animateChange(newItem)

					oldAnim.addOnEndListener {
						currentAnim.start()
						changeAnimations[oldHolder] = currentAnim
					}
				}
			}
			else {
				currentAnim = oldItem.animateChange(newItem)
				changeAnimations[oldHolder] = currentAnim
			}

			currentAnim.addOnEndListener { onChangeAnimationStopped(oldHolder) }
			return true
		}
		else
			return super.animateChange(oldHolder, newHolder, preLayoutInfo, postLayoutInfo)
	}

	override fun runPendingAnimations() {
		super.runPendingAnimations()

		changeAnimations
				.filter { (_, animator) -> !animator.isStarted }
				.forEach { (holder, animator) ->
					if(disappearAnimationsScheduled)
						animator.startDelay = removeDuration

					animator.start()
					dispatchAnimationStarted(holder)
				}
	}

	override fun endAnimation(item: ViewHolder) {
		super.endAnimation(item)
		changeAnimations[item]?.end()
	}

	override fun endAnimations() {
		super.endAnimations()
		changeAnimations.map { it.value }.forEach(Animator::end)
	}

	override fun isRunning()
			= changeAnimations
					  .map { it.value }
					  .fold(false) { result, it -> result || it.isRunning }
			  || super.isRunning()

	override fun canReuseUpdatedViewHolder(holder: ViewHolder, payloads: MutableList<Any>)
			= canHolderAnimateChanges(holder as RecyclerItem.ViewHolder) || super.canReuseUpdatedViewHolder(holder, payloads)

	private fun canHolderAnimateChanges(holder: RecyclerItem.ViewHolder): Boolean {
		if(holder.item != null)
			return holder.item!!.canAnimateChanges(adapter.items[holder.adapterPosition])
		else
			throw IllegalStateException("holder has not been bound to an item")
	}

}

fun Animator.addOnEndListener(listener: (Animator) -> Unit) {
	addListener(object : AnimatorListenerAdapter() {
		override fun onAnimationEnd(anim: Animator?) {
			listener(anim!!)
		}
	})
}

fun Animator.addOnCancelListener(listener: (Animator) -> Unit) {
	addListener(object : AnimatorListenerAdapter() {
		override fun onAnimationCancel(anim: Animator?) {
			listener(anim!!)
		}
	})
}