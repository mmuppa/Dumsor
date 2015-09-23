package dumsor.org.dumsor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class LocalData {
    /*
    Longitude ranges from -180 to +180 degrees.
    Latitude ranges from -90 to 90 degrees.
    4 decimal places of accuracy is plenty accurate.
    Therefore: the data points will be doubles with 4 decimal places.
     */

    public static final String db_name = "LOCALDATA.db";
    public static final String table_name = "LOCAL_MAP_DATA";
    public static final String key_lat = "lat";                     //Stored as REAL
    public static final String key_long = "lon";                   //Stored as REAL
    public static final String key_uid = "source";                  //Stored as TEXT
    public static final String key_datetime = "timestamp";          //"YYYY-MM-DD HH:MM:SS.SSS"
    public static final String key_power = "power";                 //Stored as INTEGER

    private Context ctxt;
    private Cursor crs;
    private DatabaseHelper my_helper;
    private SQLiteDatabase my_db;

    protected LocalData(Context context) {
        ctxt = context;
        my_helper = new DatabaseHelper(ctxt);
    }

    //places a new data point into the local data table
    protected void add_point(final String user_id, final long unix_datetime,
                             final double latitude, final double longitude,
                             final int power) {
        if (user_id != null && unix_datetime > 0)
        {
            my_helper = new DatabaseHelper(ctxt);
            my_db = my_helper.getWritableDatabase();

            ContentValues init_vals = new ContentValues();
            init_vals.put(key_uid, user_id);
            init_vals.put(key_datetime, unix_datetime);
            init_vals.put(key_long, longitude);
            init_vals.put(key_lat, latitude);
            init_vals.put(key_power, power);

            my_db.insert(table_name, null, init_vals);
            my_helper.close();
        }
    }

    protected boolean isTableEmpty() {
        my_helper = new DatabaseHelper(ctxt);
        my_db = my_helper.getReadableDatabase();
        crs = my_db.rawQuery("SELECT * FROM " + table_name, null);
        my_helper.close();
        return crs.moveToFirst();
    }

    protected void wipe_data() {
        my_helper = new DatabaseHelper(ctxt);
        my_db = my_helper.getWritableDatabase();
        my_db.execSQL("DELETE FROM " + table_name);
        my_helper.close();
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, db_name, null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            Log.i("Database", "Creating Database...");
            db.execSQL("CREATE TABLE IF NOT EXISTS LOCAL_MAP_DATA (" +
                    "lat REAL, " +
                    "lon REAL, " +
                    "source TEXT, " +
                    "timestamp TEXT, " +
                    "power INTEGER," +
                    "PRIMARY KEY (source, timestamp))");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //No changes to make so far...
        }

        /**
         * getTableAsString is used to get the entries in the database as a string
         * @param db database to connect to
         * @param start start point
         * @param end end point
         * @return return type of string
         */
        public String getTableAsString(SQLiteDatabase db, long start, long end) {
            Log.d("DB", "getTableAsString called");
            String tableString = String.format("Table %s:\n", table_name);
            Cursor allRows = db.rawQuery("SELECT * FROM " + table_name + " WHERE " +
                    key_datetime + " <= " + end + " AND " + key_datetime + " >= " + start, null);
            if (allRows.moveToFirst()) {
                String[] columnNames = allRows.getColumnNames();
                do {
                    for (String name : columnNames) {
                        tableString += String.format("%s: %s\n", name,
                                allRows.getString(allRows.getColumnIndex(name)));
                    }
                    tableString += "\n";

                } while (allRows.moveToNext());
            }

            return tableString;
        }
    }
}