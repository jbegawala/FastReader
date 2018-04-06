package jb.fastreader.library;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import jb.fastreader.R;

/**
 * Created by Junaid Begawala on 4/5/18.
 */

class LibraryAdapter extends ArrayAdapter<LibraryItem>
{
    private Context context;
    private int id;
    private List<LibraryItem> items;

    LibraryAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List libraryItems)
    {
        super(context, resource, libraryItems);
        this.context = context;
        this.id = resource;
        this.items = libraryItems;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        if ( convertView == null )
        {
            convertView = LayoutInflater.from(this.context).inflate(id, null);
        }

        LibraryItem item = this.items.get(position);
        if ( item != null )
        {
            TextView title = (TextView) convertView.findViewById(R.id.itemTitle);
            if ( title != null )
            {
                title.setText(item.media.getTitle());
            }
            TextView subtitle = (TextView) convertView.findViewById(R.id.itemSubTitle);
            if ( subtitle != null )
            {
                subtitle.setText(item.media.getSubtitle());
            }
            ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            if ( progressBar != null )
            {
                progressBar.setProgress(item.getProgress());
            }
            ImageView reload = (ImageView) convertView.findViewById(R.id.imageRestart);
            if ( reload != null )
            {
                // set touch listener for reload
            }
        }

        return convertView;
    }

    @Nullable
    @Override
    public LibraryItem getItem(int position)
    {
        return this.items.get(position);
    }
}
