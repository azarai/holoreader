package de.hdodenhof.feedreader.tasks;

import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.controllers.RSSController;
import de.hdodenhof.feedreader.handlers.ArticleHandler;
import de.hdodenhof.feedreader.helpers.SAXHelper;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class RefreshFeedsTask extends AsyncTask<Void, Integer, Void> {

        @SuppressWarnings("unused")
        private static final String TAG = RefreshFeedsTask.class.getSimpleName();               
        
        private Handler mMainUIHandler;
        private Context mContext;

        public RefreshFeedsTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
        }

        @SuppressWarnings("unchecked")
        protected Void doInBackground(Void... params) {

                ArrayList<Article> mArticles = new ArrayList<Article>();
                ArrayList<Feed> mFeeds = new ArrayList<Feed>();
                RSSController mController = new RSSController(mContext);

                try {

                        int n = 0;
                        mFeeds = mController.getFeeds();
                        for (Feed mFeed : mFeeds) {
                                n++;
                                SAXHelper mSAXHelper = new SAXHelper(mFeed.getUrl(), new ArticleHandler());
                                mArticles = (ArrayList<Article>) mSAXHelper.parse();

                                for (Article mArticle : mArticles) {
                                        mArticle.setFeedId(mFeed.getId());
                                }
                                mController.createOrUpdateArticles(mArticles);
                                
                                mFeed.setUpdated(new Date());
                                mController.updateFeed(mFeed);
                                
                                publishProgress(n);
                        }

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
