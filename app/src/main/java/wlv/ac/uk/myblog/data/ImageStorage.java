package wlv.ac.uk.myblog.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Saves attached photos inside the app's own storage (filesDir/images).
 * Only the file path is stored in SQLite, not the image itself.
 * Keeping our own copy means the message still has its image even if
 * the user deletes the original from their gallery.
 */
public class ImageStorage {

    private static final String IMAGE_FOLDER = "images";
    private static final int MAX_SIZE = 1600;   // longest side in pixels after shrinking
    private static final int JPEG_QUALITY = 85;

    private ImageStorage() {
        // only static methods
    }

    /**
     * Copies an image (from the gallery or camera) into app storage.
     * Large photos are shrunk and turned the right way up.
     *
     * @return the absolute path of the saved file
     */
    public static String saveImage(Context context, Uri source) throws IOException {
        // 1. Read only the image size first, to work out how much to shrink it
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            BitmapFactory.decodeStream(in, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new IOException("Not a valid image");
        }

        // 2. Load a smaller version of the image to save memory
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Could not read image");
        }

        // 3. Camera photos are often stored sideways with a "rotate" tag, so apply it
        bitmap = rotateIfNeeded(context, source, bitmap);

        // 4. Save as a JPEG in our own folder
        File file = new File(getImageFolder(context), "img_" + System.currentTimeMillis() + ".jpg");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        }
        return file.getAbsolutePath();
    }

    /**
     * Returns a content:// address for a temporary file the camera app can write its photo into.
     * The same file is reused each time; saveImage() copies it into images/ afterwards.
     */
    public static Uri getCameraPhotoUri(Context context) {
        File folder = new File(context.getCacheDir(), "camera");
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        File file = new File(folder, "camera_photo.jpg");
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    /** Loads a saved image for display, or returns null if the file is missing. */
    public static Bitmap loadImage(String path) {
        if (path == null) {
            return null;
        }
        return BitmapFactory.decodeFile(path);
    }

    /** Deletes a saved image file. Does nothing if the path is null. */
    public static void deleteImage(String path) {
        if (path != null) {
            //noinspection ResultOfMethodCallIgnored
            new File(path).delete();
        }
    }

    private static File getImageFolder(Context context) {
        File folder = new File(context.getFilesDir(), IMAGE_FOLDER);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        return folder;
    }

    /** Picks a power of 2 (1, 2, 4, 8...) so the longest side ends up close to MAX_SIZE. */
    private static int calculateSampleSize(int width, int height) {
        int sampleSize = 1;
        while (Math.max(width, height) / (sampleSize * 2) >= MAX_SIZE) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static Bitmap rotateIfNeeded(Context context, Uri source, Bitmap bitmap) {
        int degrees = 0;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) degrees = 90;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) degrees = 180;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) degrees = 270;
            }
        } catch (IOException e) {
            // No rotation information, keep the image as it is
        }
        if (degrees == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
