package baka.chaomian.fractalwp

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference

class PreferencesFragment : PreferenceFragmentCompat() {

    companion object {
        val colors = arrayOf("red", "green", "blue", "alpha")
    }

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
        findPreference<Preference>("change_color")?.setOnPreferenceClickListener {
            colors.forEach { color ->
                val preference = findPreference<SeekBarPreference>(color)
                val visible = preference?.isVisible
                findPreference<SeekBarPreference>(color)?.isVisible = !visible!!
            }
            true
        }
    }
}
