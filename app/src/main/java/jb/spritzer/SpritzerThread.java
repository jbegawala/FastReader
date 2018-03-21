package jb.spritzer;

import android.util.Log;

/**
 * Created by jb on 3/21/18.
 */

public class SpritzerThread implements Runnable
{
    private static final String TAG = SpritzerThread.class.getSimpleName();

    private SpritzerCore spritzerCore;
    final private ISpritzerCallback callback;
    final private boolean fireFinishEvent;

    public SpritzerThread(SpritzerCore spritzerCore, final ISpritzerCallback callback, final boolean fireFinishEvent)
    {
        this.spritzerCore = spritzerCore;
        this.callback = callback;
        this.fireFinishEvent = fireFinishEvent;
    }

    @Override
    public void run()
    {

        this.spritzerCore.setIsPlaying();

        while (this.spritzerCore.shouldPlay())
        {
            try
            {
                this.spritzerCore.processNextWord();

                if (this.spritzerCore.isWordListComplete())
                {
                    Log.i(TAG, "Word list completely displayed after processNextWord. Pausing");

                    this.spritzerCore.requestStop();
//        if (bus != null && fireFinishEvent) {
//            bus.post(new SpritzFinishedEvent());
//        }
                    if (callback != null)
                    {
                        Log.i(TAG, "Calling callback");
                        callback.onSpritzerFinished();
                    }
                }
                this.spritzerCore.mCurWordIdx++;
            }

            catch (InterruptedException e)
            {
                Log.e(TAG, "Exception spritzing");
                e.printStackTrace();
            }
        }

        this.spritzerCore.setNotPlaying();
    }

}
