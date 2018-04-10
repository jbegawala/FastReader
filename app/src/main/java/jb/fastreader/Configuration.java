package jb.fastreader;

import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

public class Configuration extends PreferenceActivity
{
    private static final String TAG = Configuration.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Nothing to go back to yet
        // getActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new ConfigurationFragment()).commit();
    }

    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return Configuration.class.getName().equals(fragmentName);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Log.i(TAG, "Selected option item: " + id);
        return super.onOptionsItemSelected(item);
    }
}
