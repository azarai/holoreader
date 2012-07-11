package de.hdodenhof.feedreader.activities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleListFragment;
import de.hdodenhof.feedreader.fragments.FeedListFragment;
import de.hdodenhof.feedreader.listadapters.RSSAdapter;
import de.hdodenhof.feedreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.feedreader.listadapters.RSSFeedAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.parser.ArticleHandler;
import de.hdodenhof.feedreader.parser.FeedHandler;
import de.hdodenhof.feedreader.parser.SAXHelper;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class HomeActivity extends FragmentActivity implements FragmentCallback, OnItemClickListener {

        @SuppressWarnings("unused")
        private static final String TAG = FragmentActivity.class.getSimpleName();

        private boolean mTwoPane = false;
        private ProgressDialog mSpinner;
        private ProgressDialog mProgresBar;
        private ArticleListFragment mArticleListFragment;

        /**
         * Handles messages from AsyncTasks started within this activity
         */
        Handler mAsyncHandler = new AsynHandler(this);

        private static class AsynHandler extends Handler {
                private final WeakReference<HomeActivity> mTargetReference;

                AsynHandler(HomeActivity target) {
                        mTargetReference = new WeakReference<HomeActivity>(target);
                }

                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        HomeActivity mTarget = mTargetReference.get();

                        switch (msg.what) {
                        case 1:
                                // added feed
                                mTarget.callbackFeedAdded();
                                break;
                        case 3:
                                // refreshed feeds
                                mTarget.callbackFeedsRefreshed();
                                break;
                        case 9:
                                // refresh progress bar
                                mTarget.callbackUpdateProgress(msg.arg1);
                                break;
                        default:
                                break;
                        }
                }
        };

        /**
         * Update feed list and dismiss spinner after new feed has been added
         */
        private void callbackFeedAdded() {
                mSpinner.dismiss();
        }

        /**
         * Update feed list and dismiss progress bar after feeds have been refreshed
         */
        private void callbackFeedsRefreshed() {
                mProgresBar.dismiss();
        }

        /**
         * Update progress bar during feeds refresh
         */
        private void callbackUpdateProgress(int progress) {
                mProgresBar.setProgress(progress);
        }

        /**
         * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                if (savedInstanceState != null) {

                }

                setContentView(R.layout.activity_home);

                mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
                if (mArticleListFragment != null) {
                        mTwoPane = true;
                }

        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment)
         */
        public void onFragmentReady(Fragment fragment) {
                if (mTwoPane && fragment instanceof FeedListFragment) {
                        ((FeedListFragment) fragment).setChoiceModeSingle();
                }
        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
         */
        public boolean isDualPane() {
                return mTwoPane;
        }

        /**
         * Starts an AsyncTask to fetch a new feed and add it to the database
         * 
         * @param feedUrl
         *                URL of the feed to fetch
         */
        private void addFeed(String feedUrl) {
                mSpinner = ProgressDialog.show(this, "", "Please wait...", true);
                AddFeedTask mAddFeedTask = new AddFeedTask(mAsyncHandler, this);
                mAddFeedTask.execute(feedUrl);
        }

        /**
         * Starts an AsyncTask to refresh all feeds currently in the database
         */
        private void refreshFeeds() {

                mProgresBar = new ProgressDialog(this);
                mProgresBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgresBar.setMessage("Loading...");
                mProgresBar.setCancelable(false);
                mProgresBar.setProgress(0);
                mProgresBar.setMax(queryFeedCount());
                mProgresBar.show();

                RefreshFeedsTask mRefreshFeedsTask = new RefreshFeedsTask(mAsyncHandler, this);
                mRefreshFeedsTask.execute();
        }

        private int queryFeedCount() {
                String[] mProjection = { FeedDAO._ID };

                Cursor mCursor = getContentResolver().query(RSSContentProvider.URI_FEEDS, mProjection, null, null, null);
                int mCount = mCursor.getCount();
                mCursor.close();

                return mCount;
        }

        /**
         * Shows a dialog to add a new feed URL
         */
        private void showAddDialog() {
                AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);

                mAlertDialog.setTitle("Add feed");
                mAlertDialog.setMessage("Input Feed URL");

                final EditText mInput = new EditText(this);
                mInput.setText("http://t3n.de/news/feed");
                mAlertDialog.setView(mInput);

                mAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                                String value = mInput.getText().toString();
                                addFeed(value);
                        }
                });

                mAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                });

                mAlertDialog.show();
        }

        /**
         * Updates all fragments or launches a new activity (depending on the activities current layout) whenever a feed or article in one of the fragments has
         * been clicked on
         * 
         * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RSSAdapter mAdapter = (RSSAdapter) parent.getAdapter();
                Cursor mCursor;
                int mFeedID;

                switch (mAdapter.getType()) {
                case RSSAdapter.TYPE_FEED:
                        mCursor = ((RSSFeedAdapter) mAdapter).getCursor();
                        mCursor.moveToPosition(position);
                        mFeedID = mCursor.getInt(mCursor.getColumnIndex(FeedDAO._ID));

                        if (mTwoPane) {
                                mArticleListFragment.selectFeed(mFeedID);
                        } else {
                                Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                                mIntent.putExtra("feedid", mFeedID);
                                startActivity(mIntent);
                        }
                        break;

                case RSSAdapter.TYPE_ARTICLE:
                        mCursor = ((RSSArticleAdapter) mAdapter).getCursor();
                        mCursor.moveToPosition(position);

                        int mArticleID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO._ID));
                        mFeedID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO.FEEDID));

                        Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                        mIntent.putExtra("articleid", mArticleID);
                        mIntent.putExtra("feedid", mFeedID);
                        startActivity(mIntent);
                        break;

                default:
                        break;
                }
        }

        /**
         * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
         */
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                MenuInflater mMenuInflater = getMenuInflater();
                mMenuInflater.inflate(R.menu.main, menu);
                return true;
        }

        /**
         * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
         */
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                case R.id.item_refresh:
                        refreshFeeds();
                        return true;
                case R.id.item_add:
                        showAddDialog();
                        return true;
                default:
                        return super.onOptionsItemSelected(item);
                }
        }

        public class AddFeedTask extends AsyncTask<String, Void, Void> {

                private Handler mMainUIHandler;
                private Context mContext;

                public AddFeedTask(Handler mainUIHandler, Context context) {
                        this.mMainUIHandler = mainUIHandler;
                        this.mContext = context;
                }

                protected Void doInBackground(String... params) {

                        String mURL = params[0];

                        try {
                                SAXHelper mSAXHelper = new SAXHelper(mURL, new FeedHandler());
                                String mName = (String) mSAXHelper.parse();

                                ContentResolver mContentResolver = mContext.getContentResolver();
                                ContentValues mContentValues = new ContentValues();

                                mContentValues.put(FeedDAO.NAME, mName);
                                mContentValues.put(FeedDAO.URL, mURL);
                                mContentValues.put(FeedDAO.UPDATED, SQLiteHelper.fromDate(new Date()));

                                mContentResolver.insert(RSSContentProvider.URI_FEEDS, mContentValues);

                        } catch (Exception e) {
                                e.printStackTrace();
                        }

                        return null;

                }

                @Override
                protected void onPreExecute() {
                        super.onPreExecute();
                }

                @Override
                protected void onPostExecute(Void result) {
                        Message mMSG = Message.obtain();
                        mMSG.what = 1;
                        mMainUIHandler.sendMessage(mMSG);
                }
        }

        public class RefreshFeedsTask extends AsyncTask<Void, Integer, Void> {

                private static final int SUMMARY_MAXLENGTH = 250;

                private Handler mMainUIHandler;
                private Context mContext;

                public RefreshFeedsTask(Handler mainUIHandler, Context context) {
                        this.mMainUIHandler = mainUIHandler;
                        this.mContext = context;
                }

                @SuppressWarnings("unchecked")
                protected Void doInBackground(Void... params) {

                        ArrayList<Article> mArticles = new ArrayList<Article>();

                        ContentResolver mContentResolver = mContext.getContentResolver();

                        try {
                                String[] mProjection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL };
                                Cursor mCursor = mContentResolver.query(RSSContentProvider.URI_FEEDS, mProjection, null, null, null);
                                mCursor.moveToFirst();

                                do {
                                        int mFeedID = mCursor.getInt(mCursor.getColumnIndex(FeedDAO._ID));
                                        SAXHelper mSAXHelper = new SAXHelper(mCursor.getString(mCursor.getColumnIndex(FeedDAO.URL)), new ArticleHandler());
                                        mArticles = (ArrayList<Article>) mSAXHelper.parse();
                                        int i = 0;

                                        ContentValues[] mContentValuesArray = new ContentValues[mArticles.size()];
                                        for (Article mArticle : mArticles) {

                                                mArticle.setFeedId(mFeedID);

                                                Document mDocument = Jsoup.parse(mArticle.getContent());
                                                Elements mIframes = mDocument.getElementsByTag("iframe");

                                                TextNode mPlaceholder = new TextNode("(video removed)", null);
                                                for (Element mIframe : mIframes) {
                                                        mIframe.replaceWith(mPlaceholder);
                                                }

                                                mArticle.setContent(mDocument.html());

                                                if (mArticle.getSummary() == null) {
                                                        String mSummary = mDocument.text();
                                                        if (mSummary.length() > SUMMARY_MAXLENGTH) {
                                                                mArticle.setSummary(mSummary.substring(0, SUMMARY_MAXLENGTH));
                                                        } else {
                                                                mArticle.setSummary(mSummary);
                                                        }
                                                }

                                                ContentValues mContentValues = new ContentValues();

                                                mContentValues.put(ArticleDAO.FEEDID, mArticle.getFeedId());
                                                mContentValues.put(ArticleDAO.GUID, mArticle.getGuid());
                                                mContentValues.put(ArticleDAO.PUBDATE, SQLiteHelper.fromDate(mArticle.getPubDate()));
                                                mContentValues.put(ArticleDAO.TITLE, mArticle.getTitle());
                                                mContentValues.put(ArticleDAO.SUMMARY, mArticle.getSummary());
                                                mContentValues.put(ArticleDAO.CONTENT, mArticle.getContent());
                                                mContentValues.put(ArticleDAO.READ, SQLiteHelper.fromBoolean(mArticle.isRead()));

                                                mContentValuesArray[i++] = mContentValues;

                                        }

                                        mContentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + "=?",
                                                        new String[] { Integer.toString(mFeedID) });
                                        mContentResolver.bulkInsert(RSSContentProvider.URI_ARTICLES, mContentValuesArray);

                                        // TODO: update instead of insert if applicable, set lastUpdate on Feed

                                        publishProgress(mCursor.getPosition() + 1);
                                } while (mCursor.moveToNext());

                        } catch (Exception e) {
                                e.printStackTrace();
                        }

                        return null;

                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                        Message mMSG = Message.obtain();
                        mMSG.what = 9;
                        mMSG.arg1 = values[0];
                        mMainUIHandler.sendMessage(mMSG);
                }

                @Override
                protected void onPreExecute() {
                        super.onPreExecute();
                }

                @Override
                protected void onPostExecute(Void result) {
                        Message mMSG = Message.obtain();
                        mMSG.what = 3;
                        mMainUIHandler.sendMessage(mMSG);
                }

        }
}