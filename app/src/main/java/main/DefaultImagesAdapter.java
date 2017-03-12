package main;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ircover.ballwallpaper.R;

class DefaultImagesAdapter extends RecyclerView.Adapter<DefaultImagesAdapter.Holder> {

    interface OnImageSelectedListener {
        void onImageSelected(BackgroundWorker.DefaultImage image, Dialog dialog);
    }

    static class Holder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView textView;

        Holder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            textView = (TextView) itemView.findViewById(R.id.text);
        }

        private void setImage(Bitmap b) {
            imageView.setImageBitmap(b);
            textView.setVisibility(b == null ? View.VISIBLE : View.GONE);
        }
    }

    private Context context;
    private BackgroundWorker.DefaultImage[] images;
    private OnImageSelectedListener listener;
    private Dialog dialog;

    DefaultImagesAdapter(Context context, OnImageSelectedListener listener, Dialog dialog) {
        this.context = context;
        this.listener = listener;
        this.dialog = dialog;
        images = BasicFunctions.getDefaultImages(context);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View result = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.default_image_item, null);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.MATCH_PARENT);
        result.setLayoutParams(lp);
        return new Holder(result);
    }

    @Override
    public void onBindViewHolder(final Holder holder, final int position) {
        holder.setImage(null);
        BasicFunctions.LoadBitmapFromDrawableResource(context, images[position].resId, BasicFunctions.getDisplayHeight(context),
                new BasicFunctions.OnBitmapLoadedListener() {
                    @Override
                    public void onBitmapLoaded(Bitmap b) {
                        if(holder.getAdapterPosition() == position) {
                            holder.setImage(b);
                        }
                    }
                });
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) {
                    listener.onImageSelected(images[holder.getAdapterPosition()], dialog);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.length;
    }
}
