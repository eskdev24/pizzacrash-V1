package com.example.pizzacrash.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core match-3 board logic. Pure Java, deterministic when given a seeded Random,
 * so it can be unit tested on the JVM without Android dependencies.
 *
 * Cell encoding: values 0..numTypes-1 are plain tiles of that type.
 * Specials are encoded as numTypes + specialIndex*numTypes + baseType,
 * where specialIndex is 1=ROW_BLAST, 2=COL_CRUSH, 3=PIZZA_BOMB (see SpecialType).
 */
public class BoardModel {

    /** One animation step produced by resolving a move. */
    public static class Step {
        public enum Kind { SWAP, SWAP_BACK, CLEAR, DROP, REFILL, SPECIAL_FX, SHUFFLE }
        public final Kind kind;
        public final int r1, c1, r2, c2;   // swap/drop endpoints; clear/refill uses r1,c1
        public final int tileType;          // for REFILL / CLEAR visuals
        public final SpecialType special;   // for CLEAR visuals / SPECIAL_FX
        public final List<int[]> cleared;   // cells cleared in this step (CLEAR/SPECIAL_FX)

        Step(Kind kind, int r1, int c1, int r2, int c2, int tileType, SpecialType special, List<int[]> cleared) {
            this.kind = kind; this.r1 = r1; this.c1 = c1; this.r2 = r2; this.c2 = c2;
            this.tileType = tileType; this.special = special; this.cleared = cleared;
        }

        static Step swap(int r1, int c1, int r2, int c2) {
            return new Step(Kind.SWAP, r1, c1, r2, c2, -1, SpecialType.NONE, null);
        }
        static Step swapBack(int r1, int c1, int r2, int c2) {
            return new Step(Kind.SWAP_BACK, r1, c1, r2, c2, -1, SpecialType.NONE, null);
        }
        static Step clear(int r, int c, List<int[]> cells, SpecialType sp) {
            return new Step(Kind.CLEAR, r, c, -1, -1, baseOf(cells), sp, cells);
        }
        static Step drop(int r1, int c1, int r2, int c2) {
            return new Step(Kind.DROP, r1, c1, r2, c2, -1, SpecialType.NONE, null);
        }
        static Step refill(int r, int c, int type) {
            return new Step(Kind.REFILL, r, c, -1, -1, type, SpecialType.NONE, null);
        }
        private static int baseOf(List<int[]> cells) {
            return cells.isEmpty() ? -1 : cells.get(0)[2];
        }
    }

    private final int rows, cols, numTypes;
    private final Random random;
    /** Live grid reference (tests mutate it directly via cells()). */
    private final int[][] grid;
    private int score = 0;
    private int movesUsed = 0;
    private final int[] collectedByType;
    private int cascadeCount = 0;

    /** Called when a group of cells has been cleared (for scoring/objectives). */
    public interface ClearListener { void onCleared(int count, int cascadeDepth); }
    private ClearListener clearListener;

    public void setClearListener(ClearListener l) { this.clearListener = l; }

    public BoardModel(int rows, int cols, int numTypes, long seed) {
        if (rows < 5 || cols < 5) throw new IllegalArgumentException("board too small");
        if (numTypes < 3) throw new IllegalArgumentException("need at least 3 tile types");
        this.rows = rows; this.cols = cols; this.numTypes = numTypes;
        this.collectedByType = new int[numTypes];
        this.random = new Random(seed);
        this.grid = new int[rows][cols];
        initNoMatches();
    }

    // ---------- score / moves / objectives ----------

    public int score() { return score; }
    public int movesUsed() { return movesUsed; }
    public int collected(int tileType) {
        return (tileType >= 0 && tileType < numTypes) ? collectedByType[tileType] : 0;
    }

    private void registerClear(List<int[]> cells) {
        for (int[] cell : cells) {
            int t = cell[2];
            if (t >= 0 && t < numTypes) collectedByType[t]++;
        }
        boolean clearedSomething = !cells.isEmpty();
        if (clearedSomething) cascadeCount++;
        int gained = cells.size() * 10 * (clearedSomething ? Math.max(1, cascadeCount) : 1);
        score += gained;
        if (clearListener != null) clearListener.onCleared(cells.size(), cascadeCount);
    }

    // ---------- encoding helpers ----------

