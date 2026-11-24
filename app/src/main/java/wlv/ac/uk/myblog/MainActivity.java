package wlv.ac.uk.myblog;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import wlv.ac.uk.myblog.data.MessageRepository;
import wlv.ac.uk.myblog.model.Message;

/**
 * Main screen. Shows the list of saved messages, newest first,
 * and lets the user search them by title or text.
 * Long-pressing a message starts selection mode for deleting a group of messages.
 */
public class MainActivity extends AppCompatActivity {

    private MessageRepository repository;
    private MessageAdapter adapter;
    private RecyclerView recyclerMessages;
    private TextView textEmpty;
    private String searchQuery = "";   // empty means "show all messages"
    private ActionMode actionMode;     // not null while the user is selecting messages

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

        adapter = new MessageAdapter(new MessageAdapter.OnMessageClickListener() {
            @Override
            public void onMessageClick(Message message) {
                if (actionMode != null) {
                    // In selection mode a tap selects/unselects instead of opening
                    toggleSelection(message);
                } else {
                    openMessage(message);
                }
            }

            @Override
            public void onMessageLongClick(Message message) {
                // Long-press starts selection mode
                if (actionMode == null) {
                    actionMode = startSupportActionMode(selectionCallback);
                }
                toggleSelection(message);
            }
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

    private void openMessage(Message message) {
        Intent intent = new Intent(this, MessageDetailActivity.class);
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_ID, message.getId());
        startActivity(intent);
    }

    // ----- selecting and deleting a group of messages -----

    /** Selects or unselects one message and updates the "N selected" title. */
    private void toggleSelection(Message message) {
        adapter.toggleSelection(message.getId());
        int count = adapter.getSelectedCount();
        if (count == 0) {
            actionMode.finish();   // nothing selected any more, leave selection mode
        } else {
            actionMode.setTitle(getString(R.string.selected_count, count));
        }
    }

    /** Controls the selection bar that replaces the toolbar while messages are selected. */
    private final ActionMode.Callback selectionCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete_selected) {
                confirmDeleteSelected();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Called when the user taps the back arrow or after deleting
            adapter.clearSelection();
            actionMode = null;
        }
    };

    private void confirmDeleteSelected() {
        int count = adapter.getSelectedCount();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_selected_title)
                .setMessage(getString(R.string.delete_selected_confirm, count))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteSelected())
                .show();
    }

    private void deleteSelected() {
        List<Long> ids = adapter.getSelectedIds();
        int deleted = repository.deleteMany(ids);
        Toast.makeText(this, getString(R.string.messages_deleted, deleted), Toast.LENGTH_SHORT).show();
        actionMode.finish();
        loadMessages();
    }

    // ----- search -----

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
