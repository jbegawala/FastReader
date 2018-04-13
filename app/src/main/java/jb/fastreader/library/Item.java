package jb.fastreader.library;

import android.support.annotation.NonNull;

import java.io.File;

import jb.fastreader.rsvp.IRSVPMedia;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

class Item implements Comparable<Item>
{
    IRSVPMedia media;
    File filePath;

    Item(@NonNull IRSVPMedia media, @NonNull File filePath)
    {
        this.media = media;
        this.filePath = filePath;
    }

    int getProgress()
    {
        return this.media.getWordIndex() * 100 / this.media.getWordCount();
    }

    File getFilePath()
    {
        return filePath;
    }
    @Override
    public int compareTo(@NonNull Item another)
    {
        return media.getTitle().compareToIgnoreCase(another.media.getTitle());
    }
}
