package com.jlcsoftware.callrecorder;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdView;
import com.jlcsoftware.receivers.MyLocalBroadcastReceiver;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class RecordingFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "ARG_COLUMN_COUNT";
    private static final String ARG_TYPE = "ARG_TYPE";

    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecordingFragment() {
    }

    public enum SORT_TYPE {
        ALL,
        INCOMING,
        OUTGOING
    }

    public static RecordingFragment newInstance(int columnCount, SORT_TYPE type) {
        RecordingFragment fragment = new RecordingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putString(ARG_TYPE, type.name());
        fragment.setArguments(args);
        return fragment;
    }

    SORT_TYPE type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            type = SORT_TYPE.valueOf(getArguments().getString(ARG_TYPE));
        }
    }

    MyRecordingRecyclerViewAdapter adapter;
    MyLocalBroadcastReceiver newRecordingReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recording_list, container, false);


        // Set the adapter
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        Context context = view.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        adapter = new MyRecordingRecyclerViewAdapter(context, type, mListener);
        newRecordingReceiver = new MyLocalBroadcastReceiver(adapter);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(newRecordingReceiver,
                new IntentFilter(LocalBroadcastActions.NEW_RECORDING_BROADCAST));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(newRecordingReceiver,
                new IntentFilter(LocalBroadcastActions.RECORDING_DELETED_BROADCAST));

        recyclerView.setAdapter(adapter);
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (null != mListener) mListener.onListFragmentInteraction(new PhoneCallRecord[0]);
                return false;
            }
        });
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != newRecordingReceiver)
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(
                    newRecordingReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
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
        void onListFragmentInteraction(PhoneCallRecord items[]);

        void onItemPlay(PhoneCallRecord item);

        boolean onListItemLongClick(View v, PhoneCallRecord selectedItem, PhoneCallRecord items[]);
    }

}
