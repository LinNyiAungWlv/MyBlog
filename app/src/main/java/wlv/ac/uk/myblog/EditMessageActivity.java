package wlv.ac.uk.myblog;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

import wlv.ac.uk.myblog.data.ImageStorage;
import wlv.ac.uk.myblog.data.MessageRepository;
import wlv.ac.uk.myblog.model.Message;

/**
 * Screen for writing a new blog message or editing an existing one.
 * If EXTRA_MESSAGE_ID is passed in the Intent, that message is loaded for editing.
 * A photo can be attached from the gallery or taken with the camera.
 * Everything is saved to the local SQLite database, so it works offline.
 */
public class EditMessageActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ID = "message_id";
    private static final String STATE_IMAGE_PATH = "image_path";

    private MessageRepository repository;
    private Message existingMessage;   // null when creating a new message
    private TextInputLayout layoutTitle;
    private TextInputLayout layoutBody;
    private TextInputEditText editTitle;
    private TextInputEditText editBody;
    private ImageView imagePreview;
    private View buttonRemoveImage;

    private String originalImagePath;  // image the message had when the screen opened
    private String imagePath;          // image currently attached (may be new)
    private boolean saved = false;

    /** Opens the Android photo picker. No storage permission is needed for this. */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {   // null means the user cancelled
                    attachImage(uri);
                }
            });

    /**
     * Opens the camera app, which saves the photo into our temporary camera file.
     * No CAMERA permission is needed because the camera app takes the photo, not us.
     */
    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {   // false means the user cancelled
                    attachImage(ImageStorage.getCameraPhotoUri(this));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_message);
        // Keep content clear of the status bar, navigation bar and the on-screen keyboard
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Toolbar with a back arrow that closes this screen
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repository = new MessageRepository(this);

        layoutTitle = findViewById(R.id.layoutTitle);
        layoutBody = findViewById(R.id.layoutBody);
        editTitle = findViewById(R.id.editTitle);
        editBody = findViewById(R.id.editBody);
        imagePreview = findViewById(R.id.imagePreview);
        buttonRemoveImage = findViewById(R.id.buttonRemoveImage);

        // Edit mode: load the existing message
        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1);
        if (messageId != -1) {
            existingMessage = repository.getById(messageId);
            if (existingMessage == null) {
                Toast.makeText(this, R.string.message_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            toolbar.setTitle(R.string.edit_message);
            originalImagePath = existingMessage.getImagePath();
            // Only fill the fields the first time. After a screen rotation
            // Android restores whatever the user had typed.
            if (savedInstanceState == null) {
                editTitle.setText(existingMessage.getTitle());
                editBody.setText(existingMessage.getBody());
            }
        }

        // After a rotation, keep the image that was attached; otherwise start with the original
        imagePath = savedInstanceState != null
                ? savedInstanceState.getString(STATE_IMAGE_PATH)
                : originalImagePath;
        showImagePreview();

        findViewById(R.id.buttonGallery).setOnClickListener(v ->
                pickImage.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        findViewById(R.id.buttonCamera).setOnClickListener(v -> openCamera());
        buttonRemoveImage.setOnClickListener(v -> replaceImage(null));
        findViewById(R.id.buttonSave).setOnClickListener(v -> saveMessage());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_IMAGE_PATH, imagePath);
    }

    /** Back arrow in the toolbar closes this screen without saving. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * If the screen is closed without saving, delete any new image file
     * that was copied in, so it doesn't waste space.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && !saved && isNewImage(imagePath)) {
            ImageStorage.deleteImage(imagePath);
        }
    }

    // ----- image handling -----

    private void openCamera() {
        try {
            takePhoto.launch(ImageStorage.getCameraPhotoUri(this));
        } catch (ActivityNotFoundException e) {
            // Device has no camera app
            Toast.makeText(this, R.string.no_camera, Toast.LENGTH_SHORT).show();
        }
    }

    /** Copies the chosen image into app storage and shows it. */
    private void attachImage(Uri uri) {
        try {
            replaceImage(ImageStorage.saveImage(this, uri));
        } catch (IOException e) {
            Toast.makeText(this, R.string.image_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Swaps the attached image (null = no image).
     * A new image picked earlier on this screen is deleted straight away;
     * the original image is only deleted once the message is saved.
     */
    private void replaceImage(String newPath) {
        if (isNewImage(imagePath)) {
            ImageStorage.deleteImage(imagePath);
        }
        imagePath = newPath;
        showImagePreview();
    }

    /** True if the path is an image added on this screen (not the message's original image). */
    private boolean isNewImage(String path) {
        return path != null && !path.equals(originalImagePath);
    }

    private void showImagePreview() {
        Bitmap bitmap = ImageStorage.loadImage(imagePath);
        if (bitmap != null) {
            imagePreview.setImageBitmap(bitmap);
            imagePreview.setVisibility(View.VISIBLE);
            buttonRemoveImage.setVisibility(View.VISIBLE);
        } else {
            imagePreview.setImageDrawable(null);
            imagePreview.setVisibility(View.GONE);
            buttonRemoveImage.setVisibility(View.GONE);
        }
    }

    // ----- saving -----

    /** Checks the input, saves the message to the database and closes the screen. */
    private void saveMessage() {
        String title = textOf(editTitle);
        String body = textOf(editBody);

        if (!isInputValid(title, body)) {
            return;
        }

        boolean ok;
        if (existingMessage == null) {
            // Create mode: add a new row
            ok = repository.insert(new Message(title, body, imagePath)) != -1;
        } else {
            // Edit mode: change the existing row
            existingMessage.setTitle(title);
            existingMessage.setBody(body);
            existingMessage.setImagePath(imagePath);
            ok = repository.update(existingMessage);
        }

        if (!ok) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        // The old image was replaced or removed, so its file is no longer needed
        if (originalImagePath != null && !originalImagePath.equals(imagePath)) {
            ImageStorage.deleteImage(originalImagePath);
        }

        saved = true;
        Toast.makeText(this, R.string.message_saved, Toast.LENGTH_SHORT).show();
        finish();   // go back to the previous screen, which reloads in onResume()
    }

    /**
     * Title and body must not be empty (spaces only also count as empty).
     * Shows an error under each empty field.
     */
    private boolean isInputValid(String title, String body) {
        boolean valid = true;

        if (title.isEmpty()) {
            layoutTitle.setError(getString(R.string.error_title_required));
            valid = false;
        } else {
            layoutTitle.setError(null);
        }

        if (body.isEmpty()) {
            layoutBody.setError(getString(R.string.error_body_required));
            valid = false;
        } else {
            layoutBody.setError(null);
        }

        return valid;
    }

    /** Returns the trimmed text of an input field, never null. */
    private String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
