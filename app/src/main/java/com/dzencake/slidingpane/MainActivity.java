package com.dzencake.slidingpane;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

	private static final String STATE_ITEMS = "items";

	private RecyclerView mRecyclerView;
	private StringsAdapter mAdapter;
	private ArrayList<String> mItems;
	private int mCounter = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			mItems = new ArrayList<>();
		} else {
			mItems = savedInstanceState.getStringArrayList(STATE_ITEMS);
		}

		mAdapter = new StringsAdapter(mItems);

		mRecyclerView = (RecyclerView) findViewById(R.id.list);
		mRecyclerView.setLayoutManager(new PredictiveAnimationsLayout());
		mRecyclerView.setAdapter(mAdapter);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(STATE_ITEMS, mItems);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_add: {
				int oldSize = mItems.size();
				Log.d("action_add", mItems.toString());
				for (int i = 0; i < 6; i++) {
					mItems.add("position " + mCounter++);
				}
				Log.d("action_add", mItems.toString());
				mAdapter.notifyItemRangeInserted(oldSize, 6);
				break;
			}
			case R.id.action_remove: {
				int position = 5;
				Log.d("action_remove", mItems.toString());
				for (int i = 5; i > 0; i--) {
					mItems.remove(position--);
				}
				Log.d("action_remove", mItems.toString());
				mAdapter.notifyItemRangeRemoved(1, 5);
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
