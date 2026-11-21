package wlv.ac.uk.myblog.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates and opens the local SQLite database used to store blog messages.
 * All data stays on the device. No cloud storage is used.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "myblog.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names, kept in one place so they are not mistyped elsewhere
    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_BODY = "body";
    public static final String COL_IMAGE_PATH = "image_path";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at";
    public static final String COL_UPLOADED = "uploaded";

    private static final String SQL_CREATE_MESSAGES =
            "CREATE TABLE " + TABLE_MESSAGES + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_TITLE + " TEXT NOT NULL, "
                    + COL_BODY + " TEXT NOT NULL, "
                    + COL_IMAGE_PATH + " TEXT, "
                    + COL_CREATED_AT + " INTEGER NOT NULL, "
                    + COL_UPDATED_AT + " INTEGER NOT NULL, "
                    + COL_UPLOADED + " INTEGER NOT NULL DEFAULT 0)";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /** Called the first time the database is created on the device. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MESSAGES);
    }

    /**
     * Called when DATABASE_VERSION is increased.
     * While the app is in development we simply drop and recreate the table.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }
}
