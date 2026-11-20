package com.bellatrixos.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/** EditText dengan input type Bellatrix. */
class BellatrixEditText(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {
    init {
        setRawInputType(inputType or 589824)
    }
}
