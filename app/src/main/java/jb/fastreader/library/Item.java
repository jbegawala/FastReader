package jb.fastreader.library;

import android.support.annotation.NonNull;

import jb.fastreader.rsvp.IRSVPMedia;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

class Item implements Comparable<Item>
{
    IRSVPMedia media;

    Item(@NonNull IRSVPMedia media)
    {
        this.media = media;
    }

    int getProgress()
    {
        return this.media.getWordIndex() * 100 / this.media.getWordCount();
    }

    @Override
    public int compareTo(@NonNull Item another)
    {
        return media.getTitle().compareToIgnoreCase(another.media.getTitle());
    }
}
