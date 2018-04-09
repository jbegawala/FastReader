package jb.fastreader.library;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jb.fastreader.R;
import jb.fastreader.fragments.SpritzFragment;
import jb.fastreader.spritz.ISpritzerMedia;

import static jb.fastreader.activities.MainActivity.JB_READER_FRAGMENT;

/**
 * Created by Junaid Begawala on 4/8/18.
 */
public class LibraryFragment extends ListFragment
{
    ArrayAdapter<LibraryItem> adapter;
    List<LibraryItem> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.library_activity, container, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.scanAndLoadMedia();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        SpritzFragment spritzFragment = (SpritzFragment) getFragmentManager().findFragmentByTag(JB_READER_FRAGMENT);
        if ( spritzFragment == null )
        {
            spritzFragment = new SpritzFragment();
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("media", this.items.get(position).media);
        spritzFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(R.id.activity, spritzFragment, JB_READER_FRAGMENT).addToBackStack(null).commit();
    }

    private void scanAndLoadMedia()
    {
        List<LibraryItem> libraryContents = new LinkedList<>();

        File[] articles = getContext().getDir(Library.ARTICLE_DIRECTORY, Context.MODE_PRIVATE).listFiles();
        for ( File article : articles )
        {
            if ( article.isDirectory() )
            {
                continue;
            }

            try ( FileInputStream fis = new FileInputStream(article) )
            {
                ;
                ObjectInputStream ois = new ObjectInputStream(fis);
                ISpritzerMedia media = (ISpritzerMedia) ois.readObject();

                if ( media != null )
                {
                    libraryContents.add(new LibraryItem(media));
                }
            }
            catch (IOException |ClassNotFoundException e) {}
        }

        Collections.sort(libraryContents);

        this.adapter = new LibraryAdapter(getContext(), R.layout.library_item, libraryContents);
        super.setListAdapter(this.adapter);
        this.items = libraryContents;
    }
}
