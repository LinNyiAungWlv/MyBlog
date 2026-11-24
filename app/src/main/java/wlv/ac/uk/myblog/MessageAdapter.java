package wlv.ac.uk.myblog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import wlv.ac.uk.myblog.model.Message;

/**
 * Shows the list of messages in the RecyclerView on the main screen.
 * Each message is displayed using the item_message.xml layout.
 * Also remembers which messages are selected when the user is choosing a group to delete.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    /** Lets MainActivity know when a message in the list is tapped or long-pressed. */
    public interface OnMessageClickListener {
        void onMessageClick(Message message);

        void onMessageLongClick(Message message);
    }

    private final List<Message> messages = new ArrayList<>();
    private final Set<Long> selectedIds = new LinkedHashSet<>();   // ids of selected messages
    private final OnMessageClickListener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public MessageAdapter(OnMessageClickListener listener) {
        this.listener = listener;
    }

    /** Replaces the messages being shown and redraws the list. */
    public void setMessages(List<Message> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    // ----- selection (for deleting a group of messages) -----

    /** Selects the message if it was not selected, or unselects it if it was. */
    public void toggleSelection(long id) {
        if (!selectedIds.remove(id)) {
            selectedIds.add(id);
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    // ----- RecyclerView methods -----

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.textTitle.setText(message.getTitle());
        holder.textBody.setText(message.getBody());
        holder.textDate.setText(dateFormat.format(new Date(message.getUpdatedAt())));

        // Selected cards show a tick and a coloured outline
        holder.card.setChecked(selectedIds.contains(message.getId()));

        holder.itemView.setOnClickListener(v -> listener.onMessageClick(message));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onMessageLongClick(message);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /** Holds the views for one row so they don't have to be looked up every time. */
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView textTitle;
        final TextView textBody;
        final TextView textDate;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            textTitle = itemView.findViewById(R.id.textTitle);
            textBody = itemView.findViewById(R.id.textBody);
            textDate = itemView.findViewById(R.id.textDate);
        }
    }
}
