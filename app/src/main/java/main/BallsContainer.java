package main;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import ircover.ballwallpaper.R;

class BallsContainer {

	private interface OnBallActionCompleteListener {
		void onBallActionComplete(Ball b);
	}

	private static class Ball {

		private static final Random random = new Random();
		private static BallColorChooser colorChooser;

		static final int DIRECTION_NONE = 0;
		static final int DIRECTION_HORIZONTAL = 1;
		static final int DIRECTION_VERTICAL = 2;
		static final float FREE_FALL_ACCELERATION = 9.8f;
		static final float SPEED_MAXIMUM = 150f;
		static final float TRAIL_ADD_INTERVAL = 0.1f;
		static final int TRAIL_MAX_COUNT = 50;
		static final int TRAIL_DRAW_POINT = 30;
		static final float HIT_DURATION = 5f;
		static final float[] Hit_Scales = { 0.9f, 0.8f, 0.75f, 0.7f, 0.75f, 0.8f, 0.9f };
		static final float HIT_DURATION_BY_SCALE = HIT_DURATION / Hit_Scales.length;

		private static Drawable ballDrawable;
		private static AnimationDrawable ballPopDrawable, ballExplodeDrawable;
		static int radius;
		private static float[] trailWidthHalfs = new float[TRAIL_DRAW_POINT];
		private static boolean showTrail;

		Point pos, vel;
		private Ball splittedWith;
		private float rotation;
		private int color;
		private AnimationDrawable drawable;
		private boolean isKilled;
		private long lastTrailAdd;
		private ConcurrentLinkedQueue<Point> trail;
		private Paint[] trailPaints;
		private float[][] trailPoss, trailTans;
		private Path trailPath, trailStepRect;
		private PathMeasure trailPathMeasure;
		private int hitDirection;
		private long hitStart;
		private float hitScaleX, hitScaleY;
		private OnBallActionCompleteListener ballRemove;

		Ball(Point startPos, Point startVel, OnBallActionCompleteListener ballRemove) {
			this.ballRemove = ballRemove;
			isKilled = false;
			pos = startPos;
			vel = startVel;
			color = colorChooser.getColor();
			rotation = 0;

			lastTrailAdd = 0;
			trail = new ConcurrentLinkedQueue<Point>();
			trailPath = new Path();
			trailStepRect = new Path();
			trailPathMeasure = new PathMeasure();

			Paint trailPaint = new Paint();
			trailPaint.setStyle(Paint.Style.FILL);
			trailPaint.setColor(color);
			trailPoss = new float[TRAIL_DRAW_POINT][];
			trailTans = new float[TRAIL_DRAW_POINT][];
			trailPaints = new Paint[TRAIL_DRAW_POINT];
			int minAlpha = 10;
			for(int i=0; i<trailPaints.length; i++) {
				trailPaints[i] = new Paint(trailPaint);
				trailPaints[i].setAlpha((int) (minAlpha + i * ((200f - minAlpha) / TRAIL_DRAW_POINT)));
				trailPoss[i] = new float[2];
				trailTans[i] = new float[2];
			}

			hitDirection = DIRECTION_NONE;
			hitScaleX = 1f;
			hitScaleY = 1f;
		}

		void Move(int width, int height, float speedMultiplier, List<Ball> balls, boolean split) {
			if(isKilled) {
				return;
			}
			//this.speedMultiplier = speedMultiplier;
			CheckCollapse(balls);
			pos.Add(vel.Multiply(speedMultiplier));
			int splitDirection = DIRECTION_NONE;
			if (pos.x > width - radius) {
				pos.x = BasicFunctions.Min(width - radius, 2 * width - (int) pos.x);
				vel.x *= -1;
				splitDirection = DIRECTION_VERTICAL;
			} else if (pos.x < radius) {
				pos.x = BasicFunctions.Max(radius, (int) -pos.x);
				vel.x *= -1;
				splitDirection = DIRECTION_VERTICAL;
			}
			if (pos.y > height - radius) {
				pos.y = BasicFunctions.Min(height - radius, 2 * height - (int) pos.y);
				vel.y *= -1;
				if(vel.y > -FREE_FALL_ACCELERATION * speedMultiplier) {//попытка предотвратить скопление внизу
					vel.y = -10f * FREE_FALL_ACCELERATION * speedMultiplier;
				}
				splitDirection = DIRECTION_HORIZONTAL;
			} else if (pos.y < radius) {
				pos.y = BasicFunctions.Max(radius, (int) -pos.y);
				vel.y *= -1;
				splitDirection = DIRECTION_HORIZONTAL;
			}
			if(splitDirection != DIRECTION_NONE) {
				setHit(splitDirection);
				if(split) {
					balls.add(0, Split(splitDirection));
				}
			}
			Accelerate();

			if(BallsContainer.rotate) {
				rotation += vel.Length() * speedMultiplier / 3;
			} else {
				rotation = 0;
			}

			if(showTrail) {
				long now = System.currentTimeMillis();
				if (now - (long) (TRAIL_ADD_INTERVAL / speedMultiplier) > lastTrailAdd) {
					lastTrailAdd = now;
					trail.add(pos.Copy());
					if (trail.size() > TRAIL_MAX_COUNT) {
						trail.remove();
					}
					FillTrail();
				}
			} else {
				trail.clear();
			}
			CalculateHit(speedMultiplier);
		}

