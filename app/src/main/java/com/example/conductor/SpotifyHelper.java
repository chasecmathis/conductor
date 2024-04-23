package com.example.conductor;

import android.app.Activity;
import android.util.Log;
import android.widget.ImageButton;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.concurrent.CompletableFuture;

/**
 * Helper class for interacting with Spotify App Remote.
 */
public class SpotifyHelper {

    // Client ID
    private final String CLIENT_ID = "7f2c54fd18184696b745e45d29052624";
    // Redirect URI
    private final String REDIRECT_URI = "conductor://callback";
    // Context
    private Activity activity;
    // Spotify App Remote
    private SpotifyAppRemote mSpotifyAppRemote;

    /**
     * Constructor for SpotifyHelper.
     *
     * @param activity The application context.
     */
    public SpotifyHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Initializes the Spotify App Remote.
     */
    public void initializeSpotifyAppRemote() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(activity, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                Log.d("Spotify", "Connected to Spotify App Remote");

                ImageButton favorite_button = (ImageButton) activity.findViewById(R.id.button_heart);
                isLiked().thenAccept(liked -> {
                    if (liked) favorite_button.setImageResource(R.drawable.ic_favorite);
                    else favorite_button.setImageResource(R.drawable.ic_favorite_border);
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("Spotify", throwable.getMessage(), throwable);
            }
        });
    }

    /**
     * Disconnects the Spotify App Remote.
     */
    public void disconnectSpotifyAppRemote() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    /**
     * Likes the currently playing song on Spotify.
     */
    public void likeSpotifySong() {
        if (mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
            mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                String trackUri = playerState.track.uri;
                mSpotifyAppRemote.getUserApi().addToLibrary(trackUri).setResultCallback(ignored -> {
                    Log.d("Spotify", "Song liked successfully");
                    ImageButton button = activity.findViewById(R.id.button_heart);
                    button.setImageResource(R.drawable.ic_favorite);
                });
            });
        }
    }

    public CompletableFuture<Boolean> isLiked() {
        CompletableFuture<Boolean> isLikedFuture = new CompletableFuture<>();
        if (mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
            mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                String trackUri = playerState.track.uri;
                mSpotifyAppRemote.getUserApi().getLibraryState(trackUri).setResultCallback(state -> {
                    isLikedFuture.complete(state.isAdded);
                });
            });
        } else {
            isLikedFuture.complete(false);
        }

        return isLikedFuture;
    }


    public void unlikeSpotifySong() {
        if (mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
            mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                String trackUri = playerState.track.uri;
                mSpotifyAppRemote.getUserApi().removeFromLibrary(trackUri).setResultCallback(ignored -> {
                    Log.d("Spotify", "Song unliked successfully");
                    ImageButton button = activity.findViewById(R.id.button_heart);
                    button.setImageResource(R.drawable.ic_favorite_border);
                });
            });
        }
    }
}
