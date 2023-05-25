package baka.chaomian.fractalwp.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import baka.chaomian.fractalwp.R

class ColorPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    var color: Int = 0
        set(value) {
            field = value
            persistInt(value)
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInt(index, 0)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        color = getPersistedInt(defaultValue as? Int ?: 0)
    }

    init {
        dialogLayoutResource = R.layout.preference_color
        isPersistent = true
        positiveButtonText = context.getString(R.string.dialog_ok)
        negativeButtonText = context.getString(R.string.dialog_cancel)
    }
}