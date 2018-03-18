package jb.fastreader.formats;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jb on 2/25/18.
 */

public class DummyHtmlPage implements ISpritzerMedia
{
    private static final String TAG = "DummyHtmlPage";
    private String pageTitle;
    private String url;
    private String pageContent;

    public DummyHtmlPage()
    {
        // "title"
        this.pageTitle = "IHOP is Giving Away Free Pancakes on Tuesday ";

        // "url"
        this.url = "https://lifehacker.com/ihop-is-giving-away-free-pancakes-on-tuesday-1823250922";

        // "text"
        this.pageContent = " Tuesday is National Pancake Day. While on the surface maybe not a holiday worth remembering, this year IHOP is offering something you might want to pencil in on your calendar: free pancakes.\n" +
                "\n" +
                "The International House of Pancakes is offering customers a free short stack of buttermilk pancakes between 7am and 7pm this Tuesday, February 27 at its locations in the United States, Canada, Puerto Rico, Guam, and Mexico.\n" +
                "\n" +
                "IHOP has been celebrating the day since 2006. While each stack of pancakes is free, the pancake chain asks that guests consider leaving a donation for the meal that is then donated to local children’s hospitals and health organizations.\n" +
                "\n" +
                "You also have to dine-in in order to take advantage of the deal.\n" +
                "\n" +
                "That said, free pancakes. Or, you could just whip some up at home. Here’s how to make some fluffy ones. ";
    }

    @Override
    public String getTitle()
    {
        return this.pageTitle;
    }

    @Override
    public String getAuthor()
    {
        try
        {
            return new URL(this.url).getHost();
        }
        catch (MalformedURLException e) {}  // Can't really do anything

        return "";
    }

    @Override
    public String loadChapter(int ignored)
    {
        return this.pageContent;
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
