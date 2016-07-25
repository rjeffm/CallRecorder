package com.jlcsoftware.callrecorder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.MediaController;
import android.widget.Toast;

import com.jlcsoftware.database.CallLog;
import com.jlcsoftware.database.Database;
import com.jlcsoftware.helpers.AboutDialog;
import com.jlcsoftware.helpers.RateMeNowDialog;

import java.io.File;
import java.util.ArrayList;


/**
 * Our MAIN Activity - MAIN LAUNCHER
 *
 */

public class MainActivity extends AppCompatActivity implements RecordingFragment.OnListFragmentInteractionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaController.MediaPlayerControl {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private Handler handler = new Handler();

    boolean isLicensed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        //set up MediaPlayer
        mediaController = new MediaController(this);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);

        RateMeNowDialog.showRateDialog(this, 10);

    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaController.hide();
        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //the MediaController will hide after 3 seconds - tap the screen to make it appear again
        mediaController.show();
        return false;
    }

    Menu optionsMenu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SwitchCompat switchCompat = (SwitchCompat) menu.findItem(R.id.onswitch).getActionView().findViewById(R.id.switch1);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppPreferences.getInstance(MainActivity.this).setRecordingEnabled(isChecked);
            }
        });
        switchCompat.setChecked(AppPreferences.getInstance(MainActivity.this).isRecordingEnabled());
        return true;
    }

    boolean doubleBackToExitPressedOnce;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        // Does the user really want to exit?
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.press_back_again), Toast.LENGTH_LONG).show();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }


        if (id == R.id.action_save) {
            if (null != selectedItems && selectedItems.length > 0) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        for (PhoneCallRecord record : selectedItems) {
                            record.getPhoneCall().setKept(true);
                            record.getPhoneCall().save(MainActivity.this);
                        }
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(LocalBroadcastActions.NEW_RECORDING_BROADCAST)); // Causes refresh

                    }
                };
                handler.post(runnable);
            }
            return true;
        }

        if (id == R.id.action_share) {
            if (null != selectedItems && selectedItems.length > 0) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<Uri> fileUris = new ArrayList<Uri>();
                        for (PhoneCallRecord record : selectedItems) {
                            fileUris.add(Uri.fromFile(new File(record.getPhoneCall().getPathToRecording())));
                        }
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_title));
                        shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, Html.fromHtml(getString(R.string.email_body_html)));
                        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body));
                        shareIntent.setType("audio/*");
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
                    }
                };
                handler.post(runnable);
            }
            return true;
        }

        if (id == R.id.action_delete) {
            if (null != selectedItems && selectedItems.length > 0) {
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        this);
                alert.setTitle(R.string.delete_recording_title);
                alert.setMessage(R.string.delete_recording_subject);
                alert.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                Database callLog = Database.getInstance(MainActivity.this);
                                for (PhoneCallRecord record : selectedItems) {
                                    int id = record.getPhoneCall().getId();
                                    callLog.removeCall(id);
                                }

                                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(LocalBroadcastActions.RECORDING_DELETED_BROADCAST));
                            }
                        };
                        handler.post(runnable);

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
            return true;
        }

        if (id == R.id.action_delete_all) {

            AlertDialog.Builder alert = new AlertDialog.Builder(
                    this);
            alert.setTitle(R.string.delete_recording_title);
            alert.setMessage(R.string.delete_all_recording_subject);
            alert.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            Database.getInstance(MainActivity.this).removeAllCalls(false);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(LocalBroadcastActions.RECORDING_DELETED_BROADCAST));
                        }
                    };
                    handler.post(runnable);

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

            return true;
        }

        if (R.id.action_whitelist == id) {
            if(permissionReadContacts) {
                Intent intent = new Intent(this, WhitelistActivity.class);
                startActivity(intent);
            }else{
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        this);
                alert.setTitle(R.string.permission_whitelist_title);
                alert.setMessage(R.string.permission_whitelist);
            }
            return true;
        }
        if (R.id.action_about == id) {
            AboutDialog.show(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Evoked by the {@link RecordCallService#displayNotification}
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (null != intent) {
            // User clicked the {@link RecordCallService#displayNotification} to listen to the recording
            long id = intent.getIntExtra("RecordingId", -1);
            if (-1 != id) {
                CallLog call = Database.getInstance(this).getCall((int) id);
                if (null != call) {
                    audioPlayer(call.getPathToRecording());
                }
                intent.putExtra("RecordingId", -1); // run only once...
            }
        }
    }

    @Override
    public void onListFragmentInteraction(PhoneCallRecord items[]) {
        optionsMenu.findItem(R.id.action_delete).setVisible(items.length > 0);
        optionsMenu.findItem(R.id.action_share).setVisible(items.length > 0);
        selectedItems = items;
        // the MediaController will hide after 3 seconds - tap the screen to make it appear again
        if (mediaController.isEnabled() && !mediaController.isShowing()) mediaController.show();
    }

    PhoneCallRecord selectedItems[];


    @Override
    public void onItemPlay(PhoneCallRecord item) {
        audioPlayer(item.getPhoneCall().getPathToRecording());
    }

    @Override
    public boolean onListItemLongClick(View v, final PhoneCallRecord record, final PhoneCallRecord items[]) {
        selectedItems = items;
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main_popup, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        popupMenu.show();
        return false;
    }

    MediaPlayer mediaPlayer;
    MediaController mediaController;

    public void audioPlayer(String path) {

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Media Player stuff
     **/

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(this.findViewById(R.id.list));

        handler.post(new Runnable() {
            public void run() {
                mediaController.setEnabled(true);
                mediaController.show(5000);
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        mediaController.hide();
        mediaController.setEnabled(false);
    }


    boolean permissionReadContacts;

    public void checkPermissions() {
        permissionReadContacts = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        if (!permissionReadContacts)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new android.app.AlertDialog.Builder(this).setCancelable(true).setMessage(R.string.str_access_permissions).setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    0x01);
                        }
                    }).show();

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            0x01);

                }
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0x01: {  // validateRequestPermissionsRequestCode in FragmentActivity requires requestCode to be of 8 bits, meaning the range is from 0 to 255.
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) { // we only asked for one permission
                        permissionReadContacts = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                }
            }
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 1)
                return RecordingFragment.newInstance(1, RecordingFragment.SORT_TYPE.INCOMING);
            else if (position == 2)
                return RecordingFragment.newInstance(1, RecordingFragment.SORT_TYPE.OUTGOING);
            else
                return RecordingFragment.newInstance(1, RecordingFragment.SORT_TYPE.ALL);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.all);
                case 1:
                    return getString(R.string.incoming);
                case 2:
                    return getString(R.string.outgoing);
            }
            return null;
        }
    }
}
