package jb.fastreader.library;

import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import jb.fastreader.FastReader;
import jb.fastreader.R;

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

    private void scanAndLoadMedia()
    {
        List<Item> libraryContents = DatabaseHelper.getInstance(getContext()).loadArticles();
        this.adapter = new Adapter(getContext(), R.layout.library_item, libraryContents);
        super.setListAdapter(this.adapter);
        this.items = libraryContents;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        jb.fastreader.rsvp.Fragment.openMediaViaIntent(getFragmentManager(), this.items.get(position).getMedia());
    }
}
