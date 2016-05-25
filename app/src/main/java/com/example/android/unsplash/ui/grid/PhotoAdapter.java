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

package com.example.android.unsplash.ui.grid;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.android.unsplash.DetailActivity;
import com.example.android.unsplash.IntentUtil;
import com.example.android.unsplash.R;
import com.example.android.unsplash.data.model.Photo;
import com.example.android.unsplash.databinding.PhotoItemBinding;
import com.example.android.unsplash.ui.ImageSize;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> {

    private final List<Photo> photos;
    private final Activity host;
    private final int requestedPhotoWidth;

    public PhotoAdapter(@NonNull Activity activity, @NonNull List<Photo> photos) {
        this.photos = photos;
        this.host = activity;
        requestedPhotoWidth = host.getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final PhotoViewHolder holder = new PhotoViewHolder(
                (PhotoItemBinding) DataBindingUtil.inflate(LayoutInflater.from(host),
                        R.layout.photo_item, parent, false));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = holder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                final Photo photo = photos.get(position);
                final PhotoItemBinding binding = holder.getBinding();
                final Intent intent = new Intent(host, DetailActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(IntentUtil.PHOTO, photo);
                intent.putExtra(IntentUtil.FONT_SIZE, binding.author.getTextSize());
                intent.putExtra(IntentUtil.PADDING,
                        new Rect(binding.author.getPaddingLeft(),
                                binding.author.getPaddingTop(),
                                binding.author.getPaddingRight(),
                                binding.author.getPaddingBottom()));
                intent.putExtra(IntentUtil.TEXT_COLOR,
                        binding.author.getCurrentTextColor());

                Pair<View, String> authorPair = new Pair<View, String>(
                        binding.author, host.getString(R.string.transition_author));
                Pair<View, String> photoPair = new Pair<View, String>(
                        binding.photo, host.getString(R.string.transition_photo));
                View decorView = host.getWindow().getDecorView();
                View statusBackground = decorView.findViewById(android.R.id.statusBarBackground);
                View navBackground = decorView.findViewById(android.R.id.navigationBarBackground);
                Pair statusPair = Pair.create(statusBackground,
                        statusBackground.getTransitionName());
                ActivityOptions options;
                // some phone doesn't has a 'navigation bar' so we do some extra check
                if (navBackground != null) {
                    Pair navPair = Pair.create(navBackground, navBackground.getTransitionName());
                    options = ActivityOptions.makeSceneTransitionAnimation(host,
                            authorPair, photoPair, statusPair, navPair);
                }else {
                    options = ActivityOptions.makeSceneTransitionAnimation(host,
                            authorPair, photoPair, statusPair);
                }
                host.startActivity(intent, options.toBundle());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(final PhotoViewHolder holder, final int position) {
        holder.getBinding().setData(photos.get(position));
        Glide.with(host)
                .load(holder.getBinding().getData().getPhotoUrl(requestedPhotoWidth))
                .placeholder(R.color.placeholder)
                .override(ImageSize.NORMAL[0], ImageSize.NORMAL[1])
                .into(holder.getBinding().photo);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    @Override
    public long getItemId(int position) {
        return photos.get(position).id;
    }


}
