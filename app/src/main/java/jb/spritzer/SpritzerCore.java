package jb.spritzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.squareup.otto.Bus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import jb.fastreader.R;
import jb.spritzer.events.SpritzProgressEvent;


/**
 * SpritzerCore parses a String into a Queue
 * of words, and displays them one-by-one
 * onto a TextView at a given WPM.
 */
public class SpritzerCore
{
    protected static final String TAG = "SpritzerCore";

    protected static final int MSG_PRINT_WORD = 1;
    protected static final int MSG_SET_ENABLED = 2;
    protected static final int CHARS_LEFT_OF_PIVOT = 3;
    protected final Object mSpritzThreadStartedSync = new Object();
    protected int mMaxWordLength = 13;
    protected String[] wordArray;                  // A parsed list of words parsed from {@link #setText(String input)}
    protected ArrayList<String> mDisplayWordList;  // The queue of words from wordArray yet to be displayed
    protected TextView textViewTarget;
    protected int wpm;
    protected Handler spritzHandler;
    private boolean isPlaying;
    private boolean playingRequested;
    private boolean spritzThreadStarted;

    protected Bus bus;
    protected int mCurWordIdx;

    public SpritzerCore(TextView target)
    {
        Log.i(TAG, "SpritzerCore: Constructor");
        // used to be its own protected method
        this.mCurWordIdx = 0;
        this.mDisplayWordList = new ArrayList<>();
        Context context = target.getContext();
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.wpm = Integer.parseInt(sharedPreferences.getString(resources.getString(R.string.config_wpm_fast_key), resources.getString(R.string.config_wpm_fast_default)));
        this.isPlaying = false;
        this.playingRequested = false;
        this.spritzThreadStarted = false;

        this.textViewTarget = target;
        this.spritzHandler = new SpritzHandler(this);
    }


