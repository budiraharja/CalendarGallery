package com.gs.gscalendar.view;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.gs.gscalendar.R;
import com.gs.gscalendar.utility.Utils;

public class CalendarActivity extends Activity implements OnTouchListener,
		OnClickListener {
	private static TextView myCalendarTitle;
	private static GridView myCalendar, myCalendarDayName;
	private static MyCalendarAdapter adapter;
	private static HorizontalScrollView horizontalScrollView;
	private static Button myCalendarBtnPrevious, myCalendarBtnNext;

	private float mLastX;
	private int mDiffX;
	private Calendar now;
	private static boolean isImageDeleted;

	private static int month;
	private static int year;

	static final int DATE_DIALOG_ID = 999;
	
	public void setMonth(int m) {
		this.month = m;
	}
	
	public int getMonth() {
		return this.month;
	}
	
	public void setYear(int y) {
		this.year = y;
	}
	
	public int getYear() {
		return this.year;
	}
	
	public static void setImageDeleted(boolean deleted) {
		isImageDeleted = deleted;
	}
	
	public boolean isImageDeleted() {
		return isImageDeleted;
	}
	
	public MyCalendarAdapter getAdapter() {
		return adapter;
	}
	
	public static GridView getGridView() {
		return myCalendar;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calendar);

		now = Calendar.getInstance();
		setMonth(now.get(Calendar.MONTH));
		setYear(now.get(Calendar.YEAR));

		myCalendarBtnPrevious = (Button) this
				.findViewById(R.id.myCalendarBtnPrevious);
		myCalendarBtnPrevious.setOnClickListener(this);

		myCalendarBtnNext = (Button) this.findViewById(R.id.myCalendarBtnNext);
		myCalendarBtnNext.setOnClickListener(this);

		myCalendarTitle = (TextView) this.findViewById(R.id.myCalendarTitle);
		myCalendarTitle.setText(Utils.getCalendarDisplayName(now, new int[] {
				Calendar.MONTH, Calendar.YEAR }, Calendar.LONG, " "));
		myCalendarTitle.setOnClickListener(this);

		myCalendarDayName = (GridView) findViewById(R.id.myCalendarDayName);
		myCalendarDayName.setAdapter(new MyCalendarDayNameAdapter(this));

		adapter = new MyCalendarAdapter(this, month, year);
		myCalendar = (GridView) findViewById(R.id.myCalendar);
		myCalendar.setAdapter(adapter);

		horizontalScrollView = (HorizontalScrollView) this
				.findViewById(R.id.horizontalScrollView);
		horizontalScrollView.setOnTouchListener(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_goto:
				showDialog(DATE_DIALOG_ID);
				break;
			case R.id.menu_refresh:
				updateCalendarView(getMonth(), getYear());
				break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		if (isImageDeleted()) {
			updateCalendarView(getMonth(), getYear());
			setImageDeleted(false);
		}
		super.onResume();
	}

	public void scrollCalendarView(final int index) {
		final int monthDisplay = getAdapter().getMonth();
		final int yearDisplay = getAdapter().getYear();

		Animation animation = Utils
			.getAnimationSlideHorizontal(this, index);
		animation.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				int mMonth = monthDisplay;
				int mYear = yearDisplay;
				
				switch (index) {
					case 1:
						if (mMonth == 11) {
							mMonth = 0;
							mYear += 1;
						} else {
							mMonth += 1;
						}
			
						break;
					case -1:
						if (mMonth == 0) {
							mMonth = 11;
							mYear -= 1;
						} else {
							mMonth -= 1;
						}
			
						break;
				}
	
				updateCalendarView(mMonth, mYear);
			}
		});
		horizontalScrollView.startAnimation(animation);
	}

	public void updateCalendarView(final int month, final int year) {
		Animation animation = Utils.getAnimationFadeIn(500);
		myCalendar.startAnimation(animation);
		
		getAdapter().updateCalendarMonthYear(month, year);
		getAdapter().notifyDataSetChanged();

		setMonth(month);
		setYear(year);
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, 1);
		
		myCalendarTitle.setText(Utils
				.getCalendarDisplayName(calendar, new int[] { Calendar.MONTH,
						Calendar.YEAR }, Calendar.LONG, " "));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DATE_DIALOG_ID:
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(R.layout.custom_numberpicker, null);
				
				String[] months = new String[12];
				Calendar calMonth = Calendar.getInstance();
				
				for (int i=0; i<12; i++) {
					calMonth.set(Calendar.MONTH, i);
					SimpleDateFormat df = new SimpleDateFormat("MMMM", Locale.getDefault());
					months[i] = df.format(calMonth.getTime());
				}
				
				final NumberPicker day = (NumberPicker) view.findViewById(R.id.numberPickerDay);
				((LinearLayout)day.getParent()).setVisibility(View.GONE);
				
				final NumberPicker month = (NumberPicker) view.findViewById(R.id.numberPickerMonth);
		        month.setMaxValue(11);
		        month.setMinValue(0);
		        month.setValue(adapter.getMonth());
		        month.setFocusable(true);
		        month.setFocusableInTouchMode(true);
		        month.setDisplayedValues(months);
		        
		        final NumberPicker year = (NumberPicker) view.findViewById(R.id.numberPickerYear);
		        year.setMaxValue(Calendar.getInstance().get(Calendar.YEAR));
		        year.setMinValue(1970);
		        year.setValue(adapter.getYear());
		        year.setFocusable(true);
		        year.setFocusableInTouchMode(true);
				
				AlertDialog.Builder builder = new Builder(CalendarActivity.this);
				builder.setView(view);
				builder.setCancelable(false);
				builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						updateCalendarView(month.getValue(), year.getValue());
						dialog.dismiss();
					}
				});
				builder.create().show();
		}
		return null;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.horizontalScrollView:
			switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				final float curX = event.getX();
				mDiffX = (int) (mLastX - curX);
				mLastX = curX;
				break;
			case MotionEvent.ACTION_UP:
				if (mDiffX < -10) { // horizontal scrolling left-right
					scrollCalendarView(-1);
					break;
				} else if (mDiffX > 10) { // horizontal scrolling right-left
					scrollCalendarView(1);
					break;
				}
				break;
			}
			break;

		default:
			break;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.myCalendarBtnPrevious:
			this.scrollCalendarView(-1);
			break;
		case R.id.myCalendarBtnNext:
			this.scrollCalendarView(1);
			break;
		case R.id.myCalendarTitle:
			showDialog(DATE_DIALOG_ID);
			break;
		default:
			break;
		}
	}

}
