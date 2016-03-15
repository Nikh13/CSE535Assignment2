package db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cse535.mobilecomputing.assignment2.MainActivity;
import cse535.mobilecomputing.assignment2.Record;

/**
 * Created by Nikhil on 2/27/16.
 */
public class DownloadDatabaseHelper extends SQLiteOpenHelper {

    private static DownloadDatabaseHelper sInstance;
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = DatabaseHelper.DATABASE_NAME;
    public final static String DATABASE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private static SQLiteDatabase sqliteDataBase;
    static String tableName = null;

    synchronized public static DownloadDatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        tableName = MainActivity.getStoredTableName(context);
        boolean dbExists = checkDataBase();
        if(!dbExists || tableName==null)
            return null;
        if (sInstance == null) {
            sInstance = new DownloadDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }


    public DownloadDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        openDataBase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void openDataBase() throws SQLException {
        //Open the database
        String myPath = DATABASE_PATH + DATABASE_NAME;
        sqliteDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public List<Record> getMostRecentRecords() {
        List<Record> recentRecordList = new ArrayList<Record>();
        String query = "SELECT ts,xval,yval,zval FROM " + tableName + " order by ts desc limit 10";

        Cursor cursor = sqliteDataBase.rawQuery(query, null);

        if (cursor.moveToLast()) {
            do {
                Record record = new Record(Integer.parseInt(cursor.getString(0)), Float.parseFloat(cursor.getString(1)),
                        Float.parseFloat(cursor.getString(2)), Float.parseFloat(cursor.getString(3)));
                recentRecordList.add(record);
            } while (cursor.moveToPrevious());
        }
        return recentRecordList;
    }

    private static boolean checkDataBase(){
        File databaseFile = new File(DATABASE_PATH + DATABASE_NAME);
        return databaseFile.exists();
    }

}
