package catglo.com.deliverydroid.widgets;


import android.content.Context;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;


public class AutoResizeTextView extends TextView {

    // Minimum text size for this text view
    public static final float MIN_TEXT_SIZE = 20;

    // Interface for resize notifications
    public interface OnTextResizeListener {
        public void onTextResize(TextView textView, float oldSize, float newSize);
    }

    // Our ellipse string
    private static final String mEllipsis = "...";

    // Registered resize listener
    private OnTextResizeListener mTextResizeListener;

    // Flag for text and/or size changes to force a resize
    private boolean mNeedsResize = false;

    // Text size that is set from code. This acts as a starting point for resizing
    private float mTextSize;

    // Temporary upper bounds on the starting text size
    private float mMaxTextSize = 0;

    // Lower bounds for text size
    private float mMinTextSize = MIN_TEXT_SIZE;

    // Text view line spacing multiplier
    private float mSpacingMult = 1.0f;

    // Text view additional line spacing
    private float mSpacingAdd = 0.0f;

    // Add ellipsis to text that overflows at the smallest text size
    private boolean mAddEllipsis = true;

    // Default constructor override
    public AutoResizeTextView(Context context) {
        this(context, null);
    }

    // Default constructor when inflating from XML file
    public AutoResizeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // Default constructor override
    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTextSize = getTextSize();
    }

    /**
     * When text changes, set the force resize flag to true and reset the text size.
     */
    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        mNeedsResize = true;
        // Since this view may be reused, it is good to reset the text size
        resetTextSize();
    }

    /**
     * If the text view size changed, set the force resize flag to true
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            mNeedsResize = true;
        }
    }

    /**
     * Register listener to receive resize notifications
     * @param listener
     */
    public void setOnResizeListener(OnTextResizeListener listener) {
        mTextResizeListener = listener;
    }

    /**
     * Override the set text size to update our internal reference values
     */
    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        mTextSize = getTextSize();
    }

    /**
     * Override the set text size to update our internal reference values
     */
    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        mTextSize = getTextSize();
    }

    /**
     * Override the set line spacing to update our internal reference values
     */
    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        mSpacingMult = mult;
        mSpacingAdd = add;
    }

    /**
     * Set the upper text size limit and invalidate the view
     * @param maxTextSize
     */
    public void setMaxTextSize(float maxTextSize) {
        mMaxTextSize = maxTextSize;
        requestLayout();
        invalidate();
    }

    /**
     * Return upper text size limit
     * @return
     */
    public float getMaxTextSize() {
        return mMaxTextSize;
    }

    /**
     * Set the lower text size limit and invalidate the view
     * @param minTextSize
     */
    public void setMinTextSize(float minTextSize) {
        mMinTextSize = minTextSize;
        requestLayout();
        invalidate();
    }

    /**
     * Return lower text size limit
     * @return
     */
    public float getMinTextSize() {
        return mMinTextSize;
    }

    /**
     * Set flag to add ellipsis to text that overflows at the smallest text size
     * @param addEllipsis
     */
    public void setAddEllipsis(boolean addEllipsis) {
        mAddEllipsis = addEllipsis;
    }

    /**
     * Return flag to add ellipsis to text that overflows at the smallest text size
     * @return
     */
    public boolean getAddEllipsis() {
        return mAddEllipsis;
    }

    /**
     * Reset the text to the original size
     */
    public void resetTextSize() {
        if(mTextSize > 0) {
            super.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
            mMaxTextSize = mTextSize;
        }
    }

    /**
     * Resize text after measuring
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(changed || mNeedsResize) {
            int widthLimit = (right - left) - getCompoundPaddingLeft() - getCompoundPaddingRight();
            int heightLimit = (bottom - top) - getCompoundPaddingBottom() - getCompoundPaddingTop();
            resizeText(widthLimit, heightLimit);
        }
        super.onLayout(changed, left, top, right, bottom);
    }


    /**
     * Resize the text size with default width and height
     */
    public void resizeText() {
        int heightLimit = getHeight() - getPaddingBottom() - getPaddingTop();
        int widthLimit = getWidth() - getPaddingLeft() - getPaddingRight();
        resizeText(widthLimit, heightLimit);
    }

    /**
     * Resize the text size with specified width and height
     * @param width
     * @param height
     */
    @SuppressWarnings("unused")
	public void resizeText(int width, int height) {
        CharSequence text = getText();
        // Do not resize if the view does not have dimensions or there is no text
        if(text == null || text.length() == 0 || height <= 0 || width <= 0 || mTextSize == 0) {
            return;
        }

        // Get the text view's paint object
        TextPaint textPaint = getPaint();

        // Store the current text size
        float oldTextSize = textPaint.getTextSize();
        // If there is a max text size set, use the lesser of that and the default text size
        float targetTextSize = mMaxTextSize > 0 ? Math.min(mTextSize, mMaxTextSize) : mTextSize;

        // Get the required text height
        int textHeight = getTextHeight(text, textPaint, width, targetTextSize);

        // Until we either fit within our text view or we had reached our min text size, incrementally try smaller sizes
        while(textHeight > height && targetTextSize > mMinTextSize) {
            targetTextSize = Math.max(targetTextSize - 2, mMinTextSize);
            textHeight = getTextHeight(text, textPaint, width, targetTextSize);
        }

        // If we had reached our minimum text size and still don't fit, append an ellipsis
        if(mAddEllipsis && targetTextSize == mMinTextSize && textHeight > height) {
        	// Draw using a static layout
        	// modified: use a copy of TextPaint for measuring
        	TextPaint paint = new TextPaint(textPaint);

            StaticLayout layout = new StaticLayout(text, textPaint, width, Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, false);
            // Check that we have a least one line of rendered text
            if(layout.getLineCount() > 0) {
                // Since the line at the specific vertical position would be cut off,
                // we must trim up to the previous line
                int lastLine = layout.getLineForVertical(height) - 1;
                // If the text would not even fit on a single line, clear it
                if(lastLine < 0) {
                    setText("");
                }
                // Otherwise, trim to the previous line and add an ellipsis
                else {
                    int start = layout.getLineStart(lastLine);
                    int end = layout.getLineEnd(lastLine);
                    float lineWidth = layout.getLineWidth(lastLine);
                    float ellipseWidth = textPaint.measureText(mEllipsis);

                    // Trim characters off until we have enough room to draw the ellipsis
                    while(width < lineWidth + ellipseWidth) {
                        if (end<start) break;
                        /*
                        I got the following crash in google play so I added the if (end<start) break;
                        java.lang.StringIndexOutOfBoundsException: length=5; regionStart=0; regionLength=-1
                        at java.lang.String.startEndAndLength(String.java:583)
                        at java.lang.String.substring(String.java:1464)
                        at java.lang.String.subSequence(String.java:1851)
                        at com.catglo.widgets.AutoResizeTextView.resizeText(AutoResizeTextView.java:262)
                        at com.catglo.widgets.AutoResizeTextView.onLayout(AutoResizeTextView.java:192)
                        at android.view.View.layout(View.java:14177)
                        at android.widget.ListView.setupChild(ListView.java:2123)
                        at android.widget.ListView.makeAndAddView(ListView.java:2036)
                        at android.widget.ListView.fillDown(ListView.java:822)
                        at android.widget.ListView.fillFromTop(ListView.java:883)
                        at android.widget.ListView.layoutChildren(ListView.java:1848)
                        at android.widget.AbsListView.onLayout(AbsListView.java:2136)
                        at android.view.View.layout(View.java:14177)
                        at android.view.ViewGroup.layout(ViewGroup.java:4399)
                        at android.widget.RelativeLayout.onLayout(RelativeLayout.java:1021)
                        at android.view.View.layout(View.java:14177)
                        at android.view.ViewGroup.layout(ViewGroup.java:4399)
                        at android.support.v4.view.ViewPager.onLayout(ViewPager.java:1589)
                        at android.view.View.layout(View.java:14177)
                        at android.view.ViewGroup.layout(ViewGroup.java:4399)
                        at android.widget.FrameLayout.onLayout(FrameLayout.java:448)
                        at android.view.View.layout(View.java:14177)
                        at android.view.ViewGroup.layout(ViewGroup.java:4399)
                        at android.widget.LinearLayout.setChildFrame(LinearLayout.java:1663)
                        at android.widget.LinearLayout.layoutVertical(LinearLayout.java:1521)
                        at android.widget.LinearLayout.onLayout(LinearLayout.java:1434)
                        at android.view.View.layout(View.java:14177)
                        at android.view.ViewGroup.layout(ViewGroup.java:4399)
                        at android.widget.FrameLayout.onLayout(FrameLayout.java:448)
                        at android.view.View.layout(View.java:14177)
                        at android.view.ViewGroup.layout(ViewGroup.java:4399)
                        at android.view.ViewRootImpl.performLayout(ViewRootImpl.java:2244)
                        at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2017)
                        at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:1190)
                        at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:4860)
                        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:766)
                        at android.view.Choreographer.doCallbacks(Choreographer.java:575)
                        at android.view.Choreographer.doFrame(Choreographer.java:542)
                        at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:751)
                        at android.os.Handler.handleCallback(Handler.java:725)
                        at android.os.Handler.dispatchMessage(Handler.java:92)
                        at android.os.Looper.loop(Looper.java:158)
                        at android.app.ActivityThread.main(ActivityThread.java:5751)
                        at java.lang.reflect.Method.invokeNative(Native Method)
                        at java.lang.reflect.Method.invoke(Method.java:511)
                        at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1083)
                        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:850)
                        at dalvik.system.NativeStart.main(Native Method)

                         */
                        lineWidth = textPaint.measureText(text.subSequence(start, --end + 1).toString());
                    }
                    setText(text.subSequence(0, end) + mEllipsis);
                }
            }
        }

        // Some devices try to auto adjust line spacing, so force default line spacing
        // and invalidate the layout as a side effect
        textPaint.setTextSize(targetTextSize);
        setLineSpacing(mSpacingAdd, mSpacingMult);

        // Notify the listener if registered
        if(mTextResizeListener != null) {
            mTextResizeListener.onTextResize(this, oldTextSize, targetTextSize);
        }

        // Reset force resize flag
        mNeedsResize = false;
    }

    // Set the text size of the text paint object and use a static layout to render text off screen before measuring
    private int getTextHeight(CharSequence source, TextPaint originalPaint, int width, float textSize) {
    	 // modified: make a copy of the original TextPaint object for measuring
    	  // (apparently the object gets modified while measuring, see also the
    	  // docs for TextView.getPaint() (which states to access it read-only)
    	  TextPaint paint = new TextPaint(originalPaint);
    	  // Update the text paint object
    	  paint.setTextSize(textSize);
    	
    	// Update the text paint object
        paint.setTextSize(textSize);
        // Measure using a static layout
        StaticLayout layout = new StaticLayout(source, paint, width, Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, true);
        return layout.getHeight();
    }

}