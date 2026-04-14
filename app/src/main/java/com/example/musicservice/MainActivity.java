package com.example.musicservice;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.musicservice.databinding.ActivityMainBinding;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    MusicService mService;
    boolean mBound = false;
    Intent serviceIntent;
    Song s;

    boolean isSecondActivity;

    public static final String TAG = "Music_service--->";

    boolean isForeground;
    MusicUpdateReceiver receiver;

    public static final String ACTION_STOP = "ACTION_STOP";

    public static final String ACTION_START = "ACTION_START";


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

        serviceIntent = new Intent(this, MusicService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        }


        Log.d(TAG, "Status "+isForeground);
        Log.d(TAG, "mainactivity: "+serviceIntent);


        receiver = new MusicUpdateReceiver((current, duration, isPlaying, formatted,isState) -> {
            binding.seekbar.setMax(duration);
            binding.seekbar.setProgress(current);
            binding.duration.setText(formatted);
            binding.state.setText(String.format(getString(R.string.state) + isState));
            updateSongUI();
            if(isPlaying){
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);

            }else{

                binding.btnPause.setVisibility(GONE);
                binding.btnPlay.setVisibility(VISIBLE);
            }

        });

        binding.switchbutton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(mBound && mService!=null){
                mService.setForegroundEnabled(isChecked);
                Log.d(TAG, "onCreate: "+isChecked);
            }

        });

        binding.btnStart.setOnClickListener(v -> {
            isForeground=binding.switchbutton.isChecked();
            serviceIntent.putExtra("is_foreground",isForeground);
//            serviceIntent.setAction(ACTION_START);
            if (isForeground){
                startForegroundService(serviceIntent);
            }
            else {
                startService(serviceIntent);
            }

        });

        binding.btnStop.setOnClickListener(v -> {
//            serviceIntent.setAction(ACTION_STOP);
//            startService(serviceIntent);
            mService.onStop();
            binding.state.setText(String.format(getString(R.string.state) + mService.isState()));
        });

        binding.btnPlay.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPlay();
            }
        });

        binding.btnPause.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPause();
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onNext();
            }
        });

        binding.btnPrev.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPrev();
            }
        });
        binding.btnSecondactivity.setOnClickListener(v -> {
            isSecondActivity=true;
            Log.d(TAG, "onCreate: "+mService.notification +"firstactivity");
            Intent intent=new Intent(this,SecondActivity.class);
            startActivity(intent);

        });

        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mService != null && mService.mediaPlayer != null)
                    mService.mediaPlayer.seekTo(seekBar.getProgress());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mBound) {
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        }
        IntentFilter filter = new IntentFilter(MusicService.ACTION_UPDATE_UI);
        registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(connection);
            unregisterReceiver(receiver);
            mBound = false;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void updateSongUI() {
        if (mService != null) {
          s = mService.getCurrentSong();
            binding.currentSong.setText(s.toString());
        }
    }

    void updateSongList() {
        if (mService != null) {
            ArrayAdapter<Song> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, mService.songs);
            binding.songList.setAdapter(adapter);
        }
    }

    ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            binding.switchbutton.setChecked(mService.notification);
            Log.d(TAG, "onServiceConnected: "+mService.notification);
            updateSongList();
            updateSongUI();

        }

        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


}