package com.rayworks.asrwordsrecognition.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;


import com.rayworks.asrwordsrecognition.R;

import timber.log.Timber;

/**
 * Created by Sean on 3/1/17.
 *
 * <p>A button for recording user's voice. It handles motion events such as simple tap, long press,
 * action up and down.
 */
@SuppressLint("AppCompatCustomView")
public class ButtonRecorder extends Button {

    private TouchListener listener;
    private boolean inActivated = false;

    public ButtonRecorder(Context context) {
        this(context, null);
    }

    public ButtonRecorder(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonRecorder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ButtonRecorder(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        listener = new TouchListener(getContext());
        setOnTouchListener(listener);
    }

    /**
     * * Sets the listener for monitoring recording events.
     *
     * @param actionListener
     */
    public void setRecordActionListener(RecordActionListener actionListener) {
        listener.setTabRecordListener(new TouchGuard(actionListener));
    }

    public ButtonRecorder setInActivated(boolean inActivated) {
        this.inActivated = inActivated;
        if (inActivated) {
            setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_mic_tapping));
        } else {
            setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_mic));
        }

        invalidate();
        return this;
    }

    private interface TabListener {
        void onSingleTap();

        void onActionUp();

        void onLongPress();
    }

    public interface RecordActionListener {
        /**
         * * Whether the callee is ready to process the recording action.
         *
         * @return true if ready for recording, otherwise false.
         */
        boolean preparedForRecording();

        /** * Starts recording */
        void onRecordStarted();

        /** * Completes recording */
        void onRecordComplete();
    }

    public static class TouchListener extends GestureDetector.SimpleOnGestureListener
            implements OnTouchListener {

        private final Context context;
        private final GestureDetector gestureDetector;
        private TabListener tabRecordListener;

        public TouchListener(Context context) {
            this.context = context;
            gestureDetector = new GestureDetector(context, this);
        }

        public TouchListener setTabRecordListener(TabListener tabRecordListener) {
            this.tabRecordListener = tabRecordListener;
            return this;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (tabRecordListener != null) {
                    tabRecordListener.onActionUp();
                }
            }
            gestureDetector.onTouchEvent(event);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            if (tabRecordListener != null) {
                tabRecordListener.onLongPress();
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (tabRecordListener != null) {
                tabRecordListener.onSingleTap();
            }
            return true;
        }
    }

    /**
     * * The touch event filter used to identify and triggered the recording start and recording
     * complete events. Tap action and long pressed action will both activate the recording.
     */
    class TouchGuard implements TabListener {
        private final RecordActionListener listener;
        private int tapCounter;
        private boolean longPressed;

        public TouchGuard(RecordActionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onSingleTap() {
            ++tapCounter;

            Timber.i(">>> onSingleTap(), tapCnt: %d", tapCounter);

            if (tapCounter == 1) {
                Timber.i(">>> %s : about to call onRecordStart()", "onSingleTap()");

                if (listener != null) {
                    if (!readyToRecord()) {
                        return;
                    }

                    setInActivated(true);
                    listener.onRecordStarted();
                }
            } else if (tapCounter == 2) {
                Timber.i(">>> onRecordComplete()");
                tapCounter = 0;
                setInActivated(false);

                if (listener != null) {
                    listener.onRecordComplete();
                }

            } else {
                tapCounter = 0;
                Timber.i(">>> tapCounter reset to 0");
            }
        }

        private boolean readyToRecord() {
            if (!listener.preparedForRecording()) {
                Timber.i(">>> callee is not ready.");
                tapCounter = 0;
                return false;
            }

            return true;
        }

        @Override
        public void onActionUp() {
            Timber.i(">>> onActionUp() with long pressed %b, tapCnt: %d ", longPressed, tapCounter);

            if (longPressed) {
                tapCounter = 0;
                longPressed = false;

                Timber.i(">>> onRecordComplete()");

                setInActivated(false);
                if (listener != null) {
                    listener.onRecordComplete();
                }
            }
        }

        @Override
        public void onLongPress() {
            if (tapCounter > 0) {
                Timber.i(
                        ">>> %s aborted : Tap action is activated, waiting for the end tap.",
                        "onLongPress()");
                return;
            }

            Timber.i(">>> %s : about to call onRecordStart()", "onLongPress()");
            if (listener != null) {
                if (!readyToRecord()) {
                    return;
                }

                setInActivated(true);
                longPressed = true;
                listener.onRecordStarted();
            }
            Timber.i(">>> onLongPress(), tapCnt: %d", tapCounter);
        }
    }
}
