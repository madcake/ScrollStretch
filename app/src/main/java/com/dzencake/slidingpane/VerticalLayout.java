package com.dzencake.slidingpane;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
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
		int oldFirstPos = getChildCount() == 0 ? 0 : getPosition(getChildAt(0));
		// Считаем оффсет
		int offset = getChildCount() == 0 ? 0 : mOrientationHelper.getDecoratedStart(getChildAt(0));
		detachAndScrapAttachedViews(recycler);
		// Заполняем
		int consumed = fill(recycler, 1, offset, oldFirstPos, 0);
		offsetChildrenVertical(-consumed);
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
		// Берём старую первую позицию
		int oldPosition = direction == 1
				? getPosition(getChildAt(0))
				: getPosition(getChildAt(getChildCount() - 1));
		// Сдвигаем views
//		offsetChildrenVertical(-dy);
		// Считаем оффсет
		int offset = direction == 1
				? mOrientationHelper.getDecoratedStart(getChildAt(0))
				: mOrientationHelper.getDecoratedEnd(getChildAt(getChildCount() - 1));
		// Заполняем
		int consumed = fill(recycler, direction, offset, oldPosition, dy);
		offsetChildrenVertical(-consumed);
		return consumed;
	}

	private int fill(RecyclerView.Recycler recycler, int direction, int offset, int oldPos, int dy) {
		int consumed = 0;
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
		// запоминаем изначальный оффсет
		int initialOffset = offset;
		// Добавляем вьюшки
		if (direction > 0) {
			for (int i = oldPos; i < getItemCount(); i++) {
				View v = cache.get(i);
				if (v == null) {
					v = recycler.getViewForPosition(i);
					measureChildWithMargins(v, 0, 0);
				}

				int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
				offset += viewHeight;
				// Что бы не потерять view
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
				if (offset > getHeight() + dy) {
					break;
				}
			}
			if (offset < getHeight()) {
				return 0;
			} else {
				if (offset < getHeight() + dy) {
					consumed = offset - getHeight();
				} else {
					consumed = dy;
				}
			}
		} else {
			for (int i = oldPos; i > -1; i--) {
				View v = cache.get(i);
				if (v == null) {
					v = recycler.getViewForPosition(i);
					measureChildWithMargins(v, 0, 0);
				}

				int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);

				offset -= viewHeight;
				if (offset < getHeight() - dy) {
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

			int lastTopPos = mOrientationHelper.getDecoratedStart(getChildAt(0));
			if (lastTopPos > 0) {
				consumed = -lastTopPos;
//				fixDy = fixDy > Math.abs(dy) ? dy : fixDy;
			}
		}

		for (int i = 0; i < cache.size(); i++) {
			recycler.recycleView(cache.valueAt(i));
		}
		return consumed;
	}
}
