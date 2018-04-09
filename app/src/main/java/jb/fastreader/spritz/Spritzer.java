package jb.fastreader.spritz;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.otto.Bus;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;
import jb.fastreader.Preferences;
import jb.fastreader.R;
import jb.fastreader.formats.*;
import jb.fastreader.library.Library;

public class Spritzer
{
    public enum BusEvent
    {
        CONTENT_PARSED,
        CONTENT_FINISHED
    }

    public enum MediaParseStatus
    {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETE
    }

    final private static  String TAG = Spritzer.class.getSimpleName();

    final private Object spritzerThreadSync = new Object();
    final private Object mediaStatusSync = new Object();

    private Context context;
    private Bus bus;
    private ISpritzerMedia media;
    private MediaParseStatus mediaParseStatus;
    private SpritzerTextView spritzerTextView;

    public Spritzer(Context context, Bus bus, TextView spritzerTextView)
    {
        Log.v(TAG, "Constructor");
        this.context = context;
        this.spritzerTextView = (SpritzerTextView) spritzerTextView;
        this.spritzerTextView.setSyncObject(this.spritzerThreadSync);
        this.spritzerTextView.setWpm(Preferences.getFastWpm(this.context));
        this.spritzerTextView.setBus(bus);
        this.bus = bus;
        this.mediaParseStatus = MediaParseStatus.NOT_STARTED;
    }

    public void openMedia(Uri uri)
    {
        this.pause();

        if ( isHttpUri(uri) )
        {
            synchronized ( this.mediaStatusSync )
            {
                this.mediaParseStatus = MediaParseStatus.IN_PROGRESS;
            }
            if ( Preferences.useDummyArticle(this.spritzerTextView.getContext()) )
            {
                this.setMedia(new DummyHtmlPage());
                synchronized ( this.mediaStatusSync )
                {
                    this.mediaParseStatus = MediaParseStatus.COMPLETE;
                }
                this.bus.post(BusEvent.CONTENT_PARSED);
                this.saveParsedContent();
            }
            else
            {
                this.sendToBoilerPipe(uri);
            }
        }
        else
        {
            this.reportFileUnsupported();
        }
    }

    public void setMedia(ISpritzerMedia media)
    {
        this.media = media;
        this.spritzerTextView.setContent(media);
    }

    public void start()
    {
        Log.i(TAG, "start");
        this.spritzerTextView.play();
    }

    public void pause()
    {
        if ( !this.spritzerTextView.isPlaying() )
        {
            return;
        }

        Log.i(TAG, "pause");
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



    private void sendToBoilerPipe(Uri uri)
    {
        final AsyncHttpClient client = new AsyncHttpClient();
        String getMethod = "http://boilerpipe-web.appspot.com/extract";
        RequestParams params = new RequestParams();
        params.put("url", uri.toString());
        params.put("extractor", "ArticleExtractor");
        params.put("output", "json");
        client.get(getMethod, params, new JsonHttpResponseHandler()
        {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // Root JSON in response is an dictionary i.e { "data : [ ... ] }
                // Handle resulting parsed JSON response here
                Log.i("Webservice success", response.toString());

                String title = "";
                String subtitle = "";
                String content = "";
                JSONObject parsedResponse;
                try {
                    parsedResponse = response.getJSONObject("response");
                    title = parsedResponse.getString("title");
                    subtitle = parsedResponse.getString("source");
                    content = parsedResponse.getString("content");
                } catch (Exception e) {

                } finally {
                    if (title == null || title.isEmpty()) {
                        title = "Error";
                    }
                    if (subtitle == null || subtitle.isEmpty()) {
                        subtitle = "Error";
                    }
                    if (content == null || content.isEmpty()) {
                        content = "Error";
                    }
                }

                Spritzer.this.setMedia(new HtmlPage(title, subtitle, content));

                synchronized (mediaStatusSync)
                {
                    mediaParseStatus = MediaParseStatus.COMPLETE;
                }

                bus.post(BusEvent.CONTENT_PARSED);
                saveParsedContent();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                Log.i("Webservice fail", res);
            }
        });
    }

    public void saveState()
    {
        // TODO: Save State with SpritzerMedia
    }

    private void saveParsedContent()
    {
        try
        {
            String fileName = UUID.nameUUIDFromBytes(this.media.getUri().getBytes()).toString();
            File file = new File(this.context.getDir(Library.ARTICLE_DIRECTORY, Context.MODE_PRIVATE), fileName);
            FileOutputStream fos = new FileOutputStream(file, false);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.media);
            oos.close();
            fos.close();
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }
    }


    public void setWpm(int wpm)
    {
        this.spritzerTextView.setWpm(wpm);
    }


    public ISpritzerMedia getMedia() {
        return media;
    }

    public MediaParseStatus getMediaParseStatus()
    {
        return this.mediaParseStatus;
    }

    public int getCurrentWordNumber()
    {
        return this.media.getWordIndex();  // word index is a zero based index whose current value is the next word to display
    }

    public int getWordCount()
    {
        return this.media.getWordCount();
    }

    public boolean isMediaSelected() {
        return media != null;
    }

    public void rewindCurrentSentence()
    {
        this.media.rewindCurrentSentence();
    }

    public void rewindPreviousSentence()
    {
        this.media.rewindPreviousSentence();
    }

    public void rewindCurrentParagraph()
    {
        this.media.rewindCurrentParagraph();
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

    public static boolean isUriSupported(Uri uri)
    {
        return ( uri != null && isHttpUri(uri) );
    }

    private static boolean isHttpUri(Uri uri)
    {
        return uri.getScheme() != null && uri.getScheme().contains("http");
    }

//    /**
//     * Return a String representing the maxChars most recently
//     * Spritzed characters.
//     *
//     * @param maxChars The max number of characters to return. Pass a value less than 1 for no limit.
//     * @return The maxChars number of most recently spritzed characters during this segment
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

}
