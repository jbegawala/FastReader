package jb.fastreader.spritz;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;

import java.util.List;

import jb.fastreader.Preferences;
import jb.fastreader.R;
import jb.fastreader.events.DummyParsedEvent;
import jb.fastreader.events.NextChapterEvent;
import jb.fastreader.formats.*;
import jb.fastreader.spritz.events.SpritzProgressEvent;

// TODO: Save State for multiple books
public class Spritzer extends SpritzerCore
{
    public static final int SPECIAL_MESSAGE_WPM = 100;
    public static final String TAG = Spritzer.class.getSimpleName();

    private final Object mSpritzThreadStartedSync = new Object();
    private int chapter;
    private ISpritzerMedia media;
    private Uri mMediaUri;
    private boolean mSpritzingSpecialMessage;
    private Handler spritzHandler;
    private Bus bus;

    private boolean isPlaying;
    private boolean playingRequested;
    private boolean spritzThreadStarted;
    private TextView textViewTarget;
    private int mCurWordIdx;

    public Spritzer(Bus bus, TextView target, int wpm)
    {
        super(target, wpm);
        setEventBus(bus);
        restoreState(true);
        Log.v(TAG, "Constructor 1");
        this.spritzHandler = new SpritzHandler(this);
    }

    public Spritzer(Bus bus, TextView target, int wpm, Uri mediaUri)
    {
        super(target, wpm);
        setEventBus(bus);
        openMedia(mediaUri);
        Log.v(TAG, "Constructor 2");
        this.spritzHandler = new SpritzHandler(this);
    }

    public void setMediaUri(Uri uri) {
        pause("setMediaUri");
        openMedia(uri);
    }

    /**
     * Swap the target TextView. Call this if your
     * host Activity is Destroyed and Re-Created.
     * Effective immediately.
     *
     * @param target
     */
    void swapTextView(TextView target) {
        Log.i(TAG, "swapTextView");
        textViewTarget = target;
        if (!this.isPlaying)
        {
            peekNextWord();
        }
    }

