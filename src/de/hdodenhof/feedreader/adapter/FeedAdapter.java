package de.hdodenhof.feedreader.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.model.Feed;

public class FeedAdapter extends ArrayAdapter<Feed> {

        private ArrayList<Feed> mFeeds;
        private LayoutInflater mLayoutInflater;

        public FeedAdapter(Context context, ArrayList<Feed> items) {
                super(context, 0, items);
                this.mFeeds = items;
                mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

                final Feed mFeed = mFeeds.get(position);

                if (mFeed != null) {

                        convertView = mLayoutInflater.inflate(R.layout.listitem_feed, null);
                        final TextView mTitle = (TextView) convertView.findViewById(R.id.list_item_feed_title);
                        final TextView mSummary = (TextView) convertView.findViewById(R.id.list_item_feed_summary);

                        if (mTitle != null) {
                                mTitle.setText(mFeed.getName());
                        }
                        if (mSummary != null) {
                                mSummary.setText(mFeed.getUrl());
                        }
                }
                return convertView;
        }

}
