package jb.fastreader.spritz;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Junaid Begawala on 3/21/18.
 */

public class SpritzTouchListener implements View.OnTouchListener
{
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
   // private final int timeForPauseThreshold = 50;

    private boolean mSetText = false;
    private boolean mAnimatingBack = false;

    private SpritzFragment spritzFragment;
    private View historyView;
    private long pressStart;
    private int tapThreshold;

    public SpritzTouchListener(SpritzFragment spritzFragment, View historyView)
    {
        this.spritzFragment = spritzFragment;
        this.historyView = historyView;
        this.params = historyView.getLayoutParams();
        this.tapThreshold = 100;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            this.pressStart = event.getEventTime();

            int coords[] = new int[2];
            this.historyView.getLocationOnScreen(coords);
            lastTouchY = firstTouchY = event.getRawY();
        }

        else
        {
            if (event.getAction() == MotionEvent.ACTION_UP )  //&& !mAnimatingBack)
            {
                if ( (event.getEventTime() - this.pressStart) < this.tapThreshold )
                {
                    this.spritzFragment.userTap();
                }

//                if (event.getRawY() - firstTouchY < movementForDragThreshold)
//                {
//                    // This is a click, not a drag
//                    // show/hide meta ui on release
//                    if (!spritzerApp.isPlaying()) pauseSpritzer();
//                    else startSpritzer("onTouch");
//                    return false;
//                }
//                peakHeight = event.getRawY() - lastTouchY + this.historyView.getHeight();
//                mAnimatingBack = true;
////                    Log.i("TOUCH", "animating back up " + initHeight + " " + transformTarget.getHeight());
//                this.invokeSpring();

            }
//            else if (event.getAction() == MotionEvent.ACTION_MOVE)
//            {
//                if (spritzerApp.isPlaying() && (event.getEventTime() - event.getDownTime() > timeForPauseThreshold))
//                {
//                    spritzerApp.pause("MotionEvent.ACTION_MOVE");
//                }
//
//                if (!mSetText)
//                {
//                    spritzHistoryView.setText(spritzerApp.getHistoryString(400));
//                    mSetText = true;
//                }
//                float newHeight = event.getRawY() - lastTouchY + this.historyView.getHeight();
////                    Log.i("MOVE", "touch-y: " + event.getRawY() + " lastTouch: " + lastTouchY + " height: " + transformTarget.getHeight());
//
//                if (newHeight > initHeight)
//                {
////                        Log.i("TOUCH", "setting height " + params.height);
//                    params.height = (int) newHeight;
//                    if (newHeight >= fullOpacityHeight) {
//                        this.historyView.setAlpha(1f);
////                            Log.i("TOUCH", "alpha 1");
//                    } else {
//                        this.historyView.setAlpha((newHeight / fullOpacityHeight) * .8f);
////                            Log.i("TOUCH", "alpha " + newHeight / fullOpacityHeight);
//                    }
//                    this.historyView.requestLayout();
//                }
//
//                lastTouchY = event.getRawY();
//            }

        }

        return true;
    }


//    private void invokeSpring()
//    {
//        mAnimatingBack = true;
//        // Create a system to run the physics loop for a set of springs.
//        SpringSystem springSystem = SpringSystem.create();
//
//        // Add a spring to the system.
//        Spring spring = springSystem.createSpring();
//
//        // Add a listener to observe the motion of the spring.
//        spring.addListener(new SimpleSpringListener() {
//
//            @Override
//            public void onSpringUpdate(Spring spring) {
//                // You can observe the updates in the spring
//                // state by asking its current value in onSpringUpdate.
//                float value = (float) spring.getCurrentValue();
//                float scale = 1f - (value);
//                //Log.i("SPRING", String.valueOf(value));
//                // 0 - initHeight
//                // 1 - peakHeight
//                if (scale < 0.05)
//                {
//                    //Log.i("SPRING", "finished");
//                    spritzHistoryView.setText("");
//                    mSetText = false;
//                    params.height = (int) initHeight;
//                    transformTarget.setAlpha(0);
//                    mAnimatingBack = false;
//                    startSpritzer("onSpringUpdate");
//                }
//                else if (mAnimatingBack)
//                {
//                    params.height = (int) ((scale * (peakHeight - initHeight)) + initHeight);
//                    if (this.historyView.getHeight() >= fullOpacityHeight * 2)
//                    {
//                        transformTarget.setAlpha(1f);
//                    }
//                    else
//                    {
//                        //fullOpacityHeight*2 = full
//                        //fullOpacityHeight = empty
//                        transformTarget.setAlpha(Math.max(0, fullOpacityHeight - transformTarget.getHeight() * 1.1f));
//                        //Log.i("TOUCH", "alpha " + touchTarget.getHeight() / fullOpacityHeight);
//                    }
//                }
//                this.historyView.requestLayout();
//            }
//        });
//
//        // Set the spring in motion; moving from 0 to 1
//        spring.setEndValue(1);
//    }
}