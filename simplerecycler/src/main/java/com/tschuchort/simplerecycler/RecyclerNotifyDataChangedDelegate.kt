package com.tschuchort.simplerecycler


/**
 * changeDelegate for a recycler adapter which takes care of notifying the adapter which data has changed
 */
interface RecyclerNotifyDataChangedDelegate {
	/**
	 * notifies the adapter of the changes between the data sets, for example
	 * by diffing (depending on implementation)
	 *
	 * @param oldData old data set
	 * @param newData new data set
	 */
	fun <T : RecyclerItem> onDataChanged(oldData: List<T>, newData: List<T>)

	fun stopBackgroundTasks()
}