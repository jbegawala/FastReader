package jb.fastreader;

import android.annotation.SuppressLint;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;

import java.util.List;

import jb.fastreader.events.HttpUrlParsedEvent;
import jb.fastreader.events.NextChapterEvent;
import jb.fastreader.formats.HtmlPage;
import jb.fastreader.formats.ISpritzerMedia;
import jb.spritzer.SpritzerCore;

/**
 * A higher-level {@link SpritzerCore} that operates
 * on Uris pointing to .epubs on disk or http urls, instead
 * of a plain String
 */
// TODO: Save State for multiple books
public class AppSpritzer extends SpritzerCore
{
    public static final int SPECIAL_MESSAGE_WPM = 100;
    public static final String TAG = AppSpritzer.class.getSimpleName();

    private int mChapter;
    private ISpritzerMedia mMedia;
    private Uri mMediaUri;
    private boolean mSpritzingSpecialMessage;

    public AppSpritzer(Bus bus, TextView target)
    {
        super(target);
        setEventBus(bus);
        restoreState(true);
        Log.v(TAG, "Constructor 1");
    }

    public AppSpritzer(Bus bus, TextView target, Uri mediaUri)
    {
        super(target);
        setEventBus(bus);
        openMedia(mediaUri);
        Log.v(TAG, "Constructor 2");
    }

    public void setMediaUri(Uri uri) {
        pause();
        openMedia(uri);
    }

    private void openMedia(Uri uri)
    {
        if (isHttpUri(uri))
        {
            mMediaUri = uri;
            // TODO why can't this just instantiate object? does callback not work in that context?
            mMedia = HtmlPage.fromUri(textViewTarget.getContext().getApplicationContext(), uri.toString(), new HtmlPage.HtmlPageParsedCallback() {
                @Override
                public void onPageParsed(HtmlPage result) {
                    restoreState(false);
                    if (bus != null) {
                        bus.post(new HttpUrlParsedEvent(result));
                    }
                }
            });
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
        return mMedia;
    }

    public int getCurrentChapter() {
        return mChapter;
    }

    public int getMaxChapter() {
        return (mMedia == null) ? 0 : mMedia.countChapters() - 1;
    }

    public boolean isMediaSelected() {
        return mMedia != null;
    }

    protected void processNextWord() throws InterruptedException {
        super.processNextWord();
        if (this.isPlaying && mPlayingRequested && isWordListComplete() && mChapter < getMaxChapter()) {
            // If we are Spritzing a special message, don't automatically proceed to the next chapter
            if (mSpritzingSpecialMessage) {
                mSpritzingSpecialMessage = false;
                return;
            }
            while (isWordListComplete() && mChapter < getMaxChapter()) {
                printNextChapter();
                if (bus != null) {
                    bus.post(new NextChapterEvent(mChapter));
                }
            }
        }
    }

    private void printNextChapter() {
        setText(loadCleanStringFromChapter(mChapter++));
        saveState();
        Log.i(TAG, "starting next chapter: " + mChapter + " length " + mDisplayWordList.size());
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
        return mMedia.loadChapter(chapter);
    }

    public void saveState()
    {
        // no point in saving article state, is there?
        if (this.mMedia != null)
        {
            Log.i(TAG, "Saving state at chapter " + mChapter + " word: " + mCurWordIdx);
            Preferences.saveState(textViewTarget.getContext(), mChapter, mMediaUri.toString(), mCurWordIdx, mMedia.getTitle(), wpm);
        }
    }

    @SuppressLint("NewApi")
    private void restoreState(boolean openLastMediaUri) {
        final Preferences.SpritzState state = Preferences.getState(textViewTarget.getContext());
        String content = "";
        if (openLastMediaUri) {
            // Open the last selected media
            if (state.hasUri()) {
                Uri mediaUri = state.getUri();
                if (Build.VERSION.SDK_INT >= 19 && !isHttpUri(mediaUri)) {
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
        } else if (state.hasTitle() && mMedia.getTitle().compareTo(state.getTitle()) == 0) {
            // Resume media at previous point
            mChapter = state.getChapter();
            content = loadCleanStringFromNextNonEmptyChapter(mChapter);
            setWpm(state.getWpm());
            mCurWordIdx = state.getWordIdx();
            Log.i(TAG, "Resuming " + mMedia.getTitle() + " from chapter " + mChapter + " word " + mCurWordIdx);
        } else {
            // Begin content anew
            mChapter = 0;
            mCurWordIdx = 0;
            setWpm(state.getWpm());
            Log.i(TAG, "Loaded wpm at: " + state.getWpm());
            content = loadCleanStringFromNextNonEmptyChapter(mChapter);
        }
        final String finalContent = content;
        if (!this.isPlaying && finalContent.length() > 0) {
            setWpm(SPECIAL_MESSAGE_WPM);
            // Set mSpritzingSpecialMessage to true, so processNextWord doesn't
            // automatically proceed to the next chapter
            mSpritzingSpecialMessage = true;
            textViewTarget.setEnabled(false);
            setTextAndStart(textViewTarget.getContext().getString(R.string.touch_to_start), new SpritzerCallback() {
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