		private void CalculateHit(float speedMultiplier) {
			if(hitDirection != DIRECTION_NONE) {
				float duration = (System.currentTimeMillis() - hitStart) * speedMultiplier;
				int scalePosition = (int) (duration / HIT_DURATION_BY_SCALE);
				if(scalePosition >= Hit_Scales.length) {
					hitDirection = DIRECTION_NONE;
					hitScaleX = 1f;
					hitScaleY = 1f;
				} else {
					float result = Hit_Scales[scalePosition];
					switch(hitDirection) {
						case DIRECTION_VERTICAL:
							hitScaleX = result;
							break;
						case DIRECTION_HORIZONTAL:
							hitScaleY = result;
							break;
					}
				}
			}
		}

		private void CheckCollapse(List<Ball> balls) {
			for (Ball otherBall : balls) {
				if (otherBall.equals(this)) {
					break;
				}
				if(!otherBall.isKilled) {
					if (pos.isClose(otherBall.pos, radius * 2)) {
						if (!otherBall.equals(splittedWith)) {
							otherBall.vel.Add(vel);
							//balls.remove(this);
							ballRemove.onBallActionComplete(this);
							return;
						}
					} else if (otherBall.equals(splittedWith)) {
						splittedWith = null;
						otherBall.splittedWith = null;
					}
				}
			}
		}

		void Draw(Canvas canvas) {
			if(!isKilled && showTrail) {
				DrawTrail(canvas);
			}
			canvas.save();
			if(hitDirection != DIRECTION_NONE) {
				canvas.scale(hitScaleX, hitScaleY, pos.x, pos.y);
			}
			canvas.rotate(rotation, pos.x, pos.y);
			if(!isKilled) {
				ballDrawable.setBounds((int) pos.x - radius, (int) pos.y - radius, (int) pos.x + radius, (int) pos.y + radius);
				ballDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
				ballDrawable.draw(canvas);
			} else {
				drawable.draw(canvas);
			}
			canvas.restore();
		}

		private void DrawTrail(Canvas canvas) {
			if(trail.size() > 1) {
				float prevWidthHalfX = 0f, prevWidthHalfY = 0f, prevX = 0f, prevY = 0f;
				boolean isFirst = true;
				for (int i = 0; i < TRAIL_DRAW_POINT; i++) {
					float //currWidthHalf = (float) (radius) * i / TRAIL_DRAW_POINT / 2f,
							currWidthHalfX = trailWidthHalfs[i] * trailTans[i][1],
							currWidthHalfY = trailWidthHalfs[i] * trailTans[i][0],
							currX = trailPoss[i][0], currY = trailPoss[i][1];
					if (!isFirst) {
						trailStepRect.reset();
						trailStepRect.moveTo(prevX - prevWidthHalfX, prevY + prevWidthHalfY);
						trailStepRect.lineTo(prevX + prevWidthHalfX, prevY - prevWidthHalfY);
						trailStepRect.lineTo(currX + currWidthHalfX, currY - currWidthHalfY);
						trailStepRect.lineTo(currX - currWidthHalfX, currY + currWidthHalfY);
						canvas.drawPath(trailStepRect, trailPaints[i]);
					} else {
						isFirst = false;
					}
					prevX = currX;
					prevY = currY;
					prevWidthHalfX = currWidthHalfX;
					prevWidthHalfY = currWidthHalfY;
				}
			}
		}

		private void FillTrail() {
			trailPath.reset();
			boolean isFirst = true;
			for(Point p : trail) {
				if(isFirst) {
					trailPath.moveTo(p.x, p.y);
					trailPoss[0][0] = p.x;
					trailPoss[0][1] = p.y;
					isFirst = false;
				} else {
					trailPath.lineTo(p.x, p.y);
				}
			}
			trailPath.lineTo(pos.x, pos.y);
			//PathMeasure trailPathMeasure = new PathMeasure(trailPath, false);
			trailPathMeasure.setPath(trailPath, false);
			float step = trailPathMeasure.getLength() / TRAIL_DRAW_POINT;
			for(int i=0; i<TRAIL_DRAW_POINT; i++) {
				trailPathMeasure.getPosTan(step * i, trailPoss[i], trailTans[i]);
			}
		}

