package jb.fastreader.library;

import android.support.annotation.NonNull;

import jb.fastreader.spritz.ISpritzerMedia;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

class LibraryItem implements Comparable<LibraryItem>
{
    ISpritzerMedia media;

    LibraryItem(@NonNull ISpritzerMedia media)
    {
        this.media = media;
    }

    int getProgress()
    {
        return this.media.getWordIndex() * 100 / this.media.getWordCount();
    }

    @Override
    public int compareTo(@NonNull LibraryItem another)
    {
        return media.getTitle().compareToIgnoreCase(another.media.getTitle());
    }
}
