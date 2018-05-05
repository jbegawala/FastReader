package jb.fastreader.rsvp;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.otto.Bus;

import org.json.JSONObject;

import java.net.URL;

import cz.msebera.android.httpclient.Header;
import com.kohlschutter.boilerpipe.extractors.ArticleExtractor;
import jb.fastreader.Settings;
import jb.fastreader.R;

class Core
{
    enum BusEvent
    {
        CONTENT_PARSED,
        CONTENT_FINISHED,
        WEBSERVICE_FAIL,
        ARTICLESAVE_FAIL
    }

    enum MediaParseStatus
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

    Core(Context context, Bus bus, TextView textView)
    {
        Log.v(TAG, "Constructor");
        this.context = context;
        this.textView = (RSVPTextView) textView;
        this.textView.setSyncObject(this.threadSync);
        this.textView.setWpm(Settings.getNormalWpm(this.context));
        this.textView.setBus(bus);
        this.bus = bus;
        this.mediaParseStatus = MediaParseStatus.NOT_STARTED;
    }

    private class Extractor extends AsyncTask<Uri, Integer, HtmlPage>
    {
        Core core;
        Bus bus;

        void init(Core core, Bus bus)
        {
            this.core = core;
            this.bus = bus;
        }

        @Override
        protected HtmlPage doInBackground(Uri... uris)
        {
            HtmlPage page = null;
            try
            {
                for ( Uri uri : uris )
                {
                    URL url = new URL(uri.toString());
                    ArticleExtractor extractor = new ArticleExtractor();
                    String content = extractor.getText(url);
                    String title = extractor.getTitle();
                    if ( title == null )
                    {
                        title = Core.this.context.getResources().getString(R.string.missing_article_title);
                    }
                    page = Core.this.createMedia(title, uri.toString(), content);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return page;
        }
    }

    void openMedia(Uri uri)
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
                this.createMedia(DummyHtmlPage.getTitle(), DummyHtmlPage.getUriString(), DummyHtmlPage.getContent());
            }
            else
            {
                if ( Settings.useWebService(this.textView.getContext()) )
                {
                    this.sendToBoilerPipe(uri);
                }
                else
                {
                    Extractor extractor = new Extractor();
                    extractor.init(this, this.bus);
                    extractor.execute(uri);
                }
            }
        }
        else
        {
            this.reportFileUnsupported();
        }
    }

    private HtmlPage createMedia(String title, String uri, String content)
    {
        this.bus.post(BusEvent.CONTENT_PARSED);
        HtmlPage media = null;
        try
        {
             media = new HtmlPage(title, uri, content, this.context);

            this.setMedia(media);

            synchronized (mediaStatusSync)
            {
                mediaParseStatus = MediaParseStatus.COMPLETE;
            }

            this.bus.post(BusEvent.CONTENT_PARSED);
        }
        catch (FailedToSave failedToSave)
        {
            synchronized (mediaStatusSync)
            {
                mediaParseStatus = MediaParseStatus.NOT_STARTED;
            }

            this.bus.post(new BusEventFail(BusEvent.ARTICLESAVE_FAIL, failedToSave.getMessage()));
        }

        return media;
    }

    void setMedia(IRSVPMedia media)
    {
        this.media = media;
        this.textView.setContent(media);
    }

    void start()
    {
        Log.i(TAG, "start");
        this.textView.play();
    }

    void pause()
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

    boolean isPlaying()
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

                Core.this.createMedia(title, subtitle, content);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t)
            {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                bus.post(new BusEventFail(BusEvent.WEBSERVICE_FAIL, Core.this.context.getString(R.string.failed_webservice) + ": " + statusCode));
                Log.i("Webservice fail", res);
            }
        });
    }

    void saveState()
    {
        if ( this.media != null)
        {
            this.media.saveState();
        }
    }

    void setWpm(int wpm)
    {
        this.textView.setWpm(wpm);
    }

    IRSVPMedia getMedia()
    {
        return media;
    }

    MediaParseStatus getMediaParseStatus()
    {
        return this.mediaParseStatus;
    }

    int getCurrentWordNumber()
    {
        return this.media.getWordIndex();  // word index is a zero based index whose current value is the next word to display
    }

    int getWordCount()
    {
        return this.media.getWordCount();
    }

    boolean isMediaSelected() {
        return media != null;
    }

    void rewindCurrentSentence()
    {
        this.media.rewindCurrentSentence();
    }

    void rewindPreviousSentence()
    {
        this.media.rewindPreviousSentence();
    }

    void rewindCurrentParagraph()
    {
        this.media.rewindCurrentParagraph();
    }

    private void reportFileUnsupported()
    {
        Toast.makeText(this.textView.getContext(), this.textView.getContext().getString(R.string.unsupported_file), Toast.LENGTH_LONG).show();
    }

    static boolean isUriSupported(Uri uri)
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

class BusEventFail
{
    final private Core.BusEvent event;
    final private String details;

    BusEventFail(Core.BusEvent event, String details)
    {
        this.event = event;
        this.details = details;
    }

    Core.BusEvent getEvent()
    {
        return this.event;
    }

    String getDetails()
    {
        return this.details;
    }
}