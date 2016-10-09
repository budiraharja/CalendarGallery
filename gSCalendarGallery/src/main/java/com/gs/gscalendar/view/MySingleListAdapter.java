package com.gs.gscalendar.view;

import com.gs.gscalendar.R;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MySingleListAdapter extends ArrayAdapter<String> {
	private Activity context;
	
	public MySingleListAdapter(Activity context, String[] objects) {
		super(context, R.id.single_list_title, objects);
		
		this.context = context;
	}

	@Override
	public int getCount() {
		return super.getCount();
	}

	@Override
	public String getItem(int position) {
		return super.getItem(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = context.getLayoutInflater().inflate(R.layout.custom_single_list_content, null);
		TextView text = (TextView) view.findViewById(R.id.single_list_title);
		
		text.setText(getItem(position));
		
		return view;
	}
	
	

}
