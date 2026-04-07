package com.example.musicservice;

public class Song {

        public String name, film, artist;
        public int resId;

        public Song(String name, String film, String artist, int resId) {
            this.name = name;
            this.film = film;
            this.artist = artist;
            this.resId = resId;
        }
        @Override
        public String toString() {
            return name + " - " + film+ " - " + " - " + artist;
        }
}
