package jb.fastreader;

import android.app.Application;

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
}
