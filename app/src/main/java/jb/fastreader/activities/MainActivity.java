package jb.fastreader.activities;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.squareup.otto.Bus;

import jb.fastreader.FastReaderApplication;
import jb.fastreader.Preferences;
import jb.fastreader.R;
import jb.fastreader.fragments.ConfigurationFragment;
import jb.fastreader.fragments.SpritzFragment;
import jb.fastreader.spritz.Spritzer;

// This is the activity that gets called when you share to this app
public class MainActivity extends FragmentActivity implements View.OnSystemUiVisibilityChangeListener
{
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String JB_READER_FRAGMENT = "jbreaderfragment";
    private static final int THEME_LIGHT = 0;
    private Bus bus;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        this.applyThemeFromConfiguration();
        Log.i(TAG, "Started onCreate");

        super.onCreate(savedInstanceState);
        this.setupActionBar();
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction().replace(R.id.container, new SpritzFragment(), JB_READER_FRAGMENT).commit();

        FastReaderApplication frApp = (FastReaderApplication) getApplication();
        this.bus = frApp.getBus();
        this.bus.register(this);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
    }

    private void applyThemeFromConfiguration()
    {
        if ( Preferences.getTheme(this) == THEME_LIGHT )
        {
            setTheme(R.style.Light);
            Log.i(TAG, "Applied light theme");
        }
        else
        {
            setTheme(R.style.Dark);
            Log.i(TAG, "Applied dark theme");
        }
    }

    private void setupActionBar()
    {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.dimSystemUi(true);

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
                Log.w(TAG, R.string.error_could_not_parse_uri + extraText);
                Toast.makeText(getApplicationContext(), R.string.error_could_not_parse_uri + extraText, Toast.LENGTH_LONG).show();
            }
            else if ( !Spritzer.isUriSupported(uri) )
            {
                Log.w(TAG, R.string.error_unsupported_uri + extraText);
                Toast.makeText(getApplicationContext(), R.string.error_unsupported_uri + extraText, Toast.LENGTH_LONG).show();
            }
            else
            {
                Log.i(TAG, "onResume: ACTION_SENT called with uri: " + uri.toString());
                this.getSpritzFragment().openURI(uri);
            }
        }

        this.markIntentAsHandled(intent);
    }

    private void dimSystemUi(boolean doDim)
    {
        final View decorView = getWindow().getDecorView();
        if (doDim)
        {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        else
        {
            decorView.setSystemUiVisibility(0);
            decorView.setOnSystemUiVisibilityChangeListener(null);
        }
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
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, ConfigurationFragment.newInstance(), "config");
            ft.addToBackStack(null);
            ft.commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private SpritzFragment getSpritzFragment()
    {
        return ((SpritzFragment) getSupportFragmentManager().findFragmentByTag(JB_READER_FRAGMENT));
    }

    @Override
    // called when the status bar changes visibility because of a call to setSystemUiVisibility(int).
    public void onSystemUiVisibilityChange(int visibility)
    {
        // Stay in low-profile mode
        if ((visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0)
        {
            this.dimSystemUi(true);
        }
    }


}
