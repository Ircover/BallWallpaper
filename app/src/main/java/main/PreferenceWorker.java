package main;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

class PreferenceWorker {

    interface PreferenceChangedActionDestroyer {
        void Destroy();
    }

    private static class MyOnSharedPreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        private HashMap<String, ArrayList<Runnable>> runnables;

        MyOnSharedPreferenceChangeListener() {
            runnables = new HashMap<String, ArrayList<Runnable>>();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            ArrayList<Runnable> list = runnables.get(key);
            if(list != null) {
                for(Runnable run : list) {
                    run.run();
                }
            }
        }

        PreferenceChangedActionDestroyer AddRunnable(final Runnable run, final String key) {
            ArrayList<Runnable> list = runnables.get(key);
            if(list == null) {
                list = new ArrayList<Runnable>();
                runnables.put(key, list);
            }
            list.add(run);
            return new PreferenceChangedActionDestroyer() {
                @Override
                public void Destroy() {
                    ArrayList<Runnable> list = runnables.get(key);
                    if(list != null) {
                        list.remove(run);
                    }
                }
            };
        }
    }

    private static final String PREFERENCES_FILENAME = "ircover.ballwallpaper.preferences";
    private static final String PREFERENCE_SHOW_ADS_BANNER = "show_ads_banner";
    static final String PREFERENCE_BALL_SIZE = "ball_size";
    static final String PREFERENCE_BALL_SPEED = "ball_speed";
    private static final String PREFERENCE_BALL_COLOR_SELECT = "ball_color_select";
    private static final String PREFERENCE_BALL_POP_ON_CLICK = "pop_on_click";
    private static final String PREFERENCE_BALL_EXPLODE_ON_POP = "explode_on_click";
    private static final String PREFERENCE_BALL_BACKGROUND_FILE = "background_file";
    private static final String PREFERENCE_BALL_BACKGROUND_RESOURCE = "background_resource";
    private static final String PREFERENCE_BALL_ROTATE = "rotate";
    private static final String PREFERENCE_BALL_SHOW_TRAIL = "show_trail";

    private static final String PREFERENCE_MARK_SHOW = "show";
    private static final String PREFERENCE_MARK_LAST_SHOW = "last_show";
    private static final String PREFERENCE_FIRST_APP_SHOW = "first_app_show";

    private static final float defaultBallSizeMultiply = 0.07f;
    private static final float minBallSizeMultiply = 0.02f;
    private static final float maxBallSizeMultiply = 0.12f;

    private static final float defaultBallSpeed = 0.025f;
    private static final float minBallSpeed = 0.01f;
    private static final float maxBallSpeed = 0.3f;

    private static final int defaultBackgroundId = 4;

    static final int ballColorRandom = 1;
    static final int ballColorFixed = 2;
    private static final int ballColorDefault = ballColorFixed;

    private static MyOnSharedPreferenceChangeListener listener;

    static boolean isShowingAdsBanner(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getBoolean(PREFERENCE_SHOW_ADS_BANNER, false);
    }

    static void setShowingAdsBanner(Context context, boolean showAds) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putBoolean(PREFERENCE_SHOW_ADS_BANNER, showAds).apply();
    }

    static int getBallSize(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getInt(PREFERENCE_BALL_SIZE,
                (int) (BasicFunctions.getMinDisplaySide(context) * defaultBallSizeMultiply));
    }

    static void setBallSize(Context context, int size) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putFloat(PREFERENCE_BALL_SPEED, size).apply();
    }

    static float getBallSpeed(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getFloat(PREFERENCE_BALL_SPEED, defaultBallSpeed);
    }

    static void setBallSpeed(Context context, float speed) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putFloat(PREFERENCE_BALL_SIZE, speed).apply();
    }

    static int getBallColorMode(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getInt(PREFERENCE_BALL_COLOR_SELECT, ballColorDefault);
    }

    static void setBallColorMode(Context context, int colorMode) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putInt(PREFERENCE_BALL_COLOR_SELECT, colorMode).apply();
    }

    static boolean isPopOnClick(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getBoolean(PREFERENCE_BALL_POP_ON_CLICK, true);
    }

    static void setPopOnClick(Context context, boolean popOnClick) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putBoolean(PREFERENCE_BALL_POP_ON_CLICK, popOnClick).apply();
    }

    static boolean isExplodeOnPop(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getBoolean(PREFERENCE_BALL_EXPLODE_ON_POP, false);
    }

    static void setExplodeOnPop(Context context, boolean popOnClick) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putBoolean(PREFERENCE_BALL_EXPLODE_ON_POP, popOnClick).apply();
    }

    static boolean isRotateBall(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getBoolean(PREFERENCE_BALL_ROTATE, false);
    }

    static void setRotateBall(Context context, boolean rotate) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putBoolean(PREFERENCE_BALL_ROTATE, rotate).apply();
    }

    static boolean isShowBallTrail(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getBoolean(PREFERENCE_BALL_SHOW_TRAIL, true);
    }

    static void setShowBallTrail(Context context, boolean rotate) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putBoolean(PREFERENCE_BALL_SHOW_TRAIL, rotate).apply();
    }

    static boolean isShowMarkDialog(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getBoolean(PREFERENCE_MARK_SHOW, true);
    }

    static void neverShowMarkDialog(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putBoolean(PREFERENCE_MARK_SHOW, false).apply();
    }

    static long getLastMarkShow(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        return prefs.getLong(PREFERENCE_MARK_LAST_SHOW, 0);
    }

    static void setMarkShowed(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        prefs.edit().putLong(PREFERENCE_MARK_LAST_SHOW, System.currentTimeMillis()).apply();
    }

    static boolean isFirstAppShow(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        if(prefs.getBoolean(PREFERENCE_FIRST_APP_SHOW, true)) {
            prefs.edit().putBoolean(PREFERENCE_FIRST_APP_SHOW, false).apply();
            return true;
        } else {
            return false;
        }
    }

    static BackgroundWorker.ImageSource getBackground(Context context) {
        SharedPreferences prefs = getPreferencesObject(context);
        String path = prefs.getString(PREFERENCE_BALL_BACKGROUND_FILE, null);
        int res = prefs.getInt(PREFERENCE_BALL_BACKGROUND_RESOURCE, defaultBackgroundId);
        BackgroundWorker.DefaultImage[] images = BasicFunctions.getDefaultImages(context);
        BackgroundWorker.DefaultImage resultImage = new BackgroundWorker.DefaultImage();
        for(BackgroundWorker.DefaultImage image : images) {
            if(image.id == res) {
                resultImage = image;
                break;
            }
        }
        return new BackgroundWorker.ImageSource(path == null ? null : Uri.parse(path), resultImage);
    }

    static void setBackground(Context context, @NonNull BackgroundWorker.ImageSource image) {
        SharedPreferences prefs = getPreferencesObject(context);
        String file = image.file == null ? null : image.file.toString();
        prefs.edit().putString(PREFERENCE_BALL_BACKGROUND_FILE, file)
                .putInt(PREFERENCE_BALL_BACKGROUND_RESOURCE, image.resource.id).apply();
    }

    static PreferenceChangedActionDestroyer AddShowAdsBannerPreferenceChangedAction(Context context, Runnable action) {
        return AddPreferenceChangedAction(context, action, PREFERENCE_SHOW_ADS_BANNER);
    }

    static PreferenceChangedActionDestroyer AddBallSizePreferenceChangedAction(Context context, Runnable action) {
        return AddPreferenceChangedAction(context, action, PREFERENCE_BALL_SIZE);
    }

    static PreferenceChangedActionDestroyer AddBallSpeedPreferenceChangedAction(Context context, Runnable action) {
        return AddPreferenceChangedAction(context, action, PREFERENCE_BALL_SPEED);
    }

    static PreferenceChangedActionDestroyer AddBallColorModePreferenceChangedAction(Context context, Runnable action) {
        return AddPreferenceChangedAction(context, action, PREFERENCE_BALL_COLOR_SELECT);
    }

    private static PreferenceChangedActionDestroyer AddPreferenceChangedAction(Context context, Runnable action, String prefName) {
        SharedPreferences prefs = getPreferencesObject(context);
        InitListener(prefs);
        return listener.AddRunnable(action, prefName);
    }

    private static void InitListener(SharedPreferences prefs) {
        if(listener == null) {
            listener = new MyOnSharedPreferenceChangeListener();
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    private static SharedPreferences getPreferencesObject(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);
    }

    static float getPrefMin(Context context, String pref) {
        int displaySize = BasicFunctions.getMinDisplaySide(context);
        if(pref.equals(PREFERENCE_BALL_SIZE)) {
            return displaySize * minBallSizeMultiply;
        } else if(pref.equals(PREFERENCE_BALL_SPEED)) {
            return minBallSpeed;
        }
        return 0;
    }
    static float getPrefMax(Context context, String pref) {
        int displaySize = BasicFunctions.getMinDisplaySide(context);
        if(pref.equals(PREFERENCE_BALL_SIZE)) {
            return displaySize * maxBallSizeMultiply;
        } else if(pref.equals(PREFERENCE_BALL_SPEED)) {
            return maxBallSpeed;
        }
        return 1;
    }
    static float getPrefValue(Context context, String pref) {
        SharedPreferences prefs = getPreferencesObject(context);
        float defValue = 0;
        if(pref.equals(PREFERENCE_BALL_SIZE)) {
            defValue = BasicFunctions.getMinDisplaySide(context) * defaultBallSizeMultiply;
            return prefs.getInt(pref, (int) defValue);
        } else if(pref.equals(PREFERENCE_BALL_SPEED)) {
            defValue = defaultBallSpeed;
        }
        return prefs.getFloat(pref, defValue);
    }
    static void setPrefValue(Context context, String pref, float value) {
        SharedPreferences prefs = getPreferencesObject(context);
        if(pref.equals(PREFERENCE_BALL_SIZE)) {
            prefs.edit().putInt(pref, (int) value).apply();
        } else {
            prefs.edit().putFloat(pref, value).apply();
        }
    }

}
