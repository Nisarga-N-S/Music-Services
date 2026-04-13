package com.example.musicservice;

import static android.content.ContentValues.TAG;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class MusicService extends Service {

    private static final String CHANNEL_ID = "MUSIC_CHANNEL_ID";
    public static final String ACTION_UPDATE_UI = "UPDATE_UI";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";


    boolean notification;

    NotificationManager manager;

    Song s;

    public MediaPlayer mediaPlayer;
    private int position = 0;
    NotificationCompat.Builder builder;
    PendingIntent pendingActivityIntent;
    Intent activityIntent;
    Intent intent;

    PendingIntent pendingPrevIntent;

    PendingIntent pendingPauseIntent;

    PendingIntent pendingNextIntent;

    NotificationChannel channel;

    PendingIntent pendingPlayIntent;

    private final IBinder binder = new LocalBinder();

    ArrayList<Song> songs = new ArrayList<>();

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                sendUIUpdate();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private void sendUIUpdate() {
        intent = new Intent(ACTION_UPDATE_UI);
        intent.putExtra("current", mediaPlayer.getCurrentPosition());
        intent.putExtra("duration", mediaPlayer.getDuration());
        intent.putExtra("isPlaying", mediaPlayer.isPlaying());
        intent.putExtra("isState", isState());
        sendBroadcast(intent);
    }

    public String isState() {

        if (mediaPlayer == null) {
            return "Stopped";
        }
        if (mediaPlayer.isPlaying()) {
            return "Playing";
        } else {
            return "Pause";
        }
    }

    public void setForegroundEnabled(boolean value) {
        notification = value;
        if (value) {
            updateNotification();
            if (builder != null) {
                startForeground(1, builder.build());
            }
        } else {
            stopForeground(true);
        }
        Log.d(TAG, "setForegroundEnabled: " + value);
    }


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
        if (mediaPlayer == null) {
            createMediaPlayer();
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        else if (notification) {
            updateNotification();
        }

    }

    public void onPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (notification) {
                updateNotification();
            }
        }
    }

    public void onNext() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            position++;
            if (position >= songs.size()) {
                position = 0;
            }
            createMediaPlayer();
            mediaPlayer.start();
            if (notification) {
                updateNotification();
            }
        }
    }

    public void onPrev() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            position--;
            if (position < 0) {
                position = songs.size() - 1;
            }
            createMediaPlayer();
            createNotificationChannel();
            mediaPlayer.start();
        }
        else if (notification) {
            updateNotification();
        }
    }

    public void onStop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        stopSelf();


    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public Song getCurrentSong() {
        return songs.get(position);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null)
            return START_NOT_STICKY;

        createNotificationChannel();

        Log.d(TAG, "onStartCommand: " + notification);


        if (mediaPlayer == null) {
            createMediaPlayer();
        }

        if (intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PREVIOUS.equals(action)) {
                onPrev();
            } else if (ACTION_PAUSE.equals(action)) {
                onPause();

            } else if (ACTION_NEXT.equals(action)) {
                onNext();

            }
        }

        notification = intent.getBooleanExtra("is_foreground", true);

        pendingPrevIntent = PendingIntent.getService(this, 1, new Intent(this, MusicService.class).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE);


        pendingPauseIntent = PendingIntent.getService(this, 2, new Intent(this, MusicService.class).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE);


        pendingNextIntent = PendingIntent.getService(this, 3, new Intent(this, MusicService.class).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE);


//        pendingPlayIntent = PendingIntent.getService(this, 3, new Intent(this, MusicService.class).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE);

        activityIntent = new Intent(this, MainActivity.class);
        pendingActivityIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (notification) {
            Log.d(TAG, "onStartCommand: " + builder);
            startForeground(1, builder.build());
        } else {
            stopForeground(true);
        }

        if (!mediaPlayer.isPlaying()) {
            onPlay();
        }

        return flags;
    }


    private void createNotificationChannel() {
      channel = new NotificationChannel(
                CHANNEL_ID, "Music Service", NotificationManager.IMPORTANCE_LOW
        );
       manager = getSystemService(NotificationManager.class);
        if (manager != null)
            manager.createNotificationChannel(channel);
    }

    private void updateNotification(){

        s=getCurrentSong();

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(s.name)
                .setContentText(s.film + " - " + s.artist)
                .setSmallIcon(R.drawable.library_music_24px)
                .addAction(R.drawable.skip_previous_24px, "Prev", pendingPrevIntent)
                .addAction(R.drawable.play_pause_24px, "Pause", pendingPauseIntent)
                .addAction(R.drawable.skip_next_24px, "Next", pendingNextIntent)
                .setContentIntent(pendingActivityIntent);

        createNotificationChannel();
        manager.notify(1,builder.build());


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateRunnable);
        super.onDestroy();
    }
}