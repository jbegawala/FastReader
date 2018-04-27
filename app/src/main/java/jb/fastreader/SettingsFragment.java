package jb.fastreader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * A simple {@link Fragment} for managing user settings.
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    public static final int STORAGE_PERMISSION_CODE = 0;
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

    @Override
    public boolean onPreferenceTreeClick(Preference preference)
    {
        if ( preference.getKey().equals("config_save_raw_extract") )
        {
            if ( PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("config_save_raw_extract", false) )
            {
                if ( ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED )
                {
                    ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                }
            }
            return true;
        }
        else
        {
            return super.onPreferenceTreeClick(preference);
        }
    }
}
