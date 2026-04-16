package com.example.musicservice;

import android.app.Notification;
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

    public static final String ACTION_STOP = "ACTION_STOP";

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
    Intent intent;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songs.add(new Song("Waka Waka", "World Cup", "Shakira", R.raw.wakka));
        songs.add(new Song("Naane ninanthe", "Brat", "Sid Sriram", R.raw.song2));
        songs.add(new Song("E Santhelu", "Sundaranga Jaana", "Shreya Ghoshal", R.raw.song3));
        songs.add(new Song("Sahib", "Sahib", "Aditya Rikahari", R.raw.song4));

        state="Stopped";
        createNotificationChannel();
        handler.post(updateRunnable);
        Log.d(TAG, "onCreate: On create is service started service");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null)
            return START_NOT_STICKY;
        Log.d(TAG, "onStartCommand: Onstart command started service");

        if (intent.hasExtra("is_foreground")) {

            notification = intent.getBooleanExtra("is_foreground", true);
        }

        String action = intent.getAction();

        if(action==null) {

            onPlay();

            if (notification) {
                updateNotification();
                if (builder != null) {
                    startForeground(1, builder.build());
                }
            }
            return START_STICKY;
        }


            switch (action) {

                case ACTION_PLAY:
                    onPlay();
                    break;

                case ACTION_PAUSE:
                    Log.d(TAG, "onStartCommand: "+"Onpauseclicked");
                    onPause();
                    break;

                case ACTION_NEXT:
                    onNext();
                    break;

                case ACTION_PREVIOUS:
                    onPrev();
                    break;

                case ACTION_STOP:
                    onStop();
                    return START_NOT_STICKY;
            }

        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: unbinded ");
        return true;
    }


    @Override
    public void onDestroy() {
        onStop();
        handler.removeCallbacks(updateRunnable);
        Log.d(TAG, "onDestroy: "+"service destroyed");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    public void sendUIUpdate() {
        intent = new Intent(ACTION_UPDATE_UI);
        if(mediaPlayer!=null){
            intent.putExtra("current", mediaPlayer.getCurrentPosition());
            intent.putExtra("duration", mediaPlayer.getDuration());
            intent.putExtra("isPlaying", mediaPlayer.isPlaying());
            intent.putExtra("isState", isState());
        }
        else{
            intent.putExtra("current",0 );
            intent.putExtra("duration", 0);
            intent.putExtra("isPlaying", false);
            intent.putExtra("isState", isState());
        }
        sendBroadcast(intent);
    }

    public String isState() {
        return state;
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
            if(manager!=null){
                manager.cancel(1);
            }
        }
        Log.d(TAG, "setForegroundEnabled: " + value);
    }


    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public Song getCurrentSong() {
        return songs.get(position);
    }


    private void createMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, songs.get(position).resId);
        mediaPlayer.setOnCompletionListener(mp ->
                onNext());
    }

    public void onPlay() {
        if(mediaPlayer==null) {
            createMediaPlayer();
        }
        mediaPlayer.start();
        state="Playing";
        updateAll();

    }

    public void onPause() {
        state="Paused";
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        Log.d(TAG, "onPause: "+"song paused");
        updateAll();

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
            state="Playing";
        }
        updateAll();
    }

    public void onPrev() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            position--;
            if (position < 0) {
                position = songs.size() - 1;
            }
            state="Playing";
            createMediaPlayer();
            mediaPlayer.start();
        }
        updateAll();
    }

    public void onStop() {
        state = "Stopped";
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        stopSelf();
        sendUIUpdate();
    }

    private void updateAll() {
        sendUIUpdate();
        if (notification) {
            updateNotification();
        }
    }


    private void setNotification() {


        pendingPrevIntent = PendingIntent.getService(this, 101, new Intent(this, MusicService.class).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE);


        pendingPauseIntent = PendingIntent.getService(this, 102, new Intent(this, MusicService.class).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE);


        pendingNextIntent = PendingIntent.getService(this, 103, new Intent(this, MusicService.class).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE);


        pendingPlayIntent = PendingIntent.getService(this, 104, new Intent(this, MusicService.class).setAction(ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE);

        activityIntent = new Intent(this, MainActivity.class);
        pendingActivityIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }
    private void updateNotification() {

        setNotification();

        s = getCurrentSong();

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(s.name)
                .setContentText(s.film + " - " + s.artist)
                .setSmallIcon(R.drawable.library_music_24px)
                .addAction(R.drawable.skip_previous_24px, "Prev", pendingPrevIntent);

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            builder.addAction(R.drawable.play_pause_24px, "Pause", pendingPauseIntent);
        } else {
            builder.addAction(R.drawable.play_pause_24px, "Play", pendingPlayIntent);
        }

        builder.addAction(R.drawable.skip_next_24px, "Next", pendingNextIntent);
        if(manager!=null) {

            manager.notify(1, builder.build());
        }
    }
    private void createNotificationChannel() {
        channel = new NotificationChannel(
                CHANNEL_ID, "Music Service", NotificationManager.IMPORTANCE_LOW
        );
        manager = getSystemService(NotificationManager.class);
        if (manager != null)
            manager.createNotificationChannel(channel);
    }
}
