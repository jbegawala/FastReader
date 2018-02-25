package jb.fastreader.fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceFragment;

import jb.fastreader.R;

/**
 * A simple {@link Fragment} for managing user preferences.
 */
public class ConfigurationFragment extends PreferenceFragment
{

    public static ConfigurationFragment newInstance()
    {
        return new ConfigurationFragment();
    }

    public ConfigurationFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