    public int numTypes() { return numTypes; }
    public int rows() { return rows; }
    public int cols() { return cols; }
    public int cell(int r, int c) { return grid[r][c]; }

    /**
     * Test/debug helper: live grid reference (mutating it mutates the board).
     * The engine's own init path only ever stores plain base types, so this is
     * safe for authoring deterministic test boards. Production code must use
     * cell()/setCell() which understand special encoding.
     */
    public int[][] cells() {
        return grid;
    }

    /** Deterministic match-free stripe pattern used by tests and tooling. */
    public void paintStripes() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = (r + 2 * c) % numTypes;
    }

    /** Flat list of every matched cell [r,c,type] (used by tests). */
    public List<int[]> findMatches() {
        List<int[]> flat = new ArrayList<>();
        for (List<int[]> group : findAllMatches()) flat.addAll(group);
        return flat;
    }

    /** Step describing a stuck-board shuffle (produced by shuffleSteps()). */
    public Step shuffleStep() {
        return new Step(Step.Kind.SHUFFLE, -1, -1, -1, -1, -1, SpecialType.NONE, null);
    }

    /** Shuffle and return the animation step for it. */
    public List<Step> shuffleSteps() {
        shuffle();
        List<Step> steps = new ArrayList<>();
        steps.add(shuffleStep());
        return steps;
    }

    public static int encodeSpecial(SpecialType sp, int baseType, int numTypes) {
        if (sp == SpecialType.NONE) return baseType;
        return numTypes + sp.ordinal() * numTypes + baseType;
    }

    public static int baseTypeOf(int stored, int numTypes) {
        if (stored < numTypes) return stored;
        return (stored - numTypes) % numTypes;
    }

    public static SpecialType specialOf(int stored, int numTypes) {
        if (stored < numTypes) return SpecialType.NONE;
        int idx = (stored - numTypes) / numTypes;
        SpecialType[] all = SpecialType.values();
        return (idx >= 0 && idx < all.length) ? all[idx] : SpecialType.NONE;
    }

    // ---------- initialization ----------

    private void initNoMatches() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int t;
                do {
                    t = random.nextInt(numTypes);
                } while (createsInitialMatch(r, c, t));
                grid[r][c] = t;
            }
        }
    }

    private boolean createsInitialMatch(int r, int c, int t) {
        if (c >= 2 && grid[r][c - 1] == t && grid[r][c - 2] == t) return true;
        if (r >= 2 && grid[r - 1][c] == t && grid[r - 2][c] == t) return true;
        return false;
    }

    // ---------- swapping ----------

    public boolean areAdjacent(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1;
    }

    /**
     * Attempt a player swap. Returns the animation steps to play, or a
     * SWAP+SWAP_BACK pair if the swap is invalid. A swap involving two
     * different specials is always valid (combo). Consumes one move via caller.
     */
    public List<Step> trySwap(int r1, int c1, int r2, int c2) {
        return trySwapImpl(r1, c1, r2, c2, true);
    }

    /** Internal swap. When countMove, a valid (non-reverted) swap consumes one move. */
    private List<Step> trySwapImpl(int r1, int c1, int r2, int c2, boolean countMove) {
        List<Step> steps = new ArrayList<>();
        if (!areAdjacent(r1, c1, r2, c2)) return steps;
        cascadeCount = 0;
        steps.add(Step.swap(r1, c1, r2, c2));
        swapCells(r1, c1, r2, c2);

        SpecialType s1 = specialOf(grid[r1][c1], numTypes);
        SpecialType s2 = specialOf(grid[r2][c2], numTypes);

        // special + special combo: activate immediately
        if (s1 != SpecialType.NONE && s2 != SpecialType.NONE && s1 != s2) {
            if (countMove) movesUsed++;
            applyCombo(r1, c1, r2, c2, steps);
            collapseAndRefill(steps);
            cascade(steps);
            ensurePlayable();
            return steps;
        }
        // special + normal: activating the special counts as a valid move
        if (s1 != SpecialType.NONE || s2 != SpecialType.NONE) {
            if (countMove) movesUsed++;
            List<int[]> act = new ArrayList<>();
            if (s1 != SpecialType.NONE) collectActivation(r1, c1, s1, act, new boolean[rows][cols]);
            else collectActivation(r2, c2, s2, act, new boolean[rows][cols]);
            clearCells(act, steps);
            collapseAndRefill(steps);
            cascade(steps);
            ensurePlayable();
            return steps;
        }

        List<List<int[]>> matches = findAllMatches();
        if (matches.isEmpty()) {
            swapCells(r1, c1, r2, c2);
            steps.add(Step.swapBack(r1, c1, r2, c2));
            return steps;
        }
        if (countMove) movesUsed++;
        resolveGroups(matches, steps);
        collapseAndRefill(steps);
        cascade(steps);
        ensurePlayable();
        return steps;
    }

    /**
     * Keep the board playable: shuffle when no valid move exists. The player
     * never loses tiles to this safety net — shuffling rearranges the multiset
     * of tiles already on the board, so it is deferred until the current
     * animation steps have been snapshotted by the caller.
     */
    private void ensurePlayable() {
        // Safety net: if the settled board has no matches and no valid move,
        // shuffle immediately so the engine can never leave a stuck state.
        if (findAllMatches().isEmpty() && !hasValidMove()) {
            shuffleInPlace();
        }
    }

    /** Shuffles the grid without emitting animation steps (internal safety net). */
    private void shuffleInPlace() {
        shuffle(); // reuses the seeded Random and match-free/playable guard loop
    }

    /**
     * Public shuffle used by the UI: rearranges the tile multiset in place and
     * returns a snapshot of every cell so callers can animate the change.
     */
    public List<int[]> shuffleWithSnapshot() {
        shuffle();
        List<int[]> all = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                all.add(new int[]{r, c, baseTypeOf(grid[r][c], numTypes)});
        return all;
    }

    /** Directly sets a cell's stored value (plain type or encoded special). */
    public void setCell(int r, int c, int storedValue) { grid[r][c] = storedValue; }

    /** Swaps two cells in the model. Public so tools/tests can author boards. */
    public void swapCells(int r1, int c1, int r2, int c2) {
        int tmp = grid[r1][c1];
        grid[r1][c1] = grid[r2][c2];
        grid[r2][c2] = tmp;
    }

    // ---------- matching ----------

    /** All maximal straight runs of length >= 3, as groups of [r,c,type]. */
    public List<List<int[]>> findAllMatches() {
        List<int[]> runs = new ArrayList<>();
        // horizontal runs
        for (int r = 0; r < rows; r++) {
            int c = 0;
            while (c < cols) {
                int t = baseTypeOf(grid[r][c], numTypes);
                int start = c;
                while (c < cols && baseTypeOf(grid[r][c], numTypes) == t) c++;
                if (c - start >= 3) {
                    int[] run = new int[]{r, start, t, c - start, 0}; // 0 = horizontal
                    runs.add(run);
                }
                c++; // advance past the single non-matching cell that ended the run
            }
        }
        // vertical runs
        for (int c = 0; c < cols; c++) {
            int r = 0;
            while (r < rows) {
                int t = baseTypeOf(grid[r][c], numTypes);
                int start = r;
                while (r < rows && baseTypeOf(grid[r][c], numTypes) == t) r++;
                if (r - start >= 3) {
                    runs.add(new int[]{start, c, t, r - start, 1}); // 1 = vertical
                }
                r++; // advance past the single non-matching cell that ended the run
            }
        }
        // merge overlapping runs of same type into groups (union-find over runs)
        return mergeRunsIntoGroups(runs);
    }

    private List<List<int[]>> mergeRunsIntoGroups(List<int[]> runs) {
        int n = runs.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        // map cell -> first run index touching it
        java.util.Map<Long, Integer> cellOwner = new java.util.HashMap<>();
        for (int i = 0; i < n; i++) {
            int[] run = runs.get(i);
            for (int k = 0; k < run[3]; k++) {
                int r = run[4] == 0 ? run[0] : run[0] + k;
                int c = run[4] == 0 ? run[1] + k : run[1];
                long key = (long) r * cols + c;
                Integer other = cellOwner.get(key);
                if (other == null) cellOwner.put(key, i);
                else union(parent, other, i);
            }
        }
        java.util.Map<Integer, List<int[]>> groups = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            int[] run = runs.get(i);
            List<int[]> g = groups.computeIfAbsent(root, k -> new ArrayList<>());
            java.util.Set<Long> seen = new java.util.HashSet<>();
            for (int[] existing : g) seen.add((long) existing[0] * cols + existing[1]);
            for (int k = 0; k < run[3]; k++) {
                int r = run[4] == 0 ? run[0] : run[0] + k;
                int c = run[4] == 0 ? run[1] + k : run[1];
                long key = (long) r * cols + c;
                if (seen.add(key)) g.add(new int[]{r, c, run[2]});
            }
        }
        return new ArrayList<>(groups.values());
    }

    private static int find(int[] p, int i) {
        while (p[i] != i) { p[i] = p[p[i]]; i = p[i]; }
        return i;
    }
    private static void union(int[] p, int a, int b) { p[find(p, a)] = find(p, b); }

    // ---------- resolution with specials ----------

    private void resolveGroups(List<List<int[]>> groups, List<Step> steps) {
        boolean[][] toClear = new boolean[rows][cols];
        List<int[]> spawn = new ArrayList<>(); // {r,c,encoded}
        for (List<int[]> group : groups) {
            int size = group.size();
            SpecialType sp = SpecialType.NONE;
            if (size >= 5) sp = SpecialType.PIZZA_BOMB;
            else if (size == 4) {
                // straight 4 -> directional blast based on orientation
                boolean horiz = group.get(0)[0] == group.get(1)[0];
                sp = horiz ? SpecialType.ROW_BLAST : SpecialType.COL_CRUSH;
            } else {
                // L/T shapes: >3 cells but not a straight line
                boolean allSameRow = true, allSameCol = true;
                for (int[] cell : group) {
                    if (cell[0] != group.get(0)[0]) allSameRow = false;
                    if (cell[1] != group.get(0)[1]) allSameCol = false;
                }
                if (!(allSameRow || allSameCol)) sp = SpecialType.PIZZA_BOMB;
            }
            // mark group cells
            List<int[]> marked = new ArrayList<>();
            for (int[] cell : group) marked.add(cell);
            // chain activations from pre-existing specials caught in the group
            boolean[][] visited = new boolean[rows][cols];
            List<int[]> activated = new ArrayList<>();
            for (int[] cell : marked) {
                SpecialType existing = specialOf(grid[cell[0]][cell[1]], numTypes);
                if (existing != SpecialType.NONE) {
                    collectActivation(cell[0], cell[1], existing, activated, visited);
                }
            }
            for (int[] cell : activated) toClear[cell[0]][cell[1]] = true;
            for (int[] cell : marked) toClear[cell[0]][cell[1]] = true;

            if (sp != SpecialType.NONE) {
                // spawn at middle cell of the group
                int[] mid = group.get(group.size() / 2);
                spawn.add(new int[]{mid[0], mid[1], encodeSpecial(sp, mid[2], numTypes)});
            }
        }
        // build clear list
        List<int[]> clearedCells = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (toClear[r][c]) clearedCells.add(new int[]{r, c, baseTypeOf(grid[r][c], numTypes)});
        if (!clearedCells.isEmpty()) {
            for (int[] cell : clearedCells) grid[cell[0]][cell[1]] = -1;
            registerClear(clearedCells);
            steps.add(Step.clear(clearedCells.get(0)[0], clearedCells.get(0)[1], clearedCells, SpecialType.NONE));
        }
        for (int[] s : spawn) {
            if (grid[s[0]][s[1]] == -1) grid[s[0]][s[1]] = s[2];
        }
    }

    /** BFS chain: specials caught by an activation also fire. */
    private void collectActivation(int r, int c, SpecialType sp, List<int[]> out, boolean[][] visited) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{r, c});
        visited[r][c] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cr = cur[0], cc = cur[1];
            SpecialType s = specialOf(grid[cr][cc], numTypes);
            if (s == SpecialType.NONE) {
                addOnce(cr, cc, out, visited);
                continue;
            }
            addOnce(cr, cc, out, visited);
            switch (s) {
                case ROW_BLAST:
                    for (int k = 0; k < cols; k++) if (!visited[cr][k]) { visited[cr][k] = true; queue.add(new int[]{cr, k}); }
                    break;
                case COL_CRUSH:
                    for (int k = 0; k < rows; k++) if (!visited[k][cc]) { visited[k][cc] = true; queue.add(new int[]{k, cc}); }
                    break;
                case PIZZA_BOMB:
                    for (int dr = -1; dr <= 1; dr++)
                        for (int dc = -1; dc <= 1; dc++) {
                            int nr = cr + dr, nc = cc + dc;
                            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
                                visited[nr][nc] = true; queue.add(new int[]{nr, nc});
                            }
                        }
                    break;
                default: break;
            }
        }
    }

    private void addOnce(int r, int c, List<int[]> out, boolean[][] counted) {
        out.add(new int[]{r, c, baseTypeOf(grid[r][c], numTypes)});
    }

    /** Two different specials swapped together: full row + column + bomb sweep. */
    private void applyCombo(int r1, int c1, int r2, int c2, List<Step> steps) {
        boolean[][] toClear = new boolean[rows][cols];
        for (int c = 0; c < cols; c++) { toClear[r1][c] = true; toClear[r2][c] = true; }
        for (int r = 0; r < rows; r++) { toClear[r][c1] = true; toClear[r][c2] = true; }
        // bomb radius around both cells
        for (int rr = r1 - 1; rr <= r1 + 1; rr++)
            for (int cc = c1 - 1; cc <= c1 + 1; cc++)
                if (rr >= 0 && rr < rows && cc >= 0 && cc < cols) toClear[rr][cc] = true;
        for (int rr = r2 - 1; rr <= r2 + 1; rr++)
            for (int cc = c2 - 1; cc <= c2 + 1; cc++)
                if (rr >= 0 && rr < rows && cc >= 0 && cc < cols) toClear[rr][cc] = true;
        List<int[]> cleared = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (toClear[r][c]) { cleared.add(new int[]{r, c, baseTypeOf(grid[r][c], numTypes)}); grid[r][c] = -1; }
        registerClear(cleared);
        steps.add(Step.clear(r1, c1, cleared, SpecialType.PIZZA_BOMB));
    }

    private void clearCells(List<int[]> cells, List<Step> steps) {
        if (cells.isEmpty()) return;
        List<int[]> norm = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int[] cell : cells) {
            long key = (long) cell[0] * cols + cell[1];
            if (seen.add(key)) {
                norm.add(new int[]{cell[0], cell[1], baseTypeOf(grid[cell[0]][cell[1]], numTypes)});
                grid[cell[0]][cell[1]] = -1;
            }
        }
        registerClear(norm);
        steps.add(Step.clear(norm.get(0)[0], norm.get(0)[1], norm, SpecialType.NONE));
    }

    // ---------- gravity & refill ----------

    private void collapseAndRefill(List<Step> steps) {
        for (int c = 0; c < cols; c++) {
            int write = rows - 1;
            for (int r = rows - 1; r >= 0; r--) {
                if (grid[r][c] != -1) {
                    if (write != r) {
                        grid[write][c] = grid[r][c];
                        grid[r][c] = -1;
                        steps.add(Step.drop(r, c, write, c));
                    }
                    write--;
                }
            }
            for (int r = write; r >= 0; r--) {
                int t = random.nextInt(numTypes);
                grid[r][c] = t;
                steps.add(Step.refill(r, c, t));
            }
        }
    }

    private void cascade(List<Step> steps) {
        int guard = 0;
        while (guard++ < 100) {
            List<List<int[]>> groups = findAllMatches();
            if (groups.isEmpty()) break;
            resolveGroups(groups, steps);
            collapseAndRefill(steps);
        }
        // Deterministic safety net: refills are random and could theoretically
        // land a no-valid-move board after 100 cascades. Guarantee playability
        // so the engine never hands control back on a stuck board.
        ensurePlayable();
    }

    // ---------- validity / shuffle / hint ----------

    public boolean hasValidMove() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (specialOf(grid[r][c], numTypes) != SpecialType.NONE) return true;
                if (c + 1 < cols && swapProducesMatch(r, c, r, c + 1)) return true;
                if (r + 1 < rows && swapProducesMatch(r, c, r + 1, c)) return true;
            }
        }
        return false;
    }

    private boolean swapProducesMatch(int r1, int c1, int r2, int c2) {
        swapCells(r1, c1, r2, c2);
        boolean m = !findAllMatches().isEmpty();
        swapCells(r1, c1, r2, c2);
        return m;
    }

    /** Shuffle until the board has no current matches and at least one valid move. */
    public void shuffle() {
        int guard = 0;
        do {
            List<Integer> all = new ArrayList<>();
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++) all.add(grid[r][c]);
            java.util.Collections.shuffle(all, random);
            int i = 0;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++) grid[r][c] = all.get(i++);
        } while (guard++ < 200 && (!findAllMatches().isEmpty() || !hasValidMove()));
    }

    /**
     * First valid player swap found, as {r1,c1,r2,c2}, or null. Specials are
     * always playable (activating one is a valid move), so any special on the
     * board yields an immediate hint.
     */
    public int[] findHintSwap() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (specialOf(grid[r][c], numTypes) != SpecialType.NONE) {
                    if (c + 1 < cols) return new int[]{r, c, r, c + 1};
                    if (r + 1 < rows) return new int[]{r, c, r + 1, c};
                }
        // Mirror hasValidMove(): only consider pairs that hold different types,
        // so swapping identical tiles never produces a misleading "hint".
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c + 1 < cols && grid[r][c] != grid[r][c + 1]
                        && swapProducesMatch(r, c, r, c + 1)) return new int[]{r, c, r, c + 1};
                if (r + 1 < rows && grid[r][c] != grid[r + 1][c]
                        && swapProducesMatch(r, c, r + 1, c)) return new int[]{r, c, r + 1, c};
            }
        }
        return null;
    }

    // ---------- boosters (no move consumed) ----------

    public List<Step> useHammer(int r, int c) {
        List<Step> steps = new ArrayList<>();
        cascadeCount = 0;
        List<int[]> cleared = new ArrayList<>();
        cleared.add(new int[]{r, c, baseTypeOf(grid[r][c], numTypes)});
        grid[r][c] = -1;
        registerClear(cleared);
        steps.add(Step.clear(r, c, cleared, SpecialType.NONE));
        collapseAndRefill(steps);
        cascade(steps);
        ensurePlayable();
        return steps;
    }

    public List<Step> useBomb(int r, int c) {
        List<Step> steps = new ArrayList<>();
        cascadeCount = 0;
        List<int[]> cleared = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] != -1) {
                    cleared.add(new int[]{nr, nc, baseTypeOf(grid[nr][nc], numTypes)});
                    grid[nr][nc] = -1;
                }
            }
        }
        registerClear(cleared);
        steps.add(Step.clear(r, c, cleared, SpecialType.PIZZA_BOMB));
        collapseAndRefill(steps);
        cascade(steps);
        ensurePlayable();
        return steps;
    }

    public List<Step> useRainbow(int tileType) {
        List<Step> steps = new ArrayList<>();
        cascadeCount = 0;
        List<int[]> cleared = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (baseTypeOf(grid[r][c], numTypes) == tileType) {
                    cleared.add(new int[]{r, c, tileType});
                    grid[r][c] = -1;
                }
            }
        }
        if (!cleared.isEmpty()) {
            registerClear(cleared);
            steps.add(Step.clear(cleared.get(0)[0], cleared.get(0)[1], cleared, SpecialType.NONE));
            collapseAndRefill(steps);
            cascade(steps);
            ensurePlayable();
        }
        return steps;
    }

    /** Swap booster: exchange any two tiles (adjacent or not), even without a match. */
    public List<Step> useSwapBoost(int r1, int c1, int r2, int c2) {
        List<Step> steps = new ArrayList<>();
        if (r1 < 0 || r1 >= rows || c1 < 0 || c1 >= cols || r2 < 0 || r2 >= rows || c2 < 0 || c2 >= cols)
            return steps;
        if (r1 == r2 && c1 == c2) return steps;
        cascadeCount = 0;
        boolean adjacent = areAdjacent(r1, c1, r2, c2);
        if (adjacent) {
            // Force the exchange first so the booster ALWAYS persists, even
            // when the swap creates no match (a plain player swap would revert).
            steps.add(Step.swap(r1, c1, r2, c2));
            swapCells(r1, c1, r2, c2);
            List<List<int[]>> groups = findAllMatches();
            if (!groups.isEmpty()) {
                resolveGroups(groups, steps);
                collapseAndRefill(steps);
                cascade(steps);
            }
        } else {
            steps.add(Step.swap(r1, c1, r2, c2));
            swapCells(r1, c1, r2, c2);
            List<List<int[]>> groups = findAllMatches();
            if (!groups.isEmpty()) {
                resolveGroups(groups, steps);
                collapseAndRefill(steps);
                cascade(steps);
            }
        }
        ensurePlayable();
        return steps;
    }
}
