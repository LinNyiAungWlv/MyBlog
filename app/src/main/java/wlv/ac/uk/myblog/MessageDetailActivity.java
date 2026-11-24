package wlv.ac.uk.myblog;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import wlv.ac.uk.myblog.data.ImageStorage;
import wlv.ac.uk.myblog.data.MessageRepository;
import wlv.ac.uk.myblog.model.Message;

/**
 * Shows a single message in full.
 * The id of the message to show is passed in the Intent (EXTRA_MESSAGE_ID).
 */
public class MessageDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ID = "message_id";

    private MessageRepository repository;
    private long messageId;
    private TextView textTitle;
    private TextView textDate;
    private TextView textBody;
    private ImageView imageMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_message_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        repository = new MessageRepository(this);
        messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1);

        textTitle = findViewById(R.id.textTitle);
        textDate = findViewById(R.id.textDate);
        textBody = findViewById(R.id.textBody);
        imageMessage = findViewById(R.id.imageMessage);
    }

    /** Load the message each time the screen is shown, so it is always up to date. */
    @Override
    protected void onResume() {
        super.onResume();
        showMessage();
    }

    private void showMessage() {
        Message message = repository.getById(messageId);
        if (message == null) {
            // Message no longer exists (e.g. it was deleted), so close the screen
            Toast.makeText(this, R.string.message_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textTitle.setText(message.getTitle());
        textBody.setText(message.getBody());

        // Show the attached image, or hide the image area if there is none
        Bitmap bitmap = ImageStorage.loadImage(message.getImagePath());
        imageMessage.setImageBitmap(bitmap);
        imageMessage.setVisibility(bitmap != null ? View.VISIBLE : View.GONE);
        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(message.getUpdatedAt()));
        textDate.setText(date);
    }

    /** Adds the Edit and Delete actions to the toolbar. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_share) {
            shareMessage();
            return true;
        } else if (itemId == R.id.action_edit) {
            // Open the editor with this message loaded
            Intent intent = new Intent(this, EditMessageActivity.class);
            intent.putExtra(EditMessageActivity.EXTRA_MESSAGE_ID, messageId);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shares the message (title, text and image) using Android's standard Share sheet,
     * so the user can send it by email or any other app they choose.
     */
    private void shareMessage() {
        Message message = repository.getById(messageId);
        if (message == null) {
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.putExtra(Intent.EXTRA_SUBJECT, message.getTitle());   // email subject
        send.putExtra(Intent.EXTRA_TEXT, message.getTitle() + "\n\n" + message.getBody());

        if (message.hasImage() && new File(message.getImagePath()).exists()) {
            // Attach the image through our FileProvider and let the receiving app read it
            Uri imageUri = ImageStorage.getShareUri(this, message.getImagePath());
            send.setType("image/jpeg");
            send.putExtra(Intent.EXTRA_STREAM, imageUri);
            send.setClipData(ClipData.newRawUri(null, imageUri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            send.setType("text/plain");
        }

        startActivity(Intent.createChooser(send, getString(R.string.share_via)));
    }

    /** Asks the user to confirm before deleting, because it cannot be undone. */
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_message_title)
                .setMessage(R.string.delete_message_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteMessage())
                .show();
    }

    private void deleteMessage() {
        if (repository.delete(messageId)) {
            Toast.makeText(this, R.string.message_deleted, Toast.LENGTH_SHORT).show();
            finish();   // back to the list, which reloads without this message
        } else {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** Back arrow in the toolbar returns to the list. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
