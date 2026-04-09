package com.example.musicservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicUpdateReceiver extends BroadcastReceiver {

     public interface UpdateListener {
            void onUpdate(int current, int duration,boolean isPlaying,String formatted,String isState);
        }
        private final UpdateListener listener;

        public MusicUpdateReceiver(UpdateListener listener) {
            this.listener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            int current = intent.getIntExtra("current", 0);
            int duration = intent.getIntExtra("duration", 0);
            boolean isPlaying=intent.getBooleanExtra("isPlaying",false);
            String isState=intent.getStringExtra("isState");
            String formatted=formatTime(current) + "/" + formatTime(duration);
            listener.onUpdate(current, duration,isPlaying,formatted,isState);
        }

    private String formatTime(int milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

}
