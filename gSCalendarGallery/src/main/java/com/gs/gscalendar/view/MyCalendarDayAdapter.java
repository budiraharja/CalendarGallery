package com.gs.gscalendar.view;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gs.gscalendar.R;
import com.gs.gscalendar.utility.Utils;

public class MyCalendarDayAdapter extends BaseAdapter {
	private static final int COLUMN_WIDTH = 100;

	private Activity context;
	private int month;
	private int year = 1970;
	private int day;
	private int selectedPosition;
	private int listSize;
	private String[] bitmapUri;
	private ImageView displayImageView;
	private TextView imageCount;
	private long[] imageIds;
	private Map<String, Integer> selectedPositions;
	private boolean isMultipleSelection;
	private ViewGroup viewGroup;

	private GetImagesTask getImageTask;
	private GetInitImagesTask initTask;

	private LruCache<String, Bitmap> mMemoryCache = null;

	public MyCalendarDayAdapter(Activity context, int day, int month, int year,
			ImageView imageView, TextView imageCount) {
		this.context = context;
		this.day = day;
		this.month = month;
		this.year = year;
		this.displayImageView = imageView;
		this.imageCount = imageCount;
		init();
	}

	private void initMemoryCache() {
		// Get memory class of this device, exceeding this amount will throw an
		// OutOfMemory exception.
		final int memClass = ((ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = 1024 * 1024 * memClass / 8;

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				// The cache size will be measured in bytes rather than number
				// of items.
				return Utils.getBitmapByteCount(bitmap);
			}
		};
	}

	private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null && mMemoryCache != null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	private Bitmap getBitmapFromMemCache(String key) {
		if (mMemoryCache != null) {
			return (Bitmap) mMemoryCache.get(key);
		}

		return null;
	}

	protected void updateAdapter(int day, int month, int year) {
		if (initTask != null) {
			initTask.cancel(true);
		}

		if (getImageTask != null) {
			getImageTask.cancel(true);
		}

		this.day = day;
		this.month = month;
		this.year = year;
		init();
	}

	private void init() {
		this.bitmapUri = null;
		this.imageIds = null;
		this.listSize = 0;
		this.selectedPosition = 0;
		this.selectedPositions = null;
		this.isMultipleSelection = false;
		
		if (CalendarDayActivity.getMultipleSelectedCount().isShown()) {
			CalendarDayActivity.getMultipleSelectedCount()
				.setVisibility(View.INVISIBLE);
		}

		Calendar cal = Calendar.getInstance();
		cal.set(getYear(), getMonth(), getDayOfMonth());

		initTask = new GetInitImagesTask();
		initTask.execute(cal.getTimeInMillis());
	}

	private void initDone() {
		this.selectedPositions = new HashMap<String, Integer>();
		isMultipleSelection = false;
		if (this.imageCount != null) {
			this.imageCount.setText(String.format(
					context.getString(R.string.image_count), 1, listSize));
		}
		initMemoryCache();
	}

	@Override
	public int getCount() {
		return this.listSize;
	}

	@Override
	public Object getItem(int index) {
		return getBitmapFromMemCache(String.valueOf(index));
	}

	@Override
	public long getItemId(int position) {
		if (imageIds != null) {
			return imageIds[position];
		}

		return 0;
	}

	public int getDayOfMonth() {
		return this.day;
	}

	public int getMonth() {
		return this.month;
	}

	public int getYear() {
		return this.year;
	}

	public String getSelectedImagePath() {
		return this.bitmapUri[selectedPosition];
	}

	public long getSelectedImageId() {
		return this.imageIds[selectedPosition];
	}

	public int getSelectedPosition() {
		return this.selectedPosition;
	}

	public void setSelectedPosition(int position) {
		if (position >= 0 && position < listSize) {
			if (this.viewGroup != null) {
				clickSelectedCellView(position);
			}
			this.selectedPosition = position;
		}
	}

	private final int getColumnWidth() {
		return Utils.convertDipToPx(context, COLUMN_WIDTH);
	}

	public boolean isMultipleSelection() {
		return this.isMultipleSelection;
	}

	public void setIsMultipleSelection(boolean val) {
		this.isMultipleSelection = val;
	}

	public Map<String, Integer> getMultipleSelected() {
		return this.selectedPositions;
	}
	
