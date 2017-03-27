package com.tschuchort.simplerecycler

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.util.Log
import rx.Observable
import rx.Subscription

private const val diffUtilMaxSize = 67108864
private const val doInBackgroundSizeThreshold = 500 //chosen randomly

class RecyclerAdapterDiffDelegate<VH: RecyclerView.ViewHolder> (val adapter: RecyclerView.Adapter<VH>) : RecyclerNotifyDataChangedDelegate {
	var detectMoves = true
	private var diffTask: Subscription? = null

	override fun <T : RecyclerItem> onDataChanged(oldData: List<T>, newData: List<T>) {
		if (oldData === newData)
			return

		if (oldData.size > diffUtilMaxSize || newData.size > diffUtilMaxSize)
			return adapter.notifyDataSetChanged()

		val doInBackground = oldData.size > doInBackgroundSizeThreshold
							 || newData.size > doInBackgroundSizeThreshold

		//stop task if it is still running with a previous data set
		diffTask?.unsubscribe()

		diffTask = Observable
				.fromCallable {
					DiffUtil.calculateDiff(object : DiffUtil.Callback() {
						override fun getOldListSize() = oldData.size

						override fun getNewListSize() = newData.size

						override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
								= oldData[oldItemPosition].viewTypeId == newData[newItemPosition].viewTypeId

						override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
								= oldData[oldItemPosition] == newData[newItemPosition]

						override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int)
								= newData[newItemPosition]

					}, detectMoves)
				}
				//TODO remove .compose { it.transformIf(doInBackground) { it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) } }
				.subscribe({
					it.dispatchUpdatesTo(adapter)
				},
				{ error: Throwable ->
					Log.e("RecyclerDiffUtil", "error running DiffUtil.calculateDiff: ${error.message}")
					adapter.notifyDataSetChanged()
				})
	}

	override fun stopBackgroundTasks()
			= diffTask?.unsubscribe()!!
}