package com.example.musicservice;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.musicservice.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Music_service--->";
    ActivityMainBinding binding;

    MusicService mService;
    boolean mBound = false;

    Intent serviceIntent;

    Song s;
    MusicUpdateReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
            if(savedInstanceState==null) {
                binding.switchbutton.setChecked(true);
            }
        serviceIntent = new Intent(this, MusicService.class);
        initReceiver();
        requestNotificationPermission();
        setupClickListeners();
        setupSeekBar();
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter(MusicService.ACTION_UPDATE_UI);
        registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mBound && mService!=null){
                mService.cancelDelayedTask();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBound && mService != null) {
            if(!mService.notification)
                mService.startDelayedTask();
            }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(connection);
            mBound= false;
            Log.d(TAG, "onStop- unbound from service");
        }
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



    private void initReceiver() {
        receiver = new MusicUpdateReceiver((current, duration, isPlaying, formatted, isState) -> {

            binding.seekbar.setMax(duration);
            binding.seekbar.setProgress(current);
            binding.duration.setText(formatted);
            binding.state.setText(String.format(getString(R.string.state) + isState));
            Log.d(TAG, "state " + isState);
            updateSongUI();
            if (isPlaying) {
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);
            }else {
                binding.btnPause.setVisibility(GONE);
                binding.btnPlay.setVisibility(VISIBLE);
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        }
    }


    private void setupClickListeners() {

        binding.switchbutton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCreate: " + isChecked);
            if (mBound && mService != null) {
                mService.setForegroundEnabled(isChecked);
            }
        });

        binding.songList.setOnItemClickListener((parent, view, position, id) -> {
        });

        binding.btnStart.setOnClickListener(v -> {
            serviceIntent.setAction(null);
            serviceIntent.putExtra("is_foreground", binding.switchbutton.isChecked());
            if (binding.switchbutton.isChecked()) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

        });
////
//        binding.btnStop.setOnClickListener(v -> {
//                    serviceIntent.setAction(MusicService.ACTION_STOP);
//                    startService(serviceIntent);
//                }
//        );
//
//        binding.btnPlay.setOnClickListener(v -> {
//            if (mBound && mService != null) {
//                mService.onPlay();
//            }
//        });
//
//        binding.btnPause.setOnClickListener(v -> {
//            if (mBound && mService != null) {
//                mService.onPause();
//            }
//
//
//        });
//
//        binding.btnNext.setOnClickListener(v ->{
//            if (mBound && mService != null) {
//                mService.onNext();
//            }
//                }
//
//        );
//
//        binding.btnPrev.setOnClickListener(v ->{
//            if (mBound && mService != null) {
//                mService.onPrev();
//            }
//
//                }
//        );

        binding.btnStop.setOnClickListener(v -> sendAction(MusicService.ACTION_STOP));
        binding.btnPlay.setOnClickListener(v -> sendAction(MusicService.ACTION_PLAY));
        binding.btnPause.setOnClickListener(v -> sendAction(MusicService.ACTION_PAUSE));
        binding.btnNext.setOnClickListener(v ->{
            Log.d(TAG, "setupClickListeners: "+s.artist+s.name);
            sendAction(MusicService.ACTION_NEXT);
        });
        binding.btnPrev.setOnClickListener(v -> sendAction(MusicService.ACTION_PREVIOUS));

        binding.btnSecondactivity.setOnClickListener(v ->
            startActivity(new Intent(this, SecondActivity.class)));
    }

    private void setupSeekBar() {
        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mService != null && mService.mediaPlayer != null) {
                    mService.mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });
    }
    ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            binding.switchbutton.setChecked(mService.notification);
            Log.d(TAG, "onServiceConnected: "+binding.switchbutton.isChecked());
            updateSongList();
            updateSongUI();
        }

        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    private void sendAction(String action) {
        serviceIntent.setAction(action);
        serviceIntent.putExtra("is_foreground",binding.switchbutton.isChecked());
        startService(serviceIntent);
    }

    void updateSongUI() {
            s = mService.getCurrentSong();
            binding.currentSong.setText(s.toString());
    }

    void updateSongList() {
        if (mService != null) {
            ArrayAdapter<Song> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    mService.songs
            );
            binding.songList.setAdapter(adapter);
        }
    }
}