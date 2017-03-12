package com.tschuchort.simplerecycler

import android.view.View
import android.view.ViewGroup

internal val ViewGroup.children: List<View>
	get() = (0..childCount - 1).map { getChildAt(it) }

