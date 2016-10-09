package com.gs.gscalendar.utility;

import java.util.Calendar;
import java.util.Locale;

import com.gs.gscalendar.R;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;

public class Utils {
	public static String getCalendarDisplayName(Calendar calendar,
			int[] fields, int style, String separator) {
		String result = "";

		for (int field : fields) {
			if (calendar.getDisplayName(field, style, Locale.getDefault()) == null) {
				result += calendar.get(field);
			} else {
				result += calendar.getDisplayName(field, style,
						Locale.getDefault());
			}
			result += separator;
		}

		return result.substring(0, result.lastIndexOf(separator));
	}
	
	public static Animation getAnimationFadeIn(int durationMilliseconds) {
		Animation animation = new AlphaAnimation(0, 1);
		animation.setInterpolator(new DecelerateInterpolator());
		animation.setStartOffset(durationMilliseconds);
		animation.setDuration(durationMilliseconds);
		
		return animation;
	}
	
	public static Animation getAnimationFadeIn() {
		return getAnimationFadeIn(1000);
	}
	
	public static Animation getAnimationFadeInVisible(final View view, int durationMilliseconds) {
		Animation animation = getAnimationFadeIn(durationMilliseconds);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});
		
		return animation;
	}
	
	public static Animation getAnimationFadeOut(int durationMilliseconds) {
		Animation animation = new AlphaAnimation(1, 0);
		animation.setInterpolator(new AccelerateInterpolator()); 
		animation.setStartOffset(durationMilliseconds);
		animation.setDuration(durationMilliseconds);
		
		return animation;
	}
	
	public static Animation getAnimationFadeOut() {
		return getAnimationFadeOut(1000);
	}
	
	public static Animation getAnimationFadeOutGone(final View view, int durationMilliseconds) {
		Animation animation = getAnimationFadeOut(durationMilliseconds);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});
		
		return animation;
	}
	
	public static Animation getAnimationSlideOutLeft(Activity activity) {
		Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_left);
		return animation;
	}
	
	public static Animation getAnimationSlideOutRight(Activity activity) {
		Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_right);
		return animation;
	}
	
	public static Animation getAnimationSlideHorizontal(Activity activity, int direction) {
		Animation animation = direction > 0 ? getAnimationSlideOutLeft(activity) : getAnimationSlideOutRight(activity);
		return animation;
	}
	
	public static DisplayMetrics getDisplayMetrics(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		return metrics;
	}
	
	public static int convertDipToPx(Activity activity, int dip) {
		DisplayMetrics metrics = getDisplayMetrics(activity);
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, metrics);
		
		return px;
	}
	
	public static int getBitmapByteCount(Bitmap bitmap) {
		try {
	        return bitmap.getByteCount();
	    } catch (NoSuchMethodError e) {
	        return bitmap.getRowBytes() * bitmap.getHeight();
	    }
	}
}
