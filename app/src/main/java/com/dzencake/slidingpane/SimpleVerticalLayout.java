package com.dzencake.slidingpane;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public class SimpleVerticalLayout extends RecyclerView.LayoutManager {

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
		int direction = Math.abs(dy) / dy;
		LayoutState layoutState = getLayoutState();
		int freeSpace;
		int scrollSpace;
		// Заполняем слой из кэша с учётом скролл смещения, так же убираем не видимые view
		if (direction == 1) {
			// смещаем координаты
			layoutState.offset -= dy;
			// Убираем лишнии view и добавляем новые
			freeSpace = fillStart(layoutState, recycler);
			// Мы могли не дотянуть, то есть views кончились, а в низу осталось пустое пространство,
			// но при этом сверху есть views
			// TODO: тут всё не так просто
//			if (freeSpace > 0 && layoutState.position > 0) {
//				layoutState.availableSpace = -freeSpace;
//				freeSpace = fillGapEnd(layoutState, recycler);
//			}
		} else {
			scrollSpace = -mOrientationHelper.getDecoratedStart(getChildAt(0))
					+ mOrientationHelper.getStartAfterPadding();
			layoutState.position = getPosition(getChildAt(getChildCount() - 1));
			layoutState.offset = mOrientationHelper.getDecoratedEnd(getChildAt(getChildCount() - 1));
			layoutState.availableSpace = dy;
			freeSpace = fillEnd(layoutState, recycler);
		}
//		freeSpace -= dy;
		offsetChildrenVertical(freeSpace > dy ? 0 : -dy);
		return dy;
	}

	// Задача метода взять существующие view и переложить их, убрав более не видимые
	private int fillStart(LayoutState layoutState, Recycler recycler) {
		int usedSpace = layoutState.offset;
		// Заводим кэш (Scrap)
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

		for (int i = layoutState.position; i < getItemCount() && usedSpace <= layoutState.availableSpace; i++) {
			View v = cache.get(i);
			if (v == null) {
				v = recycler.getViewForPosition(i);
				measureChildWithMargins(v, 0, 0);
			}
			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
			usedSpace += viewHeight;
			// Что бы не потерять view
			if (usedSpace >= 0) {
				// Если view уже была, то достаём из кэша иначе строим новую.
				if (cache.get(i) == null) {
					addView(v);
					RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
					int left = lp.leftMargin;
					int top = lp.topMargin + usedSpace - viewHeight;
					int right = getWidth() - lp.rightMargin;
					int bottom = usedSpace - lp.bottomMargin;
					layoutDecorated(v, left, top, right, bottom);
				} else {
					attachView(v);
					cache.remove(i);
				}
			}
		}
		// Утилизируем не использованные views
		for (int i = 0; i < cache.size(); i++) {
			recycler.recycleView(cache.valueAt(i));
		}
		return layoutState.availableSpace - usedSpace;
	}

	private int fillEnd(LayoutState layoutState, Recycler recycler) {
		int usedSpace = layoutState.offset;
		// Заводим кэш (Scrap)
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

		for (int i = layoutState.position; i > -1 && usedSpace > layoutState.availableSpace; i--) {
			View v = cache.get(i);
			if (v == null) {
				v = recycler.getViewForPosition(i);
				measureChildWithMargins(v, 0, 0);
			}
			int viewHeight = mOrientationHelper.getDecoratedMeasurement(v);
			usedSpace -= viewHeight;
			if (usedSpace < layoutState.availableSpace) {
				// Если view уже была, то достаём из кэша иначе строим новую.
				if (cache.get(i) == null) {
					addView(v, 0);
					RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
					int left = lp.leftMargin;
					int top = lp.topMargin + usedSpace;
					int right = getWidth() - lp.rightMargin;
					int bottom = usedSpace + viewHeight - lp.bottomMargin;
					layoutDecorated(v, left, top, right, bottom);
				} else {
					attachView(v, 0);
					cache.remove(i);
				}
			}
		}
		// Утилизируем не использованные views
		for (int i = 0; i < cache.size(); i++) {
			recycler.recycleView(cache.valueAt(i));
		}
		return layoutState.availableSpace - usedSpace;
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
	}
}
