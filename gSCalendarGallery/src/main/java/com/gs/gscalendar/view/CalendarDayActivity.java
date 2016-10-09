package com.gs.gscalendar.view;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gs.gscalendar.R;
import com.gs.gscalendar.utility.Utils;
import com.jess.ui.TwoWayGridView;

public class CalendarDayActivity extends Activity implements OnClickListener,
		OnTouchListener {
	private static Calendar now;
	private static Calendar displayDate;
	private float mLastX;
	private int mDiffX;
	private static boolean isSlideShow;
	final Handler mHandler = new Handler();
	private Timer slideShowTimer;

	private MyCalendarDayAdapter adapter;
	private static TextView myCalendarTitle, pictureNotFound, imageCount,
			slideshow, multipleSelectedCount;
	private static TwoWayGridView myGridView;
	private static Button myCalendarBtnPrevious, myCalendarBtnNext,
			myGalleryBtnPrevious, myGalleryBtnNext;
	private static ImageView thumbImage;
	private static RelativeLayout scrollView;
	private static LinearLayout myCalendarHeader;
	private ProgressDialog progressDialog;
	private GetCountImagesTask task;

	static final int DATE_DIALOG_ID = 999;

	public static TextView getImageCount() {
		return imageCount;
	}

	public static ImageView getImageView() {
		return thumbImage;
	}

	public static TwoWayGridView getGridView() {
		return myGridView;
	}

	public int getDisplayDateDay() {
		return displayDate.get(Calendar.DAY_OF_MONTH);
	}

	public int getDisplayDateMonth() {
		return displayDate.get(Calendar.MONTH);
	}

	public int getDisplayDateYear() {
		return displayDate.get(Calendar.YEAR);
	}

	public static boolean isSlideShow() {
		return isSlideShow;
	}
	
	public static TextView getMultipleSelectedCount() {
		return multipleSelectedCount;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gallery_day);

		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setMessage(getString(R.string.loading_images));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

		now = Calendar.getInstance();

		myCalendarHeader = (LinearLayout) this
				.findViewById(R.id.myCalendarHeader);
		pictureNotFound = (TextView) this.findViewById(R.id.pictureNotFound);
		slideshow = (TextView) this.findViewById(R.id.slideshow);
		multipleSelectedCount = (TextView) this.findViewById(R.id.multiple_selection_count);
		myCalendarTitle = (TextView) this.findViewById(R.id.myCalendarTitle);
		myCalendarTitle.setOnClickListener(this);
		myCalendarBtnPrevious = (Button) this
				.findViewById(R.id.myCalendarBtnPrevious);
		myCalendarBtnPrevious.setOnClickListener(this);
		myCalendarBtnNext = (Button) this.findViewById(R.id.myCalendarBtnNext);
		myCalendarBtnNext.setOnClickListener(this);
		myGalleryBtnPrevious = (Button) this
				.findViewById(R.id.myGalleryBtnPrevious);
		myGalleryBtnPrevious.setOnClickListener(this);
		myGalleryBtnNext = (Button) this.findViewById(R.id.myGalleryBtnNext);
		myGalleryBtnNext.setOnClickListener(this);

		myGridView = (TwoWayGridView) findViewById(R.id.myCalendar);
		myGridView.setAlwaysDrawnWithCacheEnabled(true);
		imageCount = (TextView) this.findViewById(R.id.imageCount);
		scrollView = (RelativeLayout) this.findViewById(R.id.scrollView);
		scrollView.setOnTouchListener(this);
		thumbImage = (ImageView) this.findViewById(R.id.thumbImage);

		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			displayDate = Calendar.getInstance();
			displayDate.set(extras.getInt("YEAR"), extras.getInt("MONTH"),
					extras.getInt("DAY"));

			setHeaderText();
			runCountTask();
		}
	}

	@Override
	protected void onRestart() {
		runCountTask();
		super.onRestart();
	}

	private void runCountTask() {
		if (task != null) {
			task.cancel(true);
		}

		task = new GetCountImagesTask();
		task.execute(displayDate.getTimeInMillis());
	}

	private class GetCountImagesTask extends AsyncTask<Long, Integer, Integer> {
		@Override
		protected Integer doInBackground(Long... param) {
			Calendar calParam = Calendar.getInstance();
			calParam.setTimeInMillis(param[0]);
			calParam.set(Calendar.HOUR_OF_DAY, 0);

			Calendar cal = (Calendar) calParam.clone();
			cal.add(Calendar.DAY_OF_MONTH, 1);

			long cellDate = calParam.getTimeInMillis();

			final String[] columns = { MediaStore.Images.Media._ID };

			Cursor imagecursor = getContentResolver().query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					columns,
					MediaStore.Images.Media.DATE_TAKEN + " >= ? AND "
							+ MediaStore.Images.Media.DATE_TAKEN + " < ? ",
					new String[] { String.valueOf(cellDate),
							String.valueOf(cal.getTimeInMillis()) }, null);

			int imagesCount = 0;

			if (imagecursor != null) {
				imagesCount = imagecursor.getCount();
				imagecursor.close();
			}

			return imagesCount;
		}

		@Override
		protected void onPreExecute() {
			progressDialog.show();
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (adapter != null) {
				updateGridView(result);
			} else {
				adapter = new MyCalendarDayAdapter(CalendarDayActivity.this,
						getDisplayDateDay(), getDisplayDateMonth(),
						getDisplayDateYear(), thumbImage, imageCount);
				myGridView.setAdapter(adapter);
				updateView(result);
			}

			progressDialog.dismiss();
			super.onPostExecute(result);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_detail, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem itemDeleteSingle = menu.findItem(R.id.menu_delete);
		MenuItem itemDeleteMulti = menu.findItem(R.id.menu_delete_multiple);
		MenuItem itemShare = menu.findItem(R.id.menu_share);
		MenuItem itemCheckAll = menu.findItem(R.id.menu_check_all);
		MenuItem itemCheckClear = menu.findItem(R.id.menu_check_clear);
		MenuItem itemSlideshow = menu.findItem(R.id.menu_slideshow);
		MenuItem itemSlideshowStop = menu.findItem(R.id.menu_slideshow_stop);

		if (adapter.isMultipleSelection()) {
			itemCheckAll.setVisible(false);
			itemCheckClear.setVisible(true);
			itemShare.setVisible(false);
			itemDeleteSingle.setVisible(false);
			itemDeleteMulti.setVisible(true);
			itemSlideshow.setVisible(false);
			itemSlideshowStop.setVisible(false);
		} else {
			itemCheckAll.setVisible(true);
			itemCheckClear.setVisible(false);
			itemShare.setVisible(true);
			itemDeleteSingle.setVisible(true);
			itemDeleteMulti.setVisible(false);
			itemSlideshow.setVisible(true);
		}

		if (isSlideShow) {
			itemSlideshow.setVisible(false);
			itemSlideshowStop.setVisible(true);
			itemCheckAll.setVisible(false);
			itemCheckClear.setVisible(false);
			itemShare.setVisible(false);
			itemDeleteSingle.setVisible(false);
			itemDeleteMulti.setVisible(false);
		} else if (!adapter.isMultipleSelection()) {
			itemSlideshow.setVisible(true);
			itemSlideshowStop.setVisible(false);
		}

		if (adapter.getCount() == 0) {
			return false;
		} else if (adapter.getCount() < 2) {
			itemSlideshow.setVisible(false);
		}

		return true;
	}

	@Override
	public void onBackPressed() {
		if (isSlideShow) {
			adapter.setIsMultipleSelection(false);
			stopSlideShow();
		}
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		stopSlideShow();
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_share:
			if (adapter.getCount() > 0) {
				shareImage();
			}
			break;
		case R.id.menu_delete:
			if (adapter.getCount() > 0) {
				deleteImage();
			}
			break;
		case R.id.menu_delete_multiple:
			if (adapter.getCount() > 0) {
				deleteImage();
			}
			break;
		case R.id.menu_back:
			finish();
			break;
		case R.id.menu_check_all:
			adapter.checkAll();
			break;
		case R.id.menu_check_clear:
			adapter.checkClear();
			multipleSelectedCount.setVisibility(View.INVISIBLE);
			break;
		case R.id.menu_slideshow:
			startSlideShow();
			break;
		case R.id.menu_slideshow_stop:
			stopSlideShow();
			break;
		}
		return true;
	}

	private void startSlideShow() {
		isSlideShow = true;
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myGalleryBtnPrevious.setVisibility(View.INVISIBLE);
		myGalleryBtnNext.setVisibility(View.INVISIBLE);
		myCalendarHeader.setAnimation(Utils.getAnimationFadeOutGone(
				myCalendarHeader, 500));
		slideshow
				.setAnimation(Utils.getAnimationFadeInVisible(slideshow, 1000));
		myGridView.setEnabled(false);

		// Create runnable for posting
		final Runnable mUpdateResults = new Runnable() {
			public void run() {
				scrollImage(1);
			}
		};

		int delay = 2000;
		int period = 5000;
		slideShowTimer = new Timer();
		slideShowTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				mHandler.post(mUpdateResults);
			}
		}, delay, period);
	}

	private void stopSlideShow() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myCalendarHeader.setAnimation(Utils.getAnimationFadeInVisible(
				myCalendarHeader, 500));
		slideshow.setVisibility(View.INVISIBLE);
		myGalleryBtnPrevious.setVisibility(View.VISIBLE);
		myGalleryBtnNext.setVisibility(View.VISIBLE);
		myGridView.setEnabled(true);
		isSlideShow = false;
		if (slideShowTimer != null) {
			slideShowTimer.cancel();
			slideShowTimer.purge();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.custom_numberpicker, null);

			final String[] months31 = { "0", "2", "4", "6", "7", "9", "11" };
			String[] months = new String[12];
			Calendar calMonth = Calendar.getInstance();

			for (int i = 0; i < 12; i++) {
				calMonth.set(Calendar.MONTH, i);
				SimpleDateFormat df = new SimpleDateFormat("MMMM",
						Locale.getDefault());
				months[i] = df.format(calMonth.getTime());
			}

			final NumberPicker year = (NumberPicker) view
					.findViewById(R.id.numberPickerYear);
			final NumberPicker month = (NumberPicker) view
					.findViewById(R.id.numberPickerMonth);
			final NumberPicker day = (NumberPicker) view
					.findViewById(R.id.numberPickerDay);

			year.setMaxValue(Calendar.getInstance().get(Calendar.YEAR));
			year.setMinValue(1970);
			year.setValue(getDisplayDateYear());
			year.setFocusable(true);
			year.setFocusableInTouchMode(true);
			year.setOnValueChangedListener(new OnValueChangeListener() {
				@Override
				public void onValueChange(NumberPicker picker, int oldVal,
						int newVal) {
					if (month.getValue() == 1) {
						if (newVal % 4 == 0) {
							day.setMaxValue(29);
						} else {
							day.setMaxValue(28);
						}
					} else {
						day.setMaxValue(30);
					}
				}
			});

			month.setMaxValue(11);
			month.setMinValue(0);
			month.setValue(getDisplayDateMonth());
			month.setFocusable(true);
			month.setFocusableInTouchMode(true);
			month.setDisplayedValues(months);
			month.setOnValueChangedListener(new OnValueChangeListener() {
				@Override
				public void onValueChange(NumberPicker picker, int oldVal,
						int newVal) {
					if (Arrays.asList(months31)
							.contains(String.valueOf(newVal))) {
						day.setMaxValue(31);
					} else {
						if (newVal == 1) {
							if (year.getValue() % 4 == 0) {
								day.setMaxValue(29);
							} else {
								day.setMaxValue(28);
							}
						} else {
							day.setMaxValue(30);
						}
					}
				}
			});

			day.setMaxValue(31);
			day.setMinValue(1);
			day.setValue(getDisplayDateDay());
			day.setFocusable(true);
			day.setFocusableInTouchMode(true);

			AlertDialog.Builder builder = new Builder(CalendarDayActivity.this);
			builder.setView(view);
			builder.setCancelable(false);
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (year.getValue() != getDisplayDateYear()
									|| month.getValue() != getDisplayDateMonth()
									|| day.getValue() != getDisplayDateDay()) {
								displayDate.set(year.getValue(),
										month.getValue(), day.getValue());
								runCountTask();
							}
							dialog.dismiss();
						}
					});
			builder.create().show();
		}
		return null;
	}

	private void shareImage() {
		WeakReference<File> fileReference = new WeakReference<File>(new File(
				adapter.getSelectedImagePath()));
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		shareIntent.setType("image/*");

		// For a file in shared storage. For data in private storage, use a
		// ContentProvider.
		Uri uri = Uri.fromFile(fileReference.get());
		shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
		startActivity(Intent.createChooser(shareIntent,
				getString(R.string.share)));
	}

	private void deleteImage() {
		AlertDialog.Builder dialog = new Builder(this);
		dialog.setIcon(android.R.drawable.ic_menu_delete);
		dialog.setTitle(getString(R.string.menu_delete));
		if (adapter.isMultipleSelection()) {
			dialog.setMessage(String.format(getString(R.string.delete_multi), adapter.getMultipleSelected().size()));
		} else {
			dialog.setMessage(getString(R.string.delete));
		}
		dialog.setCancelable(false);
		dialog.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (adapter.isMultipleSelection()) {
							DeleteImagesTask deleteTask = new DeleteImagesTask();
							deleteTask.execute();
						} else {
							getContentResolver()
									.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
											MediaStore.Images.Media._ID
													+ " = ? ",
											new String[] { String.valueOf(adapter
													.getSelectedImageId()) });

							CalendarActivity.setImageDeleted(true);
							if ((adapter.getCount() - 1) <= 0) {
								finish();
							} else {
								updateGridView(adapter.getCount() - 1);
							}
						}
						dialog.dismiss();
					}
				});
		dialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		dialog.create().show();
	}

	private class DeleteImagesTask extends AsyncTask<Long, Integer, Boolean> {
		int listCount = adapter.getCount();

		@Override
		protected Boolean doInBackground(Long... param) {
			if (adapter.getMultipleSelected() != null
					&& !adapter.getMultipleSelected().isEmpty()) {
				progressDialog.setMax(adapter.getMultipleSelected().size());

				int progress = 0;
				for (Map.Entry<String, Integer> entry : adapter
						.getMultipleSelected().entrySet()) {
					int delete = getContentResolver().delete(
							MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
							MediaStore.Images.Media._ID + " = ?",
							new String[] { String.valueOf(adapter
									.getItemId(entry.getValue())) });
					publishProgress(++progress);
					listCount--;
				}
			}

			return true;
		}

		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getString(R.string.delete_progress));
			progressDialog.setProgress(0);
			progressDialog.show();
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			progressDialog.setProgress(values[0]);
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (progressDialog.isShowing()) {
				progressDialog.dismiss();
			}

			CalendarActivity.setImageDeleted(true);

			if (listCount <= 0) {
				finish();
			} else {
				adapter.setSelectedPosition(0);
				adapter.checkClear();
				updateGridView(listCount);
			}
			super.onPostExecute(result);
		}
	}

	private void setHeaderText() {
		String dayOfWeekDisplay = Utils.getCalendarDisplayName(displayDate,
				new int[] { Calendar.DAY_OF_WEEK }, Calendar.LONG, " ");

		String dateDisplay = dayOfWeekDisplay
				+ ", "
				+ Utils.getCalendarDisplayName(displayDate, new int[] {
						Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR },
						Calendar.SHORT, " ");

		if (now.get(Calendar.DAY_OF_MONTH) == getDisplayDateDay()
				&& now.get(Calendar.MONTH) == getDisplayDateMonth()
				&& now.get(Calendar.YEAR) == getDisplayDateYear()) {
			dateDisplay = "Today";
		}

		myCalendarTitle.setText(dateDisplay);
	}
	
	private void updateView(int listSize) {
		if (listSize == 1) {
			if (pictureNotFound != null) {
				pictureNotFound.setVisibility(View.GONE);
				myGridView.setVisibility(View.VISIBLE);
				imageCount.setVisibility(View.VISIBLE);
				thumbImage.setVisibility(View.VISIBLE);
				myGalleryBtnPrevious.setVisibility(View.INVISIBLE);
				myGalleryBtnNext.setVisibility(View.INVISIBLE);
			}
		} else if (listSize > 0) {
			if (pictureNotFound != null) {
				pictureNotFound.setVisibility(View.GONE);
				myGridView.setVisibility(View.VISIBLE);
				imageCount.setVisibility(View.VISIBLE);
				thumbImage.setVisibility(View.VISIBLE);
				myGalleryBtnPrevious.setVisibility(View.VISIBLE);
				myGalleryBtnNext.setVisibility(View.VISIBLE);
			}
		} else {
			if (pictureNotFound != null) {
				pictureNotFound.setVisibility(View.VISIBLE);
				myGridView.setVisibility(View.GONE);
				imageCount.setVisibility(View.GONE);
				thumbImage.setVisibility(View.GONE);
				myGalleryBtnPrevious.setVisibility(View.INVISIBLE);
				myGalleryBtnNext.setVisibility(View.INVISIBLE);
			}
		}
	}

	public void updateGridView(int listSize) {
		updateView(listSize);
		
		adapter.updateAdapter(displayDate.get(Calendar.DAY_OF_MONTH),
				displayDate.get(Calendar.MONTH), displayDate.get(Calendar.YEAR));

		thumbImage.setImageBitmap(null);

		if (listSize > 0) {
			myGridView.setAnimation(Utils.getAnimationFadeIn());
			myGridView.smoothScrollToPosition(0);
		}

		setHeaderText();
	}

	private Handler scrollHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (!isSlideShow) {
				myGridView.setEnabled(true);
			}
			myGridView.smoothScrollToPosition(msg.arg1 + msg.arg2);
			adapter.setSelectedPosition(msg.arg1);
		}
	};

	private void scrollToLast() {
		myGridView.setEnabled(false);
		new Thread() {
			public void run() {
				while (myGridView.getLastVisiblePosition() < (adapter
						.getCount() - 1)) {
					int px = Utils.convertDipToPx(CalendarDayActivity.this,
							myGridView.getWidth());
					myGridView.smoothScrollBy(px, 1);
				}

				Message msg = new Message();
				msg.arg1 = myGridView.getLastVisiblePosition();
				msg.arg2 = 0;
				scrollHandler.sendMessage(msg);
			}
		}.start();
	}

	private void scrollToFirst() {
		myGridView.setEnabled(false);
		new Thread() {
			public void run() {
				while (myGridView.getFirstVisiblePosition() > 0) {
					int px = (Utils.convertDipToPx(CalendarDayActivity.this,
							myGridView.getWidth()));
					myGridView.smoothScrollBy(-px, 1);
				}

				Message msg = new Message();
				msg.arg1 = myGridView.getFirstVisiblePosition();
				msg.arg2 = 0;
				scrollHandler.sendMessage(msg);
			}
		}.start();
	}

	private void scrollToPosition(final int position) {
		myGridView.setEnabled(false);
		new Thread() {
			public void run() {
				int direction = position - adapter.getSelectedPosition();
				int distanceDip = 0;
				int cellWidth = myGridView.getWidth() / adapter.getCount();
				boolean isVisible = false;

				if (myGridView.getFirstVisiblePosition() >= position) {
					distanceDip = (position - 1)
							- myGridView.getFirstVisiblePosition();
				} else if (myGridView.getLastVisiblePosition() <= position) {
					distanceDip = (position + 1)
							- myGridView.getLastVisiblePosition();
				} else {
					isVisible = true;
				}

				if (position == 0) {
					scrollToFirst();
				} else if (position == adapter.getCount() - 1) {
					scrollToLast();
				} else {
					while (!isVisible) {
						int px = (Utils.convertDipToPx(
								CalendarDayActivity.this, cellWidth) * distanceDip);
						myGridView.smoothScrollBy(px, 1);

						isVisible = (position > myGridView
								.getFirstVisiblePosition())
								&& (position < myGridView
										.getLastVisiblePosition());
					}

					Message msg = new Message();
					msg.arg1 = position;
					msg.arg2 = direction;
					scrollHandler.sendMessage(msg);
				}
			}
		}.start();
	}

	private void animateScrollImage(final int direction) {
		final int position = adapter.getSelectedPosition();
		
		Animation animation = Utils
				.getAnimationSlideHorizontal(this, direction);

		if (isSlideShow) {
			animation = Utils.getAnimationFadeOut(700);
		}

		animation.setFillAfter(true);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				int posMove = position;

				switch (direction) {
				case -1:
					posMove -= 1;
					if (posMove < 0) {
						scrollToLast();
					} else {
						scrollToPosition(posMove);
					}
					break;
				case 1:
					posMove += 1;
					if (posMove >= adapter.getCount()) {
						scrollToFirst();
					} else {
						scrollToPosition(posMove);
					}
					break;
				default:
					break;
				}
			}
		});

		thumbImage.startAnimation(animation);
	}

	private void scrollImage(final int direction) {
		if (!adapter.isMultipleSelection() && adapter.getCount() > 1) {
			animateScrollImage(direction);
		}
	}

	private void scrollView(int direction) {
		stopSlideShow();

		switch (direction) {
		case -1:
			if (displayDate != null) {
				displayDate.add(Calendar.DAY_OF_MONTH, -1);
				runCountTask();
			}
			break;
		case 1:
			if (displayDate != null) {
				displayDate.add(Calendar.DAY_OF_MONTH, 1);
				runCountTask();
			}
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.myCalendarBtnPrevious:
			scrollView(-1);
			break;
		case R.id.myCalendarBtnNext:
			scrollView(1);
			break;
		case R.id.myCalendarTitle:
			showDialog(DATE_DIALOG_ID);
			break;
		case R.id.myGalleryBtnPrevious:
			if (!isSlideShow) {
				scrollImage(-1);
			}
			break;
		case R.id.myGalleryBtnNext:
			if (!isSlideShow) {
				scrollImage(1);
			}
			break;
		default:
			break;
		}
	}

	private Handler myTouchHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case -1:
				scrollImage(-1);
				break;
			case 1:
				scrollImage(1);
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.scrollView:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				final float curXDown = event.getX();
				mLastX = curXDown;
				break;
			case MotionEvent.ACTION_MOVE:
				final float curX = event.getX();
				if (mLastX > 0) {
					mDiffX = (int) (mLastX - curX);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mDiffX < -10) { // horizontal scrolling left-right
					if (!isSlideShow) {
						scrollImage(-1);
					}
					break;
				} else if (mDiffX > 10) { // horizontal scrolling right-left
					if (!isSlideShow) {
						scrollImage(1);
					}
					break;
				}
				break;
			}
			break;
		default:
			break;
		}
		return true;
	}

}
