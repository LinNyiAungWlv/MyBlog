package wlv.ac.uk.myblog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import wlv.ac.uk.myblog.data.MessageRepository;
import wlv.ac.uk.myblog.model.Message;

/**
 * Main screen. Shows the list of saved messages, newest first.
 */
public class MainActivity extends AppCompatActivity {

    private MessageRepository repository;
    private MessageAdapter adapter;
    private RecyclerView recyclerMessages;
    private TextView textEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        repository = new MessageRepository(this);

        recyclerMessages = findViewById(R.id.recyclerMessages);
        textEmpty = findViewById(R.id.textEmpty);

        // Tapping a message opens it in the detail screen
        adapter = new MessageAdapter(message -> {
            Intent intent = new Intent(this, MessageDetailActivity.class);
            intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_ID, message.getId());
            startActivity(intent);
        });
        recyclerMessages.setAdapter(adapter);

        // + button opens the editor to write a new message
        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, EditMessageActivity.class)));
    }

    /**
     * Reload the list every time this screen comes back into view,
     * so new or edited messages show up straight away.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    private void loadMessages() {
        List<Message> messages = repository.getAll();
        adapter.setMessages(messages);

        // Show the "No messages yet" text when the list is empty
        boolean isEmpty = messages.isEmpty();
        textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerMessages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
