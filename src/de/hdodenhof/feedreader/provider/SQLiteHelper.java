package de.hdodenhof.feedreader.provider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class SQLiteHelper extends SQLiteOpenHelper {

    @SuppressWarnings("unused")
    private static final String TAG = SQLiteHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "feedreader";
    private static final int DATABASE_VERSION = 7;

    private static final String FEED_TABLE_CREATE = "CREATE TABLE " + FeedDAO.TABLE + " (" + FeedDAO._ID + " integer primary key autoincrement, "
            + FeedDAO.NAME + " TEXT, " + FeedDAO.URL + " TEXT, " + FeedDAO.UPDATED + " TEXT);";

    private static final String ARTICLE_TABLE_CREATE = "CREATE TABLE " + ArticleDAO.TABLE + " (" + ArticleDAO._ID + " integer primary key autoincrement, "
            + ArticleDAO.FEEDID + " integer, " + ArticleDAO.GUID + " TEXT, " + ArticleDAO.PUBDATE + " TEXT, " + ArticleDAO.TITLE + " TEXT , "
            + ArticleDAO.SUMMARY + " TEXT, " + ArticleDAO.CONTENT + " TEXT, " + ArticleDAO.READ + " INTEGER);";

    private static final String FEED_VIEW_CREATE = "CREATE VIEW " + FeedDAO.VIEW + " AS SELECT " + FeedDAO.TABLE + "." + FeedDAO._ID + ", " + FeedDAO.TABLE
            + "." + FeedDAO.NAME + ", " + FeedDAO.TABLE + "." + FeedDAO.URL + ", " + FeedDAO.TABLE + "." + FeedDAO.UPDATED + ", COUNT(" + ArticleDAO.TABLE
            + "." + ArticleDAO._ID + ") AS " + FeedDAO.UNREAD + " FROM " + FeedDAO.TABLE + " LEFT OUTER JOIN " + ArticleDAO.TABLE + " ON " + ArticleDAO.TABLE
            + "." + ArticleDAO.FEEDID + " = " + FeedDAO.TABLE + "." + FeedDAO._ID + " AND " + ArticleDAO.TABLE + "." + ArticleDAO.READ + " = 0 GROUP BY "
            + FeedDAO.TABLE + "." + FeedDAO._ID + ", " + FeedDAO.TABLE + "." + FeedDAO.NAME + ", " + FeedDAO.TABLE + "." + FeedDAO.URL + ", " + FeedDAO.TABLE
            + "." + FeedDAO.UPDATED + ";";

    private static final String mDummydata[][] = { { "t3n News", "http://t3n.de/news/feed" },
            { "Gründerszene.de - Infos für Gründer, Unternehmer, StartUps | Gründerszene", "http://www.gruenderszene.de/feed/" },
            { "netzpolitik.org", "http://netzpolitik.org/feed" }, { "Android Developers Blog", "http://android-developers.blogspot.com/atom.xml" },
            { "mobiFlip.de", "http://feeds.feedburner.com/mobiFlip" } };

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(FEED_TABLE_CREATE);
        database.execSQL(ARTICLE_TABLE_CREATE);
        database.execSQL(FEED_VIEW_CREATE);

        for (String[] mData : mDummydata) {
            ContentValues mContentValues = new ContentValues();
            mContentValues.put(FeedDAO.NAME, mData[0]);
            mContentValues.put(FeedDAO.URL, mData[1]);
            mContentValues.put(FeedDAO.UPDATED, SQLiteHelper.fromDate(new Date()));

            database.insert(FeedDAO.TABLE, null, mContentValues);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FeedDAO.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ArticleDAO.TABLE);
        db.execSQL("DROP VIEW IF EXISTS " + FeedDAO.VIEW);
        onCreate(db);
    }

    public class FeedDAO {

        public static final String TABLE = "feeds";
        public static final String _ID = "_id";
        public static final String NAME = "name";
        public static final String URL = "url";
        public static final String UPDATED = "updated";
        public static final String UNREAD = "unread";

        public static final String VIEW = "feeds_view";

    }

    public class ArticleDAO {

        public static final String TABLE = "articles";
        public static final String _ID = "_id";
        public static final String FEEDID = "feedid";
        public static final String GUID = "guid";
        public static final String PUBDATE = "pubdate";
        public static final String TITLE = "title";
        public static final String SUMMARY = "summary";
        public static final String CONTENT = "content";
        public static final String READ = "read";

    }

    public static String fromDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static Date toDate(String date) {
        try {
            return DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }

    }

    public static int fromBoolean(boolean bool) {
        if (bool == true) {
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean toBoolean(int integer) {
        if (integer == 1) {
            return true;
        } else {
            return false;
        }
    }

}
