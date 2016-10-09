package com.gs.gscalendar;

import java.util.ArrayList;
import java.util.Calendar;

import org.junit.Test;

import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.gs.gscalendar.utility.Utils;
import com.gs.gscalendar.view.CalendarActivity;
import com.gs.gscalendar.view.MyCalendarAdapter;
import com.jayway.android.robotium.solo.Solo;

public class CalendarActivityTest extends
		ActivityInstrumentationTestCase2<CalendarActivity> {

	private Calendar now;
	private GridView mGridView;
	private MyCalendarAdapter mAdapter;
	private CalendarActivity mActivity;
	private Button mButtonPrevious, mButtonNext;
	private TextView mCalendarTitle;

	private Solo solo;

	public CalendarActivityTest() {
		super(CalendarActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		now = Calendar.getInstance();

		solo = new Solo(getInstrumentation(), getActivity());

		mActivity = getActivity();
		mAdapter = mActivity.getAdapter();
		mGridView = (GridView) mActivity.findViewById(R.id.myCalendar);
		mButtonPrevious = (Button) mActivity
				.findViewById(R.id.myCalendarBtnPrevious);
		mButtonNext = (Button) mActivity.findViewById(R.id.myCalendarBtnNext);
		mCalendarTitle = (TextView) mActivity
				.findViewById(R.id.myCalendarTitle);

		super.setUp();
	}

	@Test
	public void testOnCreate() {
		assertNotNull("activity should be launched successfully", mActivity);
		assertNotNull("adapter should not be null", mAdapter);
		assertTrue("initial month should be current month",
				now.get(Calendar.MONTH) == getActivity().getMonth());
		assertTrue("initial year should be current year",
				now.get(Calendar.YEAR) == getActivity().getYear());
		assertTrue("Grid View should has 7 x 6 dimension",
				(7 * 6) == mGridView.getCount());

		String calendarTitle = Utils.getCalendarDisplayName(now, new int[] {
				Calendar.MONTH, Calendar.YEAR }, Calendar.LONG, " ");
		assertEquals(calendarTitle, mCalendarTitle.getText());
	}

	@Test
	public void testClickButtonCalendarView() throws InterruptedException {
		int month = mAdapter.getMonth();

		ArrayList<Button> buttonList = solo.getCurrentButtons();
		int index = 0;
		for (Button button : buttonList) {
			if (button.getId() == mButtonPrevious.getId()) {
				solo.clickOnButton(index);
				Thread.sleep(100);
				assertEquals(--month, mActivity.getMonth());
				assertTrue("Grid View should has 7 x 6 dimension",
						(7 * 6) == mGridView.getCount());
			}
			if (button.getId() == mButtonNext.getId()) {
				solo.clickOnButton(index);
				Thread.sleep(100);
				assertEquals(++month, mActivity.getMonth());
				assertTrue("Grid View should has 7 x 6 dimension",
						(7 * 6) == mGridView.getCount());
			}
			index++;
		}
	}

	@UiThreadTest
	public void testUpdateCalendarView() {
		mActivity.updateCalendarView(9, 2010);

		assertNotNull("adapter should not be null", mAdapter);
		assertTrue("initial month should be current month", 9 == getActivity()
				.getMonth());
		assertTrue("initial year should be current year", 2010 == getActivity()
				.getYear());
		assertTrue("Grid View should has 7 x 6 dimension",
				(7 * 6) == mGridView.getCount());

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, 9);
		cal.set(Calendar.YEAR, 2010);

		String calendarTitle = Utils.getCalendarDisplayName(cal, new int[] {
				Calendar.MONTH, Calendar.YEAR }, Calendar.LONG, " ");

		assertEquals(calendarTitle, mCalendarTitle.getText());
	}
	
	@UiThreadTest
	public void testUpdateAdapterMonthYear() {
		mAdapter.updateCalendarMonthYear(10, 2009);
		
		assertNotNull("adapter should not be null", mAdapter);
		assertTrue("initial month should be current month", 10 == mAdapter.getMonth());
		assertTrue("initial year should be current year", 2009 == mAdapter.getYear());
		assertTrue("adapter should has 7 x 6 dimension", (7*6) == mAdapter.getCount());
	}
}
