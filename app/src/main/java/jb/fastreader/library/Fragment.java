package jb.fastreader.library;

import android.support.v4.app.ListFragment;
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

import jb.fastreader.FastReader;
import jb.fastreader.R;
import jb.fastreader.rsvp.IRSVPMedia;

import static jb.fastreader.rsvp.RSVP.RSVP_FRAGMENT;

/**
 * Created by Junaid Begawala on 4/8/18.
 */
public class Fragment extends ListFragment
{
    ArrayAdapter<Item> adapter;
    List<Item> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View root = inflater.inflate(R.layout.library_fragment, container, false);

        FastReader.setAndGetToolbar(root, this);

        return root;
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

        jb.fastreader.rsvp.Fragment rsvpFragment = (jb.fastreader.rsvp.Fragment) getFragmentManager().findFragmentByTag(RSVP_FRAGMENT);
        if ( rsvpFragment == null )
        {
            rsvpFragment = new jb.fastreader.rsvp.Fragment();
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("media", this.items.get(position).media);
        rsvpFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(R.id.activity, rsvpFragment, RSVP_FRAGMENT).addToBackStack(null).commit();
    }

    private void scanAndLoadMedia()
    {
        List<Item> libraryContents = new LinkedList<>();

        File[] articles = getContext().getDir(Library.ARTICLE_DIRECTORY, Context.MODE_PRIVATE).listFiles();
        for ( File article : articles )
        {
            if ( article.isDirectory() )
            {
                continue;
            }

            try ( FileInputStream fis = new FileInputStream(article) )
            {
                ObjectInputStream ois = new ObjectInputStream(fis);
                IRSVPMedia media = (IRSVPMedia) ois.readObject();

                if ( media != null )
                {
                    libraryContents.add(new Item(media, article));
                }
            }
            catch (IOException |ClassNotFoundException e) {}
        }

        Collections.sort(libraryContents);

        this.adapter = new Adapter(getContext(), R.layout.library_item, libraryContents);
        super.setListAdapter(this.adapter);
        this.items = libraryContents;
    }
}
