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
    boolean isForeground;
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

        updateSongUI();
        binding.duration.setText("0.00" + " / " + "0.00");

        serviceIntent = new Intent(this, MusicService.class);

        receiver = new MusicUpdateReceiver((current, duration, isPlaying, formatted,isState) -> {
            binding.seekbar.setMax(duration);
            binding.seekbar.setProgress(current);
            binding.duration.setText(formatted);
            binding.state.setText(getString(R.string.state) + isState);
            updateSongUI();
            if(isPlaying){
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);

            }else{

                binding.btnPause.setVisibility(GONE);
                binding.btnPlay.setVisibility(VISIBLE);
            }


        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        }

        binding.swtchbutton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            serviceIntent.putExtra("is_foreground",isChecked);
            if (isForeground) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            binding.btnPause.setVisibility(VISIBLE);
        });

        binding.btnStart.setOnClickListener(v -> {
            if(isForeground){
                startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
            binding.btnPause.setVisibility(VISIBLE);
        });

        binding.btnStop.setOnClickListener(v -> {
            binding.state.setText(getString(R.string.state) + " Stopped");
            mService.onStop();
        });

        binding.btnPlay.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPlay();
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnNext.setVisibility(VISIBLE);
            }
        });

        binding.btnPause.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPause();
                binding.btnPause.setVisibility(GONE);
                binding.btnPlay.setVisibility(VISIBLE);
                binding.btnNext.setVisibility(VISIBLE);
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onNext();
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);
            }
        });

        binding.btnPrev.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPrev();
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);
            }
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
        binding.materialButton.setOnClickListener(v -> {

            Intent intent=new Intent(this,SecondActivity.class);
            startActivity(intent);

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mBound){
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
        stopService(serviceIntent);
    }

    ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            updateSongList();
            updateSongUI();
        }

        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    void updateSongUI() {
        if (mService != null) {
            Song s = mService.getCurrentSong();
            String songDetails= s.name + " - " + s.film + " - " + s.artist;
            binding.currentSong.setText(songDetails);
        }
    }

    void updateSongList() {
        if (mService != null) {
            ArrayAdapter<Song> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, mService.songs);
            binding.songList.setAdapter(adapter);
        }
    }

}