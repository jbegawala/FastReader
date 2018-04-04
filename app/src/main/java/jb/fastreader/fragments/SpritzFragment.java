package jb.fastreader.fragments;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import jb.fastreader.spritz.Spritzer;
import jb.fastreader.FastReaderApplication;
import jb.fastreader.R;
import jb.fastreader.spritz.ISpritzerMedia;
import jb.fastreader.spritz.SpritzerTextView;

import android.widget.Toast;


// Main fragment
public class SpritzFragment extends Fragment
{
    private static final String TAG = SpritzFragment.class.getSimpleName();

    private Spritzer spritzerApp;

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
        View root = inflater.inflate(R.layout.fragment_spritz, container, false);

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
            public void onCheckedChanged(CompoundButton speedSwitch, boolean isChecked )
            {
                Context context = getActivity().getBaseContext();
                Resources resources = context.getResources();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                String wpmKey;
                String wpmDefault;
                if ( isChecked )
                {
                    wpmKey = resources.getString(R.string.config_wpm_fast_key);
                    wpmDefault = resources.getString(R.string.config_wpm_fast_default);
                }
                else
                {
                    wpmKey = resources.getString(R.string.config_wpm_slow_key);
                    wpmDefault = resources.getString(R.string.config_wpm_slow_default);
                }
                spritzerApp.setWpm(Integer.parseInt(sharedPreferences.getString(wpmKey, wpmDefault)));
            }
        });

        SpritzTouchListener touchListener = new SpritzTouchListener(this, this.spritzHistoryView);
        this.spritzerTextView.setOnTouchListener(touchListener);

        this.playButtonView = (ImageView) root.findViewById(R.id.playButtonView);
        this.playButtonView.setOnTouchListener(touchListener);

        this.rewindCurSentence = (ImageView) root.findViewById(R.id.rewindCurrentSentence);
        this.rewindCurSentence.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                spritzerApp.rewindCurrentSentence();
                startSpritzer();
            }
        });
        this.rewindPrevSentence = (ImageView) root.findViewById(R.id.rewindPreviousSentence);
        this.rewindPrevSentence.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                spritzerApp.rewindPreviousSentence();
                startSpritzer();
            }
        });
        this.rewindCurParagraph = (ImageView) root.findViewById(R.id.rewindCurrentParagraph);
        this.rewindCurParagraph.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                spritzerApp.rewindCurrentParagraph();
                startSpritzer();
            }
        });


        this.loadingIcon = (ProgressBar) root.findViewById(R.id.loadingIcon);

        FastReaderApplication app = (FastReaderApplication) getActivity().getApplication();
        this.bus = app.getBus();
        this.bus.register(this);

        Context context = getContext();
        Resources resources = context.getResources();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int wpm = Integer.parseInt(sharedPreferences.getString(resources.getString(R.string.config_wpm_fast_key), resources.getString(R.string.config_wpm_fast_default)));

        this.spritzerApp = new Spritzer(this.bus, spritzerTextView, wpm);

        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Should this just call pause/startSprizter?
        if (!spritzerApp.isPlaying())
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
        if (spritzerApp == null)
        {
            Toast.makeText(getContext().getApplicationContext(), "Spritz app not loaded", Toast.LENGTH_LONG).show();
//            Context context = getContext();
//            Resources resources = context.getResources();
//            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//            int wpm = Integer.parseInt(sharedPreferences.getString(resources.getString(R.string.config_wpm_fast_key), resources.getString(R.string.config_wpm_fast_default)));
//
//            spritzerApp = new Spritzer(this.bus, this.spritzerTextView, wpm, mediaUri);
//            Log.i(TAG, "feedMediaUriToSpritzer called without spritzerApp");
        }
        else
        {
            spritzerApp.openMedia(mediaUri);
            if ( spritzerApp.getMediaParseStatus() == Spritzer.MediaParseStatus.IN_PROGRESS )
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
                    endOfArticle();
                }
            });
        }
    }

    void userTap()
    {

        if (spritzerApp.isPlaying())
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
        this.spritzerApp.pause();
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
        spritzerApp.start();
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
        FragmentActivity activity = getActivity();
        if ( activity != null )
        {
            ActionBar bar = activity.getActionBar();
            if ( bar != null)
            {
                bar.hide();
            }
        }
    }
    private void showActionBar()
    {
        FragmentActivity activity = getActivity();
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
        if ( !this.spritzerApp.isMediaSelected() )
        {
            return;
        }

        ISpritzerMedia content = this.spritzerApp.getMedia();
        this.contentTitle.setText(content.getTitle());
        this.contentSubtitle.setText(content.getSubtitle());

        int currentWord = spritzerApp.getCurrentWordNumber();
        int wordCount = spritzerApp.getWordCount();
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
        if (spritzerApp != null)
        {
            spritzerApp.saveState();
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
