package db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cse535.mobilecomputing.assignment2.MainActivity;
import cse535.mobilecomputing.assignment2.Record;

/**
 * Created by Nikhil on 2/27/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper sInstance;
    private static final int DATABASE_VERSION = 1;
    String tableName = null;

    synchronized public static DatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }


    public DatabaseHelper(Context context) {
        super(context, MainActivity.tableName, null, DATABASE_VERSION);
        tableName = MainActivity.tableName;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + tableName + " (ts timestamp, xval float, yval float, zval float);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
    }

    public void addRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ts", record.timestamp);
        values.put("xval", record.xVal);
        values.put("yval", record.yVal);
        values.put("zval", record.zVal);

        db.insert(tableName, null, values);
    }

    public List<Record> getMostRecentRecords() {
        List<Record> recentRecordList = new ArrayList<Record>();
        String query = "SELECT ts,xval,yval,zval FROM " + tableName + " order by ts desc limit 10";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToLast()) {
            do {
                Record record = new Record(Integer.parseInt(cursor.getString(0)), Float.parseFloat(cursor.getString(1)),
                        Float.parseFloat(cursor.getString(2)), Float.parseFloat(cursor.getString(3)));
                recentRecordList.add(record);
            } while (cursor.moveToPrevious());
        }
        return recentRecordList;
    }

    static public int getTimeStamp() {
        Date date = new Date();
        // getTime() returns current time in milliseconds
        long time = date.getTime();
        // Passed the milliseconds to constructor of Timestamp class
        Timestamp ts = new Timestamp(time);
        int ts_int = (int) ts.getTime();
        return ts_int;

    }

}
