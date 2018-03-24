package jb.fastreader.spritz;

import android.util.Log;

/**
 * Created by jb on 3/21/18.
 */

public class SpritzerThread implements Runnable
{
    private static final String TAG = SpritzerThread.class.getSimpleName();
    private SpritzerTextView spritzerTextView;

    public SpritzerThread(SpritzerTextView spritzerTextView)
    {
        this.spritzerTextView = spritzerTextView;
    }

    @Override
    public void run()
    {
        this.spritzerTextView.setIsPlaying();

        while (this.spritzerTextView.shouldPlay())
        {
            try
            {
                this.spritzerTextView.showOneWord();
            }

            catch (InterruptedException e)
            {
                Log.e(TAG, "Exception while playing");
                e.printStackTrace();
            }
        }
        this.spritzerTextView.setNotPlaying();
    }
}