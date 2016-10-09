package com.gs.gscalendar.view;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gs.gscalendar.R;
import com.gs.gscalendar.utility.Constant;
import com.gs.gscalendar.utility.Utils;

public class MyCalendarDayNameAdapter extends BaseAdapter {
	private static final int START_DAY_OF_WEEK = Constant.START_DAY_OF_WEEK;

	private Activity context;
	private ArrayList<String> list;

	public MyCalendarDayNameAdapter(Activity context) {
		this.context = context;
		init();
	}

	private void init() {
		list = new ArrayList<String>();

		Calendar dummyWeekDay = Calendar.getInstance(Locale.getDefault());
		dummyWeekDay.set(Calendar.DAY_OF_WEEK, (START_DAY_OF_WEEK + 1));

		for (int i = 0; i < 7; i++) {
			list.add(Utils.getCalendarDisplayName(dummyWeekDay,
					new int[] { Calendar.DAY_OF_WEEK }, Calendar.SHORT, ""));
			dummyWeekDay.add(Calendar.DAY_OF_MONTH, 1);
		}
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

	@Override
	public View getView(final int position, View convertView,
			final ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = null;

		rowView = inflater.inflate(R.layout.custom_calendar_cell_header, null,
				false);

		TextView cell = (TextView) rowView
				.findViewById(R.id.calendar_day_gridcell);
		cell.setText(list.get(position));

		return rowView;
	}

}
