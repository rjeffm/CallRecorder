package com.jlcsoftware.callrecorder;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.jlcsoftware.database.Database;
import com.jlcsoftware.database.Whitelist;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class WhitelistFragment extends Fragment implements MyWhitelistItemRecyclerViewAdapter.OnListInteractionListener {

    private static final String ARG_COLUMN_COUNT = "ARG_COLUMN_COUNT";
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WhitelistFragment() {
    }

    public static WhitelistFragment newInstance(int columnCount) {
        WhitelistFragment fragment = new WhitelistFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    MyWhitelistItemRecyclerViewAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist_item_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            recyclerView.setAdapter(adapter = new MyWhitelistItemRecyclerViewAdapter(getContext(), this));
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(WhitelistRecord[] item);
    }


    @Override
    public void onListInteraction(WhitelistRecord[] item) {
        mListener.onListFragmentInteraction(item);
    }

    @Override
    public boolean onListLongClick(View v, final WhitelistRecord record) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_whitelist_popup, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_delete) {
                    removeSelectedContacts();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
        return true; // handled
    }


    public void removeSelectedContacts() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.delete_whitelist_title);
        alert.setMessage(R.string.delete_whitelist_subject);
        alert.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {


                    @Override
                    protected Void doInBackground(Void... params) {
                        Database whitelistDB = Database.getInstance(getContext());

                        WhitelistRecord[] records = adapter.getSelectedRecords();
                        for (WhitelistRecord record : records) {
                            int id = record.whitelist.getId();
                            whitelistDB.removeWhiteList(id);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        // need to run this on main thread....
                        refresh();
                    }
                };
                asyncTask.execute();
                dialog.dismiss();
            }

        });
        alert.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        alert.show();

    }

    public void addContact() {
        ListView listView = new ListView(getContext());
        final MyContactsAdapter adapter = new MyContactsAdapter(getContext());
        listView.setAdapter(adapter);


        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.add_contact);
        builder.setView(listView);

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });

        final Dialog dialog = builder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MyContactsAdapter.ContactRecord contactRecord = (MyContactsAdapter.ContactRecord) adapter.getItem(position);
                addContact(contactRecord.contactId);
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    public void addContact(String contactId) {
        Whitelist whitelist = new Whitelist();
        whitelist.setContactId(contactId);
        Database.getInstance(getContext()).addWhitelist(whitelist);
        refresh();
    }


    private void refresh() {
        adapter.refresh();
    }

}
