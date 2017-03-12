package main;

import ircover.ballwallpaper.R;
import main.util.IabHelper;
import main.util.IabResult;
import main.util.Inventory;
import main.util.Purchase;

import android.app.Activity;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SettingsActivity extends Activity implements IabHelper.OnIabSetupFinishedListener {

	private static class OnPrefSeekChangeListener implements SeekBar.OnSeekBarChangeListener {

		private String prefName;
		private float min, max;
		private Context context;

		OnPrefSeekChangeListener(String prefName, SeekBar bar) {
			context = bar.getContext();
			this.prefName = prefName;
			bar.setMax(SEEK_BAR_MAX_VALUE);
			min = PreferenceWorker.getPrefMin(context, prefName);
			max = PreferenceWorker.getPrefMax(context, prefName);
			float value = PreferenceWorker.getPrefValue(context, prefName);

			bar.setProgress((int) ((value - min) / getValuePerMark()));
		}

		private float getValuePerMark() {
			return (max - min) / SEEK_BAR_MAX_VALUE;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			float value = getValuePerMark() * progress + min;
			PreferenceWorker.setPrefValue(context, prefName, value);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

		}
	}

	private static final int SEEK_BAR_MAX_VALUE = 30;
	private static final int REQUEST_CODE_DONATE = 1;
	private static final int REQUEST_CODE_SELECT_FILE = 2;

	private AdView adView;
	private ArrayList<PreferenceWorker.PreferenceChangedActionDestroyer> destroyers;
	private IabHelper helper;
	private boolean helperSetupSucceed, adLoaded;
	private Timer timer;

	private IabHelper.OnConsumeFinishedListener onConsume = new IabHelper.OnConsumeFinishedListener() {
		@Override
		public void onConsumeFinished(Purchase purchase, IabResult result) {

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		findViewById(R.id.wallpaperSetterButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
				intent.putExtra(WallpaperManager.WALLPAPER_PREVIEW_META_DATA,
						new ComponentName(SettingsActivity.this, MyWallpaperService.class));
				startActivity(intent);
			}
		});
		findViewById(R.id.donationButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ShowDonationDialog();
			}
		});
		findViewById(R.id.resourceSelectButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ShowDefaultImagesDialog();
			}
		});

		SeekBar ballSizeBar = (SeekBar) findViewById(R.id.ballSizePrefBar);
		ballSizeBar.setOnSeekBarChangeListener(
				new OnPrefSeekChangeListener(PreferenceWorker.PREFERENCE_BALL_SIZE, ballSizeBar));
		SeekBar ballSpeedBar = (SeekBar) findViewById(R.id.ballSpeedPrefBar);
		ballSpeedBar.setOnSeekBarChangeListener(
				new OnPrefSeekChangeListener(PreferenceWorker.PREFERENCE_BALL_SPEED, ballSpeedBar));

		RadioGroup colorGroup = (RadioGroup) findViewById(R.id.ballsColorGroup);
		int colorMode = PreferenceWorker.getBallColorMode(this);
		int activeColorButtonId = 0;
		switch (colorMode) {
			case PreferenceWorker.ballColorRandom:
				activeColorButtonId = R.id.ballsColorRandom;
				break;
			case PreferenceWorker.ballColorFixed:
				activeColorButtonId = R.id.ballsColorFixed;
				break;
		}
		RadioButton activeColorButton = (RadioButton) findViewById(activeColorButtonId);
		if(activeColorButton != null) {
			activeColorButton.setChecked(true);
		}
		colorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch(checkedId) {
					case R.id.ballsColorRandom:
						PreferenceWorker.setBallColorMode(SettingsActivity.this,
								PreferenceWorker.ballColorRandom);
						break;
					case R.id.ballsColorFixed:
						PreferenceWorker.setBallColorMode(SettingsActivity.this,
								PreferenceWorker.ballColorFixed);
						break;
				}
			}
		});
		final CheckBox explodeBox = (CheckBox) findViewById(R.id.explodeBallBox);
		explodeBox.setChecked(PreferenceWorker.isExplodeOnPop(this));
		explodeBox.setEnabled(PreferenceWorker.isPopOnClick(this));
		explodeBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceWorker.setExplodeOnPop(SettingsActivity.this, isChecked);
				BallsContainer.setExplodeOnPop(isChecked);
			}
		});
		CheckBox popBox = (CheckBox) findViewById(R.id.popBallBox);
		popBox.setChecked(PreferenceWorker.isPopOnClick(this));
		popBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceWorker.setPopOnClick(SettingsActivity.this, isChecked);
				explodeBox.setEnabled(isChecked);
				BallsContainer.setPopOnClick(isChecked);
			}
		});
		CheckBox rotateBox = (CheckBox) findViewById(R.id.rotateBallBox);
		rotateBox.setChecked(PreferenceWorker.isRotateBall(this));
		rotateBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceWorker.setRotateBall(SettingsActivity.this, isChecked);
				BallsContainer.setRotate(isChecked);
			}
		});
		CheckBox trailBox = (CheckBox) findViewById(R.id.showBallTrailBox);
		trailBox.setChecked(PreferenceWorker.isShowBallTrail(this));
		trailBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceWorker.setShowBallTrail(SettingsActivity.this, isChecked);
				BallsContainer.setShowBallTrail(isChecked);
			}
		});
		findViewById(R.id.fileSelectButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SelectFile();
			}
		});
		findViewById(R.id.fileClearButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PreferenceWorker.setBackground(SettingsActivity.this, new BackgroundWorker.ImageSource());
				UpdateBackground();
			}
		});

		adLoaded = false;
		adView = (AdView) findViewById(R.id.adView);
		BasicFunctions.InitAds(getApplicationContext(), adView);
		adView.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				adLoaded = true;
				UpdateAdsBannerVisibility();
			}
		});
		UpdateAdsBannerVisibility();
		UpdateBackground();

		helperSetupSucceed = false;
		helper = new IabHelper(this, BasicFunctions.getAppLicenceKey());
		helper.startSetup(this);

		destroyers = new ArrayList<PreferenceWorker.PreferenceChangedActionDestroyer>();
		destroyers.add(PreferenceWorker.AddShowAdsBannerPreferenceChangedAction(this,
				new Runnable() {
					@Override
					public void run() {
						UpdateAdsBannerVisibility();
					}
				}));
		destroyers.add(PreferenceWorker.AddBallSizePreferenceChangedAction(this,
				new Runnable() {
					@Override
					public void run() {
						UpdateBallSize();
					}
				}));
		destroyers.add(PreferenceWorker.AddBallSpeedPreferenceChangedAction(this,
				new Runnable() {
					@Override
					public void run() {
						UpdateBallSpeed();
					}
				}));
		destroyers.add(PreferenceWorker.AddBallColorModePreferenceChangedAction(this,
				new Runnable() {
					@Override
					public void run() {
						UpdateBallColorMode();
					}
				}));

		timer = new Timer();
		if(!PreferenceWorker.isFirstAppShow(this) && PreferenceWorker.isShowMarkDialog(this)) {
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ShowMarkAppDialog();
						}
					});
				}
			}, 20000);
		}
	}

	private void ShowMarkAppDialog() {
		long showDialogInterval = CommonConstants.MILLIS_IN_DAY;
		if(System.currentTimeMillis() - PreferenceWorker.getLastMarkShow(this) > showDialogInterval) {
			Dialog dialog = Dialogs.getMarkAppDialog(this, new OnClickListener() {
				@Override
				public void onClick(View v) {
					Uri uri = Uri.parse("market://details?id=" + getPackageName());
					Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
					int flags = Intent.FLAG_ACTIVITY_NO_HISTORY |
							Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
					goToMarket.addFlags(flags);
					if (getPackageManager().resolveActivity(goToMarket, 0) == null) {
						uri = Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName());
						goToMarket = new Intent(Intent.ACTION_VIEW, uri);
						goToMarket.addFlags(flags);
					}
					startActivity(goToMarket);
				}
			});
			dialog.show();
			PreferenceWorker.setMarkShowed(this);
		}
	}

	private void SelectFile() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
	}

	@Override
	public void onIabSetupFinished(IabResult result) {
		helperSetupSucceed = result.isSuccess();
		CheckPurchaseInventory();
	}

	private void CheckPurchaseInventory() {
		try {
			helper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
				@Override
				public void onQueryInventoryFinished(IabResult result, Inventory inv) {
					if(!result.isFailure()) {
						Purchase purchase = inv.getPurchase(getDonateSKU());
						if(purchase != null && purchase.isAutoRenewing()) {
							try {
								helper.consumeAsync(purchase, onConsume);
							} catch (IabHelper.IabAsyncInProgressException e) {
								ShowHelperErrorMessage(e);
							}
						}
					}
				}
			});
		} catch (IabHelper.IabAsyncInProgressException e) {
			ShowHelperErrorMessage(e);
		}
	}

	private void UpdateAdsBannerVisibility() {
		adView.setVisibility(adLoaded && PreferenceWorker.isShowingAdsBanner(this) ? View.VISIBLE : View.GONE);
	}

	private void UpdateBallSize() {
		BallsContainer.setBallSize(this, PreferenceWorker.getBallSize(this));
	}

	private void UpdateBallSpeed() {
		BallsContainer.setSpeedMultiplier(PreferenceWorker.getBallSpeed(this));
	}

	private void UpdateBallColorMode() {
		BallsContainer.setColorChooser(BallColorChooser.getChooserFromPreferences(this));
	}

	private void UpdateBackground() {
		BackgroundWorker.ImageSource image = PreferenceWorker.getBackground(this);
		BallsContainer.setBackground(image);
		TextView fileText = (TextView) findViewById(R.id.filePathText);
		if(image.isEmpty()) {
			findViewById(R.id.fileClearButton).setVisibility(View.GONE);
			fileText.setText(R.string.background_not_selected);
		} else {
			findViewById(R.id.fileClearButton).setVisibility(View.VISIBLE);
			fileText.setText(image.toString());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(adView != null) {
			adView.destroy();
			adView = null;
		}
		if(destroyers != null) {
			for(PreferenceWorker.PreferenceChangedActionDestroyer destroyer : destroyers) {
				destroyer.Destroy();
			}
			destroyers = null;
		}
		if(helper != null) {
			try {
				helper.dispose();
			} catch (IabHelper.IabAsyncInProgressException e) {
				e.printStackTrace();
			}
			helper = null;
		}
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
		((BallsPreviewSurface)findViewById(R.id.ballsPreview)).Destroy();
	}

	private void ShowDonationDialog() {
		Dialogs.getDonateDialog(this, new OnClickListener() {
			@Override
			public void onClick(View v) {
				Donate();
			}
		}).show();
	}

	private void ShowDefaultImagesDialog() {
		Dialog dialog = Dialogs.getDefaultImagesDialog(this, new DefaultImagesAdapter.OnImageSelectedListener() {
			@Override
			public void onImageSelected(BackgroundWorker.DefaultImage image, Dialog dialog) {
				PreferenceWorker.setBackground(SettingsActivity.this,
						new BackgroundWorker.ImageSource(image));
				UpdateBackground();
				if(dialog != null) {
					dialog.dismiss();
				}
			}
		});
		dialog.show();
	}

	private void Donate() {
		if(helperSetupSucceed) {
			try {
				helper.launchPurchaseFlow(this, getDonateSKU(), REQUEST_CODE_DONATE,
						new IabHelper.OnIabPurchaseFinishedListener() {
							@Override
							public void onIabPurchaseFinished(IabResult result, Purchase info) {
								if (!result.isFailure() && info.getSku().equals(getString(R.string.sku_donate))) {
									Toast.makeText(SettingsActivity.this, R.string.thanks_for_help, Toast.LENGTH_SHORT).show();
									try {
										helper.consumeAsync(info, onConsume);
									} catch (IabHelper.IabAsyncInProgressException e) {
										ShowHelperErrorMessage(e);
									}
								}
							}
						});
			} catch (IabHelper.IabAsyncInProgressException e) {
				ShowHelperErrorMessage(e);
			}
		} else {
			Toast.makeText(this, R.string.in_process_google_setup, Toast.LENGTH_SHORT).show();
		}
	}

	private String getDonateSKU() {
		return getString(R.string.sku_donate);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQUEST_CODE_DONATE:
				if (helper != null) {
					helper.handleActivityResult(requestCode, resultCode, data);
				}
				break;
			case REQUEST_CODE_SELECT_FILE:
				if(data != null) {
					String type = getContentResolver().getType(data.getData());
					if(type != null && type.startsWith("image/")) {
						PreferenceWorker.setBackground(this, new BackgroundWorker.ImageSource(data.getData()));
						UpdateBackground();
					} else {
						Toast.makeText(this, R.string.error_file_not_image, Toast.LENGTH_SHORT).show();
					}
				}
				break;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		((BallsPreviewSurface)findViewById(R.id.ballsPreview)).Pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		((BallsPreviewSurface)findViewById(R.id.ballsPreview)).Resume();
	}

	private void ShowHelperErrorMessage(Exception e) {
		if(e instanceof IabHelper.IabAsyncInProgressException) {
			Toast.makeText(this, R.string.in_process_google, Toast.LENGTH_SHORT).show();
		} else {
			e.printStackTrace();
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
		}
	}
}
