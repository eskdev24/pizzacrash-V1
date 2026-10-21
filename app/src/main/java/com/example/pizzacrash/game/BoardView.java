package com.example.pizzacrash.game;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.List;

/**
 * Custom-Canvas match-3 board renderer. Reads BoardModel state and animates the
 * Step list produced by trySwap(). Touch input is ignored while animating so the
 * player can never issue conflicting moves mid-resolution.
 */
public class BoardView extends View {

    public interface Listener {
        void onMoveResolved(List<BoardModel.Step> steps);
        void onInvalidSwap();
        /** Booster targeting: single-cell pick (hammer/bomb) or two-cell pick (swap). */
        default void onBoosterCellPicked(int r, int c) {}
        default void onBoosterPairPicked(int r1, int c1, int r2, int c2) {}
    }

    /** Touch routing modes for booster activation flows. */
    public enum BoostMode { NONE, HAMMER, BOMB, SWAP }

    private BoardModel model;
    private Listener listener;

    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float cellSize, padX, padY;
    private int selectedR = -1, selectedC = -1;
    private int prevSelectedR = -1, prevSelectedC = -1; // tap-tap swap memory
    private float downX, downY;
    private boolean animating = false;
    private BoostMode boostMode = BoostMode.NONE;
    private int boostFirstR = -1, boostFirstC = -1; // swap-booster first pick
    private final Paint specialRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // simple per-cell animation overlays
    private float[] scaleFx;   // pop-out scale for CLEAR (1 -> 0)
    private float[] alphaFx;   // fade-in for REFILL
    private ValueAnimator currentAnim;

