package main;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;

public class AnimationHandler {

    interface OnAnimationRestartListener {
        void OnAnimationRestart(AnimationHandler handler);
    }

    private int currentFramePos;
    private AnimationDrawable drawable;
    private Handler handler;
    private OnAnimationRestartListener onAnimationRestart;

    AnimationHandler(AnimationDrawable drawable, OnAnimationRestartListener onAnimationRestart) {
        currentFramePos = 0;
        this.drawable = drawable;
        this.onAnimationRestart = onAnimationRestart;
        drawable.selectDrawable(currentFramePos);
        handler = new Handler();
        CallHandler();
    }

    private void CallHandler() {
        if(handler != null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(drawable != null) {
                        currentFramePos++;
                        if (currentFramePos >= drawable.getNumberOfFrames()) {
                            currentFramePos = 0;
                            if(onAnimationRestart != null) {
                                onAnimationRestart.OnAnimationRestart(AnimationHandler.this);
                            }
                        }
                        if(drawable != null) {
                            drawable.selectDrawable(currentFramePos);
                        }
                        CallHandler();
                    }
                }
            }, drawable.getDuration(currentFramePos));
        }
    }

    void Destroy() {
        drawable = null;
        handler = null;
    }
}
