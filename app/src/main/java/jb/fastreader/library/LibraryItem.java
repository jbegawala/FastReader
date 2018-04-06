package jb.fastreader.library;

import java.util.Random;

import jb.fastreader.formats.DummyHtmlPage;
import jb.fastreader.spritz.ISpritzerMedia;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

public class LibraryItem
{
    ISpritzerMedia media;

    public LibraryItem()
    {
        this.media = new DummyHtmlPage();
    }

    public int getProgress()
    {
        return new Random().nextInt(100);
        //return this.media.getWordIndex() * 100 / this.media.getWordCount();
    }

    public int getWpm()
    {
        return 500;
    }

}
