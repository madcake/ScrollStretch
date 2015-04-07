package com.dzencake.slidingpane;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

	private RecyclerView mRecyclerView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mRecyclerView = (RecyclerView) findViewById(R.id.list);
		mRecyclerView.setLayoutManager(new ScrollStretchLayout(this, mRecyclerView));
		mRecyclerView.setAdapter(new SimpleAdapter(30));
		mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				if (newState != recyclerView.SCROLL_STATE_IDLE && recyclerView.getChildPosition(recyclerView.getChildAt(0)) != 0) {
					return;
				}
				if (recyclerView.getChildAt(0).getTop() > 150 && recyclerView.getChildAt(0).getTop() <= 600) {
					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - 600);
				} else if (recyclerView.getChildAt(0).getTop() > 900) {
					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - recyclerView.getHeight());
				} else if (recyclerView.getChildAt(0).getTop() > 600 && recyclerView.getChildAt(0).getTop() < 900) {
					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - 600);
				} else if (recyclerView.getChildAt(0).getTop() < 150) {
					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - 0);
				}
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

			}
		});
		mRecyclerView.smoothScrollBy(0, -600);
	}

	class SimpleAdapter extends RecyclerView.Adapter<SimpleHolder> {

		private final int mCount;

		SimpleAdapter(int count) {
			mCount = count;
		}

		@Override
		public SimpleHolder onCreateViewHolder(ViewGroup parent, int i) {
			return new SimpleHolder(LayoutInflater.from(
							parent.getContext()).inflate(R.layout.item_simple, parent, false));
		}

		@Override
		public void onBindViewHolder(SimpleHolder simpleHolder, int i) {
			simpleHolder.bind(i);
		}

		@Override
		public int getItemCount() {
			return mCount;
		}
	}

	private static class SimpleHolder extends RecyclerView.ViewHolder {

		private final TextView mText;

		public SimpleHolder(View itemView) {
			super(itemView);
			mText = (TextView) itemView;
		}

		public void bind(int i) {
			mText.setText((i + 1) + " := row number");
		}
	}
}
