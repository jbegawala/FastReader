package jb.fastreader;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

/**
 * Created by Junaid Begawala on 4/4/18.
 */
public class Settings
{
    public static int getNormalWpm(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("config_wpm_normal" , resources.getInteger(R.integer.config_wpm_normal_default));
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
        if ( ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED )
        {
            return false;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("config_save_raw_extract", false);
    }
}
