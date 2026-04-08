package com.example.musicservice;

import static android.content.ContentValues.TAG;

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

    public MediaPlayer mediaPlayer;
    private int position = 0;
    NotificationCompat.Builder builder;
    PendingIntent pendingActivityIntent;
    Intent activityIntent;
    PendingIntent pendingPlayIntent;
    private final IBinder binder = new LocalBinder();

    ArrayList<Song> songs = new ArrayList<>();

    private int pausedPosition=0;

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                sendUIUpdate();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private void sendUIUpdate(){
        Intent intent = new Intent(ACTION_UPDATE_UI);
        intent.putExtra("current", mediaPlayer.getCurrentPosition());
        intent.putExtra("duration", mediaPlayer.getDuration());
        intent.putExtra("isPlaying",mediaPlayer.isPlaying());
        sendBroadcast(intent);
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
        if (mediaPlayer != null && !mediaPlayer.isPlaying())
            mediaPlayer.start();
    }

    public void onPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.pause();
    }

    public void onNext() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            position++;
            if(position>=songs.size()){
                position=0;
            }
            createMediaPlayer();
            mediaPlayer.start();
        }
    }

    public void onPrev() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            position--;
            if(position<0){
                position= songs.size()-1;
            }
            createMediaPlayer();
            mediaPlayer.start();
        }
    }


    public Song getCurrentSong() {
        return songs.get(position);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();


        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        if(intent!=null&&intent.getAction()!=null){
            String action=intent.getAction();
            if(ACTION_PREVIOUS.equals(action)){
                onPrev();
                sendUIUpdate();
            } else if (ACTION_PAUSE.equals(action)) {
                if(mediaPlayer.isPlaying()) {
                    onPause();
                    sendUIUpdate();
                }else{
                    builder.addAction(R.drawable.skip_previous_24px,"Play",pendingPlayIntent);
                    onPlay();
                    sendUIUpdate();
                }

            } else if (ACTION_NEXT.equals(action)) {
                onNext();
                sendUIUpdate();

            }
        }
        Log.d(TAG, "onStartCommand: "+intent.getAction());

        boolean notification =intent.getBooleanExtra("is_foreground",true);


        PendingIntent pendingPrevIntent=PendingIntent.getService(this,1,new Intent(this,MusicService.class).setAction(ACTION_PREVIOUS),PendingIntent.FLAG_IMMUTABLE);


        PendingIntent pendingPauseIntent=PendingIntent.getService(this,2,new Intent(this,MusicService.class).setAction(ACTION_PAUSE),PendingIntent.FLAG_IMMUTABLE);


        PendingIntent pendingNextIntent=PendingIntent.getService(this,4,new Intent(this,MusicService.class).setAction(ACTION_NEXT),PendingIntent.FLAG_IMMUTABLE);


        pendingPlayIntent=PendingIntent.getService(this,3,new Intent(this,MusicService.class),PendingIntent.FLAG_IMMUTABLE);

        activityIntent = new Intent(this, MainActivity.class);
        activityIntent.setAction(ACTION_PREVIOUS);
        pendingActivityIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Song s = getCurrentSong();

        if(notification){
           builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(s.name)
                    .setContentText(s.film + " - " + s.artist)
                    .setSmallIcon(R.drawable.library_music_24px)
                    .addAction(R.drawable.skip_previous_24px, "Prev",pendingPrevIntent)
                    .addAction(R.drawable.play_pause_24px, "Pause", pendingPauseIntent)
                    .addAction(R.drawable.skip_next_24px, "Next", pendingNextIntent)
                    .setContentIntent(pendingActivityIntent);

            startForeground(1, builder.build());
        }
        else{
            stopForeground(true);
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