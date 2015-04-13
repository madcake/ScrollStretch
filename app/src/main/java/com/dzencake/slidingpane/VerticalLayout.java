package com.dzencake.slidingpane;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class VerticalLayout extends RecyclerView.LayoutManager {

	private static final String TAG = VerticalLayout.class.getSimpleName();

	private OrientationHelper mOrientationHelper;

	public VerticalLayout() {
		// Враппер над LayoutParams с хелпер методами работы с конкретным View
		mOrientationHelper = OrientationHelper.createVerticalHelper(this);
	}

	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		//We have nothing to show for an empty data set but clear any existing views
		if (getItemCount() == 0) {
			detachAndScrapAttachedViews(recycler);
			return;
		}
		if (getChildCount() == 0 && state.isPreLayout()) {
			//Nothing to do during prelayout when empty
			return;
		}
		detachAndScrapAttachedViews(recycler);

		int offset = 0;
		for (int i = 0; i < getItemCount(); i++) {
			View v = recycler.getViewForPosition(i);
			measureChildWithMargins(v, 0, 0);
			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
			addView(v);
			layoutDecorated(v, 0, offset, getWidth(), offset + viewHeight);
			offset += viewHeight;
			if (offset > getHeight()) {
				break;
			}
		}
	}

	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
		return super.scrollVerticallyBy(dy, recycler, state);
	}

	/**
	 * Used for debugging.
	 * Logs the internal representation of children to default logger.
	 * copy of {@link LinearLayoutManager#logChildren()}
	 */
	private void logChildren() {
		Log.d(TAG, "internal representation of views on the screen");
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			Log.d(TAG, "item " + getPosition(child) + ", coord:"
					+ mOrientationHelper.getDecoratedStart(child));
		}
		Log.d(TAG, "==============");
	}
}
