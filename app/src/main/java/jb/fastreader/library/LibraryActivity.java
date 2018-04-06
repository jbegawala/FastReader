package jb.fastreader.library;

import android.app.ListActivity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

import jb.fastreader.R;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

public class LibraryActivity extends ListActivity
{
    ArrayAdapter<LibraryItem> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.library_activity);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.scanAndLoadMedia();
    }

    private void scanAndLoadMedia()
    {
        Resources resources = getApplicationContext().getResources();
        this.setTitle(resources.getString(R.string.library));
        List<LibraryItem> libraryContents = new LinkedList<>();

        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());
        libraryContents.add(new LibraryItem());

        this.adapter = new LibraryAdapter(getBaseContext(), R.layout.library_item, libraryContents);
        super.setListAdapter(this.adapter);
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);
        Toast.makeText(getBaseContext().getApplicationContext(), "Clicked on item " + position, Toast.LENGTH_LONG).show();
    }
}
