package jb.fastreader.rsvp;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
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

import jb.fastreader.Settings;
import jb.fastreader.FastReader;
import jb.fastreader.R;

import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

import static jb.fastreader.library.Library.LIBRARY_FRAGMENT;

public class Fragment extends android.support.v4.app.Fragment
{
    private static final String TAG = Fragment.class.getSimpleName();

    private Core core;

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

    private RSVPTextView textView;
    private Bus bus;

    public Fragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreateView: start");

        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.rsvp_fragment, container, false);
        this.contentTitle = ((TextView) root.findViewById(R.id.contentTitle));
        this.contentSubtitle = ((TextView) root.findViewById(R.id.contentSubtitle));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            this.contentSubtitle.setVisibility(View.GONE);
        }

        this.statusText = ((TextView) root.findViewById(R.id.statusText));
        this.statusVisual = ((ProgressBar) root.findViewById(R.id.statusVisual));

        this.speedQuickToggle = (Switch) root.findViewById(R.id.speedSwitch);
        this.speedQuickToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton speedSwitch, boolean isChecked)
            {
                Context context = getActivity().getBaseContext();
                int wpm = isChecked ? Settings.getFastWpm(context) : Settings.getSlowWpm(context);
                Fragment.this.core.setWpm(wpm);
            }
        });

        this.textView = (RSVPTextView) root.findViewById(R.id.RSVPTextView);
        //this.textView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));
        this.textView.setActivity(getActivity());
        this.textView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Fragment.this.pause();
            }
        });

        this.playButtonView = (ImageView) root.findViewById(R.id.playButtonView);
        this.playButtonView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Fragment.this.play();
            }
        });

        this.rewindCurSentence = (ImageView) root.findViewById(R.id.rewindCurrentSentence);
        this.rewindCurSentence.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Fragment.this.core.rewindCurrentSentence();
                Fragment.this.play();
            }
        });
        this.rewindPrevSentence = (ImageView) root.findViewById(R.id.rewindPreviousSentence);
        this.rewindPrevSentence.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Fragment.this.core.rewindPreviousSentence();
                Fragment.this.play();
            }
        });
        this.rewindCurParagraph = (ImageView) root.findViewById(R.id.rewindCurrentParagraph);
        this.rewindCurParagraph.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Fragment.this.core.rewindCurrentParagraph();
                Fragment.this.play();
            }
        });


        this.loadingIcon = (ProgressBar) root.findViewById(R.id.loadingIcon);

        FastReader app = (FastReader) getActivity().getApplication();
        this.bus = app.getBus();
        this.bus.register(this);
        this.core = new Core(getContext(), this.bus, textView);
        Bundle bundle = getArguments();
        if ( bundle != null )
        {
            IRSVPMedia media = (IRSVPMedia) getArguments().getSerializable("media");
            if ( media != null )
            {
                this.core.setMedia(media);
                this.pause();
            }
        }

        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Should this just call pause/startSprizter?
        if (!this.core.isPlaying())
        {
            this.updateMetaInfo();
            this.showMetaInfo();
        }
    }

    public void openURI(Uri mediaUri)
    {
        if (this.core == null)
        {
            Toast.makeText(getContext().getApplicationContext(), RSVP.class.getSimpleName() + " app not loaded", Toast.LENGTH_LONG).show();
        }
        else
        {
            this.core.openMedia(mediaUri);
            if ( this.core.getMediaParseStatus() == Core.MediaParseStatus.IN_PROGRESS )
            {
                this.init();
            }
            else
            {
                this.pause();
            }
        }
    }

    @Subscribe
    public void ProcessBusEvent(Core.BusEvent event)
    {
        Log.i(TAG, "ProcessBusEvent: " + event.name());
        if (event == Core.BusEvent.CONTENT_PARSED)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Fragment.this.loadingIcon.setVisibility(View.INVISIBLE);
                    Fragment.this.pause();
                }
            });
        }
        else if (event == Core.BusEvent.CONTENT_FINISHED)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Fragment.this.endOfArticle();
                }
            });
        }
    }

    @Subscribe
    public void ProcessBusEvent(final BusEventFail eventFail)
    {
        if ( eventFail.getEvent() == Core.BusEvent.WEBSERVICE_FAIL || eventFail.getEvent() == Core.BusEvent.ARTICLESAVE_FAIL )
        {
            // Go back to library
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    jb.fastreader.library.Fragment libraryFragment = (jb.fastreader.library.Fragment) getFragmentManager().findFragmentByTag(LIBRARY_FRAGMENT);
                    if ( libraryFragment == null )
                    {
                        libraryFragment = new jb.fastreader.library.Fragment();
                    }
                    getFragmentManager().beginTransaction().replace(R.id.activity, libraryFragment, LIBRARY_FRAGMENT).commit();

                    Toast.makeText(getContext(), eventFail.getDetails(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void init()
    {
        this.updateMetaInfo();
        this.showMetaInfo();
        this.textView.setVisibility(View.INVISIBLE);
        this.loadingIcon.setVisibility(View.VISIBLE);
        this.hideNavButtons();
    }

    private void pause()
    {
        this.core.pause();
        this.updateMetaInfo();
        this.showMetaInfo();
        this.textView.setVisibility(View.INVISIBLE);
        this.showNavButtons();
        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void play()
    {
        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        this.hideMetaInfo();
        this.textView.setVisibility(View.VISIBLE);
        this.hideNavButtons();
        this.core.start();
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
        this.loadingIcon.setVisibility(View.INVISIBLE);
        this.textView.setVisibility(View.INVISIBLE);
        this.hideNavButtons();
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

    public void updateMetaInfo()
    {
        if ( !this.core.isMediaSelected() )
        {
            return;
        }

        IRSVPMedia content = this.core.getMedia();
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

        int currentWord = this.core.getCurrentWordNumber();
        int wordCount = this.core.getWordCount();
        if ( wordCount == 0 )
        {
            this.statusVisual.setIndeterminate(true);
        }
        else
        {
            int progress = currentWord * 100 / wordCount;
            String status = String.format("%d of %d words (%d%%)", currentWord, wordCount, progress);

            Spannable spanRange = new SpannableString(status);
            TextAppearanceSpan tas = new TextAppearanceSpan(statusText.getContext(), R.style.StatusText);
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
        if (this.core != null)
        {
            this.core.saveState();
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
