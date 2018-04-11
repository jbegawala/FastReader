package jb.fastreader;

import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * A simple {@link Fragment} for managing user settings.
 */
public class SettingsFragment extends PreferenceFragment
{

    public static SettingsFragment newInstance()
    {
        return new SettingsFragment();
    }

    public SettingsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getActivity(),R.xml.settings, false);

        // Load the settings from an XML resource
        addPreferencesFromResource(R.xml.settings);
    }
}
