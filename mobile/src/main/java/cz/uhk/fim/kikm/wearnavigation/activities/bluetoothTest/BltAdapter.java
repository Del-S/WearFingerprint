package cz.uhk.fim.kikm.wearnavigation.activities.bluetoothTest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.R;

public class BltAdapter extends RecyclerView.Adapter {

    // Layout variables
    private LayoutInflater mInflater;
    // Messages list to display
    private List<String> mMessages = new ArrayList<>();

    public BltAdapter(Context context) {
        // Load inflater to create layout
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Creating single item view
        View view = mInflater.inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Get device and view to input data into
        final String message = mMessages.get(position);
        final MessageViewHolder messageViewHolder = (MessageViewHolder) holder;

        // Set text to display
        messageViewHolder.text.setText(message);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public void addMessage(String message) {
        mMessages.add(message);
        notifyDataSetChanged();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder  {
        TextView text;

        MessageViewHolder(View itemView) {
            super(itemView);

            // Find item views
            text = itemView.findViewById(R.id.im_text);
        }
    }
}
