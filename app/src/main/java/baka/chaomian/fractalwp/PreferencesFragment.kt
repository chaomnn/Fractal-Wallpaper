package baka.chaomian.fractalwp

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import baka.chaomian.fractalwp.preference.ColorPreference
import baka.chaomian.fractalwp.preference.ColorDialogFragment

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>("set_wallpaper")?.setOnPreferenceClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(requireContext(), FractalWallpaperService::class.java)
            )
            startActivity(intent)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val preference = findPreference<Preference>("current_constant")!!
        val constant = preferenceManager.sharedPreferences!!.getString(preference.key, "")!!
        val text = if (constant.isEmpty()) constant else StringBuilder(constant).apply {
            if (this.contains("+")) {
                insert(this.indexOf("+") + 1, " ")
            } else if (this.contains("-") && this.indexOf("-") > 0) {
                val index = this.indexOf("-")
                insert(index + 1, " ")
            }
        }.toString()
        preference.summary = text
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ColorPreference) {
            val dialogFragment = ColorDialogFragment.newInstance(preference.key)
            // TODO
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}
