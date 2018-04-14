package jb.fastreader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * A simple {@link Fragment} for managing user settings.
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    public SettingsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        PreferenceManager.setDefaultValues(getActivity(),R.xml.settings, false);

        // Load the settings from an XML resource
        addPreferencesFromResource(R.xml.settings);
    }
}
