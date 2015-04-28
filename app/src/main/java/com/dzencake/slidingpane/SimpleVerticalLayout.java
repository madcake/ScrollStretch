package com.dzencake.slidingpane;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.view.View;
import android.view.ViewGroup;

public class SimpleVerticalLayout extends RecyclerView.LayoutManager {
	private static final int TO_END = 1; // Движение пальца вверх, скролл к концу списка
	private static final int TO_START = -1; // Движение пальца вниз, скролл к началу списка

	private final OrientationHelper mOrientationHelper;

	public SimpleVerticalLayout() {
		// Враппер над LayoutParams с хелпер методами работы с конкретным View
		mOrientationHelper = OrientationHelper.createVerticalHelper(this);
	}

	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	/*
	 * Отвечает за заполнение RecyclerView при смене адаптера, так же вызывается когда его дети
	 * требуют обновить слой.
	 */
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
	public boolean canScrollVertically() {
		// Всегда умеет скролиться вертикально
		return true;
	}

	@Override
	public int scrollVerticallyBy(int dy, Recycler recycler, RecyclerView.State state) {
		// Не на что скролить выходим
		if (dy == 0) {
			return 0;
		}
		// Вычисляем направление скролла. direction == 1 || -1
		int direction = dy > 0 ? TO_END : TO_START;
		int absDy = Math.abs(dy);
		LayoutState layoutState = getLayoutState();
		// Запоминаем размер пространства доступный для скролинга
		int scrollSpace;
		int consumed;

		if (direction == TO_END) {
			final View endChild = getChildAt(getChildCount() - 1); // TODO: Это гарантрованно узкое место, в конечной редакции надо будет накрутить проверок.
			// Двикаемся к концу списка. Добавляем только новые, и удаляем те которые вышли за границы
			layoutState.currentPosition = getPosition(endChild) + direction;
			// Берём позицию от которой будем класть новые view
			layoutState.offset = mOrientationHelper.getDecoratedEnd(endChild);
			// Место которое доступно для новых view. Примеры (где end = mOrientationHelper.getEndAfterPadding()):
			// offset = 400, end = 1000; offset - end = -600;
			// offset = 1100, end = 1000; offset - end = 100;
			layoutState.scrollSpace = layoutState.offset - mOrientationHelper.getEndAfterPadding();
			// Нам доступно смещение которое задал пользователь минус scrollSpace
			// offset = 400, end = 1000, dy = 15; dy - (offset - end) = 585;
			// offset = 110, end = 1000, dy = 15; dy - (offset - end) = -85;
			layoutState.availableSpace = absDy - layoutState.scrollSpace;
			scrollSpace = layoutState.scrollSpace;
			// Меняем view и возвращаем оставшееся от изменений пространство.
			// На основании этого значения получаем потреблённое пространство
			// scrollSpace = 585, больше view нет; fillStart = -585;
			// scrollSpace = -100, view ещё есть и следющая добавленная требует 200;
			consumed = scrollSpace + fillStart(layoutState, recycler);
		} else {
			// Для направления к началу списка
			final View startChild = getChildAt(0); // TODO: Это гарантрованно узкое место, в конечной редакции надо будет накрутить проверок.
			// Двикаемся к концу списка. Добавляем только новые, и удаляем те которые вышли за границы
			layoutState.currentPosition = getPosition(startChild) + direction;
			// Берём позицию от которой будем класть новые view
			layoutState.offset = mOrientationHelper.getDecoratedStart(startChild);
			// Место которое доступно для новых view. Примеры (где end = mOrientationHelper.getStartAfterPadding()):
			// offset = -10, start = 0; -offset + end = 10;
			// offset = 0, start = 0; -offset - end = 0;
			layoutState.scrollSpace = -layoutState.offset + mOrientationHelper.getStartAfterPadding();
			// Нам доступно смещение которое задал пользователь минус scrollSpace
			// offset = -10, start = 0, dy = -15; dy - (-offset - end) = -25;
			// offset = 0, end = 0, dy = -15; dy - (-offset - end) = 15;
			layoutState.availableSpace = absDy - layoutState.scrollSpace;
			scrollSpace = layoutState.scrollSpace;
			// Меняем view и возвращаем оставшееся от изменений пространство.
			// На основании этого значения получаем потреблённое пространство
			// scrollSpace = 585, больше view нет; fillStart = -585;
			// scrollSpace = -100, view ещё есть и следющая добавленная требует 200;
			consumed = scrollSpace + fillEnd(layoutState, recycler);
		}

		if (consumed < 0) {
			// Нет элементов для скролинга
			return 0;
		}
		int scrolled = absDy > consumed ? consumed * direction // вытягиваем, если надо;
			: dy; // скролим
		// Совершаем скрол или вытягивание
		offsetChildrenVertical(-scrolled);
		// отрисовать edges, рассказать слушателям на сколько отскролили
		return scrolled;
	}

