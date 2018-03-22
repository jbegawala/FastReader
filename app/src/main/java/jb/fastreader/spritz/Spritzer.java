package jb.fastreader.spritz;

import android.content.UriPermission;
import android.net.Uri;
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

// TODO: Save State for multiple books
public class Spritzer extends SpritzerCore
{
    public static final int SPECIAL_MESSAGE_WPM = 100;
    public static final String TAG = Spritzer.class.getSimpleName();

    private int chapter;
    private ISpritzerMedia media;
    private Uri mMediaUri;
    private boolean mSpritzingSpecialMessage;

    public Spritzer(Bus bus, TextView target)
    {
        super(target);
        setEventBus(bus);
        restoreState(true);
        Log.v(TAG, "Constructor 1");
    }

    public Spritzer(Bus bus, TextView target, Uri mediaUri)
    {
        super(target);
        setEventBus(bus);
        openMedia(mediaUri);
        Log.v(TAG, "Constructor 2");
    }

    public void setMediaUri(Uri uri) {
        pause("setMediaUri");
        openMedia(uri);
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
