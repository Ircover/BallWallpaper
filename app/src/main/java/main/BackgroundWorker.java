package main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

class BackgroundWorker {

    static class DefaultImage {
        int id, resId;
        String name;
        DefaultImage() {
            this(ImageSource.Resource_Empty, ImageSource.Resource_Empty, "");
        }
        DefaultImage(int id, int resId, String name) {
            this.id = id;
            this.resId = resId;
            this.name = name;
        }
        boolean isEmpty() {
            return resId == ImageSource.Resource_Empty;
        }
    }

    static class ImageSource {
        static final int Resource_Empty = -1;
        Uri file;
        DefaultImage resource;
        ImageSource() {
            this((Uri)null);
        }
        ImageSource(Uri file) {
            this(file, new DefaultImage());
        }
        ImageSource(@NonNull DefaultImage resource) {
            this(null, resource);
        }
        ImageSource(Uri file, @NonNull DefaultImage resource) {
            this.file = file;
            this.resource = resource;
        }
        boolean isEmpty() {
            return file == null && resource.isEmpty();
        }

        @Override
        public String toString() {
            String result = "";
            if(!resource.isEmpty()) {
                result += resource.name;
            } else if(file != null) {
                result = file.getPath();
            }
            return result;
        }
    }

    private WeakReference<Context> context;
    private ImageSource image;
    private ConcurrentHashMap<Point, Bitmap> bitmaps;
    private boolean isProgress;

    BackgroundWorker(Context context) {
        this.context = new WeakReference<Context>(context);
        bitmaps = new ConcurrentHashMap<Point, Bitmap>();
        isProgress = false;
    }

    Bitmap getBitmap(float width, float height) {
        if(!image.isEmpty()) {
            Point size = new Point(width, height);
            if(bitmaps.containsKey(size)) {
                return bitmaps.get(size);
            } else {
                LoadBitmap(size);
            }
        }
        return null;
    }

    private void LoadBitmap(final Point size) {
        synchronized (this) {
            if (!isProgress) {
                isProgress = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(context.get() != null) {
                            try {
                                Bitmap bitmap;
                                if(image.file != null) {
                                    bitmap = MediaStore.Images.Media.getBitmap(context.get().getContentResolver(),
                                            image.file);
                                } else {
                                    bitmap = BitmapFactory.decodeResource(context.get().getResources(), image.resource.resId);
                                }

                                int dx = 0, dy = 0;
                                float ratio = size.x / bitmap.getWidth();
                                if(((float)bitmap.getHeight()) * ratio > size.y) {
                                    dy = (int) (((float)bitmap.getHeight()) * ratio - size.y);
                                } else {
                                    ratio /= ((float)bitmap.getHeight()) * ratio / size.y;
                                    dx = (int) (((float)bitmap.getWidth()) * ratio - size.x);
                                }

                                bitmap = Bitmap.createBitmap(bitmap, dx / 2, dy / 2,
                                        bitmap.getWidth() - dx, bitmap.getHeight() - dy);
                                bitmap = Bitmap.createScaledBitmap(bitmap, (int)size.x, (int)size.y, false);
                                bitmaps.put(size, bitmap);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        isProgress = false;
                    }
                }).start();
            }
        }
    }

    void setImage(ImageSource image) {
        this.image = image;
        bitmaps.clear();
    }
}
