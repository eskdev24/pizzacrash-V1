package com.example.pizzacrash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.audio.AudioManager;
import com.example.pizzacrash.data.SaveRepository;
import com.example.pizzacrash.game.BoardModel;
import com.example.pizzacrash.game.BoardView;
import com.example.pizzacrash.game.LevelConfig;
import com.example.pizzacrash.game.LevelRuntime;
import com.example.pizzacrash.game.TileType;

import java.util.List;

/**
 * Gameplay screen for a single level. Owns LevelRuntime + BoardView, HUD updates,
 * pause dialog, and win/lose results. All rules live in the game package; this
 * class only wires UI <-> engine.
 */
public class GameActivity extends AppCompatActivity implements BoardView.Listener {

    public static final String EXTRA_GLOBAL_LEVEL = "global_level";

    private LevelConfig config;
    private LevelRuntime runtime;
    private BoardView boardView;
    private SaveRepository save;
    private AudioManager audio;

    private TextView tvScore, tvMoves, tvObjective, tvBoostStatus;
    private LinearLayout objectiveRow;
    private DialogPauseController pauseCtrl;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private String pendingRainbowBooster; // id of booster awaiting ingredient pick

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        int globalLevel = getIntent().getIntExtra(EXTRA_GLOBAL_LEVEL, 1);
        config = LevelConfig.load(this, globalLevel);
        save = new SaveRepository(this);
        audio = AudioManager.get(this);

        long seed = System.currentTimeMillis() ^ ((long) globalLevel << 32);
        BoardModel model = new BoardModel(config.rows, config.cols, config.numTypes, seed);
        runtime = new LevelRuntime(config, model);

        boardView = findViewById(R.id.board_view);
        boardView.setModel(model);
        boardView.setListener(this);

        tvScore = findViewById(R.id.tv_score);
        tvMoves = findViewById(R.id.tv_moves);
        tvObjective = findViewById(R.id.tv_objective_label);
        objectiveRow = findViewById(R.id.objective_row);
        buildObjectiveChips();

        findViewById(R.id.btn_pause).setOnClickListener(v -> showPause());

        findViewById(R.id.btn_boost_hammer).setOnClickListener(v ->
                startBooster("hammer", BoardView.BoostMode.HAMMER));
        findViewById(R.id.btn_boost_bomb).setOnClickListener(v ->
                startBooster("bomb", BoardView.BoostMode.BOMB));
        findViewById(R.id.btn_boost_swap).setOnClickListener(v ->
                startBooster("swap", BoardView.BoostMode.SWAP));
        findViewById(R.id.btn_boost_rainbow).setOnClickListener(v -> startRainbow());
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        tvBoostStatus = findViewById(R.id.tv_boost_status);

