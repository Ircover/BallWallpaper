package main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;

import ircover.ballwallpaper.R;

class BasicFunctions {

    interface OnBitmapLoadedListener {
        void onBitmapLoaded(Bitmap b);
    }

    private static boolean isAdsInit = false;

    static int Max(int... values) {
        if(values.length == 0) return 0;
        int result = values[0];
        for(int value : values) {
            if(value > result) {
                result = value;
            }
        }
        return result;
    }

    static int Min(int... values) {
        if(values.length == 0) return 0;
        int result = values[0];
        for(int value : values) {
            if(value < result) {
                result = value;
            }
        }
        return result;
    }

    static void InitAds(Context context, AdView adView) {
        if(!isAdsInit) {
            MobileAds.initialize(context, context.getString(R.string.admob_app_id));
            isAdsInit = true;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        //adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
        adView.loadAd(adRequest);
    }

    static int getMinDisplaySide(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return Min(display.getWidth(), display.getHeight());
    }

    static int getDisplayHeight(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return display.getHeight();
    }

    static String getAppLicenceKey() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAubuiMDJn+nGiAqO+QL2jkituiW+qdHLRnChTxNdcJjqykeJH5Aj2NxJ0+6cyN+JwkOewzvwAu/LzHvBgDfzQrfTloo0kBh+UTrq9f9VcRva/Bv5d9f007ixvgMpSoc1t0+WOjfFFw1PiUTOzCsH+CgJ502Jnw4lmH4NyuVC6By7YXuAqS3SshCBqM5lrrmDowkcleK8d1IVRZ4/NsMUlpHGj9vb5AHaAHqNqjWv2ZpFZvu3wPAMBisdy+uKt8C1Wonnc602wL0YlKUxysZVin9abfh1ALX6Qp1EUt4kOqevWSLwKqSzcyuh2etxQc4sdDi2mDCWUMmGy5GMizJkU7QIDAQAB";
    }

    static boolean isDifferencesGreater(float a, float b, float diff) {
        return Math.abs(a - b) > diff;
    }

    static BackgroundWorker.DefaultImage[] getDefaultImages(Context context) {
        return new BackgroundWorker.DefaultImage[] {
                new BackgroundWorker.DefaultImage(1, R.drawable.banner, context.getString(R.string.default_image_banner)),
                new BackgroundWorker.DefaultImage(2, R.drawable.caramel, context.getString(R.string.default_image_caramel)),
                new BackgroundWorker.DefaultImage(3, R.drawable.moon, context.getString(R.string.default_image_moon)),
                new BackgroundWorker.DefaultImage(4, R.drawable.puzzle, context.getString(R.string.default_image_puzzle)),
                new BackgroundWorker.DefaultImage(5, R.drawable.ring_nebula, context.getString(R.string.default_image_ring_nebula)),
                new BackgroundWorker.DefaultImage(6, R.drawable.soap_bubble, context.getString(R.string.default_image_soap_bubble)),
                new BackgroundWorker.DefaultImage(7, R.drawable.theater, context.getString(R.string.default_image_theater))
        };
    }

    static void LoadBitmapFromDrawableResource(final Context context, final int resId, final int height,
                                               final OnBitmapLoadedListener listener) {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(context.getResources(), resId, options);
                options.inSampleSize = calculateInSampleSize(options, 0, height);
                options.inJustDecodeBounds = false;
                final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
                if(listener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onBitmapLoaded(bitmap);
                        }
                    });
                }
            }
        }).start();
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while (((halfHeight / inSampleSize) >= reqHeight || reqHeight == 0)
                    && (((halfWidth / inSampleSize) >= reqWidth) || reqWidth == 0)) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
