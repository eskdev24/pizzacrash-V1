package com.example.pizzacrash.game;

/**
 * Tracks win/lose state against a LevelConfig's objectives and move limit.
 * Pure logic — unit-testable on the JVM.
 */
public final class LevelRuntime {

    public enum State { PLAYING, WON, LOST }

    private final LevelConfig config;
    private final BoardModel board;
    private State state = State.PLAYING;

    public LevelRuntime(LevelConfig config, BoardModel board) {
        this.config = config;
        this.board = board;
    }

    public LevelConfig config() { return config; }
    public BoardModel board() { return board; }
    public State state() { return state; }
    public int movesLeft() { return Math.max(0, config.moveLimit - board.movesUsed()); }

    /** Re-evaluate after every resolved move. Win takes priority over loss. */
    public void evaluate() {
        if (state != State.PLAYING) return;
        if (objectivesMet()) { state = State.WON; return; }
        if (movesLeft() == 0) state = State.LOST;
    }

    public boolean objectivesMet() {
        for (int[] obj : config.objectives) {
            if (obj[0] == -1) {
                if (board.score() < obj[1]) return false;
            } else {
                if (board.collected(obj[0]) < obj[1]) return false;
            }
        }
        return true;
    }

    public int stars() {
        int s = board.score();
        if (s >= config.star3Score) return 3;
        if (s >= config.star2Score) return 2;
        if (s >= config.star1Score) return 1;
        return state == State.WON ? 1 : 0; // winning always grants at least 1 star
    }
}
