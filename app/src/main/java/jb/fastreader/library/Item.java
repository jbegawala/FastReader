package jb.fastreader.library;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import jb.fastreader.rsvp.IRSVPMedia;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

class Item implements Comparable<Item>
{
    private IRSVPMedia media;
    private int ID;
    private String filename;
    private String title;
    private String uri;
    private int position;
    private int wordCount;

    Item(int ID, String filename, String title, String uri, int position, int wordCount)
    {
        this.ID = ID;
        this.filename = filename;
        this.title = title;
        this.uri = uri;
        this.position = position;
        this.wordCount = wordCount;
    }

    String getTitle()
    {
        return this.title;
    }

    String getUriString()
    {
        return this.uri;
    }
    int getProgress()
    {
        return this.position * 100 / this.wordCount;
    }

    IRSVPMedia getMedia()
    {
        if ( this.media == null )
        {
            File filePath = new File(DatabaseHelper.getInstance(null).getArticleDirectory(), this.filename);
            try (FileInputStream fis = new FileInputStream(filePath))
            {
                ObjectInputStream ois = new ObjectInputStream(fis);
                this.media = (IRSVPMedia) ois.readObject();
            }
            catch (IOException | ClassNotFoundException e) {}
        }
        return this.media;
    }

    boolean delete()
    {
        if ( DatabaseHelper.getInstance(null).deleteArticle(this.ID) )
        {
            File filePath = new File(DatabaseHelper.getInstance(null).getArticleDirectory(), this.filename);
            filePath.delete();
            if ( this.getExtractFilePath().exists() )
            {
                this.getExtractFilePath().delete();
            }
            return true;
        }
        return false;
    }

    File getExtractFilePath()
    {
        return new File(DatabaseHelper.getInstance(null).getExtractDirectory(), this.filename + ".txt");
    }

    boolean hasExtract()
    {
        return this.getExtractFilePath().exists();
    }

    @Override
    public int compareTo(@NonNull Item another)
    {
        return this.title.compareToIgnoreCase(another.title);
    }
}