	// Задача метода взять существующие view и переложить их, убрав не видимые
	private int fillStart(LayoutState layoutState, Recycler recycler) {
		int start = layoutState.availableSpace;
		int remainingSpace = layoutState.availableSpace + mOrientationHelper.getEnd();
		// getItemCount > 0 на тот случай если адаптер вдруг изменился, а нам ещё не рассказали
		while (remainingSpace > 0 && getItemCount() > 0 && layoutState.currentPosition < getItemCount()) {
			// Запрашиваем view для позиции которую заготовили ранее
			View view = recycler.getViewForPosition(layoutState.currentPosition);
			// Для следующей итерации
			layoutState.currentPosition++;
			// Добавлеям view в конец
			addView(view);
			// Вычисляем размеры view
			measureChildWithMargins(view, 0, 0);
			// Потреблённое view пространство
			int viewConsumed = mOrientationHelper.getDecoratedMeasurement(view);
			// Вычисляем и применяем границы view
			RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
			int left = getPaddingLeft() + params.leftMargin;
			int top = layoutState.offset + params.topMargin;
			int right = left + mOrientationHelper.getDecoratedMeasurementInOther(view) - params.rightMargin;
			int bottom = layoutState.offset + viewConsumed - params.bottomMargin;
			layoutDecorated(view, left, top, right, bottom);
			layoutState.offset += viewConsumed;
			// view положили теперь надо удалить и изменить флаги
			layoutState.availableSpace -= viewConsumed;
			// remaining заведенна для внутреннего использования, scrollSpace требуется в более расширенном виде
			remainingSpace -= viewConsumed; // TODO: Думаю можно обойтись только availableSpace
			// Убираем не видимые view
			layoutState.scrollSpace += viewConsumed;
			if (layoutState.availableSpace < 0) {
				layoutState.scrollSpace += layoutState.availableSpace;
			}
			if (layoutState.scrollSpace >= 0) {
				final int childCount = getChildCount();

				for (int i = 0; i < childCount; i++) {
					View child = getChildAt(i);
					if (mOrientationHelper.getDecoratedEnd(child) > layoutState.scrollSpace) {// stop here
						for (int j = i - 1; j > -1; j--) {
							removeAndRecycleViewAt(j, recycler);
						}
						break;
					}
				}
			}
		}
		return start - layoutState.availableSpace;
	}

	private int fillEnd(LayoutState layoutState, Recycler recycler) {
		int start = layoutState.availableSpace;
		int remainingSpace = layoutState.availableSpace;
		// getItemCount > 0 на тот случай если адаптер вдруг изменился, а нам ещё не рассказали
		while (remainingSpace > 0 && getItemCount() > 0 && layoutState.currentPosition >= 0) {
			// Запрашиваем view для позиции которую заготовили ранее
			View view = recycler.getViewForPosition(layoutState.currentPosition);
			// Для следующей итерации
			layoutState.currentPosition--;
			// Добавлеям view в начало
			addView(view, 0);
			// Вычисляем размеры view
			measureChildWithMargins(view, 0, 0);
			// Потреблённое view пространство
			int viewConsumed = mOrientationHelper.getDecoratedMeasurement(view);
			// Вычисляем и применяем границы view
			RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
			int left = getPaddingLeft() + params.leftMargin;
			int top = layoutState.offset - viewConsumed + params.topMargin;
			int right = left + mOrientationHelper.getDecoratedMeasurementInOther(view) - params.rightMargin;
			int bottom = layoutState.offset - params.bottomMargin;
			layoutDecorated(view, left, top, right, bottom);
			layoutState.offset -= viewConsumed;
			// view положили теперь надо удалить и изменить флаги
			layoutState.availableSpace -= viewConsumed;
			// remaining заведенна для внутреннего использования, scrollSpace требуется в более расширенном виде
			remainingSpace -= viewConsumed;
			// Убираем не видимые view
			layoutState.scrollSpace += viewConsumed;
			if (layoutState.availableSpace < 0) {
				layoutState.scrollSpace += layoutState.availableSpace;
			}
			if (layoutState.scrollSpace >= 0) {
				final int childCount = getChildCount();
				final int limit = mOrientationHelper.getEnd() - layoutState.scrollSpace;

				for (int i = childCount - 1; i > -1; i--) {
					View child = getChildAt(i);
					if (mOrientationHelper.getDecoratedStart(child) < limit) {
						if (childCount - 1 != i) {
							for (int j = childCount - 1; j > i; j--) {
								removeAndRecycleViewAt(j, recycler);
							}
						}
						break;
					}
				}
			}
		}
		return start - layoutState.availableSpace;
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

	private LayoutState getLayoutState() {
		LayoutState state = new LayoutState();
		state.availableSpace = mOrientationHelper.getEndAfterPadding();
		if (getChildCount() > 0) { // TODO: буду ситации когда на слое не лежит не одной view
			state.position = getPosition(getChildAt(0));
			state.offset = mOrientationHelper.getDecoratedStart(getChildAt(0));
		}
		return state;
	}

	private static class LayoutState {
		/** Позиция адаптера */
		public int position;
		/** Смещение первого / последнего view */
		public int offset;
		/** Доступное для размещения view пространство */
		public int availableSpace;
		/** Текущая позиция для адаптера, от которой планируется заполнение */
		// Добавленно для обратной совместимости с position
		public int currentPosition;
		/** Доступное для скроллинга пространство */
		// Добавленно для обратной совместимости с available
		public int scrollSpace;

	}
}
