package jb.fastreader.formats;

import java.net.MalformedURLException;
import java.net.URL;

import jb.fastreader.spritz.SpritzerMedia;

/**
 * Created by Junaid Begawala
 */
public class HtmlPage extends SpritzerMedia
{

    public HtmlPage(String title, String subtitle, String content)
    {
        super(title, subtitle, content);
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
