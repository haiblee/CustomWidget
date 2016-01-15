package com.haiblee.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * Created by haibiao on 2016/1/14 14:20.
 * email: lihaibiaowork@gmail.com<br/>
 */
public class MarqueeTextView extends TextView implements Runnable{

    public static final String TAG = "MarqueeTextView";

    public static boolean DEBUG = false;

    /**跑一圈回到初始位置之后，停顿的时间*/
    private int mHaltTime = 500;

    /**滚动100px像素的时间，决定跑马灯的速度*/
    private int mUnitDuration = 1000;

    /**视图的宽度*/
    private int mViewWidth;

    /**文字的长度*/
    private int mTextLength;

    /**根据文字长度计算出来的滚动一圈所需要的时间*/
    private int mTextLengthDuration;

    /**跑马灯运行的圈数*/
    private int mRunTotalCount;

    /**当前已运行的圈数*/
    private int mAlreadyCount;

    private Scroller mScroller;

    /**跑马灯状态，静止不动*/
    public static final int STATE_SCROLL_IDLE = 0;

    /**跑马灯状态，文字往左边滑出视图的状态*/
    public static final int STATE_SCROLL_OUT = 1;

    /**跑马灯状态，文字从视图右边滑进视图的状态*/
    public static final int STATE_SCROLL_IN = 2;

    /**当前状态*/
    private int mCurrentState = STATE_SCROLL_IDLE;

    private OnMarqueeListener mListener;

    /**View初始状态的scrollX值*/
    private int mInitScrollX;

    public MarqueeTextView(Context context) {
        this(context, null);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(context,new LinearInterpolator());
        setSingleLine();
        setMaxLines(1);
    }

    /**
     * 开始允许跑马灯
     * @param count 运行的次数，if(count < 0) count = Integer.MAX_VALUE;
     */
    public void startMarquee(int count){
        if(mCurrentState != STATE_SCROLL_IDLE){
            return;
        }
        mTextLength = (int) getPaint().measureText(getText().toString());
        mViewWidth = getMeasuredWidth();
        if(mTextLength > mViewWidth){
            mTextLengthDuration = (mTextLength / 100 + 1) * mUnitDuration;
            if(count < 0) count = Integer.MAX_VALUE;
            mRunTotalCount = count;
            mAlreadyCount = 0;
            performMarqueeOut();
        }else{
            Log.i(TAG,"文字长度小于视图宽度，拒绝运行跑马灯");
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if(mCurrentState == STATE_SCROLL_IDLE){
            return;
        }
        if(mScroller.computeScrollOffset()){
            if(DEBUG){
                Log.i(TAG,"computeScroll,computeScrollOffset = true");
            }
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }else{
            if(DEBUG) {
                Log.i(TAG, "computeScroll,computeScrollOffset = false,mCurrentState = " + mCurrentState);
            }
            if(mCurrentState == STATE_SCROLL_OUT){
                //文字往左边滑出完成，马上进行从右边滑入
                performMarqueeIn();
            }else if(mCurrentState == STATE_SCROLL_IN){
                //文字从右边滑入完成，一次跑马灯完成
                mAlreadyCount++;
                //是否达到目标次数
                if(mAlreadyCount < mRunTotalCount){
                    if(DEBUG) {
                        Log.i(TAG, "computeScroll,computeScrollOffset = false,mCurrentState = " + mCurrentState + ",mAlreadyCount = " + mAlreadyCount);
                    }
                    //作稍许停顿
                    postDelayed(this,mHaltTime);
                }else{
                    if(DEBUG) {
                        Log.i(TAG, "computeScroll,computeScrollOffset = false,reset()");
                    }
                    //复位。
                    reset(true);
                }
            }
        }
    }

    public void setMarqueeListener(OnMarqueeListener listener){
        mListener = listener;
    }

    public void setUnitDuration(int unitDuration) {
        this.mUnitDuration = unitDuration;
    }

    public int getAlreadyCount() {
        return mAlreadyCount;
    }

    public int getRunTotalCount() {
        return mRunTotalCount;
    }

    public void setHaltTime(int haltTime) {
        this.mHaltTime = haltTime;
    }

    public int getMarqueeState(){
        return mCurrentState;
    }

    public void stopMarquee(){
        reset(false);
    }

    /**
     * 重置至初始状态
     * @param isNotify 是否通知监听器
     */
    private void reset(boolean isNotify){
        scrollTo(mInitScrollX,getScrollY());
        mCurrentState = STATE_SCROLL_IDLE;
        if(isNotify){
            notifyListener();
        }
        mAlreadyCount = 0;
    }
    @Override
    public void run() {
        performMarqueeOut();
    }

    private void performMarqueeOut(){
        mScroller.startScroll(getScrollX(),getScrollY(),mTextLength,getScrollY(),mTextLengthDuration);
        mCurrentState = STATE_SCROLL_OUT;
        notifyListener();
        invalidate();
        if(DEBUG) {
            Log.i(TAG, "performMarqueeOut,mTextLength = " + mTextLength + ",duration = " + mTextLengthDuration + ",mRunTotalCount = " + mRunTotalCount);
        }
    }

    private void performMarqueeIn(){
        //此时先将文字瞬间移动到右边，然后从右边滑入
        int duration = (int) (mTextLengthDuration * 1.0f / mTextLength * mViewWidth);
        mScroller.startScroll(mInitScrollX - mViewWidth, getScrollY(),mViewWidth, getScrollY(), duration);
        mCurrentState = STATE_SCROLL_IN;
        notifyListener();
        if(DEBUG) {
            Log.i(TAG, "performMarqueeIn,duration = " + duration);
        }
        invalidate();
    }

    private void notifyListener(){
        if(mListener != null){
            mListener.onStateChange(this,mCurrentState);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = getMeasuredWidth();
        mInitScrollX = getScrollX();
    }



    public interface OnMarqueeListener{
        /**
         * 跑马灯状态变化
         * @param marqueeTextView 跑马灯对象
         * @param state 跑马灯状态
         */
        void onStateChange(MarqueeTextView marqueeTextView, int state);
    }
}