	public void hideCheck() {
		this.isMultipleSelection = false;
		
		for (int i = 0; i < getCount(); i++) {
			RelativeLayout cell = (RelativeLayout) this.viewGroup
					.findViewById(i);
			if (cell != null) {
				ImageView check = (ImageView) cell
						.findViewById(R.id.cell_selected);
				if (check != null) {
					check.setImageResource(R.drawable.uncheck);
					check.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	public void checkClear() {
		this.isMultipleSelection = false;
		
		if (selectedPositions != null && !selectedPositions.isEmpty()) {
			selectedPositions.clear();
		}
		
		if (CalendarDayActivity.getMultipleSelectedCount().isShown()) {
			CalendarDayActivity.getMultipleSelectedCount().setVisibility(View.INVISIBLE);
		}
		
		CalendarDayActivity.getGridView().setSelection(selectedPosition);
		RelativeLayout cell = (RelativeLayout) this.viewGroup
				.findViewById(selectedPosition);
		if (cell != null) {
			cell.setSelected(true);
		}
		
		hideCheck();
	}
	
	public void showCheck() {
		this.isMultipleSelection = true;
		for (int i = 0; i < getCount(); i++) {
			RelativeLayout cell = (RelativeLayout) this.viewGroup
					.findViewById(i);
			if (cell != null) {
				ImageView check = (ImageView) cell
						.findViewById(R.id.cell_selected);
				if (check != null) {
					check.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	public void checkAll() {
		this.isMultipleSelection = true;
		showCheck();
		for (int i = 0; i < getCount(); i++) {
			RelativeLayout cell = (RelativeLayout) this.viewGroup
					.findViewById(i);
			if (cell != null) {
				ImageView check = (ImageView) cell
						.findViewById(R.id.cell_selected);
				if (check != null) {
					check.setImageResource(R.drawable.check);
				}
			}

			selectedPositions.put(String.valueOf(i), i);
			
			if(!CalendarDayActivity.getMultipleSelectedCount().isShown()) {
				CalendarDayActivity.getMultipleSelectedCount().setVisibility(View.VISIBLE);
			}
			
			String multipleSelectedCount = String.format(
					context.getString(R.string.multiple_image_count),
					selectedPositions.size(), getCount());
			CalendarDayActivity.getMultipleSelectedCount()
					.setText(multipleSelectedCount);
		}

		RelativeLayout lastSelectedCellLayout = (RelativeLayout) this.viewGroup
				.findViewById(selectedPosition);
		if (lastSelectedCellLayout != null) {
			lastSelectedCellLayout.setSelected(false);
		}
	}

	private class GetInitImagesTask extends AsyncTask<Long, Integer, Integer> {
		@Override
		protected Integer doInBackground(Long... param) {
			Calendar calParam = Calendar.getInstance();
			calParam.setTimeInMillis(param[0]);
			calParam.set(Calendar.HOUR_OF_DAY, 0);

			Calendar cal = (Calendar) calParam.clone();
			cal.add(Calendar.DAY_OF_MONTH, 1);

			long cellDate = calParam.getTimeInMillis();

			final String[] columns = { MediaStore.Images.Media._ID,
					MediaStore.Images.Media.DATA };

			Cursor imagecursor = context.getContentResolver().query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					columns,
					MediaStore.Images.Media.DATE_TAKEN + " >= ? AND "
							+ MediaStore.Images.Media.DATE_TAKEN + " < ? ",
					new String[] { String.valueOf(cellDate),
							String.valueOf(cal.getTimeInMillis()) }, MediaStore.Images.Media.DATE_TAKEN + " ASC");

			int imageCount = 0;
			if (imagecursor != null) {
				imageCount = imagecursor.getCount();
				imageIds = new long[imageCount];
				bitmapUri = new String[imageCount];

				if (imagecursor.moveToFirst()) {
					int index = 0;
					do {
						long id = imagecursor.getLong(imagecursor
								.getColumnIndex(MediaStore.Images.Media._ID));
						String path = imagecursor.getString(imagecursor
								.getColumnIndex(MediaStore.Images.Media.DATA));

						try {
							bitmapUri[index] = path;
							imageIds[index++] = id;
						} catch (IndexOutOfBoundsException e) {
						}
					} while (imagecursor.moveToNext());
				}

				imagecursor.close();
			}

			return imageCount;
		}

		@Override
		protected void onPostExecute(Integer result) {
			listSize = result;
			initDone();
			notifyDataSetChanged();
			super.onPostExecute(result);
		}
	}

	private class GetImagesTask extends AsyncTask<Long, Integer, Bitmap> {
		Bitmap thumbnail = null;
		int position;
		private final WeakReference<ImageView> imageViewReference;
		private final WeakReference<View> viewReference;

		public GetImagesTask(ImageView imageView, View rowView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
			viewReference = new WeakReference<View>(rowView);
		}

		@Override
		protected synchronized Bitmap doInBackground(Long... param) {
			position = Integer.parseInt(String.valueOf(param[0]));

			Calendar calParam = Calendar.getInstance();
			calParam.set(getYear(), getMonth(), getDayOfMonth());
			calParam.set(Calendar.HOUR_OF_DAY, 0);

			Calendar cal = (Calendar) calParam.clone();
			cal.add(Calendar.DAY_OF_MONTH, 1);

			final String[] columns = { MediaStore.Images.Media.DATA,
					MediaStore.Images.Media._ID,
					MediaStore.Images.Media.DATE_TAKEN,
					MediaStore.Images.Media.TITLE,
					MediaStore.Images.Media.DISPLAY_NAME };

			final String orderBy = MediaStore.Images.Media._ID;

			Cursor imagecursor = null;

			if (imageIds == null) {
				imagecursor = context.getContentResolver().query(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						columns,
						MediaStore.Images.Media.DATE_TAKEN + " >= ? AND "
								+ MediaStore.Images.Media.DATE_TAKEN + " < ? ",
						new String[] {
								String.valueOf(calParam.getTimeInMillis()),
								String.valueOf(cal.getTimeInMillis()) },
						orderBy);
			} else {
				imagecursor = context.getContentResolver().query(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
						MediaStore.Images.Media._ID + " = ? ",
						new String[] { String.valueOf(imageIds[position]) },
						orderBy);
			}

			if (imagecursor != null) {
				if (imageIds == null) {
					imageIds = new long[imagecursor.getCount()];
				} else {
					if (imagecursor.moveToFirst()) {
						String path = imagecursor.getString(imagecursor
								.getColumnIndex(MediaStore.Images.Media.DATA));
						thumbnail = decodeSampledBitmapFromFilePath(path,
								(getColumnWidth()),
								(int) (getColumnWidth() * 1.2));
					}
				}

				imagecursor.close();
			}

			return thumbnail;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (imageViewReference != null && viewReference != null
					&& result != null) {
				final View rowView = viewReference.get();
				final ImageView imageView = imageViewReference.get();

				if (imageView != null && rowView != null) {
					imageView.setAnimation(Utils.getAnimationFadeIn());
					int imageSize = Utils.convertDipToPx(context, 50);
					Bitmap resized = Bitmap.createScaledBitmap(result,
							imageSize, imageSize, true);

					try {
						addBitmapToMemoryCache(String.valueOf(position),
								resized);
						imageView.setImageBitmap(resized);

						ImageView check = (ImageView) rowView
								.findViewById(R.id.cell_selected);
						if (isMultipleSelection) {
							if (!selectedPositions.isEmpty()) {
								if (selectedPositions.containsKey(String
										.valueOf(position))) {
									check.setVisibility(View.VISIBLE);
								}
							}
						}

						if (selectedPosition == position) {
							final int rowWidth = Utils
									.getDisplayMetrics(context).widthPixels;
							final int rowHeight = Utils
									.getDisplayMetrics(context).heightPixels;
							displayImageView
									.setImageBitmap(decodeSampledBitmapFromFilePath(
											bitmapUri[position], rowWidth,
											rowHeight));
						}
					} catch (IndexOutOfBoundsException e) {
					}
				}
			}
			super.onPostExecute(result);
		}
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromFilePath(String path,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		options.inPurgeable = true;
		options.inDither = false;
		options.inScaled = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		return BitmapFactory.decodeFile(path, options);
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {

		View cellView = LayoutInflater.from(context).inflate(
				R.layout.custom_calendar_day_cell_content, null);

		this.viewGroup = parent;

		final RelativeLayout layoutCell = (RelativeLayout) cellView
				.findViewById(R.id.layoutCell);

		layoutCell.setId(position);

		final int cellDay = this.day;
		final int cellMonth = this.month;
		final int cellYear = this.year;

		Calendar cellCalendar = Calendar.getInstance();
		cellCalendar.set(cellYear, cellMonth, cellDay);

		ImageView imageView = (ImageView) cellView
				.findViewById(R.id.thumbImage);
		final ImageView check = (ImageView) layoutCell
				.findViewById(R.id.cell_selected);
		
		layoutCell.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				ImageView check = (ImageView) v.findViewById(R.id.cell_selected);
				
				if (!CalendarDayActivity.isSlideShow()) {
					if (!isMultipleSelection) {
						if (CalendarDayActivity.getGridView()
								.getFirstVisiblePosition() == position) {
							CalendarDayActivity.getGridView()
									.smoothScrollToPosition(position - 1);
						} else if (CalendarDayActivity.getGridView()
								.getLastVisiblePosition() == position) {
							CalendarDayActivity.getGridView()
									.smoothScrollToPosition(position + 1);
						}
					} else {
						if (selectedPositions.containsKey(String
								.valueOf(layoutCell.getId()))) {
							check.setImageResource(R.drawable.uncheck);
							selectedPositions.remove(String.valueOf(v.getId()));
						} else {
							check.setImageResource(R.drawable.check);
							selectedPositions.put(String.valueOf(v.getId()),
									v.getId());
						}
						
						String multipleSelectedCount = String.format(
								context.getString(R.string.multiple_image_count),
								selectedPositions.size(), getCount());
						CalendarDayActivity.getMultipleSelectedCount()
								.setText(multipleSelectedCount);

						if (selectedPositions.isEmpty()) {
							if (CalendarDayActivity.getMultipleSelectedCount().isShown()) {
								CalendarDayActivity.getMultipleSelectedCount().setVisibility(View.INVISIBLE);
							}
							
							isMultipleSelection = false;
							hideCheck();
						}
					}

					if (displayImageView != null) {
						Animation animation = Utils.getAnimationFadeOut(300);
						animation.setAnimationListener(new AnimationListener() {
							@Override
							public void onAnimationStart(Animation animation) {
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								clickSelectedCellView(v.getId());
								if (!isMultipleSelection) {
									CalendarDayActivity.getGridView()
											.setSelection(v.getId());
								}
							}
						});
						displayImageView.startAnimation(animation);
					}
				}
			}
		});

		layoutCell.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (!CalendarDayActivity.isSlideShow()) {
					if (!isMultipleSelection) {
						if (!CalendarDayActivity.getMultipleSelectedCount().isShown()) {
							CalendarDayActivity.getMultipleSelectedCount().setVisibility(View.VISIBLE);
							showCheck();
						}
					}

					isMultipleSelection = true;

					Vibrator vib = (Vibrator) context
							.getSystemService(Context.VIBRATOR_SERVICE);
					vib.vibrate(50);

					RelativeLayout lastSelectedCellLayout = (RelativeLayout) viewGroup
							.findViewById(selectedPosition);
					if (lastSelectedCellLayout != null) {
						lastSelectedCellLayout.setSelected(false);
					}
					
					if (selectedPosition == v.getId()) {
						check.setImageResource(R.drawable.check);
						selectedPositions.put(String.valueOf(v.getId()),
								v.getId());
					}
					
					String multipleSelectedCount = String.format(
							context.getString(R.string.multiple_image_count),
							selectedPositions.size(), getCount());
					CalendarDayActivity.getMultipleSelectedCount()
							.setText(multipleSelectedCount);
				}
				return false;
			}
		});

		if (selectedPosition == position && !isMultipleSelection) {
			layoutCell.setSelected(true);
		}
		
		//set multiple check view
		if (isMultipleSelection) {
			check.setVisibility(View.VISIBLE);
			if (!selectedPositions.isEmpty()) {
				if (selectedPositions.containsKey(String
						.valueOf(layoutCell.getId()))) {
					check.setImageResource(R.drawable.check);
				} else {
					check.setImageResource(R.drawable.uncheck);
				}
			}
		} else {
			check.setVisibility(View.INVISIBLE);
			if (selectedPosition == position) {
				layoutCell.setSelected(true);
			}
		}

		// get image
		Bitmap bitmap = getBitmapFromMemCache(String.valueOf(position));
		if (bitmap == null) {
			getImageTask = new GetImagesTask(imageView, cellView);
			getImageTask.execute(Long.valueOf(String.valueOf(position)));
		} else {
			try {
				imageView.setImageBitmap(bitmap);
			} catch (IndexOutOfBoundsException e) {
			}
		}

		return cellView;
	}

	private void clickSelectedCellView(int position) {
		if (!isMultipleSelection()) {
			RelativeLayout lastSelectedCellLayout = (RelativeLayout) this.viewGroup
					.findViewById(selectedPosition);
			if (lastSelectedCellLayout != null) {
				lastSelectedCellLayout.setSelected(false);
			}

			RelativeLayout selectedCellLayout = (RelativeLayout) this.viewGroup
					.findViewById(position);

			if (imageCount != null) {
				imageCount.setText(String.format(
						context.getString(R.string.image_count),
						(position + 1), listSize));
			}

			selectedCellLayout.setSelected(true);
			selectedPosition = selectedCellLayout.getId();
		} else {
			selectedPosition = position;
		}

		final int rowWidth = Utils.getDisplayMetrics(context).widthPixels;
		final int rowHeight = Utils.getDisplayMetrics(context).heightPixels;

		if (displayImageView != null) {
			displayImageView.startAnimation(Utils.getAnimationFadeIn(300));
			displayImageView.setImageBitmap(decodeSampledBitmapFromFilePath(
					bitmapUri[position], rowWidth, rowHeight));
		}
	}

}
