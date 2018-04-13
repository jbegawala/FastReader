package jb.fastreader.rsvp;

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
import jb.fastreader.Settings;
import jb.fastreader.R;
import jb.fastreader.library.Library;

public class Core
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

    final private static  String TAG = Core.class.getSimpleName();

    final private Object threadSync = new Object();
    final private Object mediaStatusSync = new Object();

    private Context context;
    private Bus bus;
    private IRSVPMedia media;
    private MediaParseStatus mediaParseStatus;
    private RSVPTextView textView;

    public Core(Context context, Bus bus, TextView textView)
    {
        Log.v(TAG, "Constructor");
        this.context = context;
        this.textView = (RSVPTextView) textView;
        this.textView.setSyncObject(this.threadSync);
        this.textView.setWpm(Settings.getFastWpm(this.context));
        this.textView.setBus(bus);
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
            if ( Settings.useDummyArticle(this.textView.getContext()) )
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

    public void setMedia(IRSVPMedia media)
    {
        this.media = media;
        this.textView.setContent(media);
    }

    public void start()
    {
        Log.i(TAG, "start");
        this.textView.play();
    }

    public void pause()
    {
        if ( !this.textView.isPlaying() )
        {
            return;
        }

        Log.i(TAG, "pause");
        this.textView.pause();

        synchronized ( this.threadSync)
        {
            while (this.textView.isPlaying())
            {
                try
                {
                    this.threadSync.wait();
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
        return this.textView.isPlaying();
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

                Core.this.setMedia(new HtmlPage(title, subtitle, content));

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
        this.saveParsedContent();
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
        this.textView.setWpm(wpm);
    }


    public IRSVPMedia getMedia() {
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

    private void reportFileUnsupported()
    {
        Toast.makeText(this.textView.getContext(), this.textView.getContext().getString(R.string.unsupported_file), Toast.LENGTH_LONG).show();
    }

    public static boolean isUriSupported(Uri uri)
    {
        return ( uri != null && isHttpUri(uri) );
    }

    private static boolean isHttpUri(Uri uri)
    {
        return uri.getScheme() != null && uri.getScheme().contains("http");
    }

//    private int calculateMonospacedCharacterLimit()
//    {
//        int maxChars = Math.round(this.getWidth() / this.calculateLengthOfPrintedMonospaceCharacters(1));
//        return maxChars * this.getLineCount();
//    }
}
