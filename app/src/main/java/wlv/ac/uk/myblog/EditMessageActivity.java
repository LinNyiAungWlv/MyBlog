package wlv.ac.uk.myblog;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import wlv.ac.uk.myblog.data.MessageRepository;
import wlv.ac.uk.myblog.model.Message;

/**
 * Screen for writing a new blog message.
 * Everything is saved to the local SQLite database, so it works offline.
 */
public class EditMessageActivity extends AppCompatActivity {

    private MessageRepository repository;
    private TextInputLayout layoutTitle;
    private TextInputLayout layoutBody;
    private TextInputEditText editTitle;
    private TextInputEditText editBody;

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

        findViewById(R.id.buttonSave).setOnClickListener(v -> saveMessage());
    }

    /** Back arrow in the toolbar closes this screen without saving. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Checks the input, saves the message to the database and closes the screen. */
    private void saveMessage() {
        String title = textOf(editTitle);
        String body = textOf(editBody);

        if (!isInputValid(title, body)) {
            return;
        }

        long id = repository.insert(new Message(title, body, null));
        if (id == -1) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.message_saved, Toast.LENGTH_SHORT).show();
        finish();   // go back to the list, which reloads in onResume()
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
