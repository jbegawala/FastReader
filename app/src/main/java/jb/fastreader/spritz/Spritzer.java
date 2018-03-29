package jb.fastreader.spritz;

import android.content.UriPermission;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;

import java.util.List;

import jb.fastreader.Preferences;
import jb.fastreader.R;
import jb.fastreader.events.DummyParsedEvent;
import jb.fastreader.formats.*;
import jb.fastreader.spritz.ISpritzerMedia;

// TODO: Save State
public class Spritzer
{

    static final int MSG_PRINT_WORD = 1;
    static final int MSG_SET_ENABLED = 2;
    public static final int SPECIAL_MESSAGE_WPM = 100;
    public static final String TAG = Spritzer.class.getSimpleName();

    private final Object spritzerThreadSync = new Object();
    private int chapter;
    private ISpritzerMedia media;
    private Uri mMediaUri;
    private boolean mSpritzingSpecialMessage;
    private Handler spritzHandler;
    private Bus bus;

    private boolean spritzThreadStarted;
    private SpritzerTextView spritzerTextView;
    private int mCurWordIdx;

    public Spritzer(Bus bus, TextView spritzerTextView, int wpm)
    {
        this.spritzerTextView = (SpritzerTextView) spritzerTextView;
        this.spritzerTextView.setSyncObject(this.spritzerThreadSync);
        this.spritzerTextView.setWpm(wpm);
        setEventBus(bus);
        restoreState(true);
        Log.v(TAG, "Constructor 1");
        //this.spritzHandler = new SpritzHandler(this);
    }

    public Spritzer(Bus bus, TextView spritzerTextView, int wpm, Uri mediaUri)
    {
        this.spritzerTextView = (SpritzerTextView) spritzerTextView;
        this.spritzerTextView.setSyncObject(this.spritzerThreadSync);
        this.spritzerTextView.setWpm(wpm);
        setEventBus(bus);
        openMedia(mediaUri);
        Log.v(TAG, "Constructor 2");
        //this.spritzHandler = new SpritzHandler(this);
    }

    public void setMediaUri(Uri uri) {
        pause("setMediaUri");
        openMedia(uri);
    }

    /**
     * Start displaying the String input
     * fed to
     */
    public void start(boolean fireFinishEvent, String source)
    {
        Log.i(TAG, "start1 called from " + source);
        this.start(null, fireFinishEvent);
    }

    /**
     * Start displaying the String input
     * fed to
     *
     * @param cb callback to be notified when SpritzerCore finished.
     *           Called from background thread.
     */
    public void start(ISpritzerCallback cb, boolean fireFinishEvent)
    {
        this.spritzerTextView.play();
    }

