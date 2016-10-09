package com.gs.gscalendar.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import com.gs.gscalendar.R;
import com.gs.gscalendar.utility.Constant;
import com.gs.gscalendar.utility.Utils;

public class MyCalendarAdapter extends BaseAdapter {
	private static final int START_DAY_OF_WEEK = Constant.START_DAY_OF_WEEK;
	private static final int DEFAULT_DATE_COLOR = R.color.gray;
	private static final int DEFAULT_DATE_OFF_MONTH_COLOR = R.color.blue;
	private static final int CURRENT_DAY_BACKGROUND = R.color.lite_blue;
	private static final int CURRENT_DAY_TEXT_COLOR = R.color.lite_blue;

	private Activity context;
	private int month;
	private int prevMonth;
	private int nextMonth;
	private int year = 1970;
	private int prevYear;
	private int nextYear;
	private int indexStartCurrMonth;
	private Calendar now;
	private Calendar dummyCalendar;
	private Date selectedDate;
	private AlertDialog actionMenuDialog;
	private ProgressDialog progressDialog;
	private ViewGroup viewGroup;
	private int loadingProgress;
	private int[] imageCounts;

	private ArrayList<String> list;
	private LruCache<String, Bitmap> mMemoryCache = null;

	public MyCalendarAdapter(Activity context, int month, int year) {
		this.context = context;
		this.month = month;
		this.year = year;
		this.loadingProgress = 0;

		this.progressDialog = new ProgressDialog(context);
		this.progressDialog.setCancelable(false);
		this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

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
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	private void removeBitmapToMemoryCache(String key) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.remove(key);
		}
	}

	private Bitmap getBitmapFromMemCache(String key) {
		return (Bitmap) mMemoryCache.get(key);
	}

	private void init() {
		initMemoryCache();
		
		this.imageCounts = new int[(7 * 6)];

		this.prevMonth = month == 0 ? 11 : (month - 1);
		this.nextMonth = month == 11 ? 0 : (month + 1);
		this.now = Calendar.getInstance();
		this.dummyCalendar = Calendar.getInstance();
		this.dummyCalendar.set(year, month, 1);

		int firstDayOfWeekInMonth = dummyCalendar.get(Calendar.DAY_OF_WEEK);

		list = new ArrayList<String>();

		// set heading spaces
		if (firstDayOfWeekInMonth > 0) {
			this.dummyCalendar.add(Calendar.DAY_OF_MONTH,
					-(firstDayOfWeekInMonth - (START_DAY_OF_WEEK + 1)));
		}

		while (dummyCalendar.get(Calendar.MONTH) != month) {
			list.add(String.valueOf(dummyCalendar.get(Calendar.DAY_OF_MONTH)));
			dummyCalendar.add(Calendar.DAY_OF_MONTH, 1);
		}

		// set start index of current month in the list
		this.indexStartCurrMonth = list.size();

		while (dummyCalendar.get(Calendar.MONTH) == month) {
			list.add(String.valueOf(dummyCalendar.get(Calendar.DAY_OF_MONTH)));
			dummyCalendar.add(Calendar.DAY_OF_MONTH, 1);
		}

		// set trailing spaces
		while (list.size() < (7 * 6)) {
			list.add(String.valueOf(dummyCalendar.get(Calendar.DAY_OF_MONTH)));
			dummyCalendar.add(Calendar.DAY_OF_MONTH, 1);
		}

		// revert dummy calendar
		this.dummyCalendar.set(year, month, 1);
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int index) {
		return list.get(index);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public void updateCalendarMonthYear(int month, int year) {
		this.month = month;
		this.year = year;
		this.loadingProgress = 0;
		init();
	}

	public void updateCellView(int position) {
		View rowView = this.viewGroup.findViewById(position);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.thumbImage);
		TextView cellTextCount = (TextView) rowView
				.findViewById(R.id.calendar_day_count);

		Calendar cellCalendar = Calendar.getInstance();
		cellCalendar.set(getYear(), getMonth(), getDayOfMonth(position));

		GetImagesTask task = new GetImagesTask(imageView, rowView,
				cellTextCount);
		task.execute(cellCalendar.getTimeInMillis(),
				Long.valueOf(String.valueOf(position)));
	}

	public int getDayOfMonth(int index) {
		return Integer.parseInt(list.get(index));
	}

	public int getMonth() {
		return this.month;
	}

	public int getYear() {
		return this.year;
	}

	public Date getSelectedDate() {
		return this.selectedDate;
	}

	private class GetImagesTask extends AsyncTask<Long, Integer, Bitmap> {
		int position;
		long cellDate;
		private final WeakReference<ImageView> imageViewReference;
		private final WeakReference<View> viewReference;
		private final WeakReference<TextView> textViewReference;

		public GetImagesTask(ImageView imageView, View rowView,
				TextView textView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
			viewReference = new WeakReference<View>(rowView);
			textViewReference = new WeakReference<TextView>(textView);
		}

		@Override
		protected void onPreExecute() {
			if (progressDialog != null && !progressDialog.isShowing()
					&& loadingProgress == 0) {
				progressDialog.setMessage(context
						.getString(R.string.loading_images));
				progressDialog.show();
			}
			super.onPreExecute();
		}

		@Override
		protected void onCancelled() {
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			super.onCancelled();
		}

		@Override
		protected void onCancelled(Bitmap result) {
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			super.onCancelled(result);
		}

		@Override
		protected Bitmap doInBackground(Long... param) {
			position = Integer.parseInt(String.valueOf(param[1]));

			Calendar calParam = Calendar.getInstance();
			calParam.setTimeInMillis(param[0]);
			calParam.set(Calendar.HOUR_OF_DAY, 0);

			Calendar cal = (Calendar) calParam.clone();
			cal.add(Calendar.DAY_OF_MONTH, 1);

			cellDate = calParam.getTimeInMillis();

			final String[] columns = { MediaStore.Images.Media.DATA,
					MediaStore.Images.Media._ID,
					MediaStore.Images.Media.DATE_TAKEN,
					MediaStore.Images.Media.TITLE,
					MediaStore.Images.Media.DISPLAY_NAME };

			final String orderBy = MediaStore.Images.Media._ID
					+ " desc limit 1";

			Cursor imagecursor = context.getContentResolver().query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					columns,
					MediaStore.Images.Media.DATE_TAKEN + " >= ? AND "
							+ MediaStore.Images.Media.DATE_TAKEN + " < ? ",
					new String[] { String.valueOf(cellDate),
							String.valueOf(cal.getTimeInMillis()) }, orderBy);

			Bitmap thumbnail = null;

			if (imagecursor != null) {
				if (imagecursor.getCount() > 0) {
					imagecursor.moveToLast();

					int id = imagecursor.getInt(imagecursor
							.getColumnIndex(MediaStore.Images.Media._ID));

					final BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = calculateInSampleSize(options, 70,
							70);
					options.inJustDecodeBounds = false;
					options.inPurgeable = true;
					options.inDither = false;
					options.inScaled = false;
					options.inPreferredConfig = Bitmap.Config.ALPHA_8;

					thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
							context.getContentResolver(), id,
							MediaStore.Images.Thumbnails.MICRO_KIND, options);
				}

				imagecursor.close();
			}

			return thumbnail;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (imageViewReference != null && viewReference != null) {
				final ImageView imageView = imageViewReference.get();
				final View rowView = viewReference.get();

				if (result != null) {
					if (imageView != null && rowView != null) {
						final Calendar cellCalendar = Calendar.getInstance();
						cellCalendar.setTimeInMillis(cellDate);

						imageView.setAnimation(Utils.getAnimationFadeIn());
						imageView.setScaleType(ScaleType.CENTER_INSIDE);
						imageView.setImageBitmap(result);

						addBitmapToMemoryCache(String.valueOf(position), result);

						if (textViewReference != null) {
							GetCountImagesTask countTask = new GetCountImagesTask(
									textViewReference.get());
							countTask.execute(cellDate, (long)position);
						}

						rowView.setBackgroundResource(R.drawable.imageview_selector);
						rowView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								openImages(
										cellCalendar.get(Calendar.DAY_OF_MONTH),
										cellCalendar.get(Calendar.MONTH),
										cellCalendar.get(Calendar.YEAR));
							}
						});

						rowView.setOnLongClickListener(new OnLongClickListener() {
							@Override
							public boolean onLongClick(final View v) {
								Vibrator vib = (Vibrator) context
										.getSystemService(Context.VIBRATOR_SERVICE);
								vib.vibrate(50);

								final int day = getDayOfMonth(v.getId());
								final int month = getMonth();
								final int year = getYear();

								Calendar cal = Calendar.getInstance();
								cal.set(year, month, day);

								String selectedDate = Utils
										.getCalendarDisplayName(cal,
												new int[] {
														Calendar.DAY_OF_MONTH,
														Calendar.MONTH,
														Calendar.YEAR },
												Calendar.LONG, " ");

								String imageCount = "";
								if (textViewReference != null) {
									imageCount = "("
											+ textViewReference.get().getText()
													.toString() + ")";
								}

								String[] longClickMenu = {
										String.format(
												context.getString(R.string.open_image_on_day),
												""),
										String.format(
												context.getString(R.string.delete_image_on_day),
												imageCount) };

								MySingleListAdapter adapter = new MySingleListAdapter(
										context, longClickMenu);
								View singleListView = context
										.getLayoutInflater().inflate(
												R.layout.custom_single_list,
												null);
								ListView list = (ListView) singleListView
										.findViewById(R.id.single_list);
								list.setAdapter(adapter);

								list.setOnItemClickListener(new OnItemClickListener() {
									@Override
									public void onItemClick(
											AdapterView<?> adapterView,
											View view, int position, long arg3) {
										switch (position) {
										case 0:
											openImages(day, month, year);
											break;
										case 1:
											deleteImages(day, month, year,
													v.getId());
											break;
										}

										if (actionMenuDialog != null) {
											actionMenuDialog.dismiss();
										}
									}
								});

								AlertDialog.Builder builder = new Builder(
										context);
								builder.setTitle(selectedDate);
								builder.setView(singleListView);

								actionMenuDialog = builder.create();
								actionMenuDialog.show();

								return false;
							}
						});
					}
				} else {
					if (imageView != null) {
						imageView.setImageBitmap(null);
					}

					if (rowView != null) {
						rowView.setBackgroundResource(R.drawable.gridview_selector);
					}

					removeBitmapToMemoryCache(String.valueOf(position));

					if (textViewReference != null) {
						TextView textCount = (TextView) textViewReference.get();
						if (textCount != null) {
							textCount.setVisibility(View.INVISIBLE);
						}
					}
				}
			}

			loadingProgress++;

			int maxIndex = CalendarActivity.getGridView()
					.getLastVisiblePosition()
					- CalendarActivity.getGridView().getFirstVisiblePosition();

			if (progressDialog != null && progressDialog.isShowing()
					&& loadingProgress >= maxIndex) {
				progressDialog.dismiss();
			}
			super.onPostExecute(result);
		}
	}

	private void openImages(int day, int month, int year) {
		Intent intent = new Intent(context, CalendarDayActivity.class);
		intent.putExtra("DAY", day);
		intent.putExtra("MONTH", month);
		intent.putExtra("YEAR", year);
		context.startActivity(intent);
	}

	private void deleteImages(int day, int month, int year, final int position) {
		final Calendar cal = Calendar.getInstance();
		cal.set(year, month, day);

		String selectedDate = Utils.getCalendarDisplayName(cal, new int[] {
				Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR },
				Calendar.LONG, " ");

		AlertDialog.Builder builder = new Builder(context);
		builder.setCancelable(false);
		builder.setMessage(String.format(
				context.getString(R.string.delete_image_on_day_warn),
				selectedDate));
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();

						DeleteImagesTask deleteTask = new DeleteImagesTask();
						deleteTask.execute(cal.getTimeInMillis(),
								(long) position);
					}
				});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}

	private class GetCountImagesTask extends AsyncTask<Long, Integer, Integer> {
		private WeakReference<TextView> textViewReference;
		private int position;

		public GetCountImagesTask(TextView textView) {
			textViewReference = new WeakReference<TextView>(textView);
		}

		@Override
		protected Integer doInBackground(Long... param) {
			position = param[1].intValue();
			
			Calendar calParam = Calendar.getInstance();
			calParam.setTimeInMillis(param[0]);
			calParam.set(Calendar.HOUR_OF_DAY, 0);

			Calendar cal = (Calendar) calParam.clone();
			cal.add(Calendar.DAY_OF_MONTH, 1);

			long cellDate = calParam.getTimeInMillis();

			final String[] columns = { MediaStore.Images.Media._ID };

			Cursor imagecursor = context.getContentResolver().query(
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
		protected void onPostExecute(Integer result) {
			if (result > 0 && textViewReference != null) {
				TextView textCount = textViewReference.get();
				if (textCount != null) {
					textCount.setText(String.valueOf(result));
				}
				imageCounts[position] = result;
			} else if (textViewReference != null) {
				TextView textCount = textViewReference.get();
				if (textCount != null) {
					textCount.setVisibility(View.GONE);
				}
			}
			super.onPostExecute(result);
		}
	}

	private class DeleteImagesTask extends AsyncTask<Long, Integer, Boolean> {
		int position;

		@Override
		protected Boolean doInBackground(Long... param) {
			Calendar calParam = Calendar.getInstance();
			calParam.setTimeInMillis(param[0]);
			calParam.set(Calendar.HOUR_OF_DAY, 0);

			Calendar cal = (Calendar) calParam.clone();
			cal.add(Calendar.DAY_OF_MONTH, 1);

			long cellDate = calParam.getTimeInMillis();

			position = param[1].intValue();

			final String[] columns = { MediaStore.Images.Media._ID };

			Cursor imagecursor = context.getContentResolver().query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					columns,
					MediaStore.Images.Media.DATE_TAKEN + " >= ? AND "
							+ MediaStore.Images.Media.DATE_TAKEN + " < ? ",
					new String[] { String.valueOf(cellDate),
							String.valueOf(cal.getTimeInMillis()) }, null);

			int imagesCount = 0;

			if (imagecursor != null) {
				imagesCount = imagecursor.getCount();
				imagecursor.moveToFirst();
				progressDialog.setMax(imagesCount);

				for (int i = 0; i < imagesCount; i++) {
					int id = imagecursor.getInt(imagecursor
							.getColumnIndex(MediaStore.Images.Media._ID));

					int delete = context.getContentResolver().delete(
							MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
							MediaStore.Images.Media._ID + " = ?",
							new String[] { String.valueOf(id) });

					publishProgress(i);
					imagecursor.moveToNext();
				}

				imagecursor.close();
			}

			return true;
		}

		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(context.getString(R.string.delete));
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
			// updateCalendarMonthYear(getMonth(), getYear());
			// notifyDataSetChanged();
			updateCellView(position);
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

	@SuppressWarnings("deprecation")
	@Override
	public View getView(final int position, View convertView,
			final ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		this.viewGroup = parent;

		View rowView = null;

		rowView = inflater.inflate(R.layout.custom_calendar_cell_content, null,
				false);

		rowView.setId(position);

		ImageView imageView = (ImageView) rowView.findViewById(R.id.thumbImage);
		TextView cellText = (TextView) rowView
				.findViewById(R.id.calendar_day_gridcell);
		final TextView cellTextCount = (TextView) rowView
				.findViewById(R.id.calendar_day_count);

		DisplayMetrics metrics = Utils.getDisplayMetrics(context);
		int rowWidth = metrics.widthPixels;
		int rowHeight = metrics.heightPixels;

		int longestDimension = Math.max(rowWidth, rowHeight);
		int paddingPx = Utils.convertDipToPx(context, 105);

		rowView.setMinimumHeight((longestDimension - paddingPx) / 6);

		cellText.setText(list.get(position));
		cellText.setId(position);

		int dummyDayOfMonth = this.getDayOfMonth(position);
		int dummyMonth = this.getMonth();
		int dummyYear = this.getYear();

		// set color date of prev & next month
		int days = this.dummyCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		if (position < indexStartCurrMonth) {
			cellText.setTextColor(context.getResources().getColor(
					DEFAULT_DATE_OFF_MONTH_COLOR));
			dummyMonth = this.prevMonth;
			if (this.month == 0) {
				dummyYear = this.prevYear;
			}
			imageView.setAlpha(85);
		} else if (position >= (indexStartCurrMonth + days)) {
			cellText.setTextColor(context.getResources().getColor(
					DEFAULT_DATE_OFF_MONTH_COLOR));
			dummyMonth = this.nextMonth;
			if (this.month == 11) {
				dummyYear = this.nextYear;
			}
			imageView.setAlpha(85);
		} else {
			cellText.setTextColor(context.getResources().getColor(
					DEFAULT_DATE_COLOR));
			if (dummyDayOfMonth == now.get(Calendar.DAY_OF_MONTH)
					&& now.get(Calendar.MONTH) == dummyCalendar
							.get(Calendar.MONTH)
					&& now.get(Calendar.YEAR) == dummyCalendar
							.get(Calendar.YEAR)) {
				cellText.setTextColor(context.getResources().getColor(
						CURRENT_DAY_TEXT_COLOR));
				cellText.setTextAppearance(context, R.style.boldText);
			}
		}

		final int cellDay = dummyDayOfMonth;
		final int cellMonth = dummyMonth;
		final int cellYear = dummyYear;

		Calendar cellCalendar = Calendar.getInstance();
		cellCalendar.set(cellYear, cellMonth, cellDay);

		// get image
		Bitmap bitmap = getBitmapFromMemCache(String.valueOf(position));
		if (bitmap == null) {
			GetImagesTask task = new GetImagesTask(imageView, rowView,
					cellTextCount);
			task.execute(cellCalendar.getTimeInMillis(),
					Long.valueOf(String.valueOf(position)));
		} else {
			imageView.setScaleType(ScaleType.CENTER_INSIDE);
			imageView.setImageBitmap(bitmap);
			
			if (imageCounts[position] == 0) {
				GetCountImagesTask taskCount = new GetCountImagesTask(cellTextCount);
				taskCount.execute(cellCalendar.getTimeInMillis(), (long)position);
			}
			cellTextCount.setText(String.valueOf(imageCounts[position]));
			
			rowView.setBackgroundResource(R.drawable.imageview_selector);
			rowView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					openImages(cellDay, cellMonth, cellYear);
				}
			});
			rowView.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(final View v) {
					Vibrator vib = (Vibrator) context
							.getSystemService(Context.VIBRATOR_SERVICE);
					vib.vibrate(50);

					final int day = getDayOfMonth(v.getId());
					final int month = getMonth();
					final int year = getYear();

					Calendar cal = Calendar.getInstance();
					cal.set(year, month, day);

					String selectedDate = Utils.getCalendarDisplayName(cal,
							new int[] { Calendar.DAY_OF_MONTH, Calendar.MONTH,
									Calendar.YEAR }, Calendar.LONG, " ");

					String imageCount = "";
					if (cellTextCount != null) {
						imageCount = "(" + cellTextCount.getText().toString()
								+ ")";
					}

					String[] longClickMenu = {
							String.format(context
									.getString(R.string.open_image_on_day), ""),
							String.format(context
									.getString(R.string.delete_image_on_day),
									imageCount) };

					MySingleListAdapter adapter = new MySingleListAdapter(
							context, longClickMenu);
					View singleListView = context.getLayoutInflater().inflate(
							R.layout.custom_single_list, null);
					ListView list = (ListView) singleListView
							.findViewById(R.id.single_list);
					list.setAdapter(adapter);

					list.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> adapterView,
								View view, int position, long arg3) {
							switch (position) {
							case 0:
								openImages(day, month, year);
								break;
							case 1:
								deleteImages(day, month, year, v.getId());
								break;
							}

							if (actionMenuDialog != null) {
								actionMenuDialog.dismiss();
							}
						}
					});

					AlertDialog.Builder builder = new Builder(context);
					builder.setTitle(selectedDate);
					builder.setView(singleListView);

					actionMenuDialog = builder.create();
					actionMenuDialog.show();

					return false;
				}
			});
		}

		return rowView;
	}

}
