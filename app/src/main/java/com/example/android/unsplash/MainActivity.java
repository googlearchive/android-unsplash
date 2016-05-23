/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.unsplash;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

import com.example.android.unsplash.data.UnsplashService;
import com.example.android.unsplash.data.model.Photo;
import com.example.android.unsplash.ui.grid.GridMarginDecoration;
import com.example.android.unsplash.ui.grid.PhotoAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends Activity {

    private static final int PHOTO_COUNT = 12;
    private static final String TAG = "MainActivity";

    private RecyclerView grid;
    private ProgressBar empty;
    private ArrayList<Photo> relevantPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        postponeEnterTransition();

        grid = (RecyclerView) findViewById(R.id.image_grid);
        empty = (ProgressBar) findViewById(android.R.id.empty);

        setupRecyclerView();

        if (savedInstanceState != null) {
            relevantPhotos = savedInstanceState.getParcelableArrayList(IntentUtil.RELEVANT_PHOTOS);
        }
        displayData();
    }

    private void displayData() {
        if (relevantPhotos != null) {
            populateGrid();
        } else {
            UnsplashService unsplashApi = new RestAdapter.Builder()
                    .setEndpoint(UnsplashService.ENDPOINT)
                    .build()
                    .create(UnsplashService.class);
            unsplashApi.getFeed(new Callback<List<Photo>>() {
                @Override
                public void success(List<Photo> photos, Response response) {
                    // the first items not interesting to us, get the last <n>
                    relevantPhotos = new ArrayList<>(photos.subList(photos.size() - PHOTO_COUNT,
                            photos.size()));
                    populateGrid();
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, "Error retrieving Unsplash feed:", error);
                }
            });
        }
    }

    private void populateGrid() {
        grid.setAdapter(new PhotoAdapter(this, relevantPhotos));
        empty.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(IntentUtil.RELEVANT_PHOTOS, relevantPhotos);
        super.onSaveInstanceState(outState);
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = (GridLayoutManager) grid.getLayoutManager();
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                /* emulating https://material-design.storage.googleapis.com/publish/material_v_4/material_ext_publish/0B6Okdz75tqQsck9lUkgxNVZza1U/style_imagery_integration_scale1.png */
                switch (position % 6) {
                    case 5:
                        return 3;
                    case 3:
                        return 2;
                    default:
                        return 1;
                }
            }
        });
        grid.addItemDecoration(new GridMarginDecoration(
                getResources().getDimensionPixelSize(R.dimen.grid_item_spacing)));
        grid.setHasFixedSize(true);
        // Start the postponed transition when the recycler view is ready to be drawn.
        grid.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                grid.getViewTreeObserver().removeOnPreDrawListener(this);
                startPostponedEnterTransition();
                return false;
            }
        });
    }
}