    public void setTextAndStart(String input, boolean fireFinishEvent)
    {
        Log.i(TAG, "setTextAndStart1");
        this.pause("setTextAndStart");
        this.setText(input);
        this.start(null, fireFinishEvent);
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

    /**
     * Prepare to Spritz the given String input
     * <p/>
     * Call {@link #start(boolean)} to begin display
     *
     * @param input
     */
    public void setText(String input)
    {
        Log.i(TAG, "setText: " + input);
        this.wordArray = input
                .replaceAll("/\\s+/g", " ")      // condense adjacent spaces
                .split(" ");                     // split on spaces
        this.refillWordDisplayList();
    }

//    public void clearText() {
//        mDisplayWordList.clear();
//        wordArray = null;
//        mCurWordIdx = 0;
//    }
//
//    public String getNextWord() {
//        if (!isWordListComplete()) {
//            return mDisplayWordList.get(mCurWordIdx);
//        }
//        return null;
//    }

    /**
     * Rewind the spritzer by the specified
     * amount of words
     *
     * @param numWords
     *
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
     *
     * @return
     */
    public int getMinutesRemainingInQueue() {
        if (mDisplayWordList.size() == 0) {
            return 0;
        }
        return (mDisplayWordList.size() - (mCurWordIdx + 1)) / wpm;
    }

    /**
     * Return the completeness of the current
     * Spritz segment as a float between 0 and 1.
     *
     * @return a float between 0 (not started) and 1 (complete)
     */
    public float getQueueCompleteness()
    {
        return (this.wordArray == null) ? 0 : ((float) mCurWordIdx) / mDisplayWordList.size();
    }


    /**
     * Swap the target TextView. Call this if your
     * host Activity is Destroyed and Re-Created.
     * Effective immediately.
     *
     * @param target
     */
    public void swapTextView(TextView target) {
        Log.i(TAG, "swapTextView");
        textViewTarget = target;
        if (!this.isPlaying)
        {
            peekNextWord();
        }
    }

    /**
     * Start displaying the String input
     * fed to {@link #setText(String)}
     */
    public void start(boolean fireFinishEvent)
    {
        Log.i(TAG, "start1");
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

        Log.i(TAG, "Starting spritzThread with queue length " + mDisplayWordList.size());

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

    private void refillWordDisplayList()
    {
        mCurWordIdx = 0;
        mDisplayWordList.clear();
        mDisplayWordList.addAll(Arrays.asList(this.wordArray));
    }

    /**
     * Read the current head of mDisplayWordList and
     * submit the appropriate Messages to spritzHandler.
     * <p/>
     * Split long words submitting the first segment of a word
     * and placing the second at the head of mDisplayWordList for processing
     * during the next cycle.
     * <p/>
     * Must be called on a background thread, as this method uses
     * {@link Thread#sleep(long)} to time pauses in display.
     *
     * @throws InterruptedException
     */
    protected void processNextWord() throws InterruptedException {
        if (mCurWordIdx < mDisplayWordList.size()) {
            String word = mDisplayWordList.get(mCurWordIdx);
            word = splitLongWord(word);

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
            Log.i(TAG, "processNextWord called with invalid mCurWordIdx: " + mCurWordIdx + " array size " + mDisplayWordList.size());
        }
    }

    /**
     * Split the given String if appropriate and
     * add the tail of the split to the head of
     * {@link #mDisplayWordList}
     * <p/>
     * Currently public for testing purposes
     *
     * @param word
     * @return
     */
    public String splitLongWord(String word) {
        if (word.length() > mMaxWordLength) {
            int splitIndex = findSplitIndex(word);
            String firstSegment;
            firstSegment = word.substring(0, splitIndex);
            // A word split is always indicated with a hyphen unless ending in a period
            if (!firstSegment.contains("-") && !firstSegment.endsWith(".")) {
                firstSegment = firstSegment + "-";
            }
            mDisplayWordList.add(mCurWordIdx + 1, word.substring(splitIndex));
            word = firstSegment;
        }
        return word;
    }

    /**
     * Determine the split index on a given String
     * e.g If it exceeds mMaxWordLength or contains a hyphen
     *
     * @param thisWord
     * @return the index on which to split the given String
     */
    private int findSplitIndex(String thisWord) {
        int splitIndex;
        // Split long words, at hyphen or dot if present.
        if (thisWord.contains("-")) {
            splitIndex = thisWord.indexOf("-") + 1;
        } else if (thisWord.contains(".")) {
            splitIndex = thisWord.indexOf(".") + 1;
        } else if (thisWord.length() > mMaxWordLength * 2) {
            // if the word is floccinaucinihilipilifcation, for example.
            splitIndex = mMaxWordLength - 1;
            // 12 characters plus a "-" == 13.
        } else {
            // otherwise we want to split near the middle.
            splitIndex = Math.round(thisWord.length() / 2F);
        }
        // in case we found a split character that was > mMaxWordLength characters in.
        if (splitIndex > mMaxWordLength) {
            // If we split the word at a splitting char like "-" or ".", we added one to the splitIndex
            // in order to ensure the splitting char appears at the head of the split. Not accounting
            // for this in the recursive call will cause a StackOverflowException
            return findSplitIndex(thisWord.substring(0,
                    wordContainsSplittingCharacter(thisWord) ? splitIndex - 1 : splitIndex));
        }
        Log.i(TAG, "Splitting long word " + thisWord + " into " + thisWord.substring(0, splitIndex) +
                    " and " + thisWord.substring(splitIndex));
        return splitIndex;
    }

    private boolean wordContainsSplittingCharacter(String word) {
        return (word.contains(".") || word.contains("-"));
    }

    private void peekNextWord() {
        if (mCurWordIdx >= 0 && !isWordListComplete() && mDisplayWordList.get(mCurWordIdx) != null) {
            printWord(mDisplayWordList.get(mCurWordIdx));
        }
    }

    /**
     * Applies the given String to this SpritzerCore's TextView,
     * padding the beginning if necessary to align the pivot character.
     * Styles the pivot character.
     *
     * @param word
     */
    private void printWord(String word) {
        int startSpan = 0;
        int endSpan = 0;
        word = word.trim();
        if (word.length() == 1) {
            StringBuilder builder = new StringBuilder();
            for (int x = 0; x < CHARS_LEFT_OF_PIVOT; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        } else if (word.length() <= CHARS_LEFT_OF_PIVOT * 2) {
            StringBuilder builder = new StringBuilder();
            int halfPoint = word.length() / 2;
            int beginPad = CHARS_LEFT_OF_PIVOT - halfPoint;
            for (int x = 0; x <= beginPad; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = halfPoint + beginPad;
            endSpan = startSpan + 1;
        } else {
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        }

        Spannable spanRange = new SpannableString(word);
        TextAppearanceSpan tas = new TextAppearanceSpan(textViewTarget.getContext(), R.style.PivotLetter);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewTarget.setText(spanRange);
    }


    private int delayMultiplierForWord(String word)
    {
        // double rest if length > 6 or contains (.,!?)
        if (word.length() >= 6 || word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\""))
        {
            return 3;
        }
        return 1;
    }

    protected boolean isWordListComplete() {
        return mCurWordIdx >= mDisplayWordList.size() - 1;
    }

    public boolean isPlaying()
    {
        return this.isPlaying;
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

    public void setMaxWordLength(int maxWordLength)
    {
        this.mMaxWordLength = maxWordLength;
    }

    public void setWpm(int wpm)
    {
        this.wpm = wpm;
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
}