		private Ball Split(int splitDirection) {
			Ball result = new Ball(pos.Copy(), vel.Copy(), ballRemove);
			result.setHit(splitDirection);
			float velPart = random.nextFloat() * 0.3f;
			if(splitDirection == DIRECTION_HORIZONTAL) {
				float buf = vel.y;
				result.vel.y = buf * (1f + velPart);
				vel.y = buf * (1f - velPart);
				result.vel.x *= 0.5;
				vel.x *= 1.2;
			} else {
				float buf = vel.x;
				result.vel.x = buf * (1f + velPart);
				vel.x = buf * (1f - velPart);
				result.vel.y *= 0.5;
				vel.y *= 1.2;
			}
			result.Accelerate();
			splittedWith = result;
			result.splittedWith = this;
			return result;
		}

		private void setHit(int direction) {
			hitDirection = direction;
			hitStart = System.currentTimeMillis();
		}

		private void Accelerate() {
			vel.y += FREE_FALL_ACCELERATION * OrientationSensorListener.getVerticalMultiplier() * speedMultiplier;
			vel.x += FREE_FALL_ACCELERATION * OrientationSensorListener.getHorizontalMultiplier() * speedMultiplier;
			vel.CheckIsGreater(SPEED_MAXIMUM);
			if(vel.x == 0f) {// TODO: выглядит костыльно
				vel.x = 0.1f * speedMultiplier;
			}
			if(vel.y == 0f) {// TODO: выглядит костыльно
				vel.y = 0.1f * speedMultiplier;
			}
		}

		void Kill(final OnBallActionCompleteListener listener) {
			isKilled = true;
			drawable = BallsContainer.explodeOnPop ? ballExplodeDrawable : ballPopDrawable;
			new AnimationHandler(drawable, new AnimationHandler.OnAnimationRestartListener() {
				@Override
				public void OnAnimationRestart(AnimationHandler handler) {
					if(listener != null) {
						listener.onBallActionComplete(Ball.this);
					}
					handler.Destroy();
				}
			});
			drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
			drawable.setBounds((int) pos.x - radius, (int) pos.y - radius, (int) pos.x + radius, (int) pos.y + radius);
		}

		static void InitBall(Context context) {
			ballDrawable = context.getResources().getDrawable(R.drawable.ball);
			ballDrawable = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(
					((BitmapDrawable) ballDrawable).getBitmap(), radius * 2, radius * 2, true));

