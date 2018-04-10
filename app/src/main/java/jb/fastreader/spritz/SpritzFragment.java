package jb.fastreader.spritz;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import jb.fastreader.Preferences;
import jb.fastreader.FastReader;
import jb.fastreader.R;

import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class SpritzFragment extends Fragment
{
    private static final String TAG = SpritzFragment.class.getSimpleName();

    private Spritzer spritzer;

    // Meta UI components
    private TextView contentTitle;
    private TextView contentSubtitle;
    private TextView statusText;
    private ProgressBar statusVisual;
    private Switch speedQuickToggle;
    private ImageView playButtonView;
    private ImageView rewindCurSentence;
    private ImageView rewindPrevSentence;
    private ImageView rewindCurParagraph;
    private ProgressBar loadingIcon;

    private TextView spritzHistoryView;
    private SpritzerTextView spritzerTextView;
    private Bus bus;

    public SpritzFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreateView: start");
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.fragment_spritz, container, false);
        this.contentTitle = ((TextView) root.findViewById(R.id.contentTitle));
        this.contentSubtitle = ((TextView) root.findViewById(R.id.contentSubtitle));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            this.contentSubtitle.setVisibility(View.GONE);
        }

        this.statusText = ((TextView) root.findViewById(R.id.statusText));
        this.statusVisual = ((ProgressBar) root.findViewById(R.id.statusVisual));

        this.spritzHistoryView = (TextView) root.findViewById(R.id.spritzHistory);
        this.spritzerTextView = (SpritzerTextView) root.findViewById(R.id.spritzText);
        //spritzerTextView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));
        //spritzHistoryView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));

        this.speedQuickToggle = (Switch) root.findViewById(R.id.speedSwitch);
        this.speedQuickToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton speedSwitch, boolean isChecked)
            {
                Context context = getActivity().getBaseContext();
                int wpm = isChecked ? Preferences.getFastWpm(context) : Preferences.getSlowWpm(context);
                SpritzFragment.this.spritzer.setWpm(wpm);
            }
        });

        SpritzTouchListener touchListener = new SpritzTouchListener(this, this.spritzHistoryView);
        this.spritzerTextView.setOnTouchListener(touchListener);

        this.playButtonView = (ImageView) root.findViewById(R.id.playButtonView);
        this.playButtonView.setOnTouchListener(touchListener);

        this.rewindCurSentence = (ImageView) root.findViewById(R.id.rewindCurrentSentence);
        this.rewindCurSentence.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SpritzFragment.this.spritzer.rewindCurrentSentence();
                SpritzFragment.this.startSpritzer();
            }
        });
        this.rewindPrevSentence = (ImageView) root.findViewById(R.id.rewindPreviousSentence);
        this.rewindPrevSentence.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SpritzFragment.this.spritzer.rewindPreviousSentence();
                SpritzFragment.this.startSpritzer();
            }
        });
        this.rewindCurParagraph = (ImageView) root.findViewById(R.id.rewindCurrentParagraph);
        this.rewindCurParagraph.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SpritzFragment.this.spritzer.rewindCurrentParagraph();
                SpritzFragment.this.startSpritzer();
            }
        });


        this.loadingIcon = (ProgressBar) root.findViewById(R.id.loadingIcon);

        FastReader app = (FastReader) getActivity().getApplication();
        this.bus = app.getBus();
        this.bus.register(this);
        this.spritzer = new Spritzer(getContext(), this.bus, spritzerTextView);
        Bundle bundle = getArguments();
        if ( bundle != null )
        {
            ISpritzerMedia media = (ISpritzerMedia) getArguments().getSerializable("media");
            if ( media != null )
            {
                this.spritzer.setMedia(media);
            }
        }

        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Should this just call pause/startSprizter?
        if (!this.spritzer.isPlaying())
        {
            this.updateMetaInfo();
            this.showMetaInfo();
        }
        else
        {
            this.hideActionBar();
        }
    }

    public void openURI(Uri mediaUri)
    {
        if (this.spritzer == null)
        {
            Toast.makeText(getContext().getApplicationContext(), "Spritz app not loaded", Toast.LENGTH_LONG).show();
        }
        else
        {
            this.spritzer.openMedia(mediaUri);
            if ( this.spritzer.getMediaParseStatus() == Spritzer.MediaParseStatus.IN_PROGRESS )
            {
                this.initSpritzer();
            }
            else
            {
                this.pauseSpritzer();
            }
        }
    }

    @Subscribe
    public void ProcessBusEvent(Spritzer.BusEvent event)
    {
        Log.i(TAG, "ProcessBusEvent: " + event.name());
        if (event == Spritzer.BusEvent.CONTENT_PARSED)
        {
            this.loadingIcon.setVisibility(View.INVISIBLE);
            this.pauseSpritzer();
        }
        else if (event == Spritzer.BusEvent.CONTENT_FINISHED)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SpritzFragment.this.endOfArticle();
                }
            });
        }
    }

    void userTap()
    {
        if (this.spritzer.isPlaying())
        {
            this.pauseSpritzer();
        }
        else
        {
            this.startSpritzer();
        }
    }

    private void initSpritzer()
    {
        this.updateMetaInfo();
        this.showMetaInfo();
        this.spritzerTextView.setVisibility(View.INVISIBLE);
        this.loadingIcon.setVisibility(View.VISIBLE);
        this.hideNavButtons();
        this.showActionBar();
    }

    private void pauseSpritzer()
    {
        this.spritzer.pause();
        this.updateMetaInfo();
        this.showMetaInfo();
        this.spritzerTextView.setVisibility(View.INVISIBLE);
        this.showNavButtons();
        this.showActionBar();
    }

    private void startSpritzer()
    {
        this.hideMetaInfo();
        this.spritzerTextView.setVisibility(View.VISIBLE);
        this.hideNavButtons();
        this.hideActionBar();
        this.spritzer.start();
    }

    private void hideNavButtons()
    {
        this.playButtonView.setVisibility(View.INVISIBLE);
        this.rewindCurSentence.setVisibility(View.INVISIBLE);
        this.rewindPrevSentence.setVisibility(View.INVISIBLE);
        this.rewindCurParagraph.setVisibility(View.INVISIBLE);
    }

    private void showNavButtons()
    {
        this.playButtonView.setVisibility(View.VISIBLE);
        this.rewindCurSentence.setVisibility(View.VISIBLE);
        this.rewindPrevSentence.setVisibility(View.VISIBLE);
        this.rewindCurParagraph.setVisibility(View.VISIBLE);
    }

    private void endOfArticle()
    {
        this.updateMetaInfo();
        this.showMetaInfo();
        this.spritzerTextView.setVisibility(View.INVISIBLE);
        this.playButtonView.setVisibility(View.INVISIBLE);
        this.showActionBar();
    }

    private void hideMetaInfo()
    {
        this.contentTitle.setVisibility(View.INVISIBLE);
        this.contentSubtitle.setVisibility(View.INVISIBLE);
        this.statusText.setVisibility(View.INVISIBLE);
        this.statusVisual.setVisibility(View.INVISIBLE);
        this.speedQuickToggle.setVisibility(View.INVISIBLE);
    }

    private void showMetaInfo()
    {
        this.contentTitle.setVisibility(View.VISIBLE);
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
        {
            this.contentSubtitle.setVisibility(View.VISIBLE);
        }
        this.statusText.setVisibility(View.VISIBLE);
        this.statusVisual.setVisibility(View.VISIBLE);
        this.speedQuickToggle.setVisibility(View.VISIBLE);
    }

    private void hideActionBar()
    {
        Activity activity = getActivity();
        if ( activity != null )
        {
            ActionBar bar = getActivity().getActionBar();
            if ( bar != null)
            {
                bar.hide();
            }
        }
    }
    private void showActionBar()
    {
        Activity activity = getActivity();
        if ( activity != null )
        {
            ActionBar bar = activity.getActionBar();
            if ( bar != null)
            {
                bar.show();
            }
        }
    }

    public void updateMetaInfo()
    {
        if ( !this.spritzer.isMediaSelected() )
        {
            return;
        }

        ISpritzerMedia content = this.spritzer.getMedia();
        this.contentTitle.setText(content.getTitle());

        String subtitle;
        try
        {
            subtitle =  new URL(content.getUri()).getHost();
        }
        catch (MalformedURLException e)
        {
            subtitle = "";
        }
        this.contentSubtitle.setText(subtitle);

        int currentWord = this.spritzer.getCurrentWordNumber();
        int wordCount = this.spritzer.getWordCount();
        if ( wordCount == 0 )
        {
            this.statusVisual.setIndeterminate(true);
        }
        else
        {
            int progress = currentWord * 100 / wordCount;
            String status = String.format("%d of %d words (%d%%)", currentWord, wordCount, progress);

            Spannable spanRange = new SpannableString(status);
            TextAppearanceSpan tas = new TextAppearanceSpan(statusText.getContext(), R.style.MinutesToGo);
            spanRange.setSpan(tas, 0, status.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.statusText.setText(spanRange);
            this.statusVisual.setMax(100);
            this.statusVisual.setProgress(progress);
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (this.spritzer != null)
        {
            this.spritzer.saveState();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (this.bus != null)
        {
            this.bus.unregister(this);
        }
    }
}
