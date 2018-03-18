package jb.fastreader.formats;

import android.content.Context;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.net.MalformedURLException;
import java.net.URL;

import jb.fastreader.http.TrustManager;

/**
 * This provides an implementation of {@link ISpritzerMedia}
 * that serves a web page
 *
 * @author defer (diogo@underdev.org)
 */
public class HtmlPage implements ISpritzerMedia
{
    private static final String TAG = "HtmlPage";
    private String pageTitle;
    private String url;
    private String pageContent;

    /**
     * Creates an {@link HtmlPage} from a url.
     * Returns immediately with an {@link HtmlPage}
     * that is not yet initialized. Pass a {@link IHtmlPageParsedCallback}
     * to be notified when page parsing is complete, and the returned HtmlPage is populated.
     *
     * @param context Application context
     * @param url The http url.
     * @param cb  A callback to be invoked when the HtmlPage is parsed
     *
     * @return An HtmlPage with null JResult;
     */
    public static HtmlPage fromUri(final Context context, String url, final IHtmlPageParsedCallback cb)
    {
        final HtmlPage page = new HtmlPage(null);
        String encodedUrlToParse = Uri.encode(url);
        String requestUrl = String.format("http://api.diffbot.com/v2/article?url=%s&token=%s", encodedUrlToParse, "2efef432c72b5a923408e04353c39a7c");
        Log.i(TAG, "Loading url: " + requestUrl);
//        TrustManager.makeTrustRequest(context, requestUrl, new TrustManager.TrustRequestCallback() {
//            @Override
//            public void onSuccess(JsonObject result) {
//                page.setResult(result);
//                recordRead(page);
//
//                if (cb != null) {
//                    cb.onPageParsed(page);
//
//                }
//            }
//        });
        Ion.getInstance(context, TrustManager.sIonInstanceName)
                .build(context)
                .load(requestUrl)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            e.printStackTrace();
                            Log.e(TAG, "Unable to parse page");
                            return;
                        }
                        page.initFromJson(result);  // Generate the HtmlPage object

                        if (cb != null)
                        {
                            cb.onPageParsed(page);
                        }
                    }
                });

        return page;
    }


    /**
     * Builds an HtmlPage from a {@link com.google.gson.JsonObject} in diffbot format.
     * See http://www.diffbot.com/products/automatic/
     *
     * @param obj The {@link com.google.gson.JsonObject} to display
     */
    private HtmlPage(JsonObject obj)
    {
        if (obj != null)
        {
            initFromJson(obj);
        }
    }

    // Diffbot json format: http://www.diffbot.com/products/automatic/
    private void initFromJson(JsonObject parsedContent)
    {
        if (parsedContent == null)
        {
            Log.e(TAG, "Error parsing page");
            return;
        }

        if (parsedContent.has("title"))
        {
            this.pageTitle = parsedContent.get("title").getAsString();
        }

        if (parsedContent.has("url"))
        {
            this.url = parsedContent.get("url").getAsString();
        }

        if (parsedContent.has("text"))
        {
            this.pageContent = parsedContent.get("text").getAsString();
        }

        // Sanitize content
        String articleContent = Html.fromHtml(this.pageContent).toString();
        this.pageContent = articleContent.replaceAll("\\n+", " (NewParagraph) ").replaceAll("(?s)<!--.*?-->", "");

        Log.v(TAG, "Finished replacing article content");
    }

    @Override
    public String getTitle()
    {
        return (this.pageTitle == null) ? "" : this.pageTitle;
    }

    @Override
    public String getAuthor()
    {
        try
        {
            if (this.url != null)
            {
                return new URL(this.url).getHost();
            }
        }
        catch (MalformedURLException e) {}  // Can't really do anything

        return "";
    }

    @Override
    public String loadChapter(int ignored)
    {
        return (this.pageContent == null) ? "" : this.pageContent;
    }

    @Override
    public String getChapterTitle(int ignored)
    {
        return "";  // TODO Should this be article title?
    }

    @Override
    public int countChapters()
    {
        return 1;  // Only one chapter for articles
    }

}
