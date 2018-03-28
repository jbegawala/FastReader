package jb.fastreader.formats;

import java.net.MalformedURLException;
import java.net.URL;

import jb.fastreader.spritz.SpritzerMedia;

/**
 * Created by jb on 2/25/18.
 */

public class DummyHtmlPage extends SpritzerMedia
{
    public DummyHtmlPage()
    {
        super("IHOP is Giving Away Free Pancakes on Tuesday ",
                "https://lifehacker.com/ihop-is-giving-away-free-pancakes-on-tuesday-1823250922",
                "Tuesday is National Pancake Day. While on the surface maybe not a holiday worth remembering, this year IHOP is offering something you might want to pencil in on your calendar: free pancakes.\n" +
                "\n" +
                "The International House of Pancakes is offering customers a free short stack of buttermilk pancakes between 7am and 7pm this Tuesday, February 27 at its locations in the United States, Canada, Puerto Rico, Guam, and Mexico.\n" +
                "\n" +
                "IHOP has been celebrating the day since 2006. While each stack of pancakes is free, the pancake chain asks that guests consider leaving a donation for the meal that is then donated to local children’s hospitals and health organizations.\n" +
                "\n" +
                "You also have to dine-in in order to take advantage of the deal.\n" +
                "\n" +
                "That said, free pancakes. Or, you could just whip some up at home. Here’s how to make some fluffy ones. ");
    }

    @Override
    public String getSubtitle()
    {
        try
        {
            return new URL(super.getSubtitle()).getHost();
        }
        catch (MalformedURLException e) {}  // Can't really do anything

        return "";
    }
}
