package jb.fastreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;


/**
 * Created by davidbrodsky on 9/21/14.
 */
public class Preferences
{
    /** SharedPreferences store names */
    private static final String UI_PREFS = "ui_prefs";
    private static final String APP_PREFS = "espritz";

    /** SharedPreferences keys */
    private static final String UI_THEME = "THEME";

    public static final String APP_URI = "uri";
    public static final String APP_TITLE = "title";
    public static final String APP_CHAPTER = "chapter";

    /** Default SharedPreferences Values */
    public static final int DEFAULT_APP_WPM = 500;


    // default to dark theme
    public static int getTheme(Context context)
    {
        return context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE).getInt(UI_THEME, 1);
    }

    public static boolean useDummyArticle(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(resources.getString(R.string.config_dev_source_key), true);
    }

    public static void setTheme(Context context, int theme)
    {
        context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE).edit().putInt(UI_THEME, theme).commit();
    }

    public static void saveState(Context context, int chapter, String uri, int wordIdx, String title, int wpm)
    {
        Resources resources = context.getResources();
        SharedPreferences.Editor editor = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(APP_CHAPTER, chapter)
                .putString(APP_URI, uri)
                .putInt(resources.getString(R.string.pref_key_word), wordIdx)
                .putString(APP_TITLE, title)
                .putInt(resources.getString(R.string.config_key_wpm), wpm)
                .apply();
    }

    public static SpritzState getState(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        return new SpritzState(
                prefs.getInt(APP_CHAPTER, 0),
                prefs.getString(APP_URI, null),
                prefs.getInt(resources.getString(R.string.pref_key_word), 0),
                prefs.getString(APP_TITLE, null),
                prefs.getInt(resources.getString(R.string.config_key_wpm), 500)
                );
    }

    public static void clearState(Context context)
    {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void setWpm(Context context, int wpm)
    {
        Resources resources = context.getResources();
        SharedPreferences.Editor editor = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE).edit();
        //editor.putInt(resources.getString(R.string.config_key_wpm), Math.max(wpm, WpmDialogFragment.MIN_WPM)).apply();
    }

    public static int getWpm(Context context)
    {
        Resources resources = context.getResources();
        SharedPreferences prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(resources.getString(R.string.config_key_wpm), DEFAULT_APP_WPM);
    }

    public static class SpritzState
    {

        private int chapter;
        private String uri;
        private int wordIdx;
        private String title;
        private int wpm;

        public SpritzState(int chapter, String uri, int wordIdx, String title, int wpm)
        {
            this.chapter = chapter;
            this.uri = uri;
            this.wordIdx = wordIdx;
            this.title = title;
            this.wpm = wpm;
        }

        public boolean hasUri() {
            return uri != null;
        }

        public Uri getUri() {
            return Uri.parse(uri);
        }

        public boolean hasTitle() {
            return title != null;
        }

        public String getTitle() {
            return title;
        }

        public int getChapter() {
            return chapter;
        }

        public int getWpm() {
            return (wpm == 0 ? DEFAULT_APP_WPM : wpm);
        }

        public int getWordIdx() {
            return wordIdx;
        }
    }

}
