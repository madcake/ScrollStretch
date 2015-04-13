package com.dzencake.slidingpane;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
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
		detachAndScrapAttachedViews(recycler);

		int offset = 0;
		for (int i = 0; i < getItemCount(); i++) {
			View v = recycler.getViewForPosition(i);
			measureChildWithMargins(v, 0, 0);
			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
			addView(v);
			RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
			layoutDecorated(v, lp.leftMargin, offset + lp.topMargin,
					getWidth() - lp.rightMargin, offset + viewHeight - lp.bottomMargin);
			offset += viewHeight;
			if (offset > getHeight()) {
				break;
			}
		}

		Log.e(TAG, "onLayoutChanged");
	}

	@Override
	public boolean canScrollVertically() {
		return true;
	}

	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (getChildCount() == 0 || dy == 0) {
			return 0;
		}
		// Вычисляем направление
		int direction = Math.abs(dy) / dy;
		// Берём старые позиции
		int oldFirstPos = getPosition(getChildAt(0));
		int oldLastPos = getPosition(getChildAt(getChildCount() - 1));
		// Сдвигаем views
		offsetChildrenVertical(-dy);
		// Считаем оффсет
		int offset = direction == 1
				? mOrientationHelper.getDecoratedStart(getChildAt(0))
				: mOrientationHelper.getDecoratedEnd(getChildAt(getChildCount() - 1));
		// Заводим кэш
		SparseArray<View> cache = new SparseArray<>();
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			View v = getChildAt(i);
			cache.put(getPosition(v), v);
		}
		// Детачим все вьюшки
		for (int i = 0; i < cache.size(); i++) {
			detachView(cache.valueAt(i));
		}
		// Добавляем вьюшки
		if (direction > 0) {
			for (int i = oldFirstPos; i < getItemCount(); i++) {
				View v = cache.get(i);
				if (v == null) {
					v = recycler.getViewForPosition(i);
					measureChildWithMargins(v, 0, 0);
				}

				int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
				offset += viewHeight;
				if (offset >= 0) {
					// Если view уже была, то достаём из кэша иначе строим новую.
					if (cache.get(i) == null) {
						addView(v);
						RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
						layoutDecorated(v, lp.leftMargin, offset - viewHeight + lp.topMargin,
								getWidth() - lp.rightMargin, offset - lp.bottomMargin);
					} else {
						attachView(v);
						cache.remove(i);
					}
				}
				if (offset > getHeight()) {
					break;
				}
			}
		} else {
			for (int i = oldLastPos; i > -1; i--) {
				View v = cache.get(i);
				if (v == null) {
					v = recycler.getViewForPosition(i);
					measureChildWithMargins(v, 0, 0);
				}

				int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);

				offset -= viewHeight;
				if (offset < getHeight()) {
					// Если view уже была, то достаём из кэша иначе строим новую.
					if (cache.get(i) == null) {
						addView(v, 0);

						RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
						layoutDecorated(v, lp.leftMargin, offset + lp.topMargin,
								getWidth() - lp.rightMargin, offset + viewHeight - lp.bottomMargin);
					} else {
						attachView(v, 0);
						cache.remove(i);
					}
				}
				if (offset < 0) {
					break;
				}
			}
		}

		for (int i = 0; i < cache.size(); i++) {
			recycler.recycleView(cache.valueAt(i));
		}
		cache.clear();
		return dy;
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
