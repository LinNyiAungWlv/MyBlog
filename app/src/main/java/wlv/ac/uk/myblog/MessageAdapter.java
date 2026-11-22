package wlv.ac.uk.myblog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import wlv.ac.uk.myblog.model.Message;

/**
 * Shows the list of messages in the RecyclerView on the main screen.
 * Each message is displayed using the item_message.xml layout.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    /** Lets MainActivity know when a message in the list is tapped. */
    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    private final List<Message> messages = new ArrayList<>();
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
        holder.itemView.setOnClickListener(v -> listener.onMessageClick(message));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /** Holds the views for one row so they don't have to be looked up every time. */
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textBody;
        final TextView textDate;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textBody = itemView.findViewById(R.id.textBody);
            textDate = itemView.findViewById(R.id.textDate);
        }
    }
}
