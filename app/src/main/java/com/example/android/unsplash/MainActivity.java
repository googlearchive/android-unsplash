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
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.unsplash.data.UnsplashService;
import com.example.android.unsplash.data.model.Photo;
import com.example.android.unsplash.databinding.PhotoItemBinding;
import com.example.android.unsplash.ui.ItemClickSupport;
import com.example.android.unsplash.ui.grid.GridMarginDecoration;
import com.example.android.unsplash.ui.grid.PhotoAdapter;
import com.example.android.unsplash.ui.grid.PhotoViewHolder;

import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends Activity {

    private static final int PHOTO_COUNT = 12;

    private RecyclerView grid;
    private ProgressBar empty;
    private int columns;
    private int gridSpacing;
    private PhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UnsplashService unsplashApi = new RestAdapter.Builder()
                .setEndpoint(UnsplashService.ENDPOINT)
                .build()
                .create(UnsplashService.class);

        grid = (RecyclerView) findViewById(R.id.image_grid);
        empty = (ProgressBar) findViewById(android.R.id.empty);
        columns = getResources().getInteger(R.integer.photo_grid_columns);
        gridSpacing = getResources().getDimensionPixelSize(R.dimen.grid_item_spacing);

        setupRecyclerView();

        unsplashApi.getFeed(new Callback<List<Photo>>() {
            @Override
            public void success(List<Photo> photos, Response response) {
                // the first items are really boring, get the last <n>
                adapter = new PhotoAdapter(MainActivity.this,
                        photos.subList(photos.size() - PHOTO_COUNT, photos.size()));
                grid.setAdapter(adapter);
                empty.setVisibility(View.GONE);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(getClass().getCanonicalName(), "Error retrieving Unsplash feed:", error);
            }
        });
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columns);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                /* emulating https://material-design.storage.googleapis.com/publish/material_v_4/material_ext_publish/0B6Okdz75tqQsck9lUkgxNVZza1U/style_imagery_integration_scale1.png */
                switch (position % 6) {
                    case 0:
                    case 1:
                    case 2:
                    case 4:
                        return 1;
                    case 3:
                        return 2;
                    default:
                        return 3;
                }
            }
        });
        grid.setLayoutManager(gridLayoutManager);
        grid.addItemDecoration(new GridMarginDecoration(gridSpacing));
        grid.setHasFixedSize(true);

        ItemClickSupport.addTo(grid).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        Photo photo = adapter.getItem(position);
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.putExtra(IntentUtil.PHOTO, photo);
                        View author = v.findViewById(R.id.author);
                        if (author instanceof TextView) {
                            intent.putExtra(IntentUtil.FONT_SIZE,
                                    ((TextView) author).getTextSize());
                            intent.putExtra(IntentUtil.PADDING,
                                    new Rect(author.getPaddingLeft(),
                                            author.getPaddingTop(),
                                            author.getPaddingRight(),
                                            author.getPaddingBottom()));
                        }

                        PhotoItemBinding binding = ((PhotoViewHolder) recyclerView
                                .getChildViewHolder(v)).getBinding();
                        Pair<View, String> authorPair = new Pair<View, String>(
                                binding.author, binding.author.getTransitionName());
                        Pair<View, String> photoPair = new Pair<View, String>(
                                binding.photo, binding.photo.getTransitionName());
                        MainActivity.this.startActivity(intent,
                                ActivityOptions.makeSceneTransitionAnimation(MainActivity.this,
                                        authorPair, photoPair).toBundle());
                    }
                });
    }
}