        updateHud();
        updateBoosterBar();
    }

    private void buildObjectiveChips() {
        objectiveRow.removeAllViews();
        for (int[] obj : config.objectives) {
            TextView chip = new TextView(this);
            chip.setText(obj[0] == -1
                    ? "★ " + obj[1]
                    : TileType.fromId(obj[0]).glyph + " 0/" + obj[1]);
            chip.setTag(obj);
            chip.setTextSize(14f);
            chip.setTextColor(0xFFFFFFFF);
            chip.setBackgroundResource(R.drawable.bg_round_button);
            int p = dp(8);
            chip.setPadding(p, p / 2, p, p / 2);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), 0, dp(4), 0);
            chip.setLayoutParams(lp);
            objectiveRow.addView(chip);
        }
        tvObjective.setVisibility(View.GONE); // chips carry their own labels
    }

    private void updateHud() {
        tvScore.setText(getString(R.string.game_score_fmt, runtime.board().score()));
        tvMoves.setText(getString(R.string.game_moves_fmt, runtime.movesLeft()));
        for (int i = 0; i < objectiveRow.getChildCount(); i++) {
            TextView chip = (TextView) objectiveRow.getChildAt(i);
            int[] obj = (int[]) chip.getTag();
            if (obj[0] == -1) chip.setText("★ " + Math.min(runtime.board().score(), obj[1]) + "/" + obj[1]);
            else chip.setText(TileType.fromId(obj[0]).glyph + " " +
                    Math.min(runtime.board().collected(obj[0]), obj[1]) + "/" + obj[1]);
        }
    }

    // ---- Boosters ----

    private void startBooster(String id, BoardView.BoostMode mode) {
        if (runtime.state() != LevelRuntime.State.PLAYING || boardView.boostMode() != BoardView.BoostMode.NONE)
            return;
        if (!save.consumeBooster(id)) {
            showBoostStatus(getString(R.string.boost_none_left));
            return;
        }
        boardView.setBoostMode(mode);
        showBoostStatus(mode == BoardView.BoostMode.SWAP
                ? getString(R.string.boost_select_two) : getString(R.string.boost_select_target));
        updateBoosterBar();
    }

    /** Rainbow needs an ingredient choice first: reuse the objective chips as pickers. */
    private void startRainbow() {
        if (runtime.state() != LevelRuntime.State.PLAYING) return;
        if (pendingRainbowBooster != null) { cancelRainbowPick(); return; }
        if (!save.consumeBooster("rainbow")) {
            showBoostStatus(getString(R.string.boost_none_left));
            return;
        }
        pendingRainbowBooster = "rainbow";
        // First ingredient type present on the board gets cleared by the rainbow.
        int chosen = -1;
        BoardModel b = runtime.board();
        outer:
        for (int t = 0; t < b.numTypes(); t++) {
            for (int r = 0; r < b.rows(); r++)
                for (int c = 0; c < b.cols(); c++)
                    if (BoardModel.baseTypeOf(b.cell(r, c), b.numTypes()) == t) { chosen = t; break outer; }
        }
        if (chosen < 0) { cancelRainbowPick(); return; }
        final int type = chosen;
        showBoostStatus("Rainbow ready \u2014 clearing " + TileType.fromId(type).glyph);
        ui.postDelayed(() -> {
            if (!"rainbow".equals(pendingRainbowBooster)) return;
            pendingRainbowBooster = null;
            hideBoostStatus();
            finishBooster(runtime.board().useRainbow(type));
        }, 600);
        updateBoosterBar();
    }

    private void cancelRainbowPick() {
        pendingRainbowBooster = null;
        for (int i = 0; i < objectiveRow.getChildCount(); i++)
            objectiveRow.getChildAt(i).setOnClickListener(null);
        save.addBoosters("rainbow", 1); // refund, not used
        hideBoostStatus();
        updateBoosterBar();
    }

    @Override
    public void onBoosterCellPicked(int r, int c) {
        BoardView.BoostMode mode = boardView.boostMode();
        boardView.setBoostMode(BoardView.BoostMode.NONE);
        hideBoostStatus();
        List<BoardModel.Step> steps = mode == BoardView.BoostMode.HAMMER
                ? runtime.board().useHammer(r, c) : runtime.board().useBomb(r, c);
        finishBooster(steps);
    }

    @Override
    public void onBoosterPairPicked(int r1, int c1, int r2, int c2) {
        boardView.setBoostMode(BoardView.BoostMode.NONE);
        hideBoostStatus();
        finishBooster(runtime.board().useSwapBoost(r1, c1, r2, c2));
    }

    /** Animate the booster result without consuming a move, then refresh HUD/state. */
    private void finishBooster(List<BoardModel.Step> steps) {
        audio.playEffect(AudioManager.SFX_BOOSTER);
        boardView.animateSteps(steps, () -> {
            updateHud();
            updateBoosterBar();
            runtime.evaluate();
            if (runtime.state() == LevelRuntime.State.WON) finishLevel(true);
            else if (runtime.state() == LevelRuntime.State.LOST) finishLevel(false);
        });
    }

    private void showHint() {
        if (runtime.state() != LevelRuntime.State.PLAYING || boardView.boostMode() != BoardView.BoostMode.NONE)
            return;
        int[] hint = runtime.board().findHintSwap();
        if (hint != null) boardView.flashHint(hint[0], hint[1], hint[2], hint[3]);
    }

    private void updateBoosterBar() {
        bindCount(R.id.btn_boost_hammer, R.string.booster_hammer, "hammer");
        bindCount(R.id.btn_boost_bomb, R.string.booster_bomb, "bomb");
        bindCount(R.id.btn_boost_swap, R.string.booster_swap, "swap");
        bindCount(R.id.btn_boost_rainbow, R.string.booster_rainbow, "rainbow");
    }

    private void bindCount(int viewId, int nameRes, String boosterId) {
        TextView tv = findViewById(viewId);
        tv.setText(getString(nameRes) + " \u00d7" + save.getBoosterCount(boosterId));
        tv.setAlpha(save.getBoosterCount(boosterId) > 0 ? 1f : 0.45f);
    }

    private void showBoostStatus(String msg) {
        tvBoostStatus.setText(msg);
        tvBoostStatus.setVisibility(View.VISIBLE);
        ui.removeCallbacks(hideBoost);
        ui.postDelayed(hideBoost, 4000);
    }

    private void hideBoostStatus() {
        ui.removeCallbacks(hideBoost);
        tvBoostStatus.setVisibility(View.GONE);
    }

    private final Runnable hideBoost = () -> tvBoostStatus.setVisibility(View.GONE);

    // ---- BoardView.Listener ----

    @Override
    public void onMoveResolved(List<BoardModel.Step> steps) {
        updateHud();
        audio.playEffect(AudioManager.SFX_MATCH);
        runtime.evaluate();
        if (runtime.state() == LevelRuntime.State.WON) finishLevel(true);
        else if (runtime.state() == LevelRuntime.State.LOST) finishLevel(false);
    }

    @Override
    public void onInvalidSwap() {
        audio.playEffect(AudioManager.SFX_INVALID);
    }

    private void finishLevel(boolean won) {
        audio.stopMusic();
        int stars = won ? runtime.stars() : 0;
        // Duplicate-safe rewards: coins are granted only when the record improves.
        boolean improved = won && save.recordResult(config.globalLevel, stars, runtime.board().score());
        if (improved) {
            save.addCoins(config.coinReward);
            save.unlockLevel(Math.min(SaveRepository.TOTAL_LEVELS, config.globalLevel + 1));
        }
        Intent i = new Intent(this, ResultActivity.class);
        i.putExtra(ResultActivity.EXTRA_WON, won);
        i.putExtra(ResultActivity.EXTRA_GLOBAL_LEVEL, config.globalLevel);
        i.putExtra(ResultActivity.EXTRA_SCORE, runtime.board().score());
        i.putExtra(ResultActivity.EXTRA_STARS, stars);
        i.putExtra(ResultActivity.EXTRA_COINS, improved ? config.coinReward : 0);
        startActivity(i);
        finish();
    }

    private void showPause() {
        if (pauseCtrl == null) pauseCtrl = new DialogPauseController(this);
        pauseCtrl.show(() -> { /* resumed */ }, () -> { audio.stopMusic(); finish(); });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Auto-pause when leaving the level so the board can't be resolved in background.
        if (!isFinishing() && runtime != null && runtime.state() == LevelRuntime.State.PLAYING) {
            showPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Singleton audio manager is app-scoped; only stop our music here.
        audio.stopMusic();
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
