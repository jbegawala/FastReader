package jb.fastreader.rsvp;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.squareup.otto.Bus;

import jb.fastreader.FastReader;
import jb.fastreader.R;
import jb.fastreader.SettingsFragment;

// This is the activity that gets called when you share to this app
public class RSVP extends AppCompatActivity
{
    public static final String TAG = RSVP.class.getSimpleName();
    private Bus bus;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        this.applyThemeFromConfiguration();
        Log.i(TAG, "Started onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getSupportFragmentManager().beginTransaction().replace(R.id.activity, new Fragment(), Fragment.RSVP_FRAGMENT).commit();

        FastReader frApp = (FastReader) getApplication();
        this.bus = frApp.getBus();
        this.bus.register(this);
    }

    private void applyThemeFromConfiguration()
    {
        setTheme(R.style.Dark);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent = getIntent();
        if ( this.isIntentMarkedAsHandled(intent) )
        {
            return;
        }

        String action = intent.getAction();
        Log.i(TAG, "onResume: with action " + action);

        // Something has been shared to this app
        if ( action.equals(Intent.ACTION_SEND) )
        {
            // Some apps send article title before URL so parse it out
            String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if ( !extraText.substring(0, 4).equalsIgnoreCase("http") )
            {
                int pos = extraText.indexOf("http");
                if ( pos >= 0 )
                {
                    extraText = extraText.substring(pos);
                }
            }

            Uri uri = Uri.parse(extraText);
            if ( uri == null )
            {
                Log.w(TAG, getResources().getString(R.string.error_could_not_parse_uri) + extraText);
                Toast.makeText(getApplicationContext(), R.string.error_could_not_parse_uri + extraText, Toast.LENGTH_LONG).show();
            }
            else if ( !Core.isUriSupported(uri) )
            {
                Log.w(TAG, getResources().getString(R.string.error_unsupported_uri) + extraText);
                Toast.makeText(getApplicationContext(), R.string.error_unsupported_uri + extraText, Toast.LENGTH_LONG).show();
            }
            else
            {
                Log.i(TAG, "onResume: ACTION_SENT called with uri: " + uri.toString());
                this.getFragment().openURI(uri);
            }
        }

        this.markIntentAsHandled(intent);
    }

    private void markIntentAsHandled(Intent intent)
    {
        intent.putExtra("handled", true);
    }

    private boolean isIntentMarkedAsHandled(Intent intent)
    {
        return intent.getBooleanExtra("handled", false);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (this.bus != null)
        {
            this.bus.unregister(this);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_config)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new SettingsFragment(), "config").addToBackStack(null).commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Fragment getFragment()
    {
        return ((Fragment) getSupportFragmentManager().findFragmentByTag(Fragment.RSVP_FRAGMENT));
    }
}
