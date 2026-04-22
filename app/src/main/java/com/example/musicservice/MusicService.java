package com.example.musicservice;

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
import java.util.Timer;
import java.util.TimerTask;


public class MusicService extends Service {

    private static final String CHANNEL_ID = "MUSIC_CHANNEL_ID";
    public static final String ACTION_UPDATE_UI = "UPDATE_UI";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";

    public static final String ACTION_STOP = "ACTION_STOP";

    public static final String ACTION_PLAY = "ACTION_PLAY";

    boolean notification=true;

    public static final String TAG = "Music_service--->";

    NotificationManager manager;

    Song s;

    public MediaPlayer mediaPlayer;
    private int position = 0;
    NotificationCompat.Builder builder;
    PendingIntent pendingActivityIntent;
    Intent activityIntent;
    Intent intent;

    String state="Stopped";

    PendingIntent pendingPrevIntent;

    PendingIntent pendingPauseIntent;

    PendingIntent pendingNextIntent;

    PendingIntent pendingPlayIntent;

    NotificationChannel channel;

    Timer timer;

    TimerTask stopTask;

    private final IBinder binder = new LocalBinder();

    ArrayList<Song> songs = new ArrayList<>();

    private final Handler handler = new Handler();
    private  void songList(){
        songs.add(new Song("Waka Waka", "World Cup", "Shakira", R.raw.wakka));
        songs.add(new Song("Naane ninanthe", "Brat", "Sid Sriram", R.raw.song2));
        songs.add(new Song("E Santhelu", "Sundaranga Jaana", "Shreya Ghoshal", R.raw.song3));
        songs.add(new Song("Sahib", "Sahib", "Aditya Rikahari", R.raw.song4));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        songList();

        createNotificationChannel();
        handler.post(updateRunnable);
        Log.d(TAG, "onCreate: On create is service started service");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return START_NOT_STICKY;
        }
        Log.d(TAG, "onStartCommand: Onstart command started service");
        if(intent.hasExtra("is_foreground")) {

            notification = intent.getBooleanExtra("is_foreground", notification);
        }

        String action = intent.getAction();

        if(action==null) {
            onPlay();
        }else{
            switch (action) {

                case ACTION_PLAY:
                    onPlay();
                    break;

                case ACTION_PAUSE:
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
        }
        if (notification) {
            updateNotification();
            if(builder!=null) {
                startForeground(1, builder.build());
            }

        }

        return START_STICKY;
    }

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: binded");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: unbinded ");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind: rebinded");
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

    public String isState() {
        return state;
    }

    public Song getCurrentSong() {
        return songs.get(position);
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

    public void setForegroundEnabled(boolean value) {
        notification = value;
        Log.d(TAG, "notification "+notification);
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


    private void createMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, songs.get(position).resId);
        mediaPlayer.setOnCompletionListener(mp ->
                onNext());
    }

    public void onPlay() {
        state="Playing";
        if(mediaPlayer==null) {
            createMediaPlayer();
        }
        mediaPlayer.start();
        updateAll();

    }

    public void onPause() {
        state="Paused";
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();

        }
        updateAll();
        Log.d(TAG, "onPause: "+"song paused");
    }

    public void onNext() {
        boolean shouldPlay="Playing".equals(state);

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        position++;
        if (position >= songs.size()) {
            position = 0;
        }
        createMediaPlayer();
        if(shouldPlay) {
            mediaPlayer.start();
            state = "Playing";
        }else{
            state="Stopped";
        }
        updateAll();


    }

    public void onPrev() {
        boolean shouldPlay="Playing".equals(state);
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        position--;
        if (position < 0) {
            position = songs.size() - 1;
        }
        createMediaPlayer();
        if(shouldPlay) {
            mediaPlayer.start();
            state = "Playing";
        }else {
            state = "Stopped";
        }
        updateAll();
    }

    public void onStop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        state = "Stopped";
        stopForeground(true);
        stopSelf();
        sendUIUpdate();
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            sendUIUpdate();
            handler.postDelayed(this, 1000);
        }
    };

    public void startDelayedTask(){
        if(timer!=null){
            timer.cancel();
        }
        timer=new Timer();
        stopTask=new TimerTask(){
            @Override
            public void run() {
                onStop();
            }
        };
        timer.schedule(stopTask,2000);
    }

    public void cancelDelayedTask(){
        if(timer!=null){
            timer.cancel();
            timer=null;
        }
    }


    private void updateAll() {
        sendUIUpdate();
        if(notification) {
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
        if(!notification)
            return;

        setNotification();

        s = getCurrentSong();

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(s.name)
                .setContentText(s.film + " - " + s.artist)
                .setSmallIcon(R.drawable.library_music_24px)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.skip_previous_24px, "Prev", pendingPrevIntent);

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            builder.addAction(R.drawable.play_pause_24px, "Pause", pendingPauseIntent);
        } else if(mediaPlayer!=null && !mediaPlayer.isPlaying()|| state.equals("Stopped")){
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