package jb.fastreader.library;

import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;

import jb.fastreader.R;
import jb.fastreader.SettingsFragment;

/**
 * Created by Junaid Begawala on 4/8/18.
 */

public class Library extends AppCompatActivity
{
    public static final String LIBRARY_FRAGMENT = "LIBRARY_FRAGMENT";

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        getSupportFragmentManager().beginTransaction().replace(R.id.activity, new Fragment()).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if ( requestCode == SettingsFragment.STORAGE_PERMISSION_CODE )
        {
            if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED )
            {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("config_save_raw_extract", false).apply();
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
