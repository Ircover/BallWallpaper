package main;

import android.app.WallpaperManager;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class MyWallpaperService extends WallpaperService {
	
	@Override
	public Engine onCreateEngine() {
		return new MyEngine();
	}
	
	private class MyEngine extends Engine {
		private boolean visible;
		private Handler handler;
		private Runnable drawRunner = new Runnable() {
			@Override
			public void run() {
				draw();
			}
		};
		private int width;
		private int height;
		private BallsContainer balls;
		private SurfaceHolder holder;
		//private SensorManager manager;
		//private OrientationSensorListener orientationListener;

		MyEngine() {
			balls = new BallsContainer(getApplicationContext());
			handler = new Handler();
			/*manager = (SensorManager) getSystemService(SENSOR_SERVICE);
			Sensor sensorAcc = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					sensorMagn = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			orientationListener = new OrientationSensorListener();
			manager.registerListener(orientationListener, sensorAcc, 20);
			manager.registerListener(orientationListener, sensorMagn, 20);*/
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			holder = surfaceHolder;
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			//manager.unregisterListener(orientationListener);
		}

		@Override
	    public void onVisibilityChanged(boolean visible) {
	      this.visible = visible;
	      if (visible) {
			  handler.post(drawRunner);
			  balls.Resume(getApplicationContext());
	      } else {
			  handler.removeCallbacks(drawRunner);
			  balls.Pause();
	      }
	    }
		
		@Override
	    public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			this.visible = false;
			handler.removeCallbacks(drawRunner);
	    }

	    @Override
	    public void onSurfaceChanged(SurfaceHolder holder, int format,
	        int width, int height) {
			this.width = width;
			this.height = height;
			OrientationSensorListener.OrientationChanged(getApplicationContext());
			super.onSurfaceChanged(holder, format, width, height);
	    }
	    
	    private void draw() {
			//SurfaceHolder holder = getSurfaceHolder();
	        Canvas canvas = null;
			if(holder != null) {
				try {
					canvas = holder.lockCanvas();
					if (canvas != null) {
						balls.Draw(canvas, width, height);
					}
				} finally {
					if (canvas != null && visible)
						holder.unlockCanvasAndPost(canvas);
				}
			}
	        handler.removeCallbacks(drawRunner);
	        if (visible) {
	            handler.postDelayed(drawRunner, 10);
	        }
		}

		@Override
		public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
			if (action.equals(WallpaperManager.COMMAND_TAP) && balls != null) {
				balls.Click(x, y);
			}
			return null;
		}
	}
}
