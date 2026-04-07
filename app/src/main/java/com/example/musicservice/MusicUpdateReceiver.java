package com.example.musicservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MusicUpdateReceiver extends BroadcastReceiver {

     public interface UpdateListener {
            void onUpdate(int current, int duration);
        }
        private final UpdateListener listener;

        public MusicUpdateReceiver(UpdateListener listener) {
            this.listener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            int current = intent.getIntExtra("current", 0);
            int duration = intent.getIntExtra("duration", 0);
            listener.onUpdate(current, duration);
        }

}