    public void pause(String info)
    {
        Log.i(TAG, "pause: Pausing spritzer from " + info);

        this.requestStop();

        synchronized (mSpritzThreadStartedSync)
        {
            while (this.isPlaying)
            {
                try
                {
                    mSpritzThreadStartedSync.wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
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


    /**
     * Rewind the spritzer by the specified
     * amount of words
     */
    public void rewind(int numWords) {
        Log.i(TAG, "rewind: I thought this wasn't used");
        // TODO implement. words, last sentence, last paragraph
        if (mCurWordIdx > numWords) {
            mCurWordIdx -= numWords;
        } else {
            mCurWordIdx = 0;
        }
    }

    /**
     * Get the estimated time remaining in the
     * currently loaded String Queue
     */
    public int getMinutesRemainingInQueue() {
        if (wordList.size() == 0) {
            return 0;
        }
        return (wordList.size() - (mCurWordIdx + 1)) / wpm;
    }

    /**
     * Return the completeness of the current
     * Spritz segment as a float between 0 and 1.
     *
     * @return a float between 0 (not started) and 1 (complete)
     */
    public float getQueueCompleteness()
    {
        return (this.wordArray == null) ? 0 : ((float) mCurWordIdx) / wordList.size();
    }


    /**
     * Start displaying the String input
     * fed to {@link #setText(String)}
     */
    public void start(boolean fireFinishEvent, String source)
    {
        Log.i(TAG, "start1 called from " + source);
        this.start(null, fireFinishEvent);
    }

    /**
     * Start displaying the String input
     * fed to {@link #setText(String)}
     *
     * @param cb callback to be notified when SpritzerCore finished.
     *           Called from background thread.
     */
    public void start(ISpritzerCallback cb, boolean fireFinishEvent)
    {
        Log.i(TAG, "start2");
        if (this.isPlaying || this.wordArray == null)
        {
            Log.w(TAG, "Start called in invalid state: isPlaying: " + this.isPlaying + " wordArray: " + this.wordArray);
            return;
        }
        Log.i(TAG, "Start called " + ((cb == null) ? "without" : "with") + " callback." );

        this.requestPlay();
        this.startTimerThread(cb, fireFinishEvent);
    }

    /**
     * Begin the background timer thread
     */
    private void startTimerThread(final ISpritzerCallback callback, final boolean fireFinishEvent)
    {
        synchronized (mSpritzThreadStartedSync)
        {
            if (!spritzThreadStarted)
            {
                new Thread(new SpritzerThread(this, callback, fireFinishEvent)).start();
            }
        }
    }



    protected boolean shouldPlay()
    {
        return playingRequested;
    }

    void requestPlay()
    {
        playingRequested = true;
    }

    void requestStop()
    {
        playingRequested = false;
    }
    void setIsPlaying()
    {

        Log.i(TAG, "Starting spritzThread with queue length " + wordList.size());

        this.isPlaying = true;
        synchronized (mSpritzThreadStartedSync)
        {
            spritzThreadStarted = true;
        }
    }

    void setNotPlaying()
    {
        Log.i(TAG, "Stopping spritzThread");

        this.isPlaying = false;

        synchronized (mSpritzThreadStartedSync)
        {
            mSpritzThreadStartedSync.notify();
        }
        spritzThreadStarted = false;
    }


    private int getInterWordDelay()
    {
        return 60000 / this.wpm;
    }
    private void openMedia(Uri uri)
    {
        if (isHttpUri(uri))
        {
            mMediaUri = uri;
            // TODO why can't this just instantiate object? does callback not work in that context?
//            media = HtmlPage.fromUri(textViewTarget.getContext().getApplicationContext(), uri.toString(), new IHtmlPageParsedCallback() {
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

    protected boolean isWordListComplete() {
        return mCurWordIdx >= wordList.size() - 1;
    }

    /**
     * Read the current head of wordList and
     * submit the appropriate Messages to spritzHandler.
     * <p/>
     * Split long words submitting the first segment of a word
     * and placing the second at the head of wordList for processing
     * during the next cycle.
     * <p/>
     * Must be called on a background thread, as this method uses
     * {@link Thread#sleep(long)} to time pauses in display.
     *
     * @throws InterruptedException
     */
    protected void processNextWord() throws InterruptedException {
        if (mCurWordIdx < wordList.size()) {
            String word = wordList.get(mCurWordIdx);

            if (bus != null) {
                bus.post(new SpritzProgressEvent(mCurWordIdx));
            }
            spritzHandler.sendMessage(spritzHandler.obtainMessage(MSG_PRINT_WORD, word));

            Thread.sleep(this.getInterWordDelay() * delayMultiplierForWord(word));

            // If word is end of a sentence, add three blanks
            if (word.contains(".") || word.contains("?") || word.contains("!")) {
                for (int x = 0; x < 3; x++) {
                    spritzHandler.sendMessage(spritzHandler.obtainMessage(MSG_PRINT_WORD, "  "));
                    Thread.sleep(getInterWordDelay());
                }
            }
        }
        else
        {
            Log.i(TAG, "processNextWord called with invalid mCurWordIdx: " + mCurWordIdx + " array size " + wordList.size());
        }
    }

    public void setWpm(int wpm)
    {
        this.wpm = wpm;
    }

    public boolean isPlaying()
    {
        return this.isPlaying;
    }
    /**
     * Applies the given String to this SpritzerCore's TextView,
     * padding the beginning if necessary to align the pivot character.
     * Styles the pivot character.
     *
     * @param word
     */
    private void printWord(String word) {

        Spannable spanRange = new SpannableString(word);
        TextAppearanceSpan tas = new TextAppearanceSpan(textViewTarget.getContext(), R.style.PivotLetter);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewTarget.setText(spanRange);
    }
    }

    private void peekNextWord() {
        if (mCurWordIdx >= 0 && !isWordListComplete() && wordList.get(mCurWordIdx) != null) {
            printWord(wordList.get(mCurWordIdx));
        }
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
        return 10;
    }

    public int getWordCount()
    {
        return 30;
    }
    public int getCurrentChapter() {
        return chapter;
    }

    public int getMaxChapter() {
        return (media == null) ? 0 : media.countChapters() - 1;
    }

    public boolean isMediaSelected() {
        return media != null;
    }

    protected void processNextWord() throws InterruptedException {
        super.processNextWord();
        if (this.isPlaying() && this.shouldPlay() && isWordListComplete() && chapter < getMaxChapter()) {
            // If we are Spritzing a special message, don't automatically proceed to the next chapter
            if (mSpritzingSpecialMessage) {
                mSpritzingSpecialMessage = false;
                return;
            }
            while (isWordListComplete() && chapter < getMaxChapter()) {
                printNextChapter();
                if (bus != null) {
                    bus.post(new NextChapterEvent(chapter));
                }
            }
        }
    }

    private void printNextChapter() {
        setText(loadCleanStringFromChapter(chapter++));
        saveState();
        Log.i(TAG, "starting next chapter: " + chapter + " length " + mDisplayWordList.size());
    }

    /**
     * Load the given chapter as sanitized text, proceeding
     * to the next chapter until a non-zero length result is found.
     *
     * This method is useful because some "Chapters" contain only HTML data
     * that isn't useful to a SpritzerCore.
     *
     * @param chapter the first chapter to load
     * @return the sanitized text of the first non-zero length chapter
     */
    private String loadCleanStringFromNextNonEmptyChapter(int chapter) {
        int chapterToTry = chapter;
        String result = "";
        while(result.length() == 0 && chapterToTry <= getMaxChapter()) {
            result = loadCleanStringFromChapter(chapterToTry);
            chapterToTry++;
        }
        return result;
    }

    /**
     * Load the given chapter as sanitized text.
     *
     * @param chapter the target chapter.
     * @return the sanitized chapter text.
     */
    private String loadCleanStringFromChapter(int chapter) {
        return media.loadChapter(chapter);
    }

    public void saveState()
    {
        // no point in saving article state, is there?
        if (this.media != null)
        {
            Log.i(TAG, "Saving state at chapter " + chapter + " word: " + mCurWordIdx);
            Preferences.saveState(textViewTarget.getContext(), chapter, mMediaUri.toString(), mCurWordIdx, media.getTitle(), wpm);
        }
    }

    private void restoreState(boolean openLastMediaUri) {
        final Preferences.SpritzState state = Preferences.getState(textViewTarget.getContext());
        String content = "";
        if (openLastMediaUri) {
            // Open the last selected media
            if (state.hasUri()) {
                Uri mediaUri = state.getUri();
                if (!isHttpUri(mediaUri)) {
                    boolean uriPermissionPersisted = false;
                    List<UriPermission> uriPermissions = textViewTarget.getContext().getContentResolver().getPersistedUriPermissions();
                    for (UriPermission permission : uriPermissions) {
                        if (permission.getUri().equals(mediaUri)) {
                            uriPermissionPersisted = true;
                            openMedia(mediaUri);
                            break;
                        }
                    }
                    if (!uriPermissionPersisted) {
                        Log.w(TAG, String.format("Permission not persisted for uri: %s. Clearing SharedPreferences ", mediaUri.toString()));
                        Preferences.clearState(textViewTarget.getContext());
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
            content = this.loadCleanStringFromNextNonEmptyChapter(chapter);
        }

        final String finalContent = content;
        if (!this.isPlaying() && finalContent.length() > 0) {
            setWpm(SPECIAL_MESSAGE_WPM);
            // Set mSpritzingSpecialMessage to true, so processNextWord doesn't
            // automatically proceed to the next chapter
            mSpritzingSpecialMessage = true;
            textViewTarget.setEnabled(false);

            //this.pause();
            this.setText(textViewTarget.getContext().getString(R.string.touch_to_start));

            this.start( new ISpritzerCallback() {
                @Override
                public void onSpritzerFinished() {
                    setText(finalContent);
                    setWpm(state.getWpm());
                    spritzHandler.sendMessage(spritzHandler.obtainMessage(MSG_SET_ENABLED));
                }
            }, false);
        }
    }

    private void reportFileUnsupported()
    {
        Toast.makeText(textViewTarget.getContext(), textViewTarget.getContext().getString(R.string.unsupported_file), Toast.LENGTH_LONG).show();
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
     */
    public String getHistoryString(int maxChars) {
        if (maxChars <= 0) maxChars = Integer.MAX_VALUE;
        if (mCurWordIdx < 2 || mDisplayWordList.size() < 2) return "";
        StringBuilder builder = new StringBuilder();
        int numWords = 0;
        while (builder.length() + mDisplayWordList.get(mCurWordIdx - (numWords + 2)).length() < maxChars) {
            builder.insert(0, mDisplayWordList.get(mCurWordIdx - (numWords + 2)) + " ");
            numWords++;
            if (mCurWordIdx - (numWords + 2) < 0) break;
        }
        return builder.toString();
    }

}
