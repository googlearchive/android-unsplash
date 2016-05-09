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
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Transitions a TextView from one font size to another. This does not
 * do any animation of TextView content and if the text changes, this
 * transition will not run.
 * <p>
 * The animation works by capturing a bitmap of the text at the start
 * and end states. It then scales the start bitmap until it reaches
 * a threshold and switches to the scaled end bitmap for the remainder
 * of the animation. This keeps the jump in bitmaps in the middle of
 * the animation, where it is less noticeable than at the beginning
 * or end of the animation.
 */
public class TextResize extends Transition {
    private static final String FONT_SIZE = "TextResize:fontSize";
    private static final String DATA = "TextResize:data";

    private static final String[] PROPERTIES = {
            // We only care about FONT_SIZE. If anything else changes, we don't
            // want this transition to be called to create an Animator.
            FONT_SIZE,
    };

    public TextResize() {
        addTarget(TextView.class);
    }

    /**
     * Constructor used from XML.
     */
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
        final float fontSize = view.getTextSize();
        transitionValues.values.put(FONT_SIZE, fontSize);
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
        float startFontSize = (Float) startValues.values.get(FONT_SIZE);
        float endFontSize = (Float) endValues.values.get(FONT_SIZE);
        final TextResizeData startData = (TextResizeData) startValues.values.get(DATA);
        final TextResizeData endData = (TextResizeData) endValues.values.get(DATA);

        // Capture the end bitmap -- it is already set up for it, so we can capture now.
        final Bitmap endBitmap = captureTextBitmap(textView);
        if (endBitmap == null) {
            endFontSize = 0;
        }

        // Capture the start bitmap -- we need to set the values to the start values first
        textView.setPadding(startData.paddingLeft, startData.paddingTop, startData.paddingRight,
                startData.paddingBottom);
        textView.setRight(textView.getLeft() + startData.width);
        textView.setBottom(textView.getTop() + startData.height);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, startFontSize);
        final Bitmap startBitmap = captureTextBitmap(textView);

        // Reset the values to the end values
        textView.setPadding(endData.paddingLeft, endData.paddingTop, endData.paddingRight,
                endData.paddingBottom);
        textView.setRight(textView.getLeft() + endData.width);
        textView.setBottom(textView.getTop() + endData.height);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, endFontSize);
        if (startBitmap == null) {
            startFontSize = 0;
        }

        if (startFontSize == 0 && endFontSize == 0) {
            return null; // Can't animate null bitmaps
        }

        // Set the colors of the TextView so that nothing is drawn.
        // Only draw the bitmaps in the overlay.
        final ColorStateList textColors = textView.getTextColors();
        final ColorStateList hintColors = textView.getHintTextColors();
        final int highlightColor = textView.getHighlightColor();
        final ColorStateList linkColors = textView.getLinkTextColors();
        textView.setTextColor(Color.TRANSPARENT);
        textView.setHintTextColor(Color.TRANSPARENT);
        textView.setHighlightColor(Color.TRANSPARENT);
        textView.setLinkTextColor(Color.TRANSPARENT);

        // Create the drawable that will be animated in the TextView's overlay.
        // Ensure that it is showing the start state now.
        final SwitchBitmapDrawable drawable =
                new SwitchBitmapDrawable(startBitmap, endBitmap, startFontSize, endFontSize);
        textView.getOverlay().add(drawable);
        drawable.setLeft(startData.paddingLeft);
        drawable.setTop(startData.paddingTop);

        // Animate the progress (scale), and the padding
        Animator progress = ObjectAnimator.ofFloat(drawable, "progress", 0, 1);
        Animator left = ObjectAnimator.ofInt(drawable, "left", startData.paddingLeft,
                endData.paddingLeft);
        Animator top = ObjectAnimator.ofInt(drawable, "top", startData.paddingTop,
                endData.paddingTop);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(left, top, progress);

        // Remove the overlay and reset the colors after the animation completes
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
        if (width == 0 || height == 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(-textView.getPaddingLeft(), -textView.getPaddingTop());
        textView.draw(canvas);
        textView.setBackground(background);
        return bitmap;
    }

    /**
     * This Drawable is used to scale the start and end bitmaps and switch between them
     * at the appropriate progress.
     */
    private static class SwitchBitmapDrawable extends Drawable {
        private final Bitmap startBitmap;
        private final Bitmap endBitmap;
        private final Paint paint = new Paint();
        private final float startFontSize;
        private final float endFontSize;
        private int left;
        private int top;
        private float progress;

        public SwitchBitmapDrawable(Bitmap startBitmap, Bitmap endBitmap, float startFontSize,
                                    float endFontSize) {
            this.startBitmap = startBitmap;
            this.endBitmap = endBitmap;
            this.startFontSize = startFontSize;
            this.endFontSize = endFontSize;
        }

        /**
         * Offsets the left of the drawable by left. Used for animating the left padding.
         * @param left The left padding in pixels.
         */
        public void setLeft(int left) {
            this.left = left;
            invalidateSelf();
        }

        /**
         * Offsets the top of the drawable by top. Used for animating the top padding.
         * @param top The top padding in pixels.
         */
        public void setTop(int top) {
            this.top = top;
            invalidateSelf();
        }

        /**
         * Sets the progress of the scaled animation. This is used to choose how the bitmaps
         * are scaled and which bitmap to use.
         * @param progress The progress of the animation, between 0 and 1, inclusive.
         */
        public void setProgress(float progress) {
            this.progress = progress;
            invalidateSelf();
        }

        /**
         * @return The left padding used to offset the drawable.
         */
        public int getLeft() {
            return left;
        }

        /**
         * @return The top padding used to offset the drawable.
         */
        public int getTop() {
            return top;
        }

        /**
         * @return The progress used to scale the bitmaps and threshold.
         */
        public float getProgress() {
            return progress;
        }

        @Override
        public void draw(Canvas canvas) {
            int saveCount = canvas.save();
            canvas.translate(left, top);

            // The threshold changes depending on the target font sizes. We want to switch
            // later if the difference is greater (scaled-up fonts look bad). To the point
            // where a bitmap of text with a font size of 0 will never be used.
            float threshold = startFontSize / (startFontSize + endFontSize);
            // fontSize is the target scaled font size
            float fontSize = startFontSize + (progress * (endFontSize - startFontSize));
            if (progress < threshold) {
                // draw start bitmap
                final float scale = fontSize / startFontSize;
                canvas.scale(scale, scale);
                canvas.drawBitmap(startBitmap, 0, 0, paint);
            } else {
                // draw end bitmap
                final float scale = fontSize / endFontSize;
                canvas.scale(scale, scale);
                canvas.drawBitmap(endBitmap, 0, 0, paint);
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

    /**
     * Contains all the non-font-size data used by the TextResize transition.
     * None of these values should trigger the transition, so they are not listed
     * in PROPERTIES. These are captured together to avoid boxing of all the
     * primitives while adding to TransitionValues.
     */
    static class TextResizeData {
        public final int paddingLeft;
        public final int paddingTop;
        public final int paddingRight;
        public final int paddingBottom;
        public final int width;
        public final int height;

        public TextResizeData(TextView textView) {
            this.paddingLeft = textView.getPaddingLeft();
            this.paddingTop = textView.getPaddingTop();
            this.paddingRight = textView.getPaddingRight();
            this.paddingBottom = textView.getPaddingBottom();
            this.width = textView.getWidth();
            this.height = textView.getHeight();
        }
    }
}