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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Toolbar;

import com.example.android.unsplash.data.model.Photo;
import com.example.android.unsplash.ui.DetailSharedElementEnterCallback;
import com.example.android.unsplash.ui.pager.DetailViewPagerAdapter;

import java.util.ArrayList;

public class DetailActivity extends Activity {

    private static final String STATE_INITIAL_ITEM = "initial";
    private ViewPager viewPager;
    private int initialItem;
    private final View.OnClickListener navigationOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAfterTransition();
                }
            };
    private DetailSharedElementEnterCallback sharedElementCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_detail);
        postponeEnterTransition();

        Intent intent = getIntent();
        sharedElementCallback = new DetailSharedElementEnterCallback(intent);
        setEnterSharedElementCallback(sharedElementCallback);
        initialItem = intent.getIntExtra(IntentUtil.SELECTED_ITEM_POSITION, 0);
        setUpViewPager(intent.<Photo>getParcelableArrayListExtra(IntentUtil.PHOTO));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(navigationOnClickListener);

        super.onCreate(savedInstanceState);
    }

    private void setUpViewPager(ArrayList<Photo> photos) {
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new DetailViewPagerAdapter(this, photos, sharedElementCallback));
        viewPager.setCurrentItem(initialItem);

        viewPager.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (viewPager.getChildCount() > 0) {
                    viewPager.removeOnLayoutChangeListener(this);
                    startPostponedEnterTransition();
                }
            }
        });

        viewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.padding_mini));
        viewPager.setPageMarginDrawable(R.drawable.page_margin);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_INITIAL_ITEM, initialItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        initialItem = savedInstanceState.getInt(STATE_INITIAL_ITEM, 0);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        setActivityResult();
        super.onBackPressed();
    }

    @Override
    public void finishAfterTransition() {
        setActivityResult();
        super.finishAfterTransition();
    }

    private void setActivityResult() {
        if (initialItem == viewPager.getCurrentItem()) {
            setResult(RESULT_OK);
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(IntentUtil.SELECTED_ITEM_POSITION, viewPager.getCurrentItem());
        setResult(RESULT_OK, intent);
    }

}
