package com.example.conductor.fragments;

import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.graphics.Bitmap;

import com.example.conductor.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShutterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShutterFragment extends Fragment {

    private MediaController mediaController;
    private ImageView album_art;

    /**
     * Default constructor for ShutterFragment.
     */
    public ShutterFragment() {
        // Required empty public constructor
    }

    /**
     * Constructor for ShutterFragment with media controller.
     *
     * @param mediaController The MediaController instance.
     */
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

        // Find and initialize album_art ImageView
        View view = getView();
        if (view != null) {
            album_art = view.findViewById(R.id.album_art);
        }
        // Update album art
        updateAlbumArt();
    }

    /**
     * Updates the album art ImageView with the current media's album art.
     */
    public void updateAlbumArt() {
        View view = getView();
        // Check if view and media controller are not null
        if (view != null && mediaController != null) {
            MediaMetadata metadata = mediaController.getMetadata();
            // Check if metadata is not null and album art is available
            if (metadata != null && metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null) {
                // Retrieve album art bitmap and set it to the ImageView
                Bitmap albumCover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                album_art.setImageBitmap(albumCover);
            }
        }
    }
}
