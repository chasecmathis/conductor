package com.example.conductor.fragments;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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

    private Handler metadataHandler;

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
        return inflater.inflate(R.layout.fragment_shutter, container, false);

    }

    @Override
    public void onResume() {
        super.onResume();

        View view = getView();
        if (view != null) {
            album_art = view.findViewById(R.id.album_art);
        }

        metadataHandler = new Handler(Looper.getMainLooper());
        metadataHandler.post(getMetadata);
    }

    @Override
    public void onPause() {
        super.onPause();
        metadataHandler.removeCallbacks(getMetadata);
    }

    private final Runnable getMetadata = new Runnable() {
        @Override
        public void run() {
            // Get metadata

            if (mediaSessionManager.getActiveSessions(new ComponentName(mediaControllerInterfaceActivity, getClass())).size() > 0) {
                MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(mediaControllerInterfaceActivity, getClass())).get(0);
                MediaMetadata metadata = controller.getMetadata();
                if (metadata != null) {
                    if (metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null) {
                        Bitmap album_cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                        album_art.setImageBitmap(album_cover);
                    }
                }
            }
            metadataHandler.postDelayed(getMetadata, metadata_MS);
        }
    };
}