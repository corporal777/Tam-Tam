package org.otunjargych.tamtam.ui.views

import android.content.Context
import android.os.Parcel
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import org.otunjargych.tamtam.R

class CustomColorSpan : ForegroundColorSpan {

    constructor(color: Int) : super(color)
    constructor(src: Parcel) : super(src)
    constructor(context: Context, res: Int) : super(ContextCompat.getColor(context, res))

}