    public BoardView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        glyphPaint.setTextAlign(Paint.Align.CENTER);
        selPaint.setStyle(Paint.Style.STROKE);
        selPaint.setStrokeWidth(dp(4));
        selPaint.setColor(Color.WHITE);
        specialRingPaint.setStyle(Paint.Style.STROKE);
        specialRingPaint.setStrokeWidth(dp(3));
        specialRingPaint.setColor(0xFFFFF176); // warm gold ring marks specials
        starPaint.setTextAlign(Paint.Align.CENTER);
        starPaint.setColor(0xFF3E2723);
    }

    public void setModel(BoardModel m) {
        this.model = m;
        scaleFx = new float[m.rows() * m.cols()];
        alphaFx = new float[m.rows() * m.cols()];
        requestLayout();
        invalidate();
    }

    public void setListener(Listener l) { this.listener = l; }

    /** Enter/leave booster targeting mode. While active, taps route to the listener. */
    public void setBoostMode(BoostMode mode) {
        this.boostMode = mode;
        this.boostFirstR = this.boostFirstC = -1;
        this.selectedR = this.selectedC = -1;
        this.prevSelectedR = this.prevSelectedC = -1;
        invalidate();
    }

    public BoostMode boostMode() { return boostMode; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        if (model == null) { super.onMeasure(wSpec, hSpec); return; }
        int w = MeasureSpec.getSize(wSpec);
        int availH = MeasureSpec.getSize(hSpec);
        cellSize = Math.min(w / (float) model.cols(),
                availH > 0 ? availH / (float) model.rows() : w / (float) model.cols());
        int vw = (int) (cellSize * model.cols());
        int vh = (int) (cellSize * model.rows());
        padX = (w - vw) / 2f;
        setMeasuredDimension(vw, vh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (model == null) return;
        glyphPaint.setTextSize(cellSize * 0.62f);
        for (int r = 0; r < model.rows(); r++) {
            for (int c = 0; c < model.cols(); c++) {
                int stored = model.cell(r, c);
                if (stored < 0) continue;
                int t = BoardModel.baseTypeOf(stored, model.numTypes());
                SpecialType sp = BoardModel.specialOf(stored, model.numTypes());
                TileType type = TileType.fromId(t);
                int idx = r * model.cols() + c;
                float s = scaleFx != null && scaleFx[idx] != 0f ? scaleFx[idx] : 1f;
                int a = alphaFx != null && alphaFx[idx] != 0f ? (int) (alphaFx[idx] * 255) : 255;

                float cx = padX + c * cellSize + cellSize / 2f;
                float cy = r * cellSize + cellSize / 2f;
                float half = (cellSize * 0.44f) * s;

                tilePaint.setAlpha(a);
                tilePaint.setColor(type.color);
                rect.set(cx - half, cy - half, cx + half, cy + half);
                canvas.drawRoundRect(rect, dp(10), dp(10), tilePaint);

                // glossy highlight strip
                tilePaint.setColor(0x33FFFFFF);
                rect.set(cx - half * 0.8f, cy - half * 0.9f, cx + half * 0.8f, cy - half * 0.35f);
                canvas.drawRoundRect(rect, dp(8), dp(8), tilePaint);

                glyphPaint.setAlpha(a);
                float baseline = cy - (glyphPaint.ascent() + glyphPaint.descent()) / 2f;
                canvas.drawText(type.glyph, cx, baseline, glyphPaint);

                if (sp != SpecialType.NONE) {
                    canvas.drawRoundRect(rect, dp(10), dp(10), specialRingPaint);
                    starPaint.setTextSize(cellSize * 0.34f);
                    String mark = sp == SpecialType.ROW_BLAST ? "\u2194"      // ↔
                            : sp == SpecialType.COL_CRUSH ? "\u2195"          // ↕
                            : "\u2605";                                        // ★ bomb
                    float my = cy + half - starPaint.getTextSize() * 0.35f;
                    canvas.drawText(mark, cx, my, starPaint);
                }

                if (r == selectedR && c == selectedC) {
                    rect.set(cx - half - dp(3), cy - half - dp(3), cx + half + dp(3), cy + half + dp(3));
                    canvas.drawRoundRect(rect, dp(12), dp(12), selPaint);
                }
            }
        }
    }

    private int dragStartR = -1, dragStartC = -1;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (model == null || animating) return true;
        float x = e.getX(), y = e.getY();
        int c = (int) ((x - padX) / cellSize);
        int r = (int) (y / cellSize);
        if (c < 0 || r < 0 || c >= model.cols() || r >= model.rows()) {
            selectedR = selectedC = -1; invalidate(); return true;
        }
        if (boostMode != BoostMode.NONE && e.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx2 = x - downX, dy2 = y - downY;
            boolean tap2 = Math.abs(dx2) < cellSize * 0.3f && Math.abs(dy2) < cellSize * 0.3f;
            if (tap2 && listener != null) {
                if (boostMode == BoostMode.SWAP) {
                    if (boostFirstR < 0) {
                        boostFirstR = r; boostFirstC = c;
                        selectedR = r; selectedC = c;
                        invalidate();
                    } else {
                        int sr = boostFirstR, sc = boostFirstC;
                        boostFirstR = boostFirstC = -1;
                        selectedR = selectedC = -1;
                        BoostMode m = boostMode;
                        boostMode = BoostMode.NONE;
                        listener.onBoosterPairPicked(sr, sc, r, c);
                        boostMode = m;
                    }
                } else {
                    BoostMode m = boostMode;
                    boostMode = BoostMode.NONE;
                    listener.onBoosterCellPicked(r, c);
                    boostMode = m;
                }
            }
            return true;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x; downY = y;
                dragStartR = r; dragStartC = c;
                selectedR = r; selectedC = c;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX, dy = y - downY;
                if (Math.abs(dx) > cellSize * 0.5f || Math.abs(dy) > cellSize * 0.5f) {
                    int tr = dragStartR, tc = dragStartC;
                    if (Math.abs(dx) > Math.abs(dy)) tc += dx > 0 ? 1 : -1;
                    else tr += dy > 0 ? 1 : -1;
                    dragStartR = dragStartC = -1; // consume gesture once
                    selectedR = selectedC = -1;
                    doSwap(r, c, tr, tc); // r,c is the dragged-from tile's neighbor origin
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                float dx = x - downX, dy = y - downY;
                boolean tap = Math.abs(dx) < cellSize * 0.3f && Math.abs(dy) < cellSize * 0.3f;
                if (tap && dragStartR >= 0) {
                    // tap-to-select then tap-adjacent-to-swap
                    if (prevSelectedR >= 0 && prevSelectedC >= 0
                            && Math.abs(prevSelectedR - r) + Math.abs(prevSelectedC - c) == 1) {
                        int sr = prevSelectedR, sc = prevSelectedC;
                        prevSelectedR = prevSelectedC = -1;
                        selectedR = selectedC = -1;
                        doSwap(sr, sc, r, c);
                    } else {
                        prevSelectedR = r; prevSelectedC = c;
                        selectedR = r; selectedC = c;
                    }
                } else {
                    prevSelectedR = prevSelectedC = -1;
                    selectedR = selectedC = -1;
                }
                dragStartR = dragStartC = -1;
                invalidate();
                return true;
            }
            default:
                return super.onTouchEvent(e);
        }
    }

    private void doSwap(int r1, int c1, int r2, int c2) {
        List<BoardModel.Step> steps = model.trySwap(r1, c1, r2, c2);
        if (steps.isEmpty()) return;
        if (steps.size() == 1 && steps.get(0).kind == BoardModel.Step.Kind.SWAP_BACK) {
            animateSwapBack(steps.get(0), () -> {
                if (listener != null) listener.onInvalidSwap();
            });
            return;
        }
        playSteps(steps, 0);
    }

    /** Sequentially play resolution steps with lightweight tweening. */
    private void playSteps(List<BoardModel.Step> steps, int index) {
        if (index >= steps.size()) {
            animating = false;
            if (listener != null) listener.onMoveResolved(steps);
            invalidate();
            return;
        }
        animating = true;
        BoardModel.Step step = steps.get(index);
        long dur = 180;
        Runnable next = () -> playSteps(steps, index + 1);
        switch (step.kind) {
            case CLEAR: {
                dur = 200;
                ValueAnimator va = ValueAnimator.ofFloat(1f, 0f);
                va.setDuration(dur);
                va.setInterpolator(new AccelerateInterpolator());
                va.addUpdateListener(a -> {
                    float v = (float) a.getAnimatedValue();
                    for (int[] cell : step.cleared) scaleFx[cell[0] * model.cols() + cell[1]] = v;
                    invalidate();
                });
                va.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator a) {
                        for (int[] cell : step.cleared) scaleFx[cell[0] * model.cols() + cell[1]] = 0f;
                        next.run();
                    }
                });
                currentAnim = va; va.start();
                break;
            }
            case REFILL: {
                dur = 220;
                ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
                va.setDuration(dur);
                va.setInterpolator(new DecelerateInterpolator());
                va.addUpdateListener(a -> {
                    float v = (float) a.getAnimatedValue();
                    for (int[] cell : step.cleared) alphaFx[cell[0] * model.cols() + cell[1]] = v;
                    invalidate();
                });
                va.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator a) {
                        for (int[] cell : step.cleared) alphaFx[cell[0] * model.cols() + cell[1]] = 0f;
                        next.run();
                    }
                });
                currentAnim = va; va.start();
                break;
            }
            default: { // SWAP / FALL / SHUFFLE — grid already updated; short pause + redraw
                ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
                va.setDuration(dur);
                va.addUpdateListener(a -> invalidate());
                va.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator a) { next.run(); }
                });
                currentAnim = va; va.start();
                break;
            }
        }
    }

    /** Public entry for booster results: play steps then invoke onDone (no listener callback). */
    public void animateSteps(List<BoardModel.Step> steps, Runnable onDone) {
        if (steps == null || steps.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        animating = true;
        playStepsForced(steps, 0, onDone);
    }

    /** Flash a suggested swap by briefly highlighting both cells. */
    public void flashHint(int r1, int c1, int r2, int c2) {
        prevSelectedR = r1; prevSelectedC = c1;
        selectedR = r2; selectedC = c2;
        invalidate();
        postDelayed(() -> {
            if (selectedR == r2 && selectedC == c2) {
                selectedR = selectedC = -1;
                prevSelectedR = prevSelectedC = -1;
                invalidate();
            }
        }, 900);
    }

    private void playStepsForced(List<BoardModel.Step> steps, int index, Runnable onDone) {
        if (index >= steps.size()) {
            animating = false;
            invalidate();
            if (onDone != null) onDone.run();
            return;
        }
        BoardModel.Step step = steps.get(index);
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(step.kind == BoardModel.Step.Kind.CLEAR ? 200 : 180);
        va.addUpdateListener(a -> invalidate());
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                playStepsForced(steps, index + 1, onDone);
            }
        });
        currentAnim = va; va.start();
    }

    private void animateSwapBack(BoardModel.Step step, Runnable done) {
        animating = true;
        int[] q = step.cleared.get(0);
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f, 0f);
        va.setDuration(240);
        va.addUpdateListener(a -> invalidate()); // visual nudge handled by selection ring
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                animating = false;
                invalidate();
                done.run();
            }
        });
        currentAnim = va; va.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (currentAnim != null) currentAnim.cancel();
        animating = false;
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
