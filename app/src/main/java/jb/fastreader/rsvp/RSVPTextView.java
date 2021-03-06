package jb.fastreader.rsvp;

import android.support.v4.app.FragmentActivity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import com.squareup.otto.Bus;

import jb.fastreader.R;

/**
 * Created by andrewgiang on 3/3/14.
 */
public class RSVPTextView extends AppCompatTextView
{
    public static final String TAG = RSVPTextView.class.getSimpleName();

    public static final int PAINT_WIDTH_DP = 4;          // thickness of guide bars in dp
                                                         // For optimal drawing should be an even number
    private Paint mPaintGuides;
    private float mPaintWidthPx;
    private int mAdditionalPadding;
    private int mPivotX;
    private Path topPivotPath;
    private Path bottomPivotPath;

    private FragmentActivity activity;
    private IRSVPMedia content = null;
    private boolean isPlaying;
    private boolean isPlayingRequest;
    private boolean threadStarted;
    private Object threadSync;
    private int wpm;
    private Bus bus;

    public RSVPTextView(Context context)
    {
        super(context);
        init();
    }

    public RSVPTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public RSVPTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(attrs);
    }

    void setContent(IRSVPMedia content)
    {
        this.content = content;
    }

    private void init(AttributeSet attrs) {
        setAdditionalPadding(attrs);
        init();

    }

    private void setAdditionalPadding(AttributeSet attrs) {
        //check padding attributes
        int[] attributes = new int[]{android.R.attr.padding, android.R.attr.paddingTop,
                android.R.attr.paddingBottom};

        final TypedArray paddingArray = getContext().obtainStyledAttributes(attrs, attributes);
        try {
            final int padding = paddingArray.getDimensionPixelOffset(0, 0);
            // final int paddingTop = paddingArray.getDimensionPixelOffset(1, 0);
            // final int paddingBottom = paddingArray.getDimensionPixelOffset(2, 0);
            final int paddingTop = 0;
            final int paddingBottom = 0;
            mAdditionalPadding = Math.max(padding, Math.max(paddingTop, paddingBottom));
            Log.i(TAG, "Additional Padding " + mAdditionalPadding);
        } finally {
            paddingArray.recycle();
        }
    }

    private void init()
    {
        mPivotX = -1;
        int pivotPadding = getPivotPadding();
        setPadding(getPaddingLeft(), pivotPadding, getPaddingRight(), pivotPadding);
        mPaintWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PAINT_WIDTH_DP, getResources().getDisplayMetrics());
        mPaintGuides = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintGuides.setStyle(Paint.Style.STROKE);
        mPaintGuides.setColor(getCurrentTextColor());
        mPaintGuides.setStrokeWidth(mPaintWidthPx);
        mPaintGuides.setAlpha(128);
    }

    void setWpm(int wpm)
    {
        this.wpm = wpm;
    }

    void setBus(Bus bus)
    {
        this.bus = bus;
    }

    void setActivity(FragmentActivity activity)
    {
        this.activity = activity;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        // Measurements for top & bottom guide line
        int beginTopX = 0;
        int endTopX = getMeasuredWidth();
        int topY = 0;

        int beginBottomX = 0;
        int endBottomX = getMeasuredWidth();
        int bottomY = getMeasuredHeight();
        // Paint the top guide and bottom guide bars
        //canvas.drawLine(beginTopX, topY, endTopX, topY, mPaintGuides);
        //canvas.drawLine(beginBottomX, bottomY, endBottomX, bottomY, mPaintGuides);

        // Measurements for pivot indicator
        if (mPivotX == -1)
        {
            mPivotX = calculatePivotXOffset();
        }


        float centerX = mPivotX + getPaddingLeft();
        final int pivotIndicatorLength = getPivotIndicatorLength();

        // Paint the pivot indicator
        this.drawPivots(canvas, centerX);
        //canvas.drawLine(centerX, topY + (mPaintWidthPx / 2), centerX, topY + (mPaintWidthPx / 2) + pivotIndicatorLength, mPaintGuides); //line through center of circle
        //canvas.drawLine(centerX, bottomY - (mPaintWidthPx / 2), centerX, bottomY - (mPaintWidthPx / 2) - pivotIndicatorLength, mPaintGuides);
    }

    private void drawPivots(Canvas canvas, float centerX)
    {
        int triSize = 3;
        if (topPivotPath == null)
        {
            topPivotPath = new Path();
            topPivotPath.setFillType(Path.FillType.EVEN_ODD);
            topPivotPath.moveTo(centerX - PAINT_WIDTH_DP * triSize, (mPaintWidthPx ));
            topPivotPath.lineTo(centerX, PAINT_WIDTH_DP * triSize + (mPaintWidthPx ));
            topPivotPath.lineTo(centerX + PAINT_WIDTH_DP * triSize, (mPaintWidthPx ));
            topPivotPath.lineTo(centerX - PAINT_WIDTH_DP * triSize, (mPaintWidthPx ));
            topPivotPath.close();
        }
        canvas.drawPath(topPivotPath, mPaintGuides);

        if (bottomPivotPath == null)
        {
            bottomPivotPath = new Path();
            bottomPivotPath.setFillType(Path.FillType.WINDING);
            bottomPivotPath.moveTo(centerX - PAINT_WIDTH_DP * triSize, getMeasuredHeight() - (mPaintWidthPx ));
            bottomPivotPath.lineTo(centerX, getMeasuredHeight() - PAINT_WIDTH_DP * triSize - (mPaintWidthPx ));
            bottomPivotPath.lineTo(centerX + PAINT_WIDTH_DP * triSize, getMeasuredHeight() - (mPaintWidthPx ));
            bottomPivotPath.lineTo(centerX - PAINT_WIDTH_DP * triSize, getMeasuredHeight() - (mPaintWidthPx ));
            bottomPivotPath.close();
        }
        canvas.drawPath(bottomPivotPath, mPaintGuides);
    }

    private int getPivotPadding()
    {
        return getPivotIndicatorLength() * 2 + mAdditionalPadding;
    }

    @Override
    public void setTextSize(float size)
    {
        super.setTextSize(size);
        int pivotPadding = getPivotPadding();
        setPadding(getPaddingLeft(), pivotPadding, getPaddingRight(), pivotPadding);
        mPivotX = calculatePivotXOffset();
    }

    private int getPivotIndicatorLength()
    {
        return getPaint().getFontMetricsInt().bottom;
    }

    private int calculatePivotXOffset()
    {
        // Measure the rendered distance of CHARS_LEFT_OF_PIVOT chars
        // plus half the pivot character
        return this.calculateLengthOfPrintedMonospaceCharacters(Media.CHARS_LEFT_OF_PIVOT + .50f);
    }

    void setSyncObject(Object threadSync)
    {
        this.threadSync = threadSync;
    }

    private int calculateLengthOfPrintedMonospaceCharacters(float numCharacters)
    {
        // Choice of character is irrelevant given monospace assumption
        // If we abandoned this assumption, we'd have to take the target text as input
        return (int) (this.getPaint().measureText("a", 0, 1) * numCharacters);
    }

    public void play()
    {
        if ( this.content == null )
        {
            return;
        }
        if ( this.isPlaying() )
        {
            return;
        }

        this.isPlayingRequest = true;

        // start background thread
        synchronized ( this.threadSync)
        {
            if ( !this.threadStarted )
            {
                this.activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        new Thread(new Runnable()
                        {
                            @Override
                            public void run() {
                                RSVPTextView.this.setIsPlaying();

                                while (RSVPTextView.this.shouldPlay())
                                {
                                    try
                                    {
                                        RSVPTextView.this.showOneWord();
                                    }

                                    catch (InterruptedException e)
                                    {
                                        Log.e(TAG, getResources().getString(R.string.error_while_playing));
                                        e.printStackTrace();
                                    }
                                }
                                RSVPTextView.this.setNotPlaying();
                            }
                        }).start();
                    }
                });
            }
        }
    }
    void setIsPlaying()
    {
        this.isPlaying = true;
        synchronized ( this.threadSync)
        {
            this.threadStarted = true;
        }
    }

    void setNotPlaying()
    {
        this.isPlaying = false;
        synchronized ( this.threadSync)
        {

            this.threadSync.notify();
        }
        this.threadStarted = false;

        if ( this.content != null & !this.content.hasNext() )
        {
            this.bus.post(Core.BusEvent.CONTENT_FINISHED);
        }
    }

    public void pause()
    {
        this.isPlayingRequest = false;
    }

    public boolean isPlaying()
    {
        return this.isPlaying;
    }

    boolean shouldPlay()
    {
        return this.isPlayingRequest;
    }

    private int getInterWordDelay()
    {
        return 60000 / this.wpm;
    }

    int getWpm()
    {
        return this.wpm;
    }

    void showOneWord() throws InterruptedException
    {
        if ( !this.content.hasNext() )
        {
            this.isPlayingRequest = false;
            return;
        }

        Word word = this.content.next();

        for ( int i = 0; i < word.getFragmentCount(); i++ )
        {
            Spannable spanRange = new SpannableString(word.getWordFragment(i));
            TextAppearanceSpan tas = new TextAppearanceSpan(getContext(), R.style.PivotLetter);
            spanRange.setSpan(tas, word.getPivotPosition(i), word.getPivotPosition(i)+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.setText(spanRange);

            Thread.sleep(this.getInterWordDelay() * word.getDelayFactor(i));
        }
    }
}