package com.example.conductor;

import android.content.Context;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class SpotifyHelper {

    // Client ID
    private final String CLIENT_ID = "7f2c54fd18184696b745e45d29052624";
    // Redirect URI
    private final String REDIRECT_URI = "conductor://callback";
    // Context
    private Context context;
    // Spotify App Remote
    private SpotifyAppRemote mSpotifyAppRemote;

    public SpotifyHelper(Context context) {
        this.context = context;
    }

    // Method to initialize mSpotifyAppRemote
    public void initializeSpotifyAppRemote() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                Log.d("Spotify", "Connected to Spotify App Remote");
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("Spotify", throwable.getMessage(), throwable);
            }
        });
    }

    public void disconnectSpotifyAppRemote() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    public void likeSpotifySong() {
        if (mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
            mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                String trackUri = playerState.track.uri;
                mSpotifyAppRemote.getUserApi().addToLibrary(trackUri).setResultCallback(ignored -> {
                    Log.d("Spotify", "Song liked successfully");
                });
            });
        }
    }
}
