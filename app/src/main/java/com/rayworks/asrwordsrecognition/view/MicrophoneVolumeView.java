package com.rayworks.asrwordsrecognition.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.rayworks.asrwordsrecognition.R;

import java.lang.ref.WeakReference;

/**
 * Created by Michael on 2016-05-31.
 *
 * <p>Customized view to show microphone volume when recording
 */
public class MicrophoneVolumeView extends View {

    public static final int ASR_SAMPLE_RATE = 1600;
    public static final int MSG_REDRAW = 0xFF;
    final Paint paint = new Paint();
    private float minDiameter;
    private float diameter;
    private float maxDiameter;
    private float centreX;
    private float centreY;
    private UpdateHandler handler;

    public MicrophoneVolumeView(Context context) {
        this(context, null, 0);
    }

    public MicrophoneVolumeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MicrophoneVolumeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        int roundColor = context.getResources().getColor(R.color.sp_grey);
        if (attrs != null) {
            TypedArray typedArray =
                    context.obtainStyledAttributes(
                            attrs, R.styleable.MicrophoneVolume, defStyle, 0);
            try {
                roundColor =
                        typedArray.getColor(
                                R.styleable.MicrophoneVolume_round_color,
                                context.getResources().getColor(R.color.sp_grey));
                minDiameter = typedArray.getDimension(R.styleable.MicrophoneVolume_minDiameter, 0);
            } finally {
                typedArray.recycle();
            }
        }
        paint.setColor(roundColor);

        handler = new UpdateHandler(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        float contentWidth = getWidth();
        float contentHeight = getHeight();

        centreX = contentWidth / 2;
        centreY = contentHeight / 2;

        maxDiameter = Math.min(contentWidth, contentHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(centreX, centreY, diameter / 2, paint);
    }

    public void setProportion(float proportion) {
        if (getVisibility() == VISIBLE) {
            System.out.println(">>> level : " + proportion);

            Message msg = Message.obtain();
            msg.what = MSG_REDRAW;
            msg.obj = proportion;
            handler.sendMessage(msg);

        } else {
            handler.removeMessages(MSG_REDRAW);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        handler.removeMessages(MSG_REDRAW);
    }

    static class UpdateHandler extends Handler {
        private WeakReference<MicrophoneVolumeView> viewRef;

        public UpdateHandler(MicrophoneVolumeView view) {
            viewRef = new WeakReference<MicrophoneVolumeView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REDRAW:
                    if (viewRef.get() != null) {
                        MicrophoneVolumeView volumeView = viewRef.get();

                        float proportion = (float) msg.obj;

                        // keep the radius inside the view
                        volumeView.diameter =
                                Math.min(
                                        volumeView.minDiameter
                                                + proportion
                                                        * 1.0f
                                                        / ASR_SAMPLE_RATE
                                                        * (volumeView.maxDiameter
                                                                - volumeView.minDiameter),
                                        volumeView.maxDiameter - 2);

                        volumeView.invalidate();
                    }

                    break;
            }
        }
    }
}
