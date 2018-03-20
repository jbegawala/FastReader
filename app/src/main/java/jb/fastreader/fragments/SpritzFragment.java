package jb.fastreader.fragments;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.squareup.otto.Bus;

import java.lang.ref.WeakReference;

import jb.fastreader.AppSpritzer;
import jb.fastreader.FastReaderApplication;
import jb.fastreader.R;
import jb.fastreader.events.ChapterSelectRequested;
import jb.fastreader.formats.ISpritzerMedia;
import jb.spritzer.SpritzerTextView;
import jb.fastreader.Preferences;


// Main fragment
public class SpritzFragment extends Fragment
{
    private static final String TAG = SpritzFragment.class.getSimpleName();

    // SpritzFragmentHandler Message codes
    protected static final int MSG_SPRITZ_TEXT = 1;
    protected static final int MSG_HIDE_CHAPTER_LABEL = 2;

    private static AppSpritzer spritzerApp;
    static float initHeight;

    // Meta UI components
    private TextView contentTitle;
    private TextView contentSubtitle;
    private TextView statusText;
    private ProgressBar statusVisual;
    private Switch speedQuickToggle;

    private TextView spritzHistoryView;
    private SpritzerTextView spritzView;
    private Bus bus;
    private SpritzFragmentHandler mHandler;

    public SpritzFragment()
    {
        // TODO should i initialize sprtizerApp?

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_spritz, container, false);

