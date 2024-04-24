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
import android.util.Log;
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

    private MediaController mediaController;

    private ImageView album_art;

    public ShutterFragment() {
        // Required empty public constructor
    }

    public ShutterFragment(MediaController mediaController) {
        this.mediaController = mediaController;
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
        updateAlbumArt();
    }


    public void updateAlbumArt() {
        View view = getView();
        if (view != null && mediaController != null) {
            MediaMetadata metadata = mediaController.getMetadata();
            if (metadata != null) {
                if (metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null) {
                    Bitmap album_cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                    album_art.setImageBitmap(album_cover);
                }
            }
        }
    }
}