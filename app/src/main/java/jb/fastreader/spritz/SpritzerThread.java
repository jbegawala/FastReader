package jb.fastreader.spritz;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

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
/**
 * A Handler intended for creation on the Main thread.
 * Messages are intended to be passed from a background
 * timing thread. This Handler communicates timing
 * thread events to the Main thread for UI update.
 */
protected static class SpritzHandler extends Handler {
    private WeakReference<SpritzerCore> mWeakSpritzer;

    public SpritzHandler(SpritzerCore muxer) {
        mWeakSpritzer = new WeakReference<SpritzerCore>(muxer);
    }

    @Override
    public void handleMessage(Message inputMessage) {
        int what = inputMessage.what;
        Object obj = inputMessage.obj;

        SpritzerCore spritzerCore = mWeakSpritzer.get();
        if (spritzerCore == null) {
            return;
        }

        switch (what) {
            case MSG_PRINT_WORD:
                spritzerCore.printWord((String) obj);
                break;
            case MSG_SET_ENABLED:
                spritzerCore.textViewTarget.setEnabled(true);
                break;
            default:
                throw new RuntimeException("Unexpected msg what=" + what);
        }
    }

}