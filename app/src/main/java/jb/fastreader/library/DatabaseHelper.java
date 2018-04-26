package jb.fastreader.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import jb.fastreader.R;

/**
 * Created by Junaid Begawala on 4/23/18.
 */

public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String ARTICLE_DIRECTORY = "articles";
    private static final String EXTRACT_DIRECTORY = "extracted";
    private static DatabaseHelper dbHelper = null;
    private Context context;

    private static final String DATABASE = "library.db";
    private static final String TABLE = "articles";
    private static final String COL_ID = "ID";
    private static final String COL_FILENAME = "Filename";
    private static final String COL_TITLE = "Title";
    private static final String COL_URI = "URI";
    private static final String COL_SAVED_DATE = "Saved_Date";
    private static final String COL_POSITION = "Position";
    private static final String COL_WORD_COUNT = "Word_Count";

    public static DatabaseHelper getInstance(Context context)
    {
        if ( dbHelper == null )
        {
            dbHelper = new DatabaseHelper(context.getApplicationContext());
        }
        return dbHelper;
    }
    private DatabaseHelper(Context context)
    {
        super(context, DATABASE, null, 1);
        this.context = context;
    }

    public File getArticleDirectory()
    {
        return this.context.getDir(ARTICLE_DIRECTORY, Context.MODE_PRIVATE);
    }

    public File getExtractDirectory()
    {
        return new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + this.context.getResources().getString(R.string.app_name) + File.separator + EXTRACT_DIRECTORY);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String statement = "CREATE TABLE IF NOT EXISTS " + TABLE
                + "(" + COL_ID + " INTEGER PRIMARY KEY"
                + "," + COL_FILENAME + " TEXT"
                + "," + COL_TITLE + " TEXT"
                + "," + COL_URI + " TEXT"
                + "," + COL_SAVED_DATE + " TEXT"
                + "," + COL_POSITION + " INTEGER"
                + "," + COL_WORD_COUNT + " INTEGER"
                + ")";
        db.execSQL(statement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    List<Item> loadArticles()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        List<Item> libraryContents = new LinkedList<>();

        Cursor response = db.rawQuery("select * from " + TABLE, null);
        int idIndex = response.getColumnIndex(COL_ID);
        int fileNameIndex = response.getColumnIndex(COL_FILENAME);
        int titleIndex = response.getColumnIndex(COL_TITLE);
        int uriIndex = response.getColumnIndex(COL_URI);
        int positionIndex = response.getColumnIndex(COL_POSITION);
        int wordCountIndex = response.getColumnIndex(COL_WORD_COUNT);

        while ( response.moveToNext() )
        {
            int ID = response.getInt(idIndex);
            String filename = response.getString(fileNameIndex);
            String title = response.getString(titleIndex);
            String uri = response.getString(uriIndex);
            int position = response.getInt(positionIndex);
            int wordCount = response.getInt(wordCountIndex);
            Item item = new Item(ID, filename, title, uri, position, wordCount);
            libraryContents.add(item);
        }

        response.close();
        Collections.sort(libraryContents);

        return libraryContents;
    }

    /**
     * Creates entry in database
     * @param filename
     * @param title
     * @param uri
     * @param position
     * @param wordCount
     * @return row ID if article was added succesfully, -1 if addition failed, -2 if article already existed.
     */
    public long addArticle(String filename, String title, String uri, int position, int wordCount)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor result = db.rawQuery("select * from " + TABLE + " where " + COL_FILENAME + "=\"" + filename + "\"", null);
        int count = result.getCount();
        result.close();
        if ( count > 0 )
        {
            return -2;
        }

        ContentValues values = new ContentValues();
        values.put(COL_FILENAME, filename);
        values.put(COL_TITLE, title);
        values.put(COL_URI, uri);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        values.put(COL_SAVED_DATE, dateFormat.format(new Date()));
        values.put(COL_POSITION, position);
        values.put(COL_WORD_COUNT, wordCount);

        return db.insert(TABLE, null, values);
    }

    public void updateArticle(long ID, int position)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_POSITION, position);
        db.update(TABLE, values, COL_ID + "=" + ID, null);
    }

    public boolean deleteArticle(int ID)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE, COL_ID + "=" + ID , null) > 0;
    }
}
