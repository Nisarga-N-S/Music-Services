package com.example.musicservice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class MusicService extends Service {

    private static final String CHANNEL_ID = "MUSIC_CHANNEL_ID";
    public static final String ACTION_UPDATE_UI = "UPDATE_UI";
    public static final String ACTION_PREVIOUS = "com.example.MusicService.ACTION_PREVIOUS";
    public static final String ACTION_PAUSE = "com.example.MusicService.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.example.MusicService.ACTION_NEXT";

    public MediaPlayer mediaPlayer;
    private int position = 0;
    NotificationCompat.Builder builder;
    PendingIntent pendingActivityIntent;

    Intent activityIntent;
    Intent actionIntent;
    PendingIntent addActionIntent;
    private final IBinder binder = new LocalBinder();

    ArrayList<Song> songs = new ArrayList<>();

    private int pausedPosition=0;

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                Intent intent = new Intent(ACTION_UPDATE_UI);
                intent.putExtra("current", mediaPlayer.getCurrentPosition());
                intent.putExtra("duration", mediaPlayer.getDuration());
                sendBroadcast(intent);
            }
            handler.postDelayed(this, 1000);
        }
    };

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songs.add(new Song("Waka Waka", "World Cup", "Shakira", R.raw.wakka));
        songs.add(new Song("Naane ninanthe", "Brat", "Sid Sriram", R.raw.song2));
        songs.add(new Song("E Santhelu", "Sundaranga Jaana", "Shreya Ghoshal", R.raw.song3));
        songs.add(new Song("Sahib", "Sahib", "Aditya Rikahari", R.raw.song4));

        createMediaPlayer();
        handler.post(updateRunnable);
    }

    private void createMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, songs.get(position).resId);
        mediaPlayer.setOnCompletionListener(mp ->
                onNext());
    }

    public void onPlay() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying())
            mediaPlayer.start();
    }

    public void onPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.pause();
//        pausedPosition=mediaPlayer.getCurrentPosition();
    }

    public void onNext() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            position = (position + 1) % songs.size();
            createMediaPlayer();
            mediaPlayer.start();
        }
    }

    public void onPrev() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            position = (position - 1 + songs.size()) % songs.size();
            createMediaPlayer();
            mediaPlayer.start();
        }
    }

//    public void resumeMusic() {
//        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
//            mediaPlayer.seekTo(pausedPosition);
//            mediaPlayer.start();
//        }
//    }

    public Song getCurrentSong() {
        return songs.get(position);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        boolean notification =intent.getBooleanExtra("is_foreground",true);

        actionIntent = new Intent(this, MusicService.class);
        actionIntent.setAction(ACTION_PREVIOUS);


        addActionIntent = PendingIntent.getService(
                this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE);


        activityIntent = new Intent(this, MainActivity.class);
       pendingActivityIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Song s = getCurrentSong();

        if(!notification) {

            stopForeground(true);
        }
        else {

           builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(s.name)
                    .setContentText(s.film + " - " + s.artist)
                    .setSmallIcon(R.drawable.library_music_24px)
                    .addAction(R.drawable.skip_previous_24px, "Prev",addActionIntent)
                    .addAction(R.drawable.play_pause_24px, "Pause", addActionIntent)
                    .addAction(R.drawable.skip_next_24px, "Next", addActionIntent)
                    .setContentIntent(pendingActivityIntent);

            startForeground(1, builder.build());
        }

        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_PREVIOUS.equals(action)) {
                onPrev();
            } else if (ACTION_PAUSE.equals(action)) {
                onPause();
                actionIntent.setAction("Play");
            } else if (ACTION_NEXT.equals(action)) {
                onNext();
            }
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Music Service", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
                manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(updateRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}