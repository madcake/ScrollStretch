package com.dzencake.slidingpane;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

	private static final String STATE_ITEMS = "items";

	private ArrayList<Integer> mItems;
	private RecyclerView mRecyclerView;
	private SimpleAdapter mAdapter;
	private VerticalLayout3 mLayoutManager;
	private int mMiddleState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			mItems = new ArrayList<>();
			mItems.add(0);
			mItems.add(1);
		} else {
			mItems = savedInstanceState.getIntegerArrayList(STATE_ITEMS);
		}

		mAdapter = new SimpleAdapter();
		mRecyclerView = (RecyclerView) findViewById(R.id.list);
		mLayoutManager = new VerticalLayout3(getSize(this).y);
//		mRecyclerView.setLayoutManager(new SimpleVerticalLayout());
		mRecyclerView.setLayoutManager(mLayoutManager);
//		mRecyclerView.setLayoutManager(new ScrollStretchLayout());
//		mRecyclerView.setLayoutManager(new LinearLayoutManager(this) {
//			@Override
//			public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
//				super.onLayoutChildren(recycler, state);
//				Log.e("TAG", "Fuckckckckckc");
//			}
//		});
		mRecyclerView.setAdapter(mAdapter);
		mMiddleState = getSize(this).y / 3;
		mLayoutManager.scrollToPositionWithOffset(0, mMiddleState);
		mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

				if (newState != RecyclerView.SCROLL_STATE_IDLE || mLayoutManager.getDummyBottom() == 0) {
					return;
				}

				int shift = Math.abs(mLayoutManager.getDummyTop());
				if (shift < mLayoutManager.getDummyHeight() / 3){
					// открываем карту
					mRecyclerView.smoothScrollBy(0, -shift);
				} else if (shift < mLayoutManager.getDummyHeight() * 2 / 3){
					mRecyclerView.smoothScrollBy(0, mMiddleState - shift);
				} else {
					// закрываем карту
					mRecyclerView.smoothScrollBy(0, mLayoutManager.getDummyHeight() - shift);
				}

//				if (recyclerView.getChildAt(0).getTop() > 150 && recyclerView.getChildAt(0).getTop() <= 600) {
//					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - 600);
//				} else if (recyclerView.getChildAt(0).getTop() > 900) {
//					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - recyclerView.getHeight());
//				} else if (recyclerView.getChildAt(0).getTop() > 600 && recyclerView.getChildAt(0).getTop() < 900) {
//					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - 600);
//				} else if (recyclerView.getChildAt(0).getTop() < 150) {
//					mRecyclerView.smoothScrollBy(0, recyclerView.getChildAt(0).getTop() - 0);
//				}
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

			}
		});
//		mRecyclerView.smoothScrollBy(0, -600);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntegerArrayList(STATE_ITEMS, mItems);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public static Point getSize(Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			display.getSize(size);
		} else {
			size.x = display.getWidth();
			size.y = display.getHeight();
		}
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
		int abSize = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
		size.y -= abSize;
		return size;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_add: {
				mItems.add(mItems.size());

//				if (mItems.size() > 4 && mItems.size() < 7) {
//					mRecyclerView.smoothScrollBy(0, 400);
//				} else if (mItems.size() > 7) {
//					mRecyclerView.smoothScrollToPosition(mItems.size() - 1);
//				}
				break;
			}
			case R.id.action_remove: {
				if (mItems.size() > 1) {
					mItems.remove(mItems.size() - 1);
				}
				break;
			}
			case R.id.action_toggle: {
				if (mRecyclerView.getAdapter() instanceof SimpleAdapter) {
					mRecyclerView.setAdapter(new VerySimpleAdapter());
				} else {
					mRecyclerView.setAdapter(mAdapter);
				}
				break;
			}
		}
		mRecyclerView.getAdapter().notifyDataSetChanged();
		return super.onOptionsItemSelected(item);
	}

	class SimpleAdapter extends RecyclerView.Adapter<BindHolder> {

		@Override
		public int getItemViewType(int position) {
			return position % 3 == 0 ? 2 : 1;
		}

		@Override
		public BindHolder onCreateViewHolder(ViewGroup parent, int itemViewType) {
			if (itemViewType == 1) {
				return new SimpleHolder(LayoutInflater.from(
						parent.getContext()).inflate(R.layout.item_simple, parent, false));
			} else {
				return new ImageHolder(LayoutInflater.from(
						parent.getContext()).inflate(R.layout.item_simple_img, parent, false));
			}
		}

		@Override
		public void onBindViewHolder(BindHolder simpleHolder, int i) {
			simpleHolder.bind(i);
		}

		@Override
		public int getItemCount() {
			return mItems.size();
		}
	}

	private static class VerySimpleAdapter extends RecyclerView.Adapter<SimpleHolder> {

		@Override
		public SimpleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new SimpleHolder(LayoutInflater.from(
					parent.getContext()).inflate(R.layout.item_simple, parent, false));
		}

		@Override
		public void onBindViewHolder(SimpleHolder holder, int position) {
			holder.bind(position);
		}

		@Override
		public int getItemCount() {
			return 3;
		}
	}

	private static class SimpleHolder extends BindHolder {

		private final TextView mText;
		private final ImageView mImg;

		public SimpleHolder(View itemView) {
			super(itemView);
			mText = (TextView) itemView.findViewById(R.id.text);
			mImg = (ImageView) itemView.findViewById(R.id.img);
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(v.getContext(), mText.getText(), Toast.LENGTH_SHORT).show();
				}
			});
		}

		public void bind(int i) {
			mText.setText((i + 1) + " := row number");
			if (i % 2 == 0) {
				mImg.setVisibility(View.GONE);
			} else {
				Picasso.with(mImg.getContext())
						.load(Uri.parse(
								"http://static.cdprojektred.com/cdprojektred/wp-content/uploads-cdpr-en/2014/09/th-3285-826-720x405.jpg"))
						.fit()
						.centerCrop()
						.into(mImg);
				mImg.setVisibility(View.VISIBLE);
			}
		}
	}

	private static class ImageHolder extends BindHolder {

		private final ImageView mImg;

		public ImageHolder(View itemView) {
			super(itemView);
			mImg = (ImageView) itemView.findViewById(R.id.img);
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(v.getContext(), mImg.getTag() + " := position", Toast.LENGTH_SHORT).show();
				}
			});
		}

		public void bind(int i) {
			Uri uri;
			if (i % 5 == 0) {
				if (i == 0){
					ViewGroup.LayoutParams lp = itemView.getLayoutParams();
					lp.height = getSize(itemView.getContext()).y;
					itemView.setLayoutParams(lp);
				}
				uri = Uri.parse("http://www.gametech.ru/sadm_images/00jin/2014/june/14/witcher/1402773249-the-witcher-3-wild-hunt-art-3.jpg");
			} else {
				uri = Uri.parse("http://www.gametech.ru/sadm_images/00jin/2014/june/14/witcher/1402773251-the-witcher-3-wild-hunt-art-2.jpg");
			}
			Picasso.with(mImg.getContext())
					.load(uri)
					.fit()
					.centerCrop()
					.into(mImg);
			mImg.setTag(i);
		}
	}

	private abstract static class BindHolder extends RecyclerView.ViewHolder {

		public BindHolder(View itemView) {
			super(itemView);
		}

		public abstract void bind(int position);
	}
}
