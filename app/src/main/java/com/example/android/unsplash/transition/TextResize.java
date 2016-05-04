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

package com.example.android.unsplash.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.Property;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TextResize extends Transition {
    private static final String DATA = "TextResize:data";

    private static final String[] PROPERTIES = {
            DATA,
    };

    private static final Property<View, Rect> PADDING =
            new Property<View, Rect>(Rect.class, "padding") {
                @Override
                public Rect get(View view) {
                    return new Rect(view.getPaddingLeft(), view.getPaddingTop(),
                            view.getPaddingRight(), view.getPaddingBottom());
                }

                @Override
                public void set(View view, Rect padding) {
                    view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
                }
            };

    public TextResize() {
        addTarget(TextView.class);
    }

    public TextResize(Context context, AttributeSet attrs) {
        super(context, attrs);
        addTarget(TextView.class);
    }

    @Override
    public String[] getTransitionProperties() {
        return PROPERTIES;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues transitionValues) {
        if (!(transitionValues.view instanceof TextView)) {
            return;
        }
        final TextView view = (TextView) transitionValues.view;
        final TextResizeData data = new TextResizeData(view);
        transitionValues.values.put(DATA, data);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
                                   TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        final TextView textView = (TextView) endValues.view;
        final TextResizeData startData = (TextResizeData) startValues.values.get(DATA);
        final TextResizeData endData = (TextResizeData) endValues.values.get(DATA);
        return animateSize(textView, startData, endData);
    }

    private Animator animateSize(final TextView textView, final TextResizeData startData,
                                 TextResizeData endData) {
        final Bitmap endBitmap = captureTextBitmap(textView);

        textView.setPadding(startData.paddingLeft, startData.paddingTop, startData.paddingRight,
                startData.paddingBottom);
        textView.setRight(textView.getLeft() + startData.width);
        textView.setBottom(textView.getTop() + startData.height);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, startData.fontSize);
        final Bitmap startBitmap = captureTextBitmap(textView);
        textView.setPadding(endData.paddingLeft, endData.paddingTop, endData.paddingRight,
                endData.paddingBottom);
        textView.setRight(textView.getLeft() + endData.width);
        textView.setBottom(textView.getTop() + endData.height);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, endData.fontSize);

        final ColorStateList textColors = textView.getTextColors();
        final ColorStateList hintColors = textView.getHintTextColors();
        final int highlightColor = textView.getHighlightColor();
        final ColorStateList linkColors = textView.getLinkTextColors();
        textView.setTextColor(Color.TRANSPARENT);
        textView.setHintTextColor(Color.TRANSPARENT);
        textView.setHighlightColor(Color.TRANSPARENT);
        textView.setLinkTextColor(Color.TRANSPARENT);

        final float endScale = endData.fontSize / startData.fontSize;
        final SwitchBitmapDrawable drawable =
                new SwitchBitmapDrawable(startBitmap, endBitmap, endScale);
        textView.getOverlay().add(drawable);
        drawable.setLeft(startData.paddingLeft);
        drawable.setTop(startData.paddingTop);

        Animator progress = ObjectAnimator.ofFloat(drawable, "progress", 0, 1);
        Animator left = ObjectAnimator.ofInt(drawable, "left", startData.paddingLeft,
                endData.paddingLeft);
        Animator top = ObjectAnimator.ofInt(drawable, "top", startData.paddingTop,
                endData.paddingTop);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(left, top, progress);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                textView.getOverlay().remove(drawable);
                textView.setTextColor(textColors);
                textView.setHintTextColor(hintColors);
                textView.setHighlightColor(highlightColor);
                textView.setLinkTextColor(linkColors);
            }
        });
        return animatorSet;
    }

    private static Bitmap captureTextBitmap(TextView textView) {
        Drawable background = textView.getBackground();
        textView.setBackground(null);
        int width = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
        int height = textView.getHeight() - textView.getPaddingTop() - textView.getPaddingBottom();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(-textView.getPaddingLeft(), -textView.getPaddingTop());
        textView.draw(canvas);
        textView.setBackground(background);
        return bitmap;
    }

    private static class SwitchBitmapDrawable extends Drawable {
        private final Bitmap mStartBitmap;
        private final Bitmap mEndBitmap;
        private final Paint mPaint = new Paint();
        private final float mEndScale;
        private final float mProgressJump;
        private int mLeft;
        private int mTop;
        private float mProgress;

        public SwitchBitmapDrawable(Bitmap startBitmap, Bitmap endBitmap, float endScale) {
            mStartBitmap = startBitmap;
            mEndBitmap = endBitmap;
            mEndScale = endScale;
            mProgressJump = endScale > 1 ? 0.3f : 0.7f;
        }

        public void setLeft(int left) {
            mLeft = left;
            invalidateSelf();
        }

        public void setTop(int top) {
            mTop = top;
            invalidateSelf();
        }

        public void setProgress(float progress) {
            mProgress = progress;
            invalidateSelf();
        }

        public int getLeft() {
            return mLeft;
        }

        public int getTop() {
            return mTop;
        }

        public float getProgress() {
            return mProgress;
        }

        @Override
        public void draw(Canvas canvas) {
            int saveCount = canvas.save();
            canvas.translate(mLeft, mTop);

            final float scale = 1 + (mProgress * (mEndScale - 1));
            if (mProgress < mProgressJump) {
                // draw start bitmap
                canvas.scale(scale, scale);
                canvas.drawBitmap(mStartBitmap, 0, 0, mPaint);
            } else {
                // draw end bitmap
                float endScale = scale / mEndScale;
                canvas.scale(endScale, endScale);
                canvas.drawBitmap(mEndBitmap, 0, 0, mPaint);
            }
            canvas.restoreToCount(saveCount);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    static class TextResizeData {
        public final float fontSize;
        public final int paddingLeft;
        public final int paddingTop;
        public final int paddingRight;
        public final int paddingBottom;
        public final int width;
        public final int height;

        public TextResizeData(TextView textView) {
            this.fontSize = textView.getTextSize();
            this.paddingLeft = textView.getPaddingLeft();
            this.paddingTop = textView.getPaddingTop();
            this.paddingRight = textView.getPaddingRight();
            this.paddingBottom = textView.getPaddingBottom();
            this.width = textView.getWidth();
            this.height = textView.getHeight();
        }

        @Override
        public boolean equals(Object that) {
            if (!(that instanceof TextResizeData)) {
                return false;
            }
            TextResizeData other = (TextResizeData) that;
            return other.fontSize == this.fontSize;
        }
    }
}