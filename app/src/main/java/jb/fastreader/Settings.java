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
        return sharedPreferences.getInt("config_wpm_fast" , resources.getInteger(R.integer.config_wpm_fast_default));
    }

    public static int getSlowWpm(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("config_wpm_slow" , resources.getInteger(R.integer.config_wpm_slow_default));
    }

    public static boolean useDummyArticle(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("config_dev_source", false);
    }

    public static boolean useWebService(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("config_extraction_usewebservice", false);
    }

    public static boolean saveRawExtract(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("config_save_raw_extract", false);
    }
}
