package com.jlcsoftware.callrecorder;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeff on 13-May-16.
 */
public class MyContactsAdapter extends BaseAdapter implements Handler.Callback {
        private List<ContactRecord> mValues;
        private Context context;

        private Handler handler;

        @Override
        public boolean handleMessage(Message msg) {
            // Add to the values
            if (null != msg.obj) {
                mValues.add((ContactRecord) msg.obj);
                notifyDataSetChanged();
            }
            return false;
        }

        public MyContactsAdapter(Context context) {
            handler = new Handler(this); // Attach to this thread
            this.context = context;
            loadAdapter(context);
        }

        public class ContactRecord {
            String name;
            Drawable image;
            String contactId;
        }

        private void loadAdapter(final Context context) {
            mValues = new ArrayList<ContactRecord>();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    InputStream input = null;

                    // query time
                    Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

                    while (cursor.moveToNext()) {
                        if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                            ContactRecord contactRecord = new ContactRecord();
                            // Get values from contacts database:
                            contactRecord.name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            contactRecord.contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                            // Get photo of contactId as input stream:
                            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactRecord.contactId));
                            input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
                            if (null != input) {
                                BitmapDrawable drawable = new BitmapDrawable(context.getResources(), input);
                                contactRecord.image = drawable;
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    contactRecord.image = context.getResources().getDrawable(R.drawable.ic_user, null);
                                } else {
                                    contactRecord.image = context.getResources().getDrawable(R.drawable.ic_user);
                                }
                            }

                            // Send to the main thread...
                            Message msg = Message.obtain();
                            msg.obj = contactRecord;
                            handler.sendMessage(msg);
                        }
                    }
                }

            };
            new Thread(runnable).start(); // run on a different thread
        }


        @Override
        public int getCount() {
            return mValues.size();
        }

        @Override
        public Object getItem(int position) {
            return mValues.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ContactRecord record = mValues.get(position);
            if (null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
            imageView.setImageDrawable(record.image);
            TextView textView = (TextView) convertView.findViewById(R.id.textView);
            textView.setText(record.name);
            return convertView;
        }
    }
