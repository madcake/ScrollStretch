package com.dzencake.slidingpane;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class VerticalLayout2 extends RecyclerView.LayoutManager {
	private OrientationHelper mOrientationHelper;

	public VerticalLayout2() {
		// Враппер над LayoutParams с хелпер методами работы с конкретным View
		mOrientationHelper = OrientationHelper.createVerticalHelper(this);
	}

	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public boolean canScrollVertically() {
		return true;
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		int position = 0;
		int offset = 0;
		if (getChildCount() > 0){
			position = getPosition(getChildAt(0));
			offset = getChildAt(0).getTop();
		}

		detachAndScrapAttachedViews(recycler);

		if (getItemCount() == 0){
			return;
		}

		fillEndGap(recycler, position, offset);

		if (getChildCount() > 0) {
			int viewsEnd = getChildAt(getChildCount() - 1).getBottom();
			if (viewsEnd < getHeight()) {
				offsetChildrenVertical(getHeight() - viewsEnd);
			}
		}

		position = 0;
		offset = 0;
		if (getChildCount() > 0){
			position = getPosition(getChildAt(0));
			offset = getChildAt(0).getTop();
		}

		fillStartGap(recycler, position, offset);

		int viewsStart = getChildAt(0).getTop();
		if (viewsStart > 0){
			offsetChildrenVertical(-viewsStart);
		}
	}

	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {



		return super.scrollVerticallyBy(dy, recycler, state);
	}

	/**
	 * Метод заполняет пустое место вьюхами, начиная с position со сдвигом topOffset. При добавлении View, offset и position
	 * будут увеличиваться, пока offset не станет больше getHeight()
	 * @param position позиция вьюшки в адаптере, с которой нужно начать заполнение
	 * @param topOffset изначальный сдвиг (topOffset будет равен getTop() у первой View, которая добавится через этот метод)
	 */
	public void fillEndGap(RecyclerView.Recycler recycler, int position, int topOffset) {
		while (position < getItemCount() && topOffset < getHeight()) {
			View v = recycler.getViewForPosition(position);
			measureChildWithMargins(v, 0, 0);

			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);

			addView(v);
			RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
			layoutDecorated(v, lp.leftMargin, topOffset + lp.topMargin,
					getWidth() - lp.rightMargin, topOffset + viewHeight - lp.bottomMargin);

			topOffset += viewHeight;
			position++;
		}
	}

	/**
	 * Метод заполняет пустое место вьюхами, начиная с position со сдвигом (bottomOffset - viewHeight). При добавлении View, offset и position
	 * будут уменьшаться, пока offset не станет меньше 0.
	 * @param position позиция вьюшки в адаптере, с которой нужно начать заполнение
	 * @param bottomOffset изначальный сдвиг (bottomOffset будет равен getBottom() у первой View, которая добавится через этот метод)
	 */
	public void fillStartGap(RecyclerView.Recycler recycler, int position, int bottomOffset) {
		if (position >= getItemCount()){
			position = getItemCount() - 1;
		}
		while (position > -1 && bottomOffset > 0) {
			View v = recycler.getViewForPosition(position);
			measureChildWithMargins(v, 0, 0);

			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);

			addView(v);
			RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
			layoutDecorated(v, lp.leftMargin, bottomOffset - viewHeight + lp.topMargin,
					getWidth() - lp.rightMargin, bottomOffset - lp.bottomMargin);

			bottomOffset -= viewHeight;
			position--;
		}
	}
}
