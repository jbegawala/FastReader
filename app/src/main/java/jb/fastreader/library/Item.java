package jb.fastreader.library;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;

import jb.fastreader.rsvp.IRSVPMedia;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

class Item implements Comparable<Item>
{
    static final int DUMMMY_ARTICLE_ID = -1;
    private static final String TAG = Item.class.getSimpleName();
    private IRSVPMedia media;
    private int ID;
    private String filename;
    private String title;
    private String uri;
    private int position;
    private int wordCount;
    private Date date;

    Item(int ID, String filename, String title, String uri, int position, int wordCount, Date date)
    {
        this.ID = ID;
        this.filename = filename;
        this.title = title;
        this.uri = uri;
        this.position = position;
        this.wordCount = wordCount;
        this.date = date;
    }

    int getID()
    {
        return this.ID;
    }

    String getTitle()
    {
        return this.title;
    }

    String getUriString()
    {
        return this.uri;
    }

    Date getDate()
    {
        return this.date;
    }

    int getProgress()
    {
        if ( this.wordCount == 0 )
        {
            Log.i(TAG, this.title + " resulted in 0 wordcount");
            return 0;
        }
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
                this.media.setIndex(this.position);
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

    File getProcessedFilePath()
    {
        return new File(DatabaseHelper.getInstance(null).getProcessedMediaDirectory(), this.filename + ".csv");
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
