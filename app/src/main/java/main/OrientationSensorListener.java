package main;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class OrientationSensorListener implements SensorEventListener {// TODO: так и не довёл до конца

    private static float angle = 0f;
    private static float zero_angle = 0f;
    private static double horizontalMultiplier, verticalMultiplier;
    static {
        CalculateMultipliers();
    }
    private float[] gravityValues, geomagneticValues;

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*String result = "";
        for(float value : event.values) {
            result += value + ", ";
        }
        Log.e("sensor", result);*/
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravityValues = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagneticValues = event.values;
        if (gravityValues != null && geomagneticValues != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravityValues, geomagneticValues);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                angle = orientation[0] - zero_angle;
                CalculateMultipliers();
                //Log.e("sensor", orientation[0] + ", " + orientation[1] + ", " + orientation[2]);
            }
        }
    }

    private static void CalculateMultipliers() {
        horizontalMultiplier = Math.sin(angle);
        verticalMultiplier = Math.cos(angle);
    }

    public static void OrientationChanged(Context context) {
        //angle -= zero_angle;
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        switch(display.getRotation()) {
            case Surface.ROTATION_0:
                zero_angle = (float) (-Math.PI);
                break;
            case Surface.ROTATION_90:
                zero_angle = (float) (-Math.PI / 2);
                break;
            case Surface.ROTATION_180:
                zero_angle = 0f;
                break;
            case Surface.ROTATION_270:
                zero_angle = (float) (-3 * Math.PI / 2);
                break;
        }
        //angle += zero_angle;
        CalculateMultipliers();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /*public static float getRotateAngle() {
        return angle;
    }*/

    public static double getHorizontalMultiplier() {
        //return Math.sin(angle);
        return horizontalMultiplier;
    }

    public static double getVerticalMultiplier() {
        //return Math.cos(angle);
        return verticalMultiplier;
    }
}
