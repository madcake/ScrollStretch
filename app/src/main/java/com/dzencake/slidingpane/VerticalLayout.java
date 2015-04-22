package com.dzencake.slidingpane;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class VerticalLayout extends RecyclerView.LayoutManager {

	private static final String TAG = VerticalLayout.class.getSimpleName();

	private OrientationHelper mOrientationHelper;
	private SavedState mPendingSavedState;

	public VerticalLayout() {
		// Враппер над LayoutParams с хелпер методами работы с конкретным View
		mOrientationHelper = OrientationHelper.createVerticalHelper(this);
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			mPendingSavedState = (SavedState) state;
			if (mPendingSavedState.offset > getHeight()) {
				mPendingSavedState.offset = getHeight(); // TODO: У нас может быть и больше
			}
			requestLayout();
		}
	}

	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		int oldFirstPos;
		int offset;
		if (mPendingSavedState == null || !mPendingSavedState.hasValidPosition()) {
			oldFirstPos = getChildCount() == 0 ? 0 : getPosition(getChildAt(0));
			// Считаем оффсет
			offset = getChildCount() == 0 ? 0 : mOrientationHelper.getDecoratedStart(getChildAt(0));
		} else {
			oldFirstPos = mPendingSavedState.position;
			offset = mPendingSavedState.offset;
		}
		detachAndScrapAttachedViews(recycler);
		// Заполняем
		int consumed = fill(recycler, 1, offset, oldFirstPos, 0);
		offsetChildrenVertical(-consumed);
		// Больше не требуется
		mPendingSavedState = null;
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
		int consumed;
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
				if (offset < dy) {
					break;
				}
			}

			if (offset > dy) {
				consumed = offset;
			} else {
				consumed = dy;
			}
		}

		for (int i = 0; i < cache.size(); i++) {
			recycler.recycleView(cache.valueAt(i));
		}
		return consumed;
	}

	@Override
	public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
									   int position) {
		LinearSmoothScroller linearSmoothScroller =
				new LinearSmoothScroller(recyclerView.getContext()) {
					@Override
					public PointF computeScrollVectorForPosition(int targetPosition) {
						return VerticalLayout.this.computeScrollVectorForPosition(targetPosition);
					}
				};
		linearSmoothScroller.setTargetPosition(position);
		startSmoothScroll(linearSmoothScroller);
	}

	public PointF computeScrollVectorForPosition(int targetPosition) {
		if (getChildCount() == 0) {
			return null;
		}
		final int firstChildPos = getPosition(getChildAt(0));
		final int direction = targetPosition < firstChildPos ? -1 : 1;
		return new PointF(0, direction);
	}

	@Override
	public Parcelable onSaveInstanceState() {
		if (mPendingSavedState != null) {
			return new SavedState(mPendingSavedState);
		}
		SavedState state = new SavedState();
		if (getChildCount() > 0) {
			state.position = getPosition(getChildAt(0));
			state.offset = mOrientationHelper.getDecoratedStart(getChildAt(0));
//			TODO:
//			state.offset = mOrientationHelper.getDecoratedStart(refChild) -
//					mOrientationHelper.getStartAfterPadding();
		} else {
			state.invalidatePosition();
		}
		return state;
	}

	static class SavedState implements Parcelable {

		int position;

		int offset;

		public SavedState() {

		}

		SavedState(Parcel in) {
			position = in.readInt();
			offset = in.readInt();
		}

		public SavedState(SavedState other) {
			position = other.position;
			offset = other.offset;
		}

		boolean hasValidPosition() {
			return position >= 0;
		}

		void invalidatePosition() {
			position = NO_POSITION;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(position);
			dest.writeInt(offset);
		}

		public static final Parcelable.Creator<SavedState> CREATOR
				= new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