    public void pause(String info)
    {
        Log.i(TAG, "pause: Pausing spritzer from " + info);

        this.spritzerTextView.pause();

        synchronized ( this.spritzerThreadSync )
        {
            while (this.spritzerTextView.isPlaying())
            {
                try
                {
                    this.spritzerThreadSync.wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isPlaying()
    {
        return this.spritzerTextView.isPlaying();
    }

    private void openMedia(Uri uri)
    {
        if (isHttpUri(uri))
        {
            mMediaUri = uri;
            // TODO why can't this just instantiate object? does callback not work in that context?
//            media = HtmlPage.fromUri(spritzerTextView.getContext().getApplicationContext(), uri.toString(), new IHtmlPageParsedCallback() {
//                @Override
//                public void onPageParsed(HtmlPage result) {
//                    restoreState(false);
//                    if (bus != null) {
//                        bus.post(new HttpUrlParsedEvent(result));
//                    }
//                }
//            });
            this.media = new DummyHtmlPage();
            restoreState(false);
            if ( bus != null )
            {
                bus.post(new DummyParsedEvent((DummyHtmlPage) this.media));
            }
        }
        else
        {
            reportFileUnsupported();
        }
    }


    public void saveState()
    {
        // no point in saving article state, is there?
        if (this.media != null)
        {
            Log.i(TAG, "Saving state at chapter " + chapter + " word: " + mCurWordIdx);
            Preferences.saveState(this.spritzerTextView.getContext(), chapter, mMediaUri.toString(), mCurWordIdx, media.getTitle(), this.spritzerTextView.getWpm());
        }
    }

    private void restoreState(boolean openLastMediaUri) {
        final Preferences.SpritzState state = Preferences.getState(this.spritzerTextView.getContext());
        String content = "";
        if (openLastMediaUri) {
            // Open the last selected media
            if (state.hasUri()) {
                Uri mediaUri = state.getUri();
                if (!isHttpUri(mediaUri)) {
                    boolean uriPermissionPersisted = false;
                    List<UriPermission> uriPermissions = this.spritzerTextView.getContext().getContentResolver().getPersistedUriPermissions();
                    for (UriPermission permission : uriPermissions) {
                        if (permission.getUri().equals(mediaUri)) {
                            uriPermissionPersisted = true;
                            openMedia(mediaUri);
                            break;
                        }
                    }
                    if (!uriPermissionPersisted) {
                        Log.w(TAG, String.format("Permission not persisted for uri: %s. Clearing SharedPreferences ", mediaUri.toString()));
                        Preferences.clearState(this.spritzerTextView.getContext());
                        return;
                    }
                } else {
                    openMedia(mediaUri);
                }
            }
//        } else if (state.hasTitle() && media.getTitle().compareTo(state.getTitle()) == 0) {
//            // Resume media at previous point
//            chapter = state.getChapter();
//            content = this.loadCleanStringFromNextNonEmptyChapter(chapter);
//            setWpm(state.getWpm());
//            mCurWordIdx = state.getWordIdx();
//            Log.i(TAG, "Resuming " + media.getTitle() + " from chapter " + chapter + " word " + mCurWordIdx);
        } else {
            // Begin content anew
            chapter = 0;
            mCurWordIdx = 0;
            setWpm(state.getWpm());
            Log.i(TAG, "Loaded wpm at: " + state.getWpm());
            this.spritzerTextView.setContent(this.media);
        }

//        final String finalContent = content;
//        if ( !this.isPlaying() )
//        {
//            this.spritzerTextView.setEnabled(true);
//
//            //this.pause();
//            //this.setText(this.spritzerTextView.getContext().getString(R.string.touch_to_start));
//
//            this.start(false, "SPritzer");
////            this.start( new ISpritzerCallback() {
////                @Override
////                public void onSpritzerFinished() {
////                    setText(finalContent);
////                    setWpm(state.getWpm());
////                    spritzHandler.sendMessage(spritzHandler.obtainMessage(MSG_SET_ENABLED));
////                }
////            }, false);
//        }
    }

    public void setWpm(int wpm)
    {
        this.spritzerTextView.setWpm(wpm);
    }

    /**
     * Pass a Bus to receive events on, such as
     * when the display of a given String is finished
     *
     * @param bus
     */
    public void setEventBus(Bus bus)
    {
        this.bus = bus;
    }
    public boolean isSpritzingSpecialMessage() {
        return mSpritzingSpecialMessage;
    }

    public ISpritzerMedia getMedia() {
        return media;
    }

    public int getCurrentWordNumber()
    {
        return this.media.getWordIndex();
    }

    public int getWordCount()
    {
        return this.media.getWordCount();
    }

    public int getCurrentChapter() {
        return chapter;
    }

    public int getMaxChapter() {
        return (media == null) ? 0 : 1;
    }

    public boolean isMediaSelected() {
        return media != null;
    }



//    /**
//     * Load the given chapter as sanitized text, proceeding
//     * to the next chapter until a non-zero length result is found.
//     *
//     * This method is useful because some "Chapters" contain only HTML data
//     * that isn't useful to a SpritzerCore.
//     *
//     * @param chapter the first chapter to load
//     * @return the sanitized text of the first non-zero length chapter
//     */
//    private String loadCleanStringFromNextNonEmptyChapter(int chapter) {
//        int chapterToTry = chapter;
//        String result = "";
//        while(result.length() == 0 && chapterToTry <= getMaxChapter()) {
//            result = loadCleanStringFromChapter(chapterToTry);
//            chapterToTry++;
//        }
//        return result;
//    }

//    /**
//     * Load the given chapter as sanitized text.
//     *
//     * @param chapter the target chapter.
//     * @return the sanitized chapter text.
//     */
//    private String loadCleanStringFromChapter(int chapter) {
//        return media.loadChapter(chapter);
//    }


    private void reportFileUnsupported()
    {
        Toast.makeText(this.spritzerTextView.getContext(), this.spritzerTextView.getContext().getString(R.string.unsupported_file), Toast.LENGTH_LONG).show();
    }

    public static boolean isHttpUri(Uri uri)
    {
        return uri.getScheme() != null && uri.getScheme().contains("http");
    }

    /**
     * Return a String representing the maxChars most recently
     * Spritzed characters.
     *
     * @param maxChars The max number of characters to return. Pass a value less than 1 for no limit.
     * @return The maxChars number of most recently spritzed characters during this segment
//     */
//    public String getHistoryString(int maxChars) {
//        if (maxChars <= 0) maxChars = Integer.MAX_VALUE;
//        if (mCurWordIdx < 2 || mDisplayWordList.size() < 2) return "";
//        StringBuilder builder = new StringBuilder();
//        int numWords = 0;
//        while (builder.length() + mDisplayWordList.get(mCurWordIdx - (numWords + 2)).length() < maxChars) {
//            builder.insert(0, mDisplayWordList.get(mCurWordIdx - (numWords + 2)) + " ");
//            numWords++;
//            if (mCurWordIdx - (numWords + 2) < 0) break;
//        }
//        return builder.toString();
//    }


//
//    /**
//     * Swap the target TextView. Call this if your
//     * host Activity is Destroyed and Re-Created.
//     * Effective immediately.
//     *
//     * @param target
//     */
//    void swapTextView(TextView target) {
//        Log.i(TAG, "swapTextView");
//        spritzerTextView = target;
//        if (!this.isPlaying)
//        {
//            peekNextWord();
//        }
//    }

//    private int calculateMonospacedCharacterLimit()
//    {
//        //should be called on SpritzerTextView
//        int maxChars = Math.round(this.getWidth() / this.calculateLengthOfPrintedMonospaceCharacters(1));
//        return maxChars * this.getLineCount();
//    }

    //    public void clearText() {
//        wordList.clear();
//        wordArray = null;
//        mCurWordIdx = 0;
//    }
//
//    public String getNextWord() {
//        if (!isWordListComplete()) {
//            return wordList.get(mCurWordIdx);
//        }
//        return null;
//    }


//    /**
//     * Rewind the spritzer by the specified
//     * amount of words
//     */
//    public void rewind(int numWords) {
//        Log.i(TAG, "rewind: I thought this wasn't used");
//        // TODO implement. words, last sentence, last paragraph
//        if (mCurWordIdx > numWords) {
//            mCurWordIdx -= numWords;
//        } else {
//            mCurWordIdx = 0;
//        }
//    }
//
//    /**
//     * Get the estimated time remaining in the
//     * currently loaded String Queue
//     */
//    public int getMinutesRemainingInQueue() {
//        if (wordList.size() == 0) {
//            return 0;
//        }
//        return (wordList.size() - (mCurWordIdx + 1)) / wpm;
//    }
//
//    /**
//     * Return the completeness of the current
//     * Spritz segment as a float between 0 and 1.
//     *
//     * @return a float between 0 (not started) and 1 (complete)
//     */
//    public float getQueueCompleteness()
//    {
//        return (this.wordArray == null) ? 0 : ((float) mCurWordIdx) / wordList.size();
//    }
}
