package com.taskflow.ui.main;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CelebrationView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private ValueAnimator animator;
    private float progress;
    private String message = "";

    public CelebrationView(Context context) {
        super(context);
        setWillNotDraw(false);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        cardPaint.setColor(Color.argb(210, 28, 35, 58));
    }

    public void play(String message, Palette palette) {
        this.message = message;
        particles.clear();
        post(() -> {
            int width = Math.max(getWidth(), 1);
            int height = Math.max(getHeight(), 1);
            for (int i = 0; i < 70; i++) {
                particles.add(new Particle(
                        width / 2f + random.nextInt(Math.max(width / 3, 1)) - width / 6f,
                        height * 0.34f,
                        (random.nextFloat() - 0.5f) * width * 0.85f,
                        -height * (0.35f + random.nextFloat() * 0.45f),
                        8f + random.nextFloat() * 10f,
                        palette.colors[random.nextInt(palette.colors.length)],
                        random.nextFloat() * 360f
                ));
            }
            startAnimator();
        });
    }

    private void startAnimator() {
        if (animator != null) {
            animator.cancel();
        }
        setVisibility(VISIBLE);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1400L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
            if (progress >= 1f) {
                setVisibility(GONE);
                ViewGroup parent = (ViewGroup) getParent();
                if (parent != null) {
                    parent.removeView(this);
                }
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float alpha = progress < 0.75f ? 1f : Math.max(0f, 1f - ((progress - 0.75f) / 0.25f));
        drawMessage(canvas, alpha);
        for (Particle particle : particles) {
            float t = progress;
            float x = particle.startX + particle.velocityX * t;
            float y = particle.startY + particle.velocityY * t + getHeight() * 0.55f * t * t;
            paint.setColor(particle.color);
            paint.setAlpha((int) (255 * alpha));
            canvas.save();
            canvas.rotate(particle.rotation + 280f * t, x, y);
            canvas.drawRoundRect(x, y, x + particle.size, y + particle.size * 0.55f, 4f, 4f, paint);
            canvas.restore();
        }
    }

    private void drawMessage(Canvas canvas, float alpha) {
        if (message == null || message.isEmpty()) {
            return;
        }
        float centerX = getWidth() / 2f;
        float centerY = getHeight() * 0.28f;
        float scale = 0.9f + 0.1f * Math.min(progress * 3f, 1f);
        textPaint.setTextSize(18f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setAlpha((int) (255 * alpha));
        cardPaint.setAlpha((int) (220 * alpha));
        RectF card = new RectF(centerX - dp(142), centerY - dp(28), centerX + dp(142), centerY + dp(28));
        canvas.save();
        canvas.scale(scale, scale, centerX, centerY);
        canvas.drawRoundRect(card, dp(18), dp(18), cardPaint);
        canvas.drawText(message, centerX, centerY + dp(7), textPaint);
        canvas.restore();
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    public static Palette completePalette() {
        return new Palette(new int[]{0xFF41E3C2, 0xFF9B7CFF, 0xFFFFD166, 0xFFFF79B8});
    }

    public static Palette addPalette() {
        return new Palette(new int[]{0xFF45B7FF, 0xFF41E3C2, 0xFFFFFFFF, 0xFFFFD166});
    }

    public static Palette reopenPalette() {
        return new Palette(new int[]{0xFFFFD166, 0xFFFF8A5B, 0xFF45B7FF, 0xFFFFFFFF});
    }

    public static class Palette {
        final int[] colors;

        Palette(int[] colors) {
            this.colors = colors;
        }
    }

    private static class Particle {
        final float startX;
        final float startY;
        final float velocityX;
        final float velocityY;
        final float size;
        final int color;
        final float rotation;

        Particle(float startX, float startY, float velocityX, float velocityY, float size, int color, float rotation) {
            this.startX = startX;
            this.startY = startY;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.color = color;
            this.rotation = rotation;
        }
    }
}
