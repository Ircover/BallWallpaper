package main;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import ircover.ballwallpaper.R;

class Dialogs {

    static Dialog getDefaultImagesDialog(Context context, DefaultImagesAdapter.OnImageSelectedListener listener) {
        final Dialog dialog = new Dialog(context, R.style.AppDialogTheme);
        setDialogDividerColor(context, dialog);
        dialog.setTitle(R.string.button_select_resource);
        dialog.setContentView(R.layout.default_images_dialog);
        dialog.findViewById(R.id.closeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        RecyclerView imageList = (RecyclerView) dialog.findViewById(R.id.imageList);
        imageList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        final DefaultImagesAdapter adapter = new DefaultImagesAdapter(context, listener, dialog);
        imageList.setAdapter(adapter);
        return dialog;
    }

    static Dialog getDonateDialog(final Context context, View.OnClickListener onOkClick) {
        final Dialog dialog = new Dialog(context, R.style.AppDialogTheme);
        setDialogDividerColor(context, dialog);
        dialog.setContentView(R.layout.donation_dialog);
        dialog.setTitle(R.string.donation);
        CheckBox showAds = (CheckBox)dialog.findViewById(R.id.showAdsBannerBox);
        showAds.setChecked(PreferenceWorker.isShowingAdsBanner(context));
        showAds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceWorker.setShowingAdsBanner(context, isChecked);
            }
        });

        dialog.findViewById(R.id.donateButton).setOnClickListener(onOkClick);
        dialog.findViewById(R.id.finishButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        return dialog;
    }

    static Dialog getMarkAppDialog(final Context context, final View.OnClickListener onOkClick) {
        final Dialog dialog = new Dialog(context, R.style.AppDialogTheme);
        setDialogDividerColor(context, dialog);
        dialog.setContentView(R.layout.mark_app_dialog);
        dialog.setTitle(R.string.mark_dialog_title);

        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.markButton:
                        if (onOkClick != null) {
                            onOkClick.onClick(v);
                        }
                    case R.id.markNeverButton:
                        PreferenceWorker.neverShowMarkDialog(context);
                }
                dialog.dismiss();
            }
        };
        dialog.findViewById(R.id.markButton).setOnClickListener(onClick);
        dialog.findViewById(R.id.markLaterButton).setOnClickListener(onClick);
        dialog.findViewById(R.id.markNeverButton).setOnClickListener(onClick);
        return dialog;
    }

    private static void setDialogDividerColor(Context context, Dialog d) {
        int dividerId = d.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
        View divider = d.findViewById(dividerId);
        divider.setBackgroundColor(context.getResources().getColor(R.color.DialogTitleTextColor));
    }

}
