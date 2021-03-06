package com.dzencake.slidingpane;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class PredictiveAnimationsLayout extends RecyclerView.LayoutManager {
	/**
	 * Движение пальца вверх, скролл к концу списка
	 */
	private static final int TO_END = 1;
	/**
	 * Движение пальца вниз, скролл к началу списка
	 */
	private static final int TO_START = -1;
	private OrientationHelper mOrientationHelper;

	public PredictiveAnimationsLayout() {
		// Враппер над LayoutParams с хелпер методами работы с конкретным View
		// TODO выпилить его кхуям и написать обычные private методы
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
	public boolean supportsPredictiveItemAnimations() {
		return true;
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

		//Nothing to do during prelayout when empty
		if (getChildCount() == 0 && state.isPreLayout()) {
			return;
		}

		int position = 0;
		int offset = 0;
		if (getChildCount() > 0) {
			position = getPosition(getChildAt(0));
			offset = getViewTop(getChildAt(0));
		}

		detachAndScrapAttachedViews(recycler);

		if (state.getItemCount() == 0) {
			return;
		}

		fillEndGap(recycler, state, position, offset, 0);

		if (getChildCount() > 0) {
			int viewsEnd = getViewBottom(getChildAt(getChildCount() - 1));
			if (viewsEnd < getHeight()) {
				offsetChildrenVertical(getHeight() - viewsEnd);
			}
		}

		position = 0;
		offset = 0;
		if (getChildCount() > 0) {
			position = getPosition(getChildAt(0)) - 1;
			offset = getViewTop(getChildAt(0));
		}

		fillStartGap(recycler, state, position, offset, 0);

		int viewsStart = getViewTop(getChildAt(0));
		if (viewsStart > 0) {
			offsetChildrenVertical(-viewsStart);
		}
	}

	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {

		if (dy == 0) {
			return 0;
		}

		int direction = dy > 0 ? TO_END : TO_START;
		int absDy = Math.abs(dy);

		if (direction == TO_END) {
			View lastView = getChildAt(getChildCount() - 1);
			fillEndGap(recycler, state, getPosition(lastView) + 1, getViewBottom(lastView), absDy);

		} else {
			View firstView = getChildAt(0);
			fillStartGap(recycler, state, getPosition(firstView) - 1, getViewTop(firstView), absDy);
		}

		offsetChildrenVertical(-dy);

		// Ограничение по скроллу снизу
		View lastView = getChildAt(getChildCount() - 1);
		if (getPosition(lastView) == getItemCount() - 1 && getViewBottom(lastView) < getHeight()) {
			dy -= getHeight() - getViewBottom(lastView);
			offsetChildrenVertical(getHeight() - getViewBottom(lastView));
		}
		// Ограничение по скроллу сверху
		View firstView = getChildAt(0);
		if (getPosition(firstView) == 0 && getViewTop(firstView) > 0) {
			dy += getViewTop(firstView);
			offsetChildrenVertical(-getViewTop(firstView));
		}

		// Удаляем View за границами списка
		for (int i = getChildCount() - 1; i > -1; i--) {
			View child = getChildAt(i);
			if (getViewBottom(child) < 0 || getViewTop(child) > getHeight()) {
				detachViewAt(i);
				recycler.recycleView(child);
			}
		}

		return dy;
	}

	/**
	 * Метод заполняет пустое место вьюхами, начиная с position со сдвигом topOffset. При добавлении View, offset и position
	 * будут увеличиваться, пока offset не станет больше getHeight()
	 *
	 * @param position      позиция вьюшки в адаптере, с которой нужно начать заполнение
	 * @param topOffset     изначальный сдвиг (topOffset будет равен getTop() у первой View, которая добавится через этот метод)
	 * @param additionalGap дополнительное место для заполнения вьюшек
	 */
	private void fillEndGap(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int topOffset, int additionalGap) {
		if (position < 0) {
			position = 0;
		}
		while (position < state.getItemCount() && topOffset < getHeight() + additionalGap) {
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
	 *
	 * @param position      позиция вьюшки в адаптере, с которой нужно начать заполнение
	 * @param bottomOffset  изначальный сдвиг (bottomOffset будет равен getBottom() у первой View, которая добавится через этот метод)
	 * @param additionalGap дополнительное место для заполнения вьюшек
	 */
	private void fillStartGap(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int bottomOffset, int additionalGap) {
		if (position >= state.getItemCount()) {
			position = state.getItemCount() - 1;
		}
		while (position > -1 && bottomOffset > -additionalGap) {
			View v = recycler.getViewForPosition(position);
			measureChildWithMargins(v, 0, 0);

			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);

			addView(v, 0);
			RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
			layoutDecorated(v, lp.leftMargin, bottomOffset - viewHeight + lp.topMargin,
					getWidth() - lp.rightMargin, bottomOffset - lp.bottomMargin);

			bottomOffset -= viewHeight;
			position--;
		}
	}

	private int getViewTop(View view) {
		return mOrientationHelper.getDecoratedStart(view);
	}

	private int getViewBottom(View view) {
		return mOrientationHelper.getDecoratedEnd(view);
	}
}
