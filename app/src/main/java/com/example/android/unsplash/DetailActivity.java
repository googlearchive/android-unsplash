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
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.bumptech.glide.Glide;
import com.example.android.unsplash.data.model.Photo;
import com.example.android.unsplash.databinding.ActivityDetailBinding;
import com.example.android.unsplash.ui.ImageSize;

public class DetailActivity extends Activity {

    public static final String EXTRA_PHOTO = "EXTRA_PHOTO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int requestedPhotoWidth = getResources().getDisplayMetrics().widthPixels;
        ActivityDetailBinding binding = DataBindingUtil
                .setContentView(this, R.layout.activity_detail);
        binding.setData((Photo) getIntent().getParcelableExtra(EXTRA_PHOTO));

        int slideDuration = getResources().getInteger(R.integer.detail_desc_slide_duration);

        Glide.with(this)
                .load(binding.getData().getPhotoUrl(requestedPhotoWidth))
                .placeholder(R.color.placeholder)
                .override(ImageSize.NORMAL[0], ImageSize.NORMAL[1])
                .into(binding.photo);
        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAfterTransition();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide(Gravity.BOTTOM);
            slide.addTarget(R.id.description);
            slide.setInterpolator(AnimationUtils.loadInterpolator(this, android.R.interpolator
                    .linear_out_slow_in));
            slide.setDuration(slideDuration);
            getWindow().setEnterTransition(slide);
        }
    }

}
