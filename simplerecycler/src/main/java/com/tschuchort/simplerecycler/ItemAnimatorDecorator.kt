package com.tschuchort.simplerecycler

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ItemAnimator
import android.support.v7.widget.RecyclerView.ViewHolder
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.*

/**
 * base class for decorating ItemAnimators
 * currently only supports decorating change animations
 */
abstract class ItemAnimatorDecorator(val decorated: ItemAnimator) : ItemAnimator() {

	// list of holders that are currently being animated by the decorated ItemAnimator
	// they may be pending or running
	private val animatingHolders = object {
		val appearing = HashSet<ViewHolder>()
		val changing = HashSet<ViewHolder>()
		val disappearing = HashSet<ViewHolder>()
		val persisting = HashSet<ViewHolder>()
	}

	val appearAnimationsScheduled get() = animatingHolders.appearing.isNotEmpty()
	val changeAnimationsScheduled get() = animatingHolders.changing.isNotEmpty()
	val disappearAnimationsScheduled get() = animatingHolders.disappearing.isNotEmpty()
	val persistAnimationsScheduled get() = animatingHolders.persisting.isNotEmpty()

	init {
		decorated.setAnimationsFinishedListener { holder ->
			dispatchAnimationFinished(holder)

			//remove holder from the lists of currently animating holders
			animatingHolders.appearing.remove(holder)
			animatingHolders.changing.remove(holder)
			animatingHolders.disappearing.remove(holder)
			animatingHolders.persisting.remove(holder)
		}
	}

	final override fun animateAppearance(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {
		animatingHolders.appearing.add(viewHolder)
		return decorated.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo)
	}

	override fun animateChange(oldHolder: ViewHolder, newHolder: ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo): Boolean {
		animatingHolders.changing.add(oldHolder)
		return decorated.animateChange(oldHolder, newHolder, preLayoutInfo, postLayoutInfo)
	}

	final override fun animateDisappearance(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo?): Boolean {
		animatingHolders.disappearing.add(viewHolder)
		return decorated.animateDisappearance(viewHolder, preLayoutInfo, postLayoutInfo)
	}

	final override fun animatePersistence(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo): Boolean {
		animatingHolders.appearing.add(viewHolder)
		return decorated.animatePersistence(viewHolder, preLayoutInfo, postLayoutInfo)
	}

	final override fun canReuseUpdatedViewHolder(viewHolder: ViewHolder)
			= decorated.canReuseUpdatedViewHolder(viewHolder)

	override fun canReuseUpdatedViewHolder(holder: ViewHolder, payloads: MutableList<Any>)
			= decorated.canReuseUpdatedViewHolder(holder, payloads)

	override fun endAnimation(item: ViewHolder)
			= decorated.endAnimation(item)

	override fun endAnimations()
			= decorated.endAnimations()

	final override fun getAddDuration()
			= decorated.addDuration

	override fun getChangeDuration()
			= decorated.changeDuration

	final override fun getMoveDuration()
			= decorated.moveDuration

	final override fun getRemoveDuration()
			= decorated.removeDuration

	override fun isRunning()
			= decorated.isRunning

	override fun obtainHolderInfo(): ItemHolderInfo
			= decorated.obtainHolderInfo()

	/**
	 * will also be called for animations started by the decorated class
	 */
	override fun onAnimationFinished(viewHolder: ViewHolder) {
		// don't call the decorated function here, or it would be called twice. Because it is already being called
		// inside decorated.dispatchAnimationFinished() which in turn calls this.dispatchAnimationFinished()
	}

	/**
	 * attention: will not be called for animations that are started by the decorated class
	 */
	override fun onAnimationStarted(viewHolder: ViewHolder) {
	}

	override fun recordPostLayoutInformation(state: RecyclerView.State, viewHolder: ViewHolder)
			= decorated.recordPostLayoutInformation(state, viewHolder)

	override fun recordPreLayoutInformation(state: RecyclerView.State, viewHolder: ViewHolder, changeFlags: Int, payloads: MutableList<Any>)
			= decorated.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads)

	override fun runPendingAnimations()
			= decorated.runPendingAnimations()

	/**
	 * must be called by subclass after all change animations are finished, even if there were no animations to run
	 */
	open fun dispatchChangeAnimationsFinished() {
		if(!decorated.isRunning)
			dispatchAnimationsFinished()
		else
			decorated.isRunning { dispatchChangeAnimationsFinished() }
	}

	final override fun setAddDuration(newAddDuration: Long) {
		decorated.addDuration = newAddDuration
	}

	override fun setChangeDuration(newChangeDuration: Long) {
		decorated.changeDuration = newChangeDuration
	}

	final override fun setMoveDuration(newMoveDuration: Long) {
		decorated.moveDuration = newMoveDuration
	}

	final override fun setRemoveDuration(newRemoveDuration: Long) {
		decorated.removeDuration = newRemoveDuration
	}
}

private fun ItemAnimator.setAnimationsFinishedListener(onAnimationFinished: (ViewHolder) -> Unit) {
	val animatorListenerInvocationHandler = InvocationHandler { _, method, args ->
		if(method.name == "onAnimationFinished") {
			if(args[0] is ViewHolder)
				onAnimationFinished(args[0] as ViewHolder)
			else
				throw RuntimeException("unknown parameter in method invoked on itemAnimatorListener proxy")
		}
		else
			throw RuntimeException("unknown method invoked on ItemAnimatorListener proxy")
	}

	//get private listener interface
	val animatorListenerClass = Class.forName(
			"android.support.v7.widget.RecyclerView\$ItemAnimator\$ItemAnimatorListener")

	val animatorClass = Class.forName(
			"android.support.v7.widget.RecyclerView\$ItemAnimator")

	//create instance that implements the listener interface
	val animatorListenerProxy = Proxy.newProxyInstance(
			animatorListenerClass.classLoader,
			arrayOf(animatorListenerClass),
			animatorListenerInvocationHandler)

	val setListenerMethod = animatorClass.getDeclaredMethod("setListener", animatorListenerClass)

	//remove private modifier
	setListenerMethod.isAccessible = true

	//call setListener method on this ItemAnimator
	setListenerMethod.invoke(this, animatorListenerProxy)
}