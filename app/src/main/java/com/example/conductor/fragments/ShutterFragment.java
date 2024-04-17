package com.example.conductor.fragments;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.conductor.MediaControllerInterfaceActivity;
import com.example.conductor.R;
import android.widget.ImageView;
import android.graphics.Bitmap;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ShutterFragment extends Fragment {

    private MediaSessionManager mediaSessionManager;
    private MediaControllerInterfaceActivity mediaControllerInterfaceActivity;

    private final int metadata_MS = 500;


    private HandlerThread metadataThread;
    private Handler metadataHandler;

    private TextView title_text;
    private TextView artist_text;
    private TextView album_text;
    private ImageView album_art;

    public ShutterFragment() {
        // Required empty public constructor
    }

    public ShutterFragment(MediaSessionManager mediaSessionManager, MediaControllerInterfaceActivity mediaControllerInterfaceActivity) {
        this.mediaSessionManager = mediaSessionManager;
        this.mediaControllerInterfaceActivity = mediaControllerInterfaceActivity;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_shutter, container, false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            title_text = view.findViewById(R.id.shutter_title);
            artist_text = view.findViewById(R.id.shutter_artist);
            album_text = view.findViewById(R.id.shutter_album);
            album_art = view.findViewById(R.id.album_art);
        }
        startMetadataThread();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMetadataThread();
    }

    private final Runnable getMetadata = new Runnable() {
        @Override
        public void run() {
            // Get metadata

            if (mediaSessionManager.getActiveSessions(new ComponentName(mediaControllerInterfaceActivity, getClass())).size() > 0) {
                MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(mediaControllerInterfaceActivity, getClass())).get(0);
                MediaMetadata metadata = controller.getMetadata();
                if (metadata != null) {
                    if (metadata.getText(MediaMetadata.METADATA_KEY_TITLE) != null) {
                        String title = getString(R.string.song_title) + " " + metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
                        title_text.setText(title);
                    }
                    if (metadata.getText(MediaMetadata.METADATA_KEY_ARTIST) != null) {
                        String artist = getString(R.string.song_artist) + " " + metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
                        artist_text.setText(artist);
                    }
                    if (metadata.getText(MediaMetadata.METADATA_KEY_ALBUM) != null) {
                        String album = getString(R.string.song_album) + " " + metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);
                        album_text.setText(album);
                    }

                    if (metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null) {
                        Bitmap album_cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                        album_art.setImageBitmap(album_cover);
                    }
                }
            }
            metadataHandler.postDelayed(getMetadata, metadata_MS);
        }
    };

    private void startMetadataThread() {
        metadataThread = new HandlerThread("shutter");
        metadataThread.start();
        metadataHandler = new Handler(metadataThread.getLooper());
        metadataHandler.post(getMetadata);
    }

    private void stopMetadataThread() {
        if (metadataThread != null) {
            metadataThread.quitSafely();
            try {
                metadataThread.join();
                metadataThread = null;
                metadataHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}