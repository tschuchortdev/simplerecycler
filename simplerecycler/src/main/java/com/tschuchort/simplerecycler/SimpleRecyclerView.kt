package com.tschuchort.simplerecycler

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import java.util.*


open class SimpleRecyclerView
	@JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
	: RecyclerView(ctx, attrs, defStyleAttr) {

	init {
		itemAnimator = A(DefaultItemAnimator())
	}

	override fun setItemAnimator(animator: ItemAnimator?) {
		if (animator != null)
			super.setItemAnimator(A(animator))
		else
			super.setItemAnimator(animator)
	}

	override fun getItemAnimator(): ItemAnimator?
			= (super.getItemAnimator() as? ItemAnimatorChangeDecorator)?.decorated


	private inner class A(animator: ItemAnimator) : ItemAnimatorChangeDecorator(animator) {
		// contains only animations that haven't ended yet
		// they might be running, paused or pending/not started yet
		private var changeAnimations: HashMap<RecyclerItem.ViewHolder, Animator> = HashMap()

		// called when one of our custom change animations stopped (ended or cancelled)
		fun onChangeAnimationStopped(holder: ViewHolder) {
			changeAnimations.remove(holder)
			dispatchAnimationFinished(holder)
		}

		override fun animateChange(oldHolder: ViewHolder, newHolder: ViewHolder,
								   preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo): Boolean {
			if (oldHolder == newHolder
				&& oldHolder is RecyclerItem.ViewHolder
				&& canHolderAnimateChanges(oldHolder)) {

				val oldItem = oldHolder.item ?: throw IllegalStateException("can't animate change for view holder that wasn't bound")
				val newItem = (adapter as GenericAdapter).items[newHolder.adapterPosition]

				val animation = oldItem.animateChange(newItem)

				animation.addListener(object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(`_`: Animator?) {
						onChangeAnimationStopped(oldHolder)
					}

					override fun onAnimationCancel(`_`: Animator?) {
						onChangeAnimationStopped(oldHolder)
					}
				})

				//changeDuration = 2000L
				//animation.duration = changeDuration

				// if there is already an animation running, schedule the new one to run afterwards
				// if there is an animation pending, replace it with the new one
				val oldAnim = changeAnimations[oldHolder]
				if(oldAnim != null) {
					oldAnim.removeAllListeners()

					if(oldAnim.isRunning) {
						try {
							changeAnimations.put(oldHolder, oldItem.animateChange(newItem, oldAnim))
						}
						catch(e: NotImplementedError) {
							// TODO: continue old animation and run new one afterwards
						}
					}
					else
						changeAnimations.put(oldHolder, animation)
				}
				else
					changeAnimations.put(oldHolder, animation)

				return true
			}
			else
				return super.animateChange(oldHolder, newHolder, preLayoutInfo, postLayoutInfo)
		}

		override fun runPendingAnimations() {
			super.runPendingAnimations()

			changeAnimations
					.filter { !it.value.isStarted }
					.forEach {
						it.value.start()
						dispatchAnimationStarted(it.key)
					}

			dispatchChangeAnimationsFinished()
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

		override fun setChangeDuration(newChangeDuration: Long) {
			super.setChangeDuration(newChangeDuration)

			changeAnimations
					.map { it.value }
					.forEach { it.duration = changeDuration }
		}

		private fun canHolderAnimateChanges(holder: RecyclerItem.ViewHolder): Boolean {
			if(holder.item != null)
				return holder.item!!.canAnimateChanges((adapter as GenericAdapter).items[holder.adapterPosition])
			else
				throw IllegalStateException("holder has not been bound to an item")
		}

	}
}
