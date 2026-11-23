package wlv.ac.uk.myblog;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.DateFormat;
import java.util.Date;

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
        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(message.getUpdatedAt()));
        textDate.setText(date);
    }

    /** Back arrow in the toolbar returns to the list. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
