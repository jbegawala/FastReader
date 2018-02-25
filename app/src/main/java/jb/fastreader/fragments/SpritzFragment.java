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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
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

    private TextView authorView;
    private TextView titleView;
    private TextView chapterView;
    private TextView spritzHistoryView;
    private ProgressBar progressBar;
    private SpritzerTextView spritzView;
    private Bus bus;
    private SpritzFragmentHandler mHandler;

    public SpritzFragment()
    {
        // TODO should i initialize sprtizerApp?

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
//        Why is this commented out?
        if (AppSpritzer.isHttpUri(mediaUri))
        {
            spritzerApp.setTextAndStart(getString(R.string.loading), false);
            this.progressBar.setIndeterminate(true);
        }
    }

    /**
     * Update the UI related to Book Title, Author,
     * and current progress. Everything but the {@link SpritzerTextView}
     */
    public void updateMetaUi() {
        if (!spritzerApp.isMediaSelected()) {
            return;
        }

        ISpritzerMedia book = spritzerApp.getMedia();

        authorView.setText(book.getAuthor());
        titleView.setText(book.getTitle());

        int curChapter = spritzerApp.getCurrentChapter();

        String chapterText = spritzerApp.getMedia().getChapterTitle(curChapter);

        int startSpan = chapterText.length();
        chapterText = String.format("%d of %d words (%d%%)", 10, 20, 50);
        int endSpan = chapterText.length();
        Spannable spanRange = new SpannableString(chapterText);
        TextAppearanceSpan tas = new TextAppearanceSpan(chapterView.getContext(), R.style.MinutesToGo);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        chapterView.setText(spanRange);

        final int progressScale = 10;
        int progress;
        // If the spritzer is showing a special message
        // don't factor current word queue completeness
        // into progress.
        if (spritzerApp.isSpritzingSpecialMessage()) {
            progress = curChapter;
        } else {
            progress = curChapter * progressScale + ((int) (progressScale * (spritzerApp.getQueueCompleteness())));
        }
        progressBar.setMax((spritzerApp.getMaxChapter() + 1) * progressScale);

        progressBar.setProgress(progress);
    }

    private void hideMetaInfo()
    {
        this.authorView.setVisibility(View.INVISIBLE);
        this.titleView.setVisibility(View.INVISIBLE);
        this.chapterView.setVisibility(View.INVISIBLE);
        this.progressBar.setVisibility(View.INVISIBLE);
    }

    private void showMetaInfo()
    {
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
        {
            this.authorView.setVisibility(View.VISIBLE);
        }
        this.titleView.setVisibility(View.VISIBLE);
        this.chapterView.setVisibility(View.VISIBLE);
        this.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideActionBar()
    {
        ActionBar bar = getActivity().getActionBar();
        if ( bar != null)
        {
            bar.hide();
        }
    }
    private void showActionBar()
    {
        ActionBar bar = getActivity().getActionBar();
        if ( bar != null)
        {
            bar.show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_spritz, container, false);
        authorView = ((TextView) root.findViewById(R.id.author));

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            authorView.setVisibility(View.GONE);
        }
        titleView = ((TextView) root.findViewById(R.id.url));
        chapterView = ((TextView) root.findViewById(R.id.chapter));
        chapterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spritzerApp.getMaxChapter() > 1) {
                    bus.post(new ChapterSelectRequested());
                }
            }
        });

        this.spritzHistoryView = (TextView) root.findViewById(R.id.spritzHistory);
        this.progressBar = ((ProgressBar) root.findViewById(R.id.progress));
        this.spritzView = (SpritzerTextView) root.findViewById(R.id.spritzText);
        //spritzView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));
        //spritzHistoryView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));
        this.setupViews(spritzView, spritzHistoryView);
        return root;
    }

    private void pauseSpritzer() {
        spritzerApp.pause();
        this.updateMetaUi();
        this.showMetaInfo();
        this.showActionBar();
    }

    private void startSpritzer() {
        spritzerApp.start(true);
        this.hideMetaInfo();
        this.hideActionBar();
    }

    /**
     * Adjust the target View's height in proportion to
     * drag events. On drag release, snap the view back into
     * it's original place.
     */
    private void setupViews(final View touchTarget, final View transformTarget) {
        touchTarget.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (initHeight == 0)
                    initHeight = transformTarget.getHeight();
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
            public boolean onTouch(View v, MotionEvent event) {
                if (params == null) params = transformTarget.getLayoutParams();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (spritzerApp.isPlaying())
                        spritzerApp.pause();
                    else
                        spritzerApp.start(true);
//                    Log.i("TOUCH", "Down");
                    int coords[] = new int[2];
                    transformTarget.getLocationOnScreen(coords);
                    lastTouchY = firstTouchY = event.getRawY();
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (spritzerApp.isPlaying() && (event.getEventTime() - event.getDownTime() > timeForPauseThreshold)) spritzerApp.pause();
                    if (!mSetText) {
                        spritzHistoryView.setText(spritzerApp.getHistoryString(400));
                        mSetText = true;
                    }
                    float newHeight = event.getRawY() - lastTouchY + transformTarget.getHeight();
//                    Log.i("MOVE", "touch-y: " + event.getRawY() + " lastTouch: " + lastTouchY + " height: " + transformTarget.getHeight());
                    if (newHeight > initHeight) {
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
                if (event.getAction() == MotionEvent.ACTION_UP && !mAnimatingBack) {
                    if (event.getRawY() - firstTouchY < movementForDragThreshold) {
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
                        if (scale < 0.05) {
                            //Log.i("SPRING", "finished");
                            spritzHistoryView.setText("");
                            mSetText = false;
                            params.height = (int) initHeight;
                            transformTarget.setAlpha(0);
                            mAnimatingBack = false;
                            startSpritzer();
                        } else if (mAnimatingBack) {
                            params.height = (int) ((scale * (peakHeight - initHeight)) + initHeight);
                            if (transformTarget.getHeight() >= fullOpacityHeight * 2) {
                                transformTarget.setAlpha(1f);
                            } else {
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
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        FastReaderApplication app = (FastReaderApplication) getActivity().getApplication();
        bus = app.getBus();
        bus.register(this);
        mHandler = new SpritzFragmentHandler(this);
        if (spritzerApp == null)
        {
            spritzerApp = new AppSpritzer(bus, spritzView);
            spritzView.setSpritzer(spritzerApp);
            if (spritzerApp.getMedia() == null) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SPRITZ_TEXT, getString(R.string.select_epub)), 1500);
            } else {
                // AppSpritzer loaded the last book being read
                updateMetaUi();
                showMetaInfo();
            }
        }
        else
        {
            spritzerApp.setEventBus(bus);
            spritzView.setSpritzer(spritzerApp);
            if (!spritzerApp.isPlaying()) {
                updateMetaUi();
                showMetaInfo();
            } else {
                // If the spritzer is currently playing, be sure to hide the ActionBar
                // Might the Android linter be a bit aggressive with these null checks?
                if (getActivity() != null && getActivity().getActionBar() != null) {
                    hideActionBar();
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (spritzerApp != null) {
            spritzerApp.saveState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bus != null) {
            bus.unregister(this);
        }
    }

    public AppSpritzer getSpritzer()
    {
        return this.spritzerApp;
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
                            spritzer.chapterView.setVisibility(View.INVISIBLE);
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
