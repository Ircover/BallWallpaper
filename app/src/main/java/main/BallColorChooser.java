package main;

import android.content.Context;
import android.graphics.Color;

import java.lang.ref.WeakReference;
import java.util.Random;

import ircover.ballwallpaper.R;

abstract class BallColorChooser {

    protected WeakReference<Context> context;
    private BallColorChooser(Context context) {
        this.context = new WeakReference<Context>(context);
    }

    private static class UnsupportedColorModeException extends RuntimeException {
    }

    public abstract int getColor();

    static BallColorChooser getChooserFromPreferences(Context context) {
        return getChooser(context, PreferenceWorker.getBallColorMode(context));
    }

    static BallColorChooser getChooser(Context context, int colorMode) {
        switch (colorMode) {
            case PreferenceWorker.ballColorRandom:
                return new BallColorRandomChooser(context);
            case PreferenceWorker.ballColorFixed:
                return new BallColorFixedChooser(context);
            default:
                throw new UnsupportedColorModeException();
        }
    }

    private static class BallColorRandomChooser extends BallColorChooser {
        private static final Random random = new Random();

        BallColorRandomChooser(Context context) {
            super(context);
        }

        @Override
        public int getColor() {
            return Color.rgb(random.nextInt(200) + 56, random.nextInt(200) + 56, random.nextInt(200) + 56);
        }
    }

    private static class BallColorFixedChooser extends BallColorChooser {
        BallColorFixedChooser(Context context) {
            super(context);
        }

        @Override
        public int getColor() {
            if(context.get() != null) {
                return context.get().getResources().getColor(R.color.BallColorFixed);
            }
            return 0;
        }
    }
}
