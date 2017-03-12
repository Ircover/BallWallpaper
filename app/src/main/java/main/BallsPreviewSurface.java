package main;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class BallsPreviewSurface extends SurfaceView implements SurfaceHolder.Callback {

    private class DrawThread extends Thread {

        private long prevTime;
        private boolean isInterrupted, isPaused;

        DrawThread() {
            isInterrupted = false;
            prevTime = System.currentTimeMillis();
            isPaused = false;
        }

        @Override
        public void run() {
            while(!isInterrupted) {
                Canvas canvas = null;
                SurfaceHolder surfaceHolder = getHolder();
                try {
                    if(!isPaused) {
                        long now = System.currentTimeMillis();
                        long elapsedTime = now - prevTime;
                        if (elapsedTime > 100) {
                            prevTime = System.currentTimeMillis();
                        }
                        canvas = surfaceHolder.lockCanvas(null);
                        Draw(canvas);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            isInterrupted = true;
        }

        void Pause() {
            isPaused = true;
        }

        void Resume() {
            isPaused = false;
        }
    }

    private DrawThread thread;
    private BallsContainer balls;
    private boolean isMoved, surfaceCreated;
    private float touchX, touchY;
    int minHeight;

    public BallsPreviewSurface(Context context) {
        super(context);
        Init();
    }

    public BallsPreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        Init();
    }

    public BallsPreviewSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init();
    }

    private void Init() {
        getHolder().addCallback(this);
        if(!isInEditMode()) {
            balls = new BallsContainer(getContext(), false) {
                @Override
                protected Point CreateFirstBallPos() {
                    return getFirstBallPos();
                }

                @Override
                protected Point CreateFirstBallVel() {
                    return getFirstBallVel();
                }
            };
            balls.setPreview(true);
        }
        isMoved = false;
        minHeight = (int) (BasicFunctions.getMinDisplaySide(getContext()) * 0.6f);
        surfaceCreated = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec),
            heightSize = MeasureSpec.getSize(heightMeasureSpec),
            widthMode = MeasureSpec.getMode(widthMeasureSpec),
            heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if(widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = 200;
        }
        if(heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = 200;
        }
        heightSize = BasicFunctions.Max(heightSize, minHeight);
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;
        Resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Pause();
    }

    void Pause() {
        if(thread != null) {
            thread.Pause();
        }
        if(balls != null) {
            balls.Pause();
        }
    }

    void Resume() {
        if(surfaceCreated) {
            if (thread == null) {
                thread = new DrawThread();
                thread.start();
            } else {
                thread.Resume();
            }
        }
        if(balls != null) {
            balls.Resume(getContext());
        }
    }

    protected void Draw(Canvas canvas) {
        if(balls != null) {
            balls.CreateFirstBall(getFirstBallPos(), getFirstBallVel());// TODO: 25.01.2017 и почему я не могу без костылей?
            balls.Draw(canvas, getWidth(), getHeight());
        }
    }

    private Point getFirstBallPos() {
        int widthSize = getWidth(), heightSize = getHeight();
        return new Point(widthSize / 2f, heightSize / 3f);
    }
    private Point getFirstBallVel() {
        return new Point(0f, 0f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                touchY = event.getY();
                isMoved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float diff = 3f;
                if(BasicFunctions.isDifferencesGreater(touchX, event.getX(), diff) ||
                        BasicFunctions.isDifferencesGreater(touchY, event.getY(), diff)) {
                    isMoved = true;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if(!isMoved && balls != null) {
                    balls.Click(event.getX(), event.getY());
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    void Destroy() {
        if(thread != null) {
            thread.interrupt();
            thread = null;
        }
        if(balls != null) {
            balls = null;
        }
    }
}