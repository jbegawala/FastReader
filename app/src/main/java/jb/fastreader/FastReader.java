package jb.fastreader;

import android.app.Application;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class FastReader extends Application
{
    private Bus mBus;

    @Override
    public void onCreate()
    {
        super.onCreate();
        this.mBus = new Bus(ThreadEnforcer.ANY);
    }

    /**
     * Obtains the Bus that is used throughout the App.
     *
     * @return The bus instance used throughout the app.
     */
    public Bus getBus()
    {
        return this.mBus;
    }

    public static Toolbar setAndGetToolbar(View root, final Fragment fragment)
    {

        Toolbar toolbar = (Toolbar) root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(fragment.getResources().getColor(R.color.background_light, null));
        toolbar.inflateMenu(R.menu.toolbar);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                if ( item.getItemId() == R.id.action_config )
                {
                    fragment.getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.activity, new SettingsFragment()).commit();
                    return true;
                }
                return false;
            }
        });

        fragment.setHasOptionsMenu(true);

        return toolbar;
    }
}
