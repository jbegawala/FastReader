package jb.fastreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

/**
 * Created by Junaid Begawala on 4/4/18.
 */
public class Settings
{
    public static int getFastWpm(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(sharedPreferences.getString(resources.getString(R.string.config_wpm_fast_key), resources.getString(R.string.config_wpm_fast_default)));
    }

    public static int getSlowWpm(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(sharedPreferences.getString(resources.getString(R.string.config_wpm_slow_key), resources.getString(R.string.config_wpm_slow_default)));
    }

    public static boolean useDummyArticle(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(resources.getString(R.string.config_dev_source_key), true);
    }
}
