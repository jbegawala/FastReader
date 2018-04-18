package jb.fastreader.library;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import jb.fastreader.R;

/**
 * Created by Junaid Begawala on 4/5/18.
 */
class Adapter extends ArrayAdapter<Item>
{
    private Context context;
    private int id;
    private List<Item> items;

    Adapter(@NonNull Context context, @LayoutRes int resource, @NonNull List libraryItems)
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

        final Item item = this.items.get(position);
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
                String host;

                try
                {
                    host =  new URL(item.media.getUri()).getHost();
                }
                catch (MalformedURLException e)
                {
                    host = "";
                }
                subtitle.setText(host);
            }

            final ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            if ( progressBar != null )
            {
                progressBar.setProgress(item.getProgress());
            }

            ImageView reloadIcon = (ImageView) convertView.findViewById(R.id.imageRestart);
            if ( reloadIcon != null )
            {
                reloadIcon.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        item.media.restart();
                        progressBar.setProgress(0);
                    }
                });
            }

            ImageView deleteIcon = (ImageView) convertView.findViewById(R.id.imageTrash);
            if ( deleteIcon !=null )
            {
                deleteIcon.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        AlertDialog.Builder deleteAlert = new AlertDialog.Builder(getContext(), R.style.dialogThemeDark);
                        deleteAlert.setMessage(R.string.confirm_article_delete);
                        deleteAlert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                if ( item.getFilePath().delete() )
                                {
                                    Adapter.super.remove(item);
                                }
                                else
                                {
                                    Toast.makeText(getContext(), getContext().getString(R.string.failed_article_delete), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        deleteAlert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.cancel();
                            }
                        });
                        deleteAlert.show();
                    }
                });
            }
        }

        return convertView;
    }

    @Nullable
    @Override
    public Item getItem(int position)
    {
        return this.items.get(position);
    }
}
