package baka.chaomian.fractalwp.preference;

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceDialogFragmentCompat
import baka.chaomian.fractalwp.databinding.PreferenceColorBinding
import baka.chaomian.fractalwp.view.ColorSliderView
import baka.chaomian.fractalwp.view.ColorWheelView

class ColorDialogFragment : PreferenceDialogFragmentCompat() {

    companion object {
        private const val SAVE_STATE_COLOR = "ColorDialogFragment.color"

        fun newInstance(key: String): ColorDialogFragment = ColorDialogFragment().apply {
            arguments = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
        }
    }

    private fun getArgbColor(colorWheel: ColorWheelView, colorSlider: ColorSliderView?): Int {
        return Color.HSVToColor(floatArrayOf(colorWheel.hue, colorWheel.saturation, colorSlider?.value ?: 1f))
    }

    private val colorPreference: ColorPreference get() = preference as ColorPreference

    private var color = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        color = savedInstanceState?.getInt(SAVE_STATE_COLOR) ?: colorPreference.color
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val binding = PreferenceColorBinding.bind(view)

        val initialHsv = FloatArray(3).also { Color.colorToHSV(color, it) }

        binding.colorWheel.apply {
            hue = initialHsv[0]
            saturation = initialHsv[1]
        }

        binding.colorSlider.apply {
            hue = initialHsv[0]
            saturation = initialHsv[1]
            value = initialHsv[2]
        }

        binding.selectedColor.setBackgroundColor(color)

        binding.colorWheel.setOnClickListener {
            binding.colorSlider.hue = binding.colorWheel.hue
            binding.colorSlider.saturation = binding.colorWheel.saturation
            color = getArgbColor(binding.colorWheel, binding.colorSlider)
            binding.selectedColor.setBackgroundColor(color)
        }
        binding.colorSlider.setOnClickListener {
            color = getArgbColor(binding.colorWheel, binding.colorSlider)
            binding.selectedColor.setBackgroundColor(color)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_COLOR, color)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            colorPreference.color = color
        }
    }
}