        this.contentTitle = ((TextView) root.findViewById(R.id.contentTitle));
        this.contentSubtitle = ((TextView) root.findViewById(R.id.contentSubtitle));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            this.contentSubtitle.setVisibility(View.GONE);
        }

        this.statusText = ((TextView) root.findViewById(R.id.statusText));
        this.statusText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spritzerApp.getMaxChapter() > 1) {
                    bus.post(new ChapterSelectRequested());
                }
            }
        });
        this.statusVisual = ((ProgressBar) root.findViewById(R.id.statusVisual));

        this.spritzHistoryView = (TextView) root.findViewById(R.id.spritzHistory);
        this.spritzView = (SpritzerTextView) root.findViewById(R.id.spritzText);
        //spritzView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));
        //spritzHistoryView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));

        this.speedQuickToggle = (Switch) root.findViewById(R.id.speedSwitch);
        this.speedQuickToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton speedSwitch, boolean isChecked )
            {
                if ( isChecked )
                {
                    spritzerApp.setWpm(600);
                }
                else
                {
                    spritzerApp.setWpm(300);
                }
            }
        });

        this.setupViews(this.spritzView, this.spritzHistoryView);

        return root;
    }

    private void setupViews(final View touchTarget, final View transformTarget)
    {
        touchTarget.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (initHeight == 0)
                {
                    initHeight = transformTarget.getHeight();
                }
            }
        });
        touchTarget.setOnTouchListener(new View.OnTouchListener() {

            private ViewGroup.LayoutParams params;
            private float peakHeight;
            private float lastTouchY;
            private float firstTouchY;
            private final float fullOpacityHeight = 300;

            /** The distance between ACTION_DOWN and ACTION_UP, above which should
             * be interpreted as a drag, below which a click.
             */
            private final float movementForDragThreshold = 20;

            /** The time between ACTION_DOWN and ACTION_MOVE, above which should
             * be interpreted as a drag, and the spritzer paused
             */
            private final int timeForPauseThreshold = 50;

            private boolean mSetText = false;
            private boolean mAnimatingBack = false;

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (params == null)
                {
                    params = transformTarget.getLayoutParams();
                }

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    if (spritzerApp.isPlaying())
                    {
                        spritzerApp.pause("MotionEvent.ACTION_DOWN");
                    }
                    else
                    {
                        spritzerApp.start(true);
                    }
                    Log.i("SpritzFragmentTOUCH", "Down");
                    int coords[] = new int[2];
                    transformTarget.getLocationOnScreen(coords);
                    lastTouchY = firstTouchY = event.getRawY();
                }

                else if (event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    if (spritzerApp.isPlaying() && (event.getEventTime() - event.getDownTime() > timeForPauseThreshold))
                    {
                        spritzerApp.pause("MotionEvent.ACTION_MOVE");
                    }

                    if (!mSetText)
                    {
                        spritzHistoryView.setText(spritzerApp.getHistoryString(400));
                        mSetText = true;
                    }
                    float newHeight = event.getRawY() - lastTouchY + transformTarget.getHeight();
//                    Log.i("MOVE", "touch-y: " + event.getRawY() + " lastTouch: " + lastTouchY + " height: " + transformTarget.getHeight());

                    if (newHeight > initHeight)
                    {
//                        Log.i("TOUCH", "setting height " + params.height);
                        params.height = (int) newHeight;
                        if (newHeight >= fullOpacityHeight) {
                            transformTarget.setAlpha(1f);
//                            Log.i("TOUCH", "alpha 1");
                        } else {
                            transformTarget.setAlpha((newHeight / fullOpacityHeight) * .8f);
//                            Log.i("TOUCH", "alpha " + newHeight / fullOpacityHeight);
                        }
                        transformTarget.requestLayout();
                    }

                    lastTouchY = event.getRawY();
                }

                else if (event.getAction() == MotionEvent.ACTION_UP && !mAnimatingBack) {
                    if (event.getRawY() - firstTouchY < movementForDragThreshold)
                    {
                        // This is a click, not a drag
                        // show/hide meta ui on release
                        if (!spritzerApp.isPlaying()) pauseSpritzer();
                        else startSpritzer();
                        return false;
                    }
                    peakHeight = event.getRawY() - lastTouchY + transformTarget.getHeight();
                    mAnimatingBack = true;
//                    Log.i("TOUCH", "animating back up " + initHeight + " " + transformTarget.getHeight());
                    invokeSpring(transformTarget);

                }

                return true;
            }

            private void invokeSpring(final View targetView) {
                mAnimatingBack = true;
                // Create a system to run the physics loop for a set of springs.
                SpringSystem springSystem = SpringSystem.create();

                // Add a spring to the system.
                Spring spring = springSystem.createSpring();

                // Add a listener to observe the motion of the spring.
                spring.addListener(new SimpleSpringListener() {

                    @Override
                    public void onSpringUpdate(Spring spring) {
                        // You can observe the updates in the spring
                        // state by asking its current value in onSpringUpdate.
                        float value = (float) spring.getCurrentValue();
                        float scale = 1f - (value);
                        //Log.i("SPRING", String.valueOf(value));
                        // 0 - initHeight
                        // 1 - peakHeight
                        if (scale < 0.05)
                        {
                            //Log.i("SPRING", "finished");
                            spritzHistoryView.setText("");
                            mSetText = false;
                            params.height = (int) initHeight;
                            transformTarget.setAlpha(0);
                            mAnimatingBack = false;
                            startSpritzer();
                        }
                        else if (mAnimatingBack)
                        {
                            params.height = (int) ((scale * (peakHeight - initHeight)) + initHeight);
                            if (transformTarget.getHeight() >= fullOpacityHeight * 2)
                            {
                                transformTarget.setAlpha(1f);
                            }
                            else
                            {
                                //fullOpacityHeight*2 = full
                                //fullOpacityHeight = empty
                                transformTarget.setAlpha(Math.max(0, fullOpacityHeight - transformTarget.getHeight() * 1.1f));
                                //Log.i("TOUCH", "alpha " + touchTarget.getHeight() / fullOpacityHeight);
                            }
                        }
                        transformTarget.requestLayout();
                    }
                });

                // Set the spring in motion; moving from 0 to 1
                spring.setEndValue(1);
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        FastReaderApplication app = (FastReaderApplication) getActivity().getApplication();
        this.bus = app.getBus();
        this.bus.register(this);
        if (spritzerApp == null)
        {
            spritzerApp = new AppSpritzer(this.bus, spritzView);
            spritzView.setSpritzer(spritzerApp);
            if (spritzerApp.getMedia() == null)
            {
                mHandler = new SpritzFragmentHandler(this);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SPRITZ_TEXT, getString(R.string.select_epub)), 1500);
            }
            else
            {
                this.updateMetaInfo();
                this.showMetaInfo();
            }
        }
        else
        {
            spritzerApp.setEventBus(this.bus);
            spritzView.setSpritzer(spritzerApp);
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
    }

    public void feedMediaUriToSpritzer(Uri mediaUri)
    {
        if (spritzerApp == null)
        {
            spritzerApp = new AppSpritzer(this.bus, this.spritzView, mediaUri);
            this.spritzView.setSpritzer(spritzerApp);
            Log.i(TAG, "feedMediaUriToSpritzer called without spritzerApp");
        }
        else
        {
            spritzerApp.setMediaUri(mediaUri);
            Log.i(TAG, "feedMediaUriToSpritzer called with existing spritzerApp");
        }

        String configWpm = getResources().getString(R.string.config_key_wpm);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//        Log.i(TAG, "configwpm: " + configWpm);
//        int wpm = prefs.getInt("config_key_wpm",300);
//        Log.i(TAG, " Set WPM to: " + wpm);
        int wpm = Preferences.DEFAULT_APP_WPM;
        spritzerApp.setWpm(wpm);
//        Commenting out because synchronous call
//        if (AppSpritzer.isHttpUri(mediaUri))
//        {
//            spritzerApp.setTextAndStart(getString(R.string.loading), false);
//            this.statusVisual.setIndeterminate(true);
//        }
    }

    private void startSpritzer()
    {
        this.hideMetaInfo();
        this.hideActionBar();
        spritzerApp.start(true);
    }

    private void pauseSpritzer()
    {
        spritzerApp.pause("SpritzFragment.pauseSpritzer");
        this.updateMetaInfo();
        this.showMetaInfo();
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
        if (!spritzerApp.isMediaSelected())
        {
            return;
        }
        ISpritzerMedia content = spritzerApp.getMedia();

        this.contentTitle.setText(content.getTitle());
        this.contentSubtitle.setText(content.getAuthor());

        int progress = 0;
        String status = "";
        //if (!spritzerApp.isSpritzingSpecialMessage())
        //{
            int currentWord = spritzerApp.getCurrentWordNumber();
            int wordCount = spritzerApp.getWordCount();
            progress = currentWord * 100 / wordCount;
            status = String.format("%d of %d words (%d%%)", currentWord, wordCount, progress);
        //}
        Spannable spanRange = new SpannableString(status);
        TextAppearanceSpan tas = new TextAppearanceSpan(statusText.getContext(), R.style.MinutesToGo);
        spanRange.setSpan(tas, 0, status.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        this.statusText.setText(spanRange);
        this.statusVisual.setMax(100);
        this.statusVisual.setProgress(progress);
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

    /**
     * A Handler bound to the UI thread. Used to conveniently
     * handle actions that should occur after some delay.
     */
    protected class SpritzFragmentHandler extends Handler {

        private WeakReference<SpritzFragment> mWeakSpritzFragment;

        public SpritzFragmentHandler(SpritzFragment fragment) {
            mWeakSpritzFragment = new WeakReference<SpritzFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Object obj = msg.obj;

            SpritzFragment spritzer = mWeakSpritzFragment.get();
            if (spritzer == null) {
                return;
            }
            switch (what) {
                case MSG_HIDE_CHAPTER_LABEL:
                    if (getActivity() != null) {
                        if (spritzerApp != null && spritzerApp.isPlaying()) {
                            spritzer.statusText.setVisibility(View.INVISIBLE);
                        }
                    }
                    break;
                case MSG_SPRITZ_TEXT:
                    if (spritzerApp != null) {
                        spritzerApp.setTextAndStart((String) obj, false);
                    }
                    break;
            }
        }
    }
}
