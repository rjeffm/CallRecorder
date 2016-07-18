package com.jlcsoftware.callrecorder;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.jlcsoftware.callrecorder.RecordingFragment.OnListFragmentInteractionListener;
import com.jlcsoftware.database.CallLog;
import com.jlcsoftware.database.Database;
import com.jlcsoftware.receivers.MyLocalBroadcastReceiver;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link CallLog} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class MyRecordingRecyclerViewAdapter extends RecyclerView.Adapter<MyRecordingRecyclerViewAdapter.ViewHolder> implements Handler.Callback, MyLocalBroadcastReceiver.OnNewRecordingListener {

    private List<PhoneCallRecord> mValues;
    private final OnListFragmentInteractionListener mListener;
    private SparseBooleanArray selectedItems;

    // Just for kicks... Communication to this thread is handled using a Handler so I can play with it...

    private static int WHAT_NOTIFY_CHANGES = 1;

    private Handler handler;

    @Override
    public boolean handleMessage(Message msg) {
        // Add to the values
        if (null != msg.obj) {
            mValues.add((PhoneCallRecord) msg.obj);
        }
        if (msg.what == WHAT_NOTIFY_CHANGES) {
            notifyDataSetChanged();
        }
        return false;
    }

    private final Drawable inImage;
    private final Drawable outImage;
    private final Context context;
    RecordingFragment.SORT_TYPE type;

    public MyRecordingRecyclerViewAdapter(final Context context, RecordingFragment.SORT_TYPE type, OnListFragmentInteractionListener listener) {
        this.context = context;
        this.type = type;
        mListener = listener;
        handler = new Handler(this);
        selectedItems = new SparseBooleanArray();
        inImage = context.getResources().getDrawable(R.drawable.phone_in);
        outImage = context.getResources().getDrawable(R.drawable.phone_out);
        loadAdapter(context, true);
    }

    public PhoneCallRecord[] getSelectedRecords() {
        ArrayList<PhoneCallRecord> selected = new ArrayList<>();
        List<Integer> items = getSelectedItems();
        for (Integer pos : items) {
            selected.add(mValues.get(pos));
        }
        return selected.toArray(new PhoneCallRecord[selected.size()]);
    }


    public boolean isSelected(int pos) {
        return selectedItems.get(pos, false);
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
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

    private void loadAdapter(final Context context, final boolean eachChange) {
        mValues = new ArrayList<PhoneCallRecord>();
        selectedItems = new SparseBooleanArray();

        /* This COULD have been implemented with a AsyncTask, But I wanted to play with the Handler class
        * could also used a standard Java Thread (which I did just because) */
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final ArrayList<CallLog> allCalls;
                if (type == RecordingFragment.SORT_TYPE.ALL)
                    allCalls = Database.getInstance(context).getAllCalls();
                else
                    allCalls = Database.getInstance(context).getAllCalls(type == RecordingFragment.SORT_TYPE.OUTGOING);

                for (CallLog phoneCall : allCalls) {
                    PhoneCallRecord record = new PhoneCallRecord(phoneCall);
                    resolveContactInfo(context, record);
                    // Send to the main thread...
                    Message msg = Message.obtain();
                    if (eachChange) {
                        msg.what = WHAT_NOTIFY_CHANGES;
                    }
                    msg.obj = record;
                    handler.sendMessage(msg);
                }
                if (!eachChange || allCalls.size() == 0) {
                    // All done, show the changes....
                    Message msg = Message.obtain();
                    msg.what = WHAT_NOTIFY_CHANGES;
                    handler.sendMessage(msg);
                }

            }
        };
        new Thread(runnable).start();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = mValues.get(position);
        holder.mContactName.setText(holder.mItem.getName());

        if (holder.mItem.getPhoneCall().isKept()) {
            holder.mContactName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_lock_black_24dp,0);
        } else {
            holder.mContactName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        if (holder.mItem.getImage() != null)
            holder.mContactPic.setImageDrawable(holder.mItem.getImage());

        if (holder.mItem.getPhoneCall().isOutgoing()) {
            holder.mInOutImage.setImageDrawable(outImage);
        } else {
            holder.mInOutImage.setImageDrawable(inImage);
        }


        float time = holder.mItem.getPhoneCall().getEndTime().getTimeInMillis() - holder.mItem.getPhoneCall().getStartTime().getTimeInMillis();
        holder.mTimeView.setText(String.format("%.2f", ((time / 1000) / 60)));

        SimpleDateFormat dateFormat = new SimpleDateFormat();
        holder.mDateView.setText(dateFormat.format(holder.mItem.getPhoneCall().getStartTime().getTime()));
     /*   holder.mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_UP==event.getAction()){
                    toggleSelection(position);
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onListFragmentInteraction(holder.mItem);
                    }
                }
                return false;
            }});*/
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    toggleSelection(position);
                    holder.itemView.setSelected(selectedItems.get(position, false));
                    mListener.onListFragmentInteraction(getSelectedRecords());
                }
            }
        });

        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (null != mListener) {
                    if (!isSelected(position)) toggleSelection(position);
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    return mListener.onListItemLongClick(v, holder.mItem, getSelectedRecords());
                }
                return false;
            }
        });

        holder.mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onItemPlay(holder.mItem);
                }
            }
        });

        // Selection State
        holder.itemView.setSelected(selectedItems.get(position, false));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }


    @Override
    public void OnBroadcastReceived(Intent intent) {
        loadAdapter(context, false);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final CheckBox mCheckBox;
        public final ImageView mContactPic;
        public final TextView mContactName;
        public final TextView mTimeView;
        public final TextView mDateView;
        public final ImageButton mPlayBtn;
        public final ImageView mInOutImage;

        public PhoneCallRecord mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mCheckBox = (CheckBox) view.findViewById(R.id.checkBox);
            mContactPic = (ImageView) view.findViewById(R.id.imageView);
            mInOutImage = (ImageView) view.findViewById(R.id.imageView2);
            mContactName = (TextView) view.findViewById(R.id.textView);
            mTimeView = (TextView) view.findViewById(R.id.textView3);
            mDateView = (TextView) view.findViewById(R.id.textView4);
            mPlayBtn = (ImageButton) view.findViewById(R.id.imageButton2);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContactName.getText() + "'";
        }
    }


    private void resolveContactInfo(Context context, PhoneCallRecord record) {
        String name = null;
        String contactId = null;
        InputStream input = null;
        String phoneNumber = record.getPhoneCall().getPhoneNumber();
        if (null == phoneNumber || phoneNumber.isEmpty()) return;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return;

        try {
            // define the columns the query should return
            String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};
            // encode the phone number and build the filter URI
            Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            // query time
            Cursor cursor = context.getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor.moveToFirst()) {
                // Get values from contacts database:
                contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
                record.setContactId(contactId);
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                record.setName(name);

                // Already in the cache?
                if (null == record.getImage()) { // no...
                    // Get photo of contactId as input stream:
                    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
                    input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
                    if (null != input) {
                        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), input);
                        record.setImage(drawable);
                    }
                }
            }
        } catch (Exception e) {

        }
    }

}
