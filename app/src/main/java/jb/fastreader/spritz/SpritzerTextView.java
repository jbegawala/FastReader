package jb.fastreader.spritz;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * Created by andrewgiang on 3/3/14.
 */
public class SpritzerTextView extends AppCompatTextView implements View.OnClickListener
{
    public static final String TAG = SpritzerTextView.class.getName();

    public static final int PAINT_WIDTH_DP = 4;          // thickness of guide bars in dp
    // For optimal drawing should be an even number
    private SpritzerCore spritzerCore;
    private Paint mPaintGuides;
    private float mPaintWidthPx;
    private boolean mDefaultClickListener = false;
    private int mAdditionalPadding;
    private int mPivotX;
    private boolean mShouldRefreshMaxLineChars;
    private Path topPivotPath;
    private Path bottomPivotPath;

    public SpritzerTextView(Context context) {
        super(context);
        init();
    }

    public SpritzerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SpritzerTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setAdditionalPadding(attrs);
        mDefaultClickListener = false;  // missing SpritzerTextView_clickControls
        init();

    }

    private void setAdditionalPadding(AttributeSet attrs) {
        //check padding attributes
        int[] attributes = new int[]{android.R.attr.padding, android.R.attr.paddingTop,
                android.R.attr.paddingBottom};

        final TypedArray paddingArray = getContext().obtainStyledAttributes(attrs, attributes);
        try {
            final int padding = paddingArray.getDimensionPixelOffset(0, 0);
            final int paddingTop = paddingArray.getDimensionPixelOffset(1, 0);
            final int paddingBottom = paddingArray.getDimensionPixelOffset(2, 0);
            mAdditionalPadding = Math.max(padding, Math.max(paddingTop, paddingBottom));
            Log.i(TAG, "Additional Padding " + mAdditionalPadding);
        } finally {
            paddingArray.recycle();
        }
    }

    private void init() {
        mShouldRefreshMaxLineChars = true;
        mPivotX = -1;
        int pivotPadding = getPivotPadding();
        setPadding(getPaddingLeft(), pivotPadding, getPaddingRight(), pivotPadding);
        mPaintWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PAINT_WIDTH_DP, getResources().getDisplayMetrics());
        spritzerCore = new SpritzerCore(this);
        mPaintGuides = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintGuides.setStyle(Paint.Style.STROKE);
        mPaintGuides.setColor(getCurrentTextColor());
        mPaintGuides.setStrokeWidth(mPaintWidthPx);
        mPaintGuides.setAlpha(128);
        if (mDefaultClickListener) {
            this.setOnClickListener(this);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
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
        if (mPivotX == -1) {
            mPivotX = calculatePivotXOffset();
        }

        // Measurement for max chars for this TextView
        if (mShouldRefreshMaxLineChars) {
            spritzerCore.setMaxWordLength(this.calculateMonospacedCharacterLimit());
            mShouldRefreshMaxLineChars = false;
        }

        float centerX = mPivotX + getPaddingLeft();
        final int pivotIndicatorLength = getPivotIndicatorLength();

        // Paint the pivot indicator
        drawPivots(canvas, centerX);
        //canvas.drawLine(centerX, topY + (mPaintWidthPx / 2), centerX, topY + (mPaintWidthPx / 2) + pivotIndicatorLength, mPaintGuides); //line through center of circle
        //canvas.drawLine(centerX, bottomY - (mPaintWidthPx / 2), centerX, bottomY - (mPaintWidthPx / 2) - pivotIndicatorLength, mPaintGuides);
    }

    private int calculateMonospacedCharacterLimit()
    {
        int maxChars = Math.round(this.getWidth() / this.calculateLengthOfPrintedMonospaceCharacters(1));
        return maxChars * this.getLineCount();
    }
    private int calculateLengthOfPrintedMonospaceCharacters(float numCharacters)
    {
        // Choice of character is irrelevant given monospace assumption
        // If we abandoned this assumption, we'd have to take the target text as input
        return (int) (this.getPaint().measureText("a", 0, 1) * numCharacters);
    }
    private void drawPivots(Canvas canvas, float centerX) {
        int triSize = 3;
        if (topPivotPath == null) {
            topPivotPath = new Path();
            topPivotPath.setFillType(Path.FillType.EVEN_ODD);
            topPivotPath.moveTo(centerX - PAINT_WIDTH_DP * triSize, (mPaintWidthPx ));
            topPivotPath.lineTo(centerX, PAINT_WIDTH_DP * triSize + (mPaintWidthPx ));
            topPivotPath.lineTo(centerX + PAINT_WIDTH_DP * triSize, (mPaintWidthPx ));
            topPivotPath.lineTo(centerX - PAINT_WIDTH_DP * triSize, (mPaintWidthPx ));
            topPivotPath.close();
        }
        canvas.drawPath(topPivotPath, mPaintGuides);
        if (bottomPivotPath == null) {
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

    private int getPivotPadding() {
        return getPivotIndicatorLength() * 2 + mAdditionalPadding;
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        int pivotPadding = getPivotPadding();
        setPadding(getPaddingLeft(), pivotPadding, getPaddingRight(), pivotPadding);
        mPivotX = calculatePivotXOffset();
        mShouldRefreshMaxLineChars = true;
    }

    private int getPivotIndicatorLength() {
        return getPaint().getFontMetricsInt().bottom;
    }

    private int calculatePivotXOffset() {
        // Measure the rendered distance of CHARS_LEFT_OF_PIVOT chars
        // plus half the pivot character
        return this.calculateLengthOfPrintedMonospaceCharacters(SpritzerCore.CHARS_LEFT_OF_PIVOT + .50f);
    }

    /**
     * Set a custom spritzer
     *
     * @param spritzer
     */
    public void setSpritzer(SpritzerCore spritzer) {
        spritzerCore = spritzer;
        spritzerCore.swapTextView(this);
    }

    /**
     * Will play the spritz text that was set in setSpritzText
     */
    public void play() {
        spritzerCore.start(true, "SpritzerTextView.play");
    }

    public void pause() {
        spritzerCore.pause("SpritzerTextView.pause");
    }


    public SpritzerCore getSpritzer() {
        return spritzerCore;
    }

    @Override
    public void onClick(View v) {
        if (spritzerCore.isPlaying()) {
            pause();
        } else {
            play();
        }

    }
}