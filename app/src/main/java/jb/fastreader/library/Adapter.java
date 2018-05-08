package jb.fastreader.library;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.PopupMenu;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import jb.fastreader.R;
import jb.fastreader.Settings;
import jb.fastreader.rsvp.IRSVPMedia;

/**
 * Created by Junaid Begawala on 4/5/18.
 */
class Adapter extends ArrayAdapter<Item>
{
    private Context context;
    private int id;
    private List<Item> items;

    Adapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Item> libraryItems)
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
            convertView = LayoutInflater.from(this.context).inflate(this.id, null);
        }

        final Item item = this.items.get(position);
        if ( item != null )
        {
            TextView title = (TextView) convertView.findViewById(R.id.itemTitle);
            if ( title != null )
            {
                title.setText(item.getTitle());
            }
            TextView subtitle = (TextView) convertView.findViewById(R.id.itemSubTitle);
            if ( subtitle != null )
            {
                String host;

                try
                {
                    host =  new URL(item.getUriString()).getHost();
                }
                catch (MalformedURLException e)
                {
                    host = "";
                }
                subtitle.setText(host);
            }

            TextView date = (TextView) convertView.findViewById(R.id.itemDate);
            if ( date != null )
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                date.setText(dateFormat.format(item.getDate()));
            }

            final ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            if ( progressBar != null )
            {
                progressBar.setProgress(item.getProgress());
            }

            AppCompatImageButton actionMenu = (AppCompatImageButton) convertView.findViewById(R.id.actionMenu);
            if ( actionMenu != null )
            {
                actionMenu.setFocusable(false);
                actionMenu.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Context wrapper = new ContextThemeWrapper(getContext(), R.style.popupThemeDark);
                        PopupMenu popup = new PopupMenu(wrapper, view);
                        popup.getMenuInflater().inflate(R.menu.library_item_action_menu, popup.getMenu());

                        // Don't show option to display extract if not configured, no permission, or no extract
                        if ( !Settings.saveRawExtract(getContext()) || !item.hasExtract() )
                        {
                            MenuItem showRawMenuItem = popup.getMenu().findItem(R.id.actionDebugMenu);
                            if ( showRawMenuItem != null )
                            {
                                showRawMenuItem.setVisible(false);
                            }
                        }
                        popup.show();
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                        {
                             @Override
                             public boolean onMenuItemClick(MenuItem menuItem)
                             {
                                 if ( menuItem.getItemId() == R.id.actionShare )
                                 {
                                     Intent intent = new Intent(Intent.ACTION_SEND);
                                     intent.setType("text/plain");
                                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                                     intent.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
                                     intent.putExtra(Intent.EXTRA_TEXT, item.getUriString());
                                     getContext().startActivity(Intent.createChooser(intent, getContext().getString(R.string.action_share)));
                                 }
                                 else if ( menuItem.getItemId() == R.id.actionRestart )
                                 {
                                     IRSVPMedia media = item.getMedia();
                                     media.setIndex(0);
                                     if ( progressBar != null )
                                     {
                                         progressBar.setProgress(0);
                                     }
                                     FragmentManager fragmentManager = ((AppCompatActivity) Adapter.this.context).getSupportFragmentManager();
                                     jb.fastreader.rsvp.Fragment.openMedia(fragmentManager, media);
                                 }
                                 else if ( menuItem.getItemId() == R.id.actionDelete )
                                 {
                                     AlertDialog.Builder deleteAlert = new AlertDialog.Builder(getContext(), R.style.dialogThemeDark);
                                     deleteAlert.setMessage(R.string.confirm_article_delete);
                                     deleteAlert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                                     {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which)
                                         {
                                             if ( item.delete() )
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
                                 else if ( menuItem.getItemId() == R.id.actionShowRaw )
                                 {
                                     Intent intent = new Intent(Intent.ACTION_VIEW);
                                     String authority = getContext().getApplicationContext().getPackageName() +  ".provider";
                                     Uri extractFile = FileProvider.getUriForFile(getContext(), authority, item.getExtractFilePath());
                                     intent.setDataAndType(extractFile,"text/plain");
                                     intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                     getContext().startActivity(intent);
                                 }
                                 else if ( menuItem.getItemId() == R.id.actionShowProcessed )
                                 {
                                     Intent intent = new Intent(Intent.ACTION_VIEW);
                                     String authority = getContext().getApplicationContext().getPackageName() +  ".provider";
                                     Uri extractFile = FileProvider.getUriForFile(getContext(), authority, item.getProcessedFilePath());
                                     intent.setDataAndType(extractFile,"text/csv");
                                     intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                     getContext().startActivity(intent);
                                 }
                                 return false;
                             }
                        });
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
