package com.dzencake.slidingpane;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class StringsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private List<String> mStrings;

	public StringsAdapter(List<String> strings) {
		mStrings = strings;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new Holder(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false));
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		((Holder) holder).text.setText(mStrings.get(position));
	}

	@Override
	public int getItemCount() {
		return mStrings == null ? 0 : mStrings.size();
	}

	private static class Holder extends RecyclerView.ViewHolder {

		TextView text;

		public Holder(View itemView) {
			super(itemView);
			text = (TextView) itemView.findViewById(android.R.id.text1);
		}
	}
}
