package com.example.pizzacrash.game;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/** Tests for the data-driven level generator covering all 200 levels' validity. */
public class LevelConfigTest {

    @Test
    public void exactlyTwoHundredLevelsTenWorlds() {
        assertEquals(200, LevelConfig.TOTAL_LEVELS);
        assertEquals(10, LevelConfig.WORLDS);
        assertEquals(20, LevelConfig.LEVELS_PER_WORLD);
    }

    @Test
    public void worldAndLocalNumbering() {
        LevelConfig l1 = LevelConfig.forGlobalLevel(1);
        assertEquals(1, l1.world); assertEquals(1, l1.localLevel);
        LevelConfig l20 = LevelConfig.forGlobalLevel(20);
        assertEquals(1, l20.world); assertEquals(20, l20.localLevel);
        LevelConfig l21 = LevelConfig.forGlobalLevel(21);
        assertEquals(2, l21.world); assertEquals(1, l21.localLevel);
        LevelConfig l200 = LevelConfig.forGlobalLevel(200);
        assertEquals(10, l200.world); assertEquals(20, l200.localLevel);
    }

    @Test
    public void allLevelsAreWellFormed() {
        for (int g = 1; g <= 200; g++) {
            LevelConfig c = LevelConfig.forGlobalLevel(g);
            assertTrue("rows", c.rows >= 5 && c.rows <= 10);
            assertTrue("cols", c.cols >= 5 && c.cols <= 10);
            assertTrue("types", c.numTypes >= 4 && c.numTypes <= 6);
            assertTrue("moves", c.moveLimit >= 25);
            assertTrue("objectives", !c.objectives.isEmpty());
            for (int[] obj : c.objectives) {
                if (obj[0] >= 0) {
                    assertTrue("objective type in range", obj[0] < c.numTypes);
                    assertTrue("amount positive", obj[1] > 0);
                } else {
                    assertEquals(-1, obj[0]);
                    assertTrue("score target positive", obj[1] > 0);
                }
            }
            assertTrue("star thresholds ordered",
                    c.star1Score <= c.star2Score && c.star2Score <= c.star3Score);
            assertTrue("coin reward positive", c.coinReward > 0);
        }
    }

    @Test
    public void difficultyCurveMonotonicWithinBounds() {
        // Later worlds must not be easier on moves than early worlds overall.
        LevelConfig first = LevelConfig.forGlobalLevel(1);
        LevelConfig last = LevelConfig.forGlobalLevel(200);
        assertTrue(first.moveLimit > last.moveLimit);
        assertTrue(last.numTypes >= first.numTypes);
        assertTrue(last.rows * last.cols >= first.rows * first.cols);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLevelZero() {
        LevelConfig.forGlobalLevel(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLevel201() {
        LevelConfig.forGlobalLevel(201);
    }

    @Test
    public void everyFifthLevelHasScoreObjective() {
        for (int g = 5; g <= 200; g += 5) {
            boolean hasScore = false;
            for (int[] obj : LevelConfig.forGlobalLevel(g).objectives)
                if (obj[0] == -1) hasScore = true;
            assertTrue("level " + g + " missing score objective", hasScore);
        }
    }

    // ---------- Phase 4: playability validation ----------

    /**
     * For a sample of levels across all 10 worlds, a scripted greedy player
     * (always takes the first valid move via findHintSwap) must satisfy every
     * objective within the configured move limit. This guards against the
     * generator producing impossible levels.
     */
    @Test
    public void sampledLevelsAreWinnableByGreedyPlayer() {
        // Sweep ALL 200 levels x 3 seeds — guards against the generator ever
        // producing an unwinnable level.
        for (int g = 1; g <= LevelConfig.TOTAL_LEVELS; g++) {
            for (long seedOffset = 0; seedOffset < 3; seedOffset++) {
                int[] samples = {g};
                for (int gg : samples) {
                    LevelConfig c = LevelConfig.forGlobalLevel(gg);
                    BoardModel b = new BoardModel(c.rows, c.cols, c.numTypes, 4242L + gg + seedOffset * 100000L);
                    LevelRuntime rt = new LevelRuntime(c, b);
                    while (rt.state() == LevelRuntime.State.PLAYING && rt.movesLeft() > 0) {
                        int[] hint = b.findHintSwap();
                        if (hint == null) break; // shuffle keeps board playable; shouldn't happen
                        List<BoardModel.Step> steps = b.trySwap(hint[0], hint[1], hint[2], hint[3]);
                        assertFalse("swap should be valid at level " + gg,
                                steps.size() == 1 && steps.get(0).kind == BoardModel.Step.Kind.SWAP_BACK);
                        rt.evaluate();
                    }
                    assertEquals("level " + gg + " (seed " + seedOffset + ") should be winnable by greedy play",
                            LevelRuntime.State.WON, rt.state());
                    assertTrue("winning gives >=1 star at level " + gg, rt.stars() >= 1);
                }
            }
        }
    }

    /** Score thresholds must be ordered and star1 reachable on a greedy win. */
    @Test
    public void starThresholdsAreOrdered() {
        for (int g = 1; g <= LevelConfig.TOTAL_LEVELS; g++) {
            LevelConfig c = LevelConfig.forGlobalLevel(g);
            assertTrue("threshold order at " + g,
                    c.star1Score < c.star2Score && c.star2Score < c.star3Score);
        }
    }
}
