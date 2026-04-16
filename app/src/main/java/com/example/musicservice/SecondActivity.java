package com.example.musicservice;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.example.musicservice.MusicService.TAG;

import android.Manifest;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.musicservice.databinding.ActivitySecondBinding;

public class SecondActivity extends AppCompatActivity {

    ActivitySecondBinding binding;
    MusicService mService;
    boolean mBound = false;
    Intent serviceIntent;

    MusicUpdateReceiver receiver;

    boolean isfirstActivity;

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

        receiver = new MusicUpdateReceiver((current, duration, isPlaying, formatted, isState) -> {
            binding.seekbar.setMax(duration);
            binding.seekbar.setProgress(current);
            binding.duration.setText(formatted);
            binding.state.setText(String.format("State: " + isState));

            updateSongUI();

            if (isPlaying) {
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

        binding.btnPlay.setOnClickListener(v -> sendAction(MusicService.ACTION_PLAY));
        binding.btnPause.setOnClickListener(v -> sendAction(MusicService.ACTION_PAUSE));
        binding.btnNext.setOnClickListener(v -> sendAction(MusicService.ACTION_NEXT));
        binding.btnPrev.setOnClickListener(v -> sendAction(MusicService.ACTION_PREVIOUS));

        binding.btnFirstactivity.setOnClickListener(v -> {
            isfirstActivity=true;
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

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

    private void sendAction(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        registerReceiver(receiver,
                new IntentFilter(MusicService.ACTION_UPDATE_UI),
                Context.RECEIVER_EXPORTED);

        if (mService != null) {
            updateSongUI();
        }
    }

    @Override
    protected void onStop() {

//        if(!isfirstActivity){
//            mService.onStop();
//        }
        super.onStop();

        if (mBound) {
            unbindService(connection);
            Log.d(TAG, "onStop: "+"second activity unbounded from service");
            mBound= false;
        }

        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {}
    }

    ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            updateSongUI();

            if (mService.mediaPlayer != null && mService.mediaPlayer.isPlaying()) {
                binding.btnPause.setVisibility(VISIBLE);
                binding.btnPlay.setVisibility(GONE);
            } else {
                binding.btnPause.setVisibility(GONE);
                binding.btnPlay.setVisibility(VISIBLE);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    void updateSongUI() {
        if (mService != null) {
            Song s = mService.getCurrentSong();
            binding.currentSong.setText(s.toString());
        }
    }
}