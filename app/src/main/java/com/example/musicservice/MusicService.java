package com.example.musicservice;
import static com.example.musicservice.MainActivity.ACTION_START;
import static com.example.musicservice.MainActivity.ACTION_STOP;

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

    public static final String ACTION_PLAY = "ACTION_PLAY";

    boolean notification;

    public static final String TAG = "Music_service--->";

    NotificationManager manager;

    Song s;

    public MediaPlayer mediaPlayer;
    private int position = 0;
    NotificationCompat.Builder builder;
    PendingIntent pendingActivityIntent;
    Intent activityIntent;

    String state;

    PendingIntent pendingPrevIntent;

    PendingIntent pendingPauseIntent;

    PendingIntent pendingNextIntent;

    PendingIntent pendingPlayIntent;

    NotificationChannel channel;

    private final IBinder binder = new LocalBinder();

    ArrayList<Song> songs = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            sendUIUpdate();
            handler.postDelayed(this, 1000);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        songs.add(new Song("Waka Waka", "World Cup", "Shakira", R.raw.wakka));
        songs.add(new Song("Naane ninanthe", "Brat", "Sid Sriram", R.raw.song2));
        songs.add(new Song("E Santhelu", "Sundaranga Jaana", "Shreya Ghoshal", R.raw.song3));
        songs.add(new Song("Sahib", "Sahib", "Aditya Rikahari", R.raw.song4));

        state = "Stopped";
        handler.post(updateRunnable);
        Log.d(TAG, "onCreate: On create is service started service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        if (intent == null)
            return START_NOT_STICKY;

        Log.d(TAG, "onStartCommand: Onstart command started service");


        if (mediaPlayer == null) {
            createMediaPlayer();
        }



        if (intent.getAction() != null) {
            String action = intent.getAction();

            switch (action) {

                case ACTION_PREVIOUS:
                    onPrev();
                    sendUIUpdate();
                    break;
                case ACTION_PAUSE:
                    onPause();
                    sendUIUpdate();
                    Log.d(TAG, "onStartCommand: " + ACTION_PAUSE);
                    break;
                case ACTION_PLAY:
                    onPlay();
                    sendUIUpdate();
                    break;
                case ACTION_NEXT:
                    onNext();
                    Log.d(TAG, "onStartCommand: "+"onNext called");
                    sendUIUpdate();
                    break;
                case ACTION_STOP:
                    onStop();
                    break;
            }
        }


        notification = intent.getBooleanExtra("is_foreground", false);

        if (notification) {
            updateNotification();
            if (builder != null) {
                startForeground(1, builder.build());
                Log.d(TAG, "onStartCommand: " + "started foreground service");
            }
        }

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            onPlay();
        }


        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateRunnable);
        super.onDestroy();
    }

    public String isState() {
        return state;
    }

    public Song getCurrentSong() {
        return songs.get(position);
    }

    private void createMediaPlayer() {
        Log.d(TAG, "createMediaPlayer: " + mediaPlayer);
        mediaPlayer = MediaPlayer.create(this, songs.get(position).resId);
        mediaPlayer.setOnCompletionListener(mp ->
                onNext());
    }

    public void sendUIUpdate() {
        Intent intent = new Intent(ACTION_UPDATE_UI);
        if (mediaPlayer != null) {
            intent.putExtra("current", mediaPlayer.getCurrentPosition());
            intent.putExtra("duration", mediaPlayer.getDuration());
            intent.putExtra("isPlaying", mediaPlayer.isPlaying());
            intent.putExtra("isState", isState());
        } else {
            intent.putExtra("current", 0);
            intent.putExtra("duration", 0);
            intent.putExtra("isPlaying", false);
            intent.putExtra("isState", isState());
        }
        sendBroadcast(intent);
    }

    public void setForegroundEnabled(boolean value) {
        notification = value;
        createNotificationChannel();
        if (value) {
            updateNotification();
            if (builder != null) {
                startForeground(1, builder.build());
                Log.d(TAG, "setForegroundEnabled: " + "service started");
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

    public void onPlay() {
        if (mediaPlayer == null) {
            createMediaPlayer();
        }
        mediaPlayer.start();
        state = "Playing";
        if (notification) {
            updateNotification();
        }

    }

    public void onPause() {
        state = "Paused";
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
        Log.d(TAG, "onPause: " + "onpause called");
        if (notification) {
            updateNotification();
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
            state = "Playing";
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
            state = "Playing";
            createMediaPlayer();
            mediaPlayer.start();
        }
        if (notification) {
            updateNotification();
        }
    }

    public void onStop() {
        state = "Stopped";
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        stopSelf();
    }


    private void setNotification() {


        pendingPrevIntent = PendingIntent.getService(this, 1, new Intent(this, MusicService.class).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE);


        pendingPauseIntent = PendingIntent.getService(this, 2, new Intent(this, MusicService.class).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE);


        pendingPlayIntent = PendingIntent.getService(this, 4, new Intent(this, MusicService.class).setAction(ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE);


        pendingNextIntent = PendingIntent.getService(this, 3, new Intent(this, MusicService.class).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE);

        activityIntent = new Intent(this, MainActivity.class);
        pendingActivityIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }


    private void createNotificationChannel() {
        channel = new NotificationChannel(
                CHANNEL_ID, "Music Service", NotificationManager.IMPORTANCE_LOW
        );
        manager = getSystemService(NotificationManager.class);
        if (manager != null)
            manager.createNotificationChannel(channel);
    }

    private void updateNotification() {

        setNotification();

        s = getCurrentSong();

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(s.name)
                .setContentText(s.film + " - " + s.artist)
                .setSmallIcon(R.drawable.library_music_24px)
                .addAction(R.drawable.skip_previous_24px, "Prev", pendingPrevIntent)
//        if (mediaPlayer.isPlaying()) {
           .addAction(R.drawable.play_pause_24px, "Pause", pendingPauseIntent)
//            Log.d(TAG, "updateNotification: " + pendingPauseIntent);
//        }
//        } else {
//            builder.addAction(R.drawable.play_pause_24px, "Play", pendingPlayIntent);
//        }

           .addAction(R.drawable.skip_next_24px, "Next", pendingNextIntent);

        manager.notify(1, builder.build());
    }


}




