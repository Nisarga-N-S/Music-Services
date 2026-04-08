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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.musicservice.databinding.ActivitySecondBinding;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SecondActivity extends AppCompatActivity {

    ActivitySecondBinding binding;
    MusicService mService;
    boolean mBound = false;
    Intent serviceIntent;

    MusicUpdateReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecondBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        receiver = new MusicUpdateReceiver((current, duration, isPlaying) -> {
            binding.seekbar.setMax(duration);
            binding.seekbar.setProgress(current);
            binding.duration.setText(formatTime(current) + "/" + formatTime(duration));
            updateSongUI();
            if (isPlaying) {
                binding.state.setText(getString(R.string.state) + " Playing");
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);

            } else {
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

        binding.btnPlay.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPlay();
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);
                binding.state.setText(getString(R.string.state) + " Playing");
            }
        });

        binding.btnPause.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPause();
                binding.btnPause.setVisibility(GONE);
                binding.btnPlay.setVisibility(VISIBLE);
                binding.state.setText(getString(R.string.state) + " Pause");
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onNext();
                updateSongUI();
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);
            }
        });

        binding.btnPrev.setOnClickListener(v -> {
            if (mBound && mService != null) {
                mService.onPrev();
                updateSongUI();
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);
            }
        });

        binding.materialButton.setOnClickListener(v -> {
           serviceIntent=new Intent(this, MainActivity.class);
           startActivity(serviceIntent);
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

        private String formatTime(int milliseconds) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }

        @Override
        protected void onResume() {
            super.onResume();
            IntentFilter filter = new IntentFilter(MusicService.ACTION_UPDATE_UI);
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        }

        @Override
        protected void onStop() {
            super.onStop();
            if (mBound) {
                stopService(serviceIntent);
                unbindService(connection);
                unregisterReceiver(receiver);
                mBound = false;
            }
        }

        ServiceConnection connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
                mService = binder.getService();
                mBound = true;
                updateSongUI();
            }

            public void onServiceDisconnected(ComponentName name) {
                mBound = false;
            }
        };

        void updateSongUI() {
            if (mService != null) {
                Song s = mService.getCurrentSong();
                binding.currentSong.setText(s.name + " - " + s.film + " - " + s.artist);
            }
        }


}