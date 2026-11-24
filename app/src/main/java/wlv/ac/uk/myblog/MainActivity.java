package wlv.ac.uk.myblog;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import wlv.ac.uk.myblog.data.MessageRepository;
import wlv.ac.uk.myblog.model.Message;

/**
 * Main screen. Shows the list of saved messages, newest first,
 * and lets the user search them by title or text.
 */
public class MainActivity extends AppCompatActivity {

    private MessageRepository repository;
    private MessageAdapter adapter;
    private RecyclerView recyclerMessages;
    private TextView textEmpty;
    private String searchQuery = "";   // empty means "show all messages"

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

    /** Adds the search icon to the toolbar and listens for typed text. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));

        // Filter the list on every key press
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();   // hide the keyboard
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText.trim();
                loadMessages();
                return true;
            }
        });
        return true;
    }

    /**
     * Loads all messages, or only the matching ones if the user is searching,
     * and shows a message instead of the list when there is nothing to show.
     */
    private void loadMessages() {
        boolean searching = !searchQuery.isEmpty();
        List<Message> messages = searching
                ? repository.search(searchQuery)
                : repository.getAll();
        adapter.setMessages(messages);

        boolean isEmpty = messages.isEmpty();
        if (isEmpty) {
            textEmpty.setText(searching
                    ? getString(R.string.no_search_results, searchQuery)
                    : getString(R.string.empty_messages));
        }
        textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerMessages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