			/*BitmapFactory.Options options = new BitmapFactory.Options();
			options.outWidth = radius * 2;
			options.outHeight = radius * 2;
			Bitmap trail = BitmapFactory.decodeResource(context.getResources(), R.drawable.ball_trail, options);
			trailShader = new BitmapShader(trail, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);*/
			InitBallKill(context);
			for(int i=0; i<TRAIL_DRAW_POINT; i++) {
				trailWidthHalfs[i] = (float) (radius) * i / TRAIL_DRAW_POINT / 2f;
			}
		}
		static void InitBallKill(Context context) {
			ballPopDrawable = (AnimationDrawable) context.getResources().getDrawable(R.drawable.ball_pop_animation);
			ballExplodeDrawable = (AnimationDrawable) context.getResources().getDrawable(R.drawable.ball_explode_animation);
		}
	}
	private static class MovingThread extends Thread {

		private boolean isInterrupted, isPaused;
		private WeakReference<BallsContainer> balls;

		MovingThread(BallsContainer b) {
			balls = new WeakReference<BallsContainer>(b);
			isInterrupted = false;
		}

		@Override
		public void run() {
			while(balls.get() != null && !isInterrupted) {
				if(!isPaused) {
					for (Ball b : balls.get().balls) {
						b.Move(balls.get().width, balls.get().height, speedMultiplier, balls.get().balls, !balls.get().isPreview);
					}
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
					isInterrupted = true;
				}
			}
		}

		void Pause() {
			isPaused = true;
		}

		void Resume() {
			isPaused = false;
		}
	}
	private MovingThread movingThread;
	
	private CopyOnWriteArrayList<Ball> balls;
	private static final Point defaultStartPos = new Point(100, 500), defaultStartVel = new Point(30, 10);
	private static float speedMultiplier;
	private static boolean popOnClick, explodeOnPop, rotate;

	private int width, height;
	private static BackgroundWorker converter;
	private boolean isPreview = false;
	private OnBallActionCompleteListener ballRemove = new OnBallActionCompleteListener() {
		@Override
		public void onBallActionComplete(Ball b) {
			RemoveBall(b);
		}
	};
	
	BallsContainer(Context context) {
		this(context, true);
	}
	BallsContainer(Context context, boolean createNewBall) {
		this(context, defaultStartPos.Copy(), defaultStartVel.Copy(), createNewBall);
	}
	private BallsContainer(Context context, Point startPos, Point startVel, boolean createNewBall) {
		if(converter == null) {
			converter = new BackgroundWorker(context);
		}
		setBallSize(context, PreferenceWorker.getBallSize(context));
		setSpeedMultiplier(PreferenceWorker.getBallSpeed(context));
		setColorChooser(BallColorChooser.getChooserFromPreferences(context));
		setPopOnClick(PreferenceWorker.isPopOnClick(context));
		setBackground(PreferenceWorker.getBackground(context));
		setRotate(PreferenceWorker.isRotateBall(context));
		setExplodeOnPop(PreferenceWorker.isExplodeOnPop(context));
		setShowBallTrail(PreferenceWorker.isShowBallTrail(context));
		balls = new CopyOnWriteArrayList<Ball>();
		if(createNewBall) {
			balls.add(new Ball(startPos, startVel, ballRemove));
		}
		movingThread = new MovingThread(this);
		movingThread.start();
	}

	private boolean isEmpty() {
		return balls.isEmpty();
	}

	void CreateFirstBall(Point startPos, Point startVel) {
		if(isEmpty()) {
			balls.add(new Ball(startPos, startVel, ballRemove));
		}
	}
	
	void Draw(Canvas canvas, int width, int height) {
		this.width = width;
		this.height = height;
		Bitmap backgroundBitmap = converter.getBitmap(width, height);
		if(backgroundBitmap == null) {
			canvas.drawColor(Color.BLACK);
		} else {
			canvas.drawBitmap(backgroundBitmap, 0, 0, null);
		}
		for (Ball b : balls) {
			b.Draw(canvas);
		}
	}

	void setPreview(boolean isPreview) {
		this.isPreview = isPreview;
	}

	void Click(float x, float y) {
		if(popOnClick) {
			Ball corpseBall = null;
			for (Ball b : balls) {
				if (!b.isKilled && b.pos.isClose(new Point(x, y), Ball.radius * 1.5f)) {
					corpseBall = b;
				}
			}
			if (corpseBall != null) {
				KillBall(corpseBall);
			}
		}
	}

	private void KillBall(Ball b) {
		if(explodeOnPop) {
			for(Ball ball : balls) {
				Point vector = ball.pos.Difference(b.pos);
				double distance = vector.Length(),
						maxDistance = 300;
				if(distance < maxDistance) {
					float power = (float) (maxDistance / distance);
					ball.vel.Add(vector.Normalize().Multiply(power * 200));
				}
			}
		}
		b.Kill(ballRemove);
	}

	private void RemoveBall(Ball b) {
		balls.remove(b);
		if(balls.isEmpty()) {
			balls.add(new Ball(CreateFirstBallPos(), CreateFirstBallVel(), ballRemove));
		}
	}

	void Pause() {
		movingThread.Pause();
	}

	void Resume(Context context) {
		movingThread.Resume();
		setColorChooser(BallColorChooser.getChooserFromPreferences(context));//костыльненько
	}

	protected Point CreateFirstBallPos() {
		return defaultStartPos.Copy();
	}

	protected Point CreateFirstBallVel() {
		return defaultStartVel.Copy();
	}

	static void setBallSize(Context context, int size) {
		Ball.radius = size;
		Ball.InitBall(context);
	}

	static void setSpeedMultiplier(float speedMultiplier) {
		BallsContainer.speedMultiplier = speedMultiplier;
	}

	static void setColorChooser(BallColorChooser chooser) {
		Ball.colorChooser = chooser;
	}

	static void setPopOnClick(boolean popOnClick) {
		BallsContainer.popOnClick = popOnClick;
	}

	static void setBackground(BackgroundWorker.ImageSource image) {
		converter.setImage(image);
	}

	static void setRotate(boolean rotate) {
		BallsContainer.rotate = rotate;
	}

	static void setExplodeOnPop(boolean explodeOnPop) {
		BallsContainer.explodeOnPop = explodeOnPop;
	}

	static void setShowBallTrail(boolean showTrail) {
		Ball.showTrail = showTrail;
	}
}
