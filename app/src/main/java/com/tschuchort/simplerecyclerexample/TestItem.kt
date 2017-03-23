package com.tschuchort.simplerecyclerexample

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import com.tschuchort.simplerecycler.AutoViewHolder
import com.tschuchort.simplerecycler.RecyclerItem
import com.tschuchort.simplerecycler.XmlRecyclerItem

class TestItem(var text: String) : XmlRecyclerItem() {

	override val layoutId = R.layout.recycler_item

	override fun bindViewHolder(holder: AutoViewHolder) {
		holder.findViewById<TextView>(R.id.item_text)!!.text = text
	}

	override fun canAnimateChanges(other: RecyclerItem) = true

	override fun animateChange(other: RecyclerItem): Animator {
		val textView = (holder!! as AutoViewHolder).findViewById<TextView>(R.id.item_text)!!
		textView.text = (other as TestItem).text

		val anim = ObjectAnimator.ofFloat(holder!!.itemView, View.ROTATION_X, 0f, 360f)
		anim.duration = 5000L
		anim.interpolator = AccelerateDecelerateInterpolator()
		return anim
	}

	override fun animateChange(other: RecyclerItem, interruptedAnimation: Animator?): Animator {
		(holder!! as AutoViewHolder).findViewById<TextView>(R.id.item_text)!!.text = (other as TestItem).text

		// continue new animation where the old animation was
		val animState = (interruptedAnimation as ObjectAnimator).animatedFraction
		val anim = ObjectAnimator.ofFloat(holder!!.itemView, View.ROTATION_X, animState, 360f)
		anim.duration = (5000L * (360f - animState)/360f).toLong()
		anim.interpolator = LinearInterpolator()
		return anim
	}

	override fun equals(other: Any?) = (other as? TestItem)?.text == text

	override fun hashCode() = text.hashCode()
}