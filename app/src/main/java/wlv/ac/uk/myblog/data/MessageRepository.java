package wlv.ac.uk.myblog.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import wlv.ac.uk.myblog.model.Message;

/**
 * All database operations for messages go through this class.
 * The screens never write SQL themselves. They just call these methods.
 */
public class MessageRepository {

    // Newest edited messages appear first in the list
    private static final String ORDER_NEWEST_FIRST = DbHelper.COL_UPDATED_AT + " DESC";

    private final DbHelper dbHelper;

    public MessageRepository(Context context) {
        // Use the application context so we don't keep an Activity alive by accident
        dbHelper = new DbHelper(context.getApplicationContext());
    }

    /**
     * Saves a new message and returns its new id (or -1 if it failed).
     * The id is also set on the message object.
     */
    public long insert(Message message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(DbHelper.TABLE_MESSAGES, null, toContentValues(message));
        message.setId(id);
        return id;
    }

    /**
     * Saves changes to an existing message. Updates the "updated at" time.
     * Returns true if a row was changed.
     */
    public boolean update(Message message) {
        message.setUpdatedAt(System.currentTimeMillis());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.update(DbHelper.TABLE_MESSAGES, toContentValues(message),
                DbHelper.COL_ID + " = ?", new String[]{String.valueOf(message.getId())});
        return rows > 0;
    }

    /**
     * Deletes one message by id, and its image file if it has one.
     * Returns true if it was deleted.
     */
    public boolean delete(long id) {
        Message message = getById(id);   // read first so we know which image to delete
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DbHelper.TABLE_MESSAGES,
                DbHelper.COL_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows > 0 && message != null) {
            ImageStorage.deleteImage(message.getImagePath());
        }
        return rows > 0;
    }

    /**
     * Deletes a group of selected messages in one transaction,
     * so either all of them are deleted or none are.
     * Their image files are deleted afterwards. Returns how many were deleted.
     */
    public int deleteMany(List<Long> ids) {
        // Remember the image files before the rows are gone
        List<String> imagePaths = new ArrayList<>();
        for (long id : ids) {
            Message message = getById(id);
            if (message != null && message.hasImage()) {
                imagePaths.add(message.getImagePath());
            }
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = 0;
        db.beginTransaction();
        try {
            for (long id : ids) {
                deleted += db.delete(DbHelper.TABLE_MESSAGES,
                        DbHelper.COL_ID + " = ?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // Only remove the files once the database delete has succeeded
        if (deleted > 0) {
            for (String path : imagePaths) {
                ImageStorage.deleteImage(path);
            }
        }
        return deleted;
    }

    /** Returns one message, or null if no message has that id. */
    public Message getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(DbHelper.TABLE_MESSAGES, null,
                DbHelper.COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        }
    }

    /** Returns every message, newest first. */
    public List<Message> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(DbHelper.TABLE_MESSAGES, null,
                null, null, null, null, ORDER_NEWEST_FIRST)) {
            return listFromCursor(cursor);
        }
    }

    /**
     * Returns messages whose title or body contains the search text (case-insensitive).
     * The text is passed as a "?" argument, not joined into the SQL string,
     * which protects against SQL injection.
     */
    public List<Message> search(String text) {
        String pattern = "%" + text.trim() + "%";
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(DbHelper.TABLE_MESSAGES, null,
                DbHelper.COL_TITLE + " LIKE ? OR " + DbHelper.COL_BODY + " LIKE ?",
                new String[]{pattern, pattern},
                null, null, ORDER_NEWEST_FIRST)) {
            return listFromCursor(cursor);
        }
    }

    /** Marks a message as uploaded to the online platform. */
    public void markUploaded(long id) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COL_UPLOADED, 1);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(DbHelper.TABLE_MESSAGES, values,
                DbHelper.COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ----- helpers to convert between Message objects and database rows -----

    private ContentValues toContentValues(Message message) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COL_TITLE, message.getTitle());
        values.put(DbHelper.COL_BODY, message.getBody());
        values.put(DbHelper.COL_IMAGE_PATH, message.getImagePath());
        values.put(DbHelper.COL_CREATED_AT, message.getCreatedAt());
        values.put(DbHelper.COL_UPDATED_AT, message.getUpdatedAt());
        values.put(DbHelper.COL_UPLOADED, message.isUploaded() ? 1 : 0);
        return values;
    }

    private Message fromCursor(Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COL_ID)));
        message.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_TITLE)));
        message.setBody(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_BODY)));
        message.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_IMAGE_PATH)));
        message.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COL_CREATED_AT)));
        message.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COL_UPDATED_AT)));
        message.setUploaded(cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COL_UPLOADED)) == 1);
        return message;
    }

    private List<Message> listFromCursor(Cursor cursor) {
        List<Message> messages = new ArrayList<>();
        while (cursor.moveToNext()) {
            messages.add(fromCursor(cursor));
        }
        return messages;
    }
}
