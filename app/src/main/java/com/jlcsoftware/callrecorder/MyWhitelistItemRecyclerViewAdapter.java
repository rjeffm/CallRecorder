package com.jlcsoftware.callrecorder;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jlcsoftware.callrecorder.WhitelistFragment.OnListFragmentInteractionListener;
import com.jlcsoftware.database.Database;
import com.jlcsoftware.database.Whitelist;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RecyclerView.Adapter} that can display a {@link WhitelistRecord} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class MyWhitelistItemRecyclerViewAdapter extends RecyclerView.Adapter<MyWhitelistItemRecyclerViewAdapter.ViewHolder> implements Handler.Callback {

    public interface OnListInteractionListener {
        void onListInteraction(WhitelistRecord[] items);
        boolean onListLongClick(View v,WhitelistRecord item);
    }

    private static int WHAT_NOTIFY_CHANGES = 1;

    private List<WhitelistRecord> mValues;
    private final OnListInteractionListener mListener;
    private SparseBooleanArray selectedItems;

    public boolean isSelected(int pos){
        return selectedItems.get(pos, false);
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        }
        else {
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items =
                new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }
    private Handler handler;

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == WHAT_NOTIFY_CHANGES) {
            // Add to the values
            if (null != msg.obj) {
                mValues.add((WhitelistRecord) msg.obj);
            }
            notifyDataSetChanged();
        }
        return false;
    }

    Context context;

    public MyWhitelistItemRecyclerViewAdapter(Context context, OnListInteractionListener listener) {
        handler = new Handler(this);
        selectedItems = new SparseBooleanArray();
        mListener = listener;
        this.context = context;
        loadAdapter(context);
    }

    private void loadAdapter(final Context context) {
        mValues = new ArrayList<WhitelistRecord>();
        // Using a Runnable and a separate Thread with Message on the UI thread
        // is a nice way to make the list display more interactive
        // no need for a progress indicator, since the user can see the list populating
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final ArrayList<Whitelist> all = Database.getInstance(context).getAllWhitelist();
                for (Whitelist whitelist : all) {
                    WhitelistRecord record = new WhitelistRecord(whitelist);
                    resolveContactInfo(context, record);
                    // Send to the main thread...
                    Message msg = Message.obtain();
                    msg.what = WHAT_NOTIFY_CHANGES;
                    msg.obj = record;
                    handler.sendMessage(msg);
                }
                if(all.size()==0){
                    Message msg = Message.obtain();
                    msg.what = WHAT_NOTIFY_CHANGES;
                    msg.obj = null;
                    handler.sendMessage(msg);
                }
            }
        };
        new Thread(runnable).start();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_whitelist_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = mValues.get(position);
        holder.mImageView.setImageDrawable(mValues.get(position).getImage());
        holder.mNameView.setText(mValues.get(position).getName());


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    toggleSelection(position);
                    holder.itemView.setSelected(selectedItems.get(position, false));
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListInteraction(getSelectedRecords());
                }
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (null != mListener) {
                    if(!isSelected(position)) toggleSelection(position);
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    return mListener.onListLongClick(v,holder.mItem);
                }
                return false;
            }
        });
        // Selection State
        holder.itemView.setSelected(selectedItems.get(position, false));
    }

    public WhitelistRecord[] getSelectedRecords() {
        ArrayList<WhitelistRecord> selected = new ArrayList<>();
        List<Integer> items = getSelectedItems();
        for(Integer pos : items){
            selected.add(mValues.get(pos));
        }
        return selected.toArray(new WhitelistRecord[selected.size()]);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mImageView;
        public final TextView mNameView;
        public WhitelistRecord mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.imageView);
            mNameView = (TextView) view.findViewById(R.id.textView);

        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }

    /**
     * Get the contact info details from the Contacts Content Provider
     * @param context
     * @param record
     */
    private void resolveContactInfo(Context context, WhitelistRecord record) {
        String name = null;
        InputStream input = null;

        // define the columns the query should return
        String[] projection = new String[]{ContactsContract.Contacts.DISPLAY_NAME};
        // encode the phone number and build the filter URI
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(record.getContactId()));
        // query time
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor.moveToFirst()) {
            // Get values from contacts database:
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            record.setName(name);

            // Get photo of contactId as input stream:
            uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(record.getContactId()));
            input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);

            if (null != input) {
                BitmapDrawable drawable = new BitmapDrawable(context.getResources(), input);
                record.setImage(drawable);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    record.setImage(context.getResources().getDrawable(R.drawable.ic_user, null));
                } else {
                    record.setImage(context.getResources().getDrawable(R.drawable.ic_user));
                }
            }
        }
    }

    /**
     * Something changed, reload the adapter
     */
    public void refresh() {
        clearSelections();
        loadAdapter(context);
    }

}
