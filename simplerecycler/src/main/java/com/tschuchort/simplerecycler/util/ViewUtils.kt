package com.tschuchort.endlessrecycler

import android.view.View
import android.view.ViewGroup

/**
 * Created by Thilo on 12/19/2016.
 */

internal val ViewGroup.children: List<View>
	get() = (0..childCount - 1).map { getChildAt(it) }

