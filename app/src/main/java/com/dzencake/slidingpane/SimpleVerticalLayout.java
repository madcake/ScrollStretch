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
		// Собираем состояние слоя
		LayoutState layoutState = getLayoutState();
		// Уюираем все view
		detachAndScrapAttachedViews(recycler);
		// Заполняем пустоты от меньшей позиции адаптера к большей
		int freeSpace = fillGapStart(layoutState, recycler);
		// Если есть ещё место и элементы то заполняем от большей к меньшей
		// TODO: тут всё не так просто
		if (freeSpace > 0 && layoutState.position > 0) {
			layoutState.availableSpace = -freeSpace;
			freeSpace = fillGapEnd(layoutState, recycler);
		} else {
			freeSpace = 0;
		}
		// Надо сдвинуть все view на незаполненое пространство, что бы views не болтались где попало
		offsetChildrenVertical(layoutState.offset - freeSpace);
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
		int scrollSpace = 0;
		int consumed = 0;

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

	// Задача метода взять существующие view и переложить их, убрав более не видимые
	private int fillStart(LayoutState layoutState, Recycler recycler) {
		int start = layoutState.availableSpace;
		int remainingSpace = layoutState.availableSpace;
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
			// We calculate everything with View's bounding box (which includes decor and margins)
			// To calculate correct layout position, we subtract margins.
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
			// We calculate everything with View's bounding box (which includes decor and margins)
			// To calculate correct layout position, we subtract margins.
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


	// Заполняем пустоты сверху вниз. Начало заполнения может быть < 0.
	private int fillGapStart(LayoutState layoutState, Recycler recycler) {
		int usedSpace = layoutState.offset;
		int itemsCount = getItemCount();

		for (int i = layoutState.position; i < itemsCount && usedSpace <= layoutState.availableSpace; i++) {
			// Получаем view. Внутри она либо создаётся адаптером, либо берётся из кэша
			View v = recycler.getViewForPosition(i);
			// Считаем размеры view
			measureChildWithMargins(v, 0, 0);
			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
			// usedSpace будет хранить нижние границы view
			usedSpace += viewHeight;
			// Что бы не потерять view
			// Добавляем view только если она видна и не ушла за границы экрана. Пример: -10 + 100
			if (usedSpace >= 0) { // TODO: если не видна, то надо вернуть view в Recycler
				// Строим view
				addView(v);
				RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
				// Формируем координаты view. usedSpace - хранит нижнюю позицию view.
				int left = lp.leftMargin;
				int top = lp.topMargin + usedSpace - viewHeight;
				int right = getWidth() - lp.rightMargin;
				int bottom = usedSpace - lp.bottomMargin;
				layoutDecorated(v, left, top, right, bottom);
			}
		}
		// Возвращаем размер не заполненного пространства
		return layoutState.availableSpace - usedSpace;
	}

	private int fillGapEnd(LayoutState layoutState, Recycler recycler) {
		int usedSpace = layoutState.offset;

		for (int i = layoutState.position; i > -1 && usedSpace > layoutState.availableSpace; i--) {
			// Получаем view. Внутри она либо создаётся адаптером, либо берётся из кэша
			View v = recycler.getViewForPosition(i);
			// Считаем размеры view
			measureChildWithMargins(v, 0, 0);
			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
			// usedSpace будет хранить верхнии границы view
			usedSpace -= viewHeight;
			// Что бы не потерять view
			// Добавляем View только если она видна и не ушла за границы экрана. Пример: -10 + 100
			if (usedSpace >= layoutState.availableSpace) { // TODO: если не видна, то надо вернуть view в Recycler
				// Строим view и всегда кладём её на верх
				addView(v, 0);
				RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
				// Формируем координаты view. usedSpace - хранит нижнюю позицию view.
				int left = lp.leftMargin;
				int top = lp.topMargin + usedSpace;
				int right = getWidth() - lp.rightMargin;
				int bottom = usedSpace + viewHeight - lp.bottomMargin;
				layoutDecorated(v, left, top, right, bottom);
			}
		}
		return layoutState.availableSpace - usedSpace;
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
