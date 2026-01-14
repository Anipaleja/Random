package FinalProject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/** Connect 5 game application (FinalProject). */
public class Connect5GUI {

    private static final char EMPTY = '.';
    private static final char BLACK = 'B';
    private static final char WHITE = 'W';
    private static final char BLUE = 'U';
    private static final char GREEN = 'G';
    private static final char RED = 'R';

    private static final char[] COLOR_ORDER = new char[] { BLACK, WHITE, BLUE, GREEN };

    // Chessboard tiles - Mutable for Theme Selection
    // Default: Bright Wood Theme (High Contrast)
    private static Color LIGHT_TILE = new Color(0xfbc273); // Custom Light Gold
    private static Color DARK_TILE = new Color(0x95533b); // Custom Dark Brown

    // Theme (Strict Monochrome / Wireframe)
    private static final Color APP_BG = Color.WHITE;
    private static final Color PANEL_BG = Color.WHITE;
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_LIGHT = Color.BLACK;
    private static final Color TEXT_DARK = Color.BLACK; // Black text on White buttons

    // Buttons are White with Black Text and Black Border
    private static final Color ACCENT_GOLD = Color.WHITE;

    private static final Color BORDER_BROWN = Color.BLACK;

    /**
     * Custom Icon implementation for rendering scalable circular game pieces.
     * Supports configurable size, fill color, outline color, and stroke width.
     */
    private static class CircleIcon implements Icon {
        private final int size;
        private final Color fill;
        private final Color outline;
        private final float stroke;

        CircleIcon(int size, Color fill, Color outline, float stroke) {
            this.size = size;
            this.fill = fill;
            this.outline = outline;
            this.stroke = stroke;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = Math.max(2, size / 12);
            int d = size - pad * 2;

            g2.setColor(fill);
            g2.fillOval(x + pad, y + pad, d, d);

            if (outline != null && stroke > 0f) {
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(stroke));
                g2.drawOval(x + pad, y + pad, d, d);
            }
            g2.dispose();
        }
    }

    /**
     * Converts a player symbol to its human-readable color name.
     * 
     * @param sym The player symbol (B, W, U, G, R)
     * @return The color name as a String
     */
    private static String colorName(char sym) {
        if (sym == BLACK)
            return "Black";
        if (sym == WHITE)
            return "White";
        if (sym == BLUE)
            return "Blue";
        if (sym == GREEN)
            return "Green";
        if (sym == RED)
            return "Red";
        return "Unknown";
    }

    /**
     * Gets the default color for a player symbol.
     * 
     * @param sym The player symbol (B, W, U, G, R)
     * @return The default Color for the symbol
     */
    private static Color getDefaultColor(char sym) {
        if (sym == BLACK)
            return Color.BLACK;
        if (sym == WHITE)
            return Color.WHITE;
        if (sym == BLUE)
            return new Color(30, 90, 210);
        if (sym == GREEN)
            return new Color(30, 160, 80);
        if (sym == RED)
            return new Color(210, 50, 50);
        return Color.GRAY;
    }

    /**
     * Generates a scalable piece icon with the specified color and size.
     * 
     * @param c  The fill color for the icon
     * @param px The diameter of the icon in pixels
     * @return An Icon instance with a black border for visibility
     */
    private static Icon iconFor(Color c, int px) {
        int size = Math.max(12, px);
        // Always add a black border to ensure visibility for all colors
        return new CircleIcon(size, c, Color.BLACK, Math.max(1f, size / 16f));
    }

    /**
     * Represents a player in the Connect 5 game.
     * Tracks player name, symbol, pieces placed, moves made, and custom color.
     */
    static class Player {
        String name;
        char symbol;
        int piecesPlaced;
        int movesMade; // Added field for Phase 2
        Color color;

        /**
         * Constructs a new Player with the given name and symbol.
         * 
         * @param name   The player's display name
         * @param symbol The player's unique symbol (B, W, U, G, R)
         */
        Player(String name, char symbol) {
            this.name = name;
            this.symbol = symbol;
            this.piecesPlaced = 0;
            this.movesMade = 0;
            this.color = getDefaultColor(symbol);
        }

        /**
         * Makes a move for this player. Overridden by AIPlayer.
         * 
         * @param board The game board
         * @param phase The current game phase ("PLACEMENT" or "MOVEMENT")
         */
        public void makeMove(Board board, String phase) {
        }
    }

    /**
     * AI-controlled player that uses advanced algorithms (minimax, alpha-beta
     * pruning)
     * to determine optimal moves. Supports three difficulty levels: BEGINNER,
     * MEDIUM, and SMART.
     * Includes learning capabilities to avoid previously losing move sequences.
     */
    static class AIPlayer extends Player {
        String difficulty;
        private final Random rand = new Random();
        private static final long TIME_LIMIT_MS = 9900; // 9.9 seconds to maximize think time

        // --- AI Learning Memory ---
        private static final String MEMORY_FILE = "ai_memory.txt";
        private static final Set<String> badSequences = new HashSet<>();

        static {
            loadBadSequences();
        }

        private static void loadBadSequences() {
            try {
                if (Files.exists(Paths.get(MEMORY_FILE))) {
                    List<String> lines = Files.readAllLines(Paths.get(MEMORY_FILE));
                    badSequences.addAll(lines);
                    // System.out.println("AI Loaded " + lines.size() + " bad sequences.");
                }
            } catch (IOException e) {
                // Ignore silent fail
            }
        }

        /**
         * Saves a losing game's move history to avoid repeating the same mistakes.
         * 
         * @param history The list of moves that led to a loss
         */
        static void saveLosingGame(List<String> history) {
            if (history.isEmpty())
                return;
            String seq = String.join(";", history);
            if (!badSequences.contains(seq)) {
                badSequences.add(seq);
                try {
                    Files.write(Paths.get(MEMORY_FILE), (seq + System.lineSeparator()).getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Constructs an AI player with specified difficulty.
         * 
         * @param name       The AI player's display name
         * @param symbol     The AI player's unique symbol
         * @param difficulty The difficulty level: "BEGINNER", "MEDIUM", or "SMART"
         */
        AIPlayer(String name, char symbol, String difficulty) {
            super(name, symbol);
            this.difficulty = difficulty;
        }

        /**
         * Overrides the Player's makeMove method to implement AI logic.
         * This method is typically called by the game loop to initiate the AI's turn.
         * 
         * @param board The current game board state.
         * @param phase The current phase of the game ("PLACEMENT" or "MOVEMENT").
         */
        @Override
        public void makeMove(Board board, String phase) {
            // AI logic to choose and execute a move will be implemented here or in
            // chooseMove.
        }

        /**
         * Determines and executes the AI's chosen move on the board.
         * This method encapsulates the core AI decision-making process.
         * 
         * @param board The current game board state.
         */
        public void chooseMove(Board board) {
            // Implementation for AI to choose and apply a move.
        }

        /**
         * Represents a potential move (either placement or movement).
         * Includes score evaluation for move ordering.
         */
        static class Move {
            int fromC, fromR, toC, toR;
            boolean placement;
            int score;

            Move(int toC, int toR) {
                this(0, 0, toC, toR);
                placement = true;
            }

            Move(int fromC, int fromR, int toC, int toR) {
                this.fromC = fromC;
                this.fromR = fromR;
                this.toC = toC;
                this.toR = toR;
                this.placement = false;
            }

            String toHistoryString() {
                if (placement)
                    return "P:" + toC + "," + toR;
                return "M:" + fromC + "," + fromR + ":" + toC + "," + toR;
            }
        }

        // Exception to break recursion on timeout
        private static class TimeoutException extends RuntimeException {
        }

        /**
         * Selects the best move for the AI player using the configured difficulty.
         * 
         * @param game The current game state
         * @return The selected Move, or null if no valid moves exist
         */
        Move pickMove(Game game) {
            String d = (difficulty == null) ? "BEGINNER" : difficulty.toUpperCase();

            ArrayList<Move> moves = generateMoves(game, game.board.grid, this.symbol, game.gamePhase);

            // --- Learning: Avoid bad paths ---
            String currentHist = String.join(";", game.moveHistory);
            if (!game.moveHistory.isEmpty())
                currentHist += ";"; // Add separator if not empty

            for (Move m : moves) {
                String candidate = currentHist + m.toHistoryString();
                // Check if this move starts any known bad sequence
                for (String bad : badSequences) {
                    if (bad.startsWith(candidate)) {
                        // Found a match! This path leads to a known loss.
                        m.score -= 50000;
                        break;
                    }
                }
            }
            // ---------------------------------

            if (moves.isEmpty())
                return null;

            // 1) Immediate win check (FAST)
            Move win = findWinningMove(game, this.symbol);
            if (win != null)
                return win;

            // 2) Immediate block check (MEDIUM/SMART)
            if (!"BEGINNER".equals(d)) {
                Move block = findBestBlockAnyOpponent(game, this.symbol);
                if (block != null)
                    return block;
            }

            // 3) Difficulty logic
            if ("BEGINNER".equals(d))
                return moves.get(rand.nextInt(moves.size()));
            if ("MEDIUM".equals(d))
                return pickBestHeuristic(game, this.symbol, moves);

            // SMART: Iterative Deepening with Time Control
            return pickIterativeDeepening(game, this.symbol, moves);
        }

        /**
         * Uses iterative deepening with alpha-beta pruning to find the best move.
         * Continues searching deeper until the time limit is reached.
         * 
         * @param game  The current game state
         * @param me    This AI player's symbol
         * @param moves List of possible moves to evaluate
         * @return The best move found within the time limit
         */
        private Move pickIterativeDeepening(Game game, char me, ArrayList<Move> moves) {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + TIME_LIMIT_MS;

            Move bestMove = moves.get(0);
            // In placement, huge branching factor, so max depth around 6-8 is good.
            // In movement, we can go deeper potentially.
            int maxDepthRaw = ("PLACEMENT".equals(game.gamePhase)) ? 10 : 12; // Increased limits

            // Make a WORKING COPY of the grid once, then use backtracking
            char[][] workingGrid = copyGrid(game.board.grid);

            // Initial sort with shallow heuristic
            ArrayList<Move> ordered = orderAndCapMoves(game, workingGrid, moves, me, me, game.gamePhase, true);

            try {
                // Iterative Deepening: Depth 1, 2, 3...
                for (int depth = 1; depth <= maxDepthRaw; depth++) {
                    // Check time before starting a new depth
                    if (System.currentTimeMillis() >= endTime)
                        break;

                    Move currentBest = null;
                    int bestVal = Integer.MIN_VALUE;

                    // Root level of Alpha-Beta
                    for (Move m : ordered) {
                        if (System.currentTimeMillis() >= endTime)
                            throw new TimeoutException();

                        applyMove(workingGrid, m, me);

                        // If immediate win, take it
                        if (winnerOnGrid(workingGrid, game.connectTarget) == me) {
                            undoMove(workingGrid, m, me); // Be tidy
                            return m;
                        }

                        int nextIndex = (game.currentPlayerIndex + 1) % game.players.length;
                        int val;
                        try {
                            val = alphaBeta(game, workingGrid, nextIndex, me, depth - 1,
                                    Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, endTime);
                        } finally {
                            undoMove(workingGrid, m, me); // BACKTRACK
                        }

                        // Positional tie-breaker
                        val += centerScore(m.toC, m.toR);

                        if (val > bestVal) {
                            bestVal = val;
                            currentBest = m;
                        }
                    }

                    if (currentBest != null) {
                        bestMove = currentBest;
                        // Optimization: Move best move to front for next iteration
                        ordered.remove(bestMove);
                        ordered.add(0, bestMove);
                        // System.out.println("ID Depth " + depth + " best: " + bestVal);
                    }
                }
            } catch (TimeoutException e) {
                // Time up, return best move found so far
            }
            return bestMove;
        }

        /**
         * Alpha-beta pruning minimax algorithm for game tree search.
         * 
         * @param game        The current game state
         * @param grid        The board grid (working copy for backtracking)
         * @param playerIndex The current player's index
         * @param me          This AI player's symbol
         * @param depth       Remaining search depth
         * @param alpha       Alpha value for pruning
         * @param beta        Beta value for pruning
         * @param endTime     Time limit for the search
         * @return The evaluated score for this position
         */
        private int alphaBeta(Game game, char[][] grid, int playerIndex, char me, int depth, int alpha, int beta,
                long endTime) {
            if (System.currentTimeMillis() >= endTime)
                throw new TimeoutException();

            char winner = winnerOnGrid(grid, game.connectTarget);
            if (winner != 0 || depth <= 0) {
                return terminalScore(game, grid, me, winner, depth);
            }

            String phase = computePhase(game, grid);
            char sym = game.players[playerIndex].symbol;

            ArrayList<Move> moves = generateMoves(game, grid, sym, phase);
            if (moves.isEmpty())
                return evaluatePosition(game, grid, me);

            boolean maximize = (sym == me);
            ArrayList<Move> ordered = orderAndCapMoves(game, grid, moves, sym, me, phase, maximize);

            int nextIndex = (playerIndex + 1) % game.players.length;

            if (maximize) {
                int best = Integer.MIN_VALUE / 2;
                for (Move m : ordered) {
                    applyMove(grid, m, sym);
                    try {
                        int val = alphaBeta(game, grid, nextIndex, me, depth - 1, alpha, beta, endTime);
                        best = Math.max(best, val);
                        alpha = Math.max(alpha, best);
                    } finally {
                        undoMove(grid, m, sym); // BACKTRACK
                    }
                    if (beta <= alpha)
                        break;
                }
                return best;
            } else {
                int best = Integer.MAX_VALUE / 2;
                for (Move m : ordered) {
                    applyMove(grid, m, sym);
                    try {
                        int val = alphaBeta(game, grid, nextIndex, me, depth - 1, alpha, beta, endTime);
                        best = Math.min(best, val);
                        beta = Math.min(beta, best);
                    } finally {
                        undoMove(grid, m, sym); // BACKTRACK
                    }
                    if (beta <= alpha)
                        break;
                }
                return best;
            }
        }

        /**
         * Generates all legal moves for the given player in the current game state.
         * 
         * @param game  The current game state
         * @param grid  The board grid
         * @param sym   The player symbol to generate moves for
         * @param phase The current game phase ("PLACEMENT" or "MOVEMENT")
         * @return List of all legal moves
         */
        private ArrayList<Move> generateMoves(Game game, char[][] grid, char sym, String phase) {
            ArrayList<Move> list = new ArrayList<>();
            boolean placementPhase = "PLACEMENT".equals(phase);
            if (placementPhase) {
                int placed = countPieces(grid, sym);
                if (placed >= 8) // PLACEMENT_LIMIT
                    return list;
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        if (grid[r][c] == EMPTY)
                            list.add(new Move(c, r));
            } else {
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++) {
                        if (grid[r][c] != sym)
                            continue;
                        for (int dr = -1; dr <= 1; dr++)
                            for (int dc = -1; dc <= 1; dc++) {
                                if (dr == 0 && dc == 0)
                                    continue;
                                int nr = r + dr, nc = c + dc;
                                if (nc < 0 || nc >= 8 || nr < 0 || nr >= 8)
                                    continue;
                                if (grid[nr][nc] == EMPTY)
                                    list.add(new Move(c, r, nc, nr));
                            }
                    }
            }
            return list;
        }

        private String computePhase(Game game, char[][] grid) {
            for (Player p : game.players) {
                if (countPieces(grid, p.symbol) < 8) // PLACEMENT_LIMIT
                    return "PLACEMENT";
            }
            return "MOVEMENT";
        }

        private int countPieces(char[][] grid, char sym) {
            int count = 0;
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++)
                    if (grid[r][c] == sym)
                        count++;
            return count;
        }

        /**
         * Finds an immediate winning move for the specified player.
         * 
         * @param game The current game state
         * @param sym  The player symbol
         * @return A winning move if one exists, otherwise null
         */
        private Move findWinningMove(Game game, char sym) {
            ArrayList<Move> moves = generateMoves(game, game.board.grid, sym, game.gamePhase);
            for (Move m : moves) {
                char[][] g2 = copyGrid(game.board.grid);
                applyMove(g2, m, sym);
                if (winnerOnGrid(g2, game.connectTarget) == sym)
                    return m;
            }
            return null;
        }

        /**
         * Finds the best blocking move to prevent all opponents from winning.
         * Evaluates which move minimizes the number of opponent winning threats.
         * 
         * @param game The current game state
         * @param me   This AI player's symbol
         * @return The best blocking move, or null if no threats exist
         */
        private Move findBestBlockAnyOpponent(Game game, char me) {
            ArrayList<Character> oppSyms = new ArrayList<>();
            for (Player p : game.players)
                if (p.symbol != me)
                    oppSyms.add(p.symbol);

            boolean threat = false;
            for (char opp : oppSyms) {
                ArrayList<Move> omoves = generateMoves(game, game.board.grid, opp, game.gamePhase);
                for (Move om : omoves) {
                    char[][] g2 = copyGrid(game.board.grid);
                    applyMove(g2, om, opp);
                    if (winnerOnGrid(g2, game.connectTarget) == opp) {
                        threat = true;
                        break;
                    }
                }
                if (threat)
                    break;
            }
            if (!threat)
                return null;

            ArrayList<Move> myMoves = generateMoves(game, game.board.grid, me, game.gamePhase);
            Move best = null;
            int bestThreats = Integer.MAX_VALUE;

            for (Move mm : myMoves) {
                char[][] g2 = copyGrid(game.board.grid);
                applyMove(g2, mm, me);
                String phase2 = computePhase(game, g2);

                int threats = 0;
                for (char opp : oppSyms) {
                    ArrayList<Move> omoves = generateMoves(game, g2, opp, phase2);
                    for (Move om : omoves) {
                        char[][] g3 = copyGrid(g2);
                        applyMove(g3, om, opp);
                        if (winnerOnGrid(g3, game.connectTarget) == opp)
                            threats++;
                    }
                }
                if (threats < bestThreats) {
                    bestThreats = threats;
                    best = mm;
                    if (bestThreats == 0)
                        break;
                }
            }
            return best;
        }

        /**
         * Selects the best move using a simple heuristic evaluation.
         * Used for MEDIUM difficulty.
         * 
         * @param game  The current game state
         * @param me    This AI player's symbol
         * @param moves List of candidate moves
         * @return The move with the highest heuristic score
         */
        private Move pickBestHeuristic(Game game, char me, ArrayList<Move> moves) {
            Move best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Move m : moves) {
                char[][] g2 = copyGrid(game.board.grid);
                applyMove(g2, m, me);
                int score = evaluatePosition(game, g2, me) + centerScore(m.toC, m.toR);
                if (score > bestScore) {
                    bestScore = score;
                    best = m;
                }
            }
            return (best != null) ? best : moves.get(rand.nextInt(moves.size()));
        }

        private int terminalScore(Game game, char[][] grid, char me, char winner, int depthRemaining) {
            if (winner == me)
                return 1_000_000_000 - (100 * (100 - depthRemaining));
            if (winner != 0)
                return -1_000_000_000 + (100 * (100 - depthRemaining));
            return evaluatePosition(game, grid, me);
        }

        private ArrayList<Move> orderAndCapMoves(Game game, char[][] grid, ArrayList<Move> moves,
                char mover, char me, String phase, boolean maximize) {
            for (Move m : moves) {
                applyMove(grid, m, mover);
                m.score = evaluatePosition(game, grid, me);
                if (mover == me)
                    m.score += centerScore(m.toC, m.toR);
                undoMove(grid, m, mover); // Backtrack
            }
            moves.sort((a, b) -> maximize ? Integer.compare(b.score, a.score) : Integer.compare(a.score, b.score));
            if (moves.size() > 28) {
                return new ArrayList<>(moves.subList(0, "PLACEMENT".equals(phase) ? 14 : 28));
            }
            return moves;
        }

        /**
         * Evaluates the overall position from the AI's perspective.
         * Balances offensive potential with defensive threats.
         * 
         * @param game The current game state
         * @param grid The board grid
         * @param me   This AI player's symbol
         * @return The heuristic score (higher is better for the AI)
         */
        private int evaluatePosition(Game game, char[][] grid, char me) {
            // Heuristic score: My Score - (Opponent Max Score * 0.9)
            // We want to be aggressive but also respect enemy threats.

            // Calculate my potential
            int myScore = heuristic(game, grid, me);

            // Calculate opponent potentials
            int oppMax = 0;
            for (Player p : game.players) {
                if (p.symbol == me)
                    continue;
                oppMax = Math.max(oppMax, heuristic(game, grid, p.symbol));
            }

            return myScore - (int) (0.9 * oppMax);
        }

        private int centerScore(int c, int r) {
            // Favor center (3,3)-(4,4)
            int dc = Math.abs(c - 3) + Math.abs(c - 4);
            int dr = Math.abs(r - 3) + Math.abs(r - 4);
            return 50 - (dc + dr) * 5;
        }

        /**
         * Advanced heuristic function that counts potential winning lines.
         * Evaluates all possible windows of the target connection length.
         * 
         * @param game The current game state
         * @param grid The board grid
         * @param me   The player symbol to evaluate for
         * @return The heuristic score for this player
         */
        private int heuristic(Game game, char[][] grid, char me) {
            int score = 0;
            int n = game.connectTarget;

            // Scanning windows of length 5 (target)
            // For each direction, check windows
            int[][] dirs = { { 0, 1 }, { 1, 0 }, { 1, 1 }, { 1, -1 } };
            for (int[] d : dirs) {
                int dr = d[0], dc = d[1];
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        // only evaluate if window fits
                        if (r + dr * (n - 1) < 0 || r + dr * (n - 1) >= 8 || c + dc * (n - 1) < 0
                                || c + dc * (n - 1) >= 8)
                            continue;
                        score += windowScore(grid, r, c, dr, dc, me, n);
                    }
                }
            }

            score += centerControl(grid, me) * 4;
            return score;
        }

        /**
         * Evaluates a single window (line) on the board.
         * Distinguishes between open and closed lines for accurate scoring.
         * 
         * @param grid The board grid
         * @param r0   Starting row
         * @param c0   Starting column
         * @param dr   Row direction delta
         * @param dc   Column direction delta
         * @param me   The player symbol to evaluate for
         * @param len  The window length (connect target)
         * @return The score for this window
         */
        private int windowScore(char[][] grid, int r0, int c0, int dr, int dc, char me, int len) {
            int meCount = 0;
            int oppCount = 0;

            for (int i = 0; i < len; i++) {
                char v = grid[r0 + dr * i][c0 + dc * i];
                if (v == me)
                    meCount++;
                else if (v != EMPTY)
                    oppCount++;
                else
                    ; // empty
            }

            // If mixed with opponent pieces, it's blocked (useless for me)
            if (meCount > 0 && oppCount > 0)
                return 0;

            // If only opponent pieces (and empty), negative score?
            // The heuristic function calls this for "me" and "opp" separately,
            // so here we only yield positive points for "me" presence.
            if (meCount == 0)
                return 0; // Purely empty or purely opponent is handled when we evaluate opponent

            // Determine if the line is OPEN (both ends empty/available) or CLOSED (one end
            // blocked)
            // We need to look at the cells immediately before and after the window, if they
            // exist.
            boolean openStart = false;
            boolean openEnd = false;

            int rStart = r0 - dr;
            int cStart = c0 - dc;
            if (rStart >= 0 && rStart < 8 && cStart >= 0 && cStart < 8) {
                if (grid[rStart][cStart] == EMPTY)
                    openStart = true;
            }

            int rEnd = r0 + dr * len;
            int cEnd = c0 + dc * len;
            if (rEnd >= 0 && rEnd < 8 && cEnd >= 0 && cEnd < 8) {
                if (grid[rEnd][cEnd] == EMPTY)
                    openEnd = true;
            }

            // Scoring Weights
            if (meCount == 5)
                return 1_000_000; // WIN

            if (meCount == 4) {
                if (openStart && openEnd)
                    return 900_000; // OPEN 4: Unstoppable
                if (openStart || openEnd)
                    return 50_000; // CLOSED 4: Must block
                return 1000; // Dead 4 (blocked both ends, but technically 5 pieces fit in window? No, window
                             // is 5.)
                // Actually if window is size 5 and we have 4, there is 1 empty spot INSIDE.
                // So it's not fully blocked. "Open/Closed" refers to external growth potential
                // but for Connect 5, an empty spot inside IS the growth potential.
                // Re-think: "Open 4" usually means .XXXX. which is 6 slots.
                // Here we evaluate a 5-slot window.
                // If 4 pieces + 1 empty in a 5-slot window, that IS a potential win.
                // We don't need external checks as much as internal checks.
            }

            // Standard Connect-4/5 logic usually looks at larger patterns,
            // but fixed window-5 is simple.
            // Let's stick to the prompt's request: ".XXX." vs "OXXX."
            // That implies checking OUTSIDE the window.
            // My openStart/openEnd checks do exactly that.

            if (meCount == 3) {
                // XXX.. or .XXX. or ..XXX inside the window
                if (openStart && openEnd)
                    return 50_000; // OPEN 3 (.XXX.) -> Very dangerous
                if (openStart || openEnd)
                    return 1000; // CLOSED 3 (OXXX.) -> Manageable
                return 100; // Blocked 3
            }

            if (meCount == 2) {
                if (openStart && openEnd)
                    return 500;
                return 50;
            }

            return 10;
        }

        private int centerControl(char[][] grid, char sym) {
            int count = 0;
            for (int r = 2; r <= 5; r++)
                for (int c = 2; c <= 5; c++)
                    if (grid[r][c] == sym)
                        count++;
            return count;
        }

        private char[][] copyGrid(char[][] src) {
            char[][] dst = new char[8][8];
            for (int r = 0; r < 8; r++)
                System.arraycopy(src[r], 0, dst[r], 0, 8);
            return dst;
        }

        private void applyMove(char[][] grid, Move m, char sym) {
            if (m.placement)
                grid[m.toR][m.toC] = sym;
            else {
                grid[m.fromR][m.fromC] = EMPTY;
                grid[m.toR][m.toC] = sym;
            }
        }

        private void undoMove(char[][] grid, Move m, char sym) {
            if (m.placement) {
                grid[m.toR][m.toC] = EMPTY;
            } else {
                grid[m.toR][m.toC] = EMPTY;
                grid[m.fromR][m.fromC] = sym;
            }
        }

        private char winnerOnGrid(char[][] grid, int connectTarget) {
            int n = connectTarget;
            int[][] dirs = new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++) {
                    char s = grid[r][c];
                    if (s == EMPTY)
                        continue;
                    for (int[] d : dirs) {
                        int dc = d[0], dr = d[1];
                        int count = 1;
                        int nc = c + dc, nr = r + dr;
                        while (nc >= 0 && nc < 8 && nr >= 0 && nr < 8 && grid[nr][nc] == s) {
                            count++;
                            if (count >= n)
                                return s;
                            nc += dc;
                            nr += dr;
                        }
                    }
                }
            return 0;
        }
    }

    // =========================
    // Board
    // =========================
    /**
     * Represents the 8x8 game board grid.
     * Handles placement and movement validation logic.
     */
    static class Board {
        char[][] grid = new char[8][8];

        /**
         * Initializes the board grid by filling all cells with the EMPTY symbol.
         */
        public void initializeBoard() {
            for (char[] row : grid)
                java.util.Arrays.fill(row, EMPTY);
        }

        /**
         * Places a piece on the board at the specified position.
         * 
         * @param pos    Array containing [column, row]
         * @param symbol The player's symbol
         * @return true if placement was successful, false if invalid
         */
        public boolean placePiece(int[] pos, char symbol) {
            int c = pos[0], r = pos[1];
            if (c < 0 || c >= 8 || r < 0 || r >= 8)
                return false;
            if (grid[r][c] != EMPTY)
                return false;
            grid[r][c] = symbol;
            return true;
        }

        /**
         * Moves a piece from one position to an adjacent position.
         * Only allows moves of 1 square in any direction (including diagonals).
         * 
         * @param from   Array containing source [column, row]
         * @param to     Array containing destination [column, row]
         * @param symbol The player's symbol
         * @return true if move was successful, false if invalid
         */
        public boolean movePiece(int[] from, int[] to, char symbol) {
            int fc = from[0], fr = from[1], tc = to[0], tr = to[1];
            if (fc < 0 || fc >= 8 || fr < 0 || fr >= 8)
                return false;
            if (tc < 0 || tc >= 8 || tr < 0 || tr >= 8)
                return false;
            if (grid[fr][fc] != symbol)
                return false;
            if (grid[tr][tc] != EMPTY)
                return false;

            int dc = Math.abs(tc - fc);
            int dr = Math.abs(tr - fr);
            if (dc > 1 || dr > 1 || (dc == 0 && dr == 0))
                return false;

            grid[fr][fc] = EMPTY;
            grid[tr][tc] = symbol;
            return true;
        }

        /**
         * Converts board coordinates to chess-style notation (e.g., "A1", "H8").
         * 
         * @param c Column index (0-7)
         * @param r Row index (0-7)
         * @return String representation (e.g., "A1")
         */
        public static String posToLabel(int c, int r) {
            return "" + (char) ('A' + c) + (r + 1);
        }
    }

    // =========================
    // Logger
    // =========================
    /**
     * Handles writing game events and board states to a text file log.
     */
    static class Logger {
        String fileName;
        private PrintWriter out;

        /**
         * Constructs a logger that writes to the specified file.
         * 
         * @param fileName The name of the log file
         */
        Logger(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Writes a line of text to the log file.
         * 
         * @param text The text to write
         */
        public void writeToFile(String text) {
            try {
                if (out == null)
                    out = new PrintWriter(new FileWriter(fileName, true));
                out.println(text);
                out.flush();
            } catch (IOException ignored) {
            }
        }

        /**
         * Closes the log file and flushes any remaining data.
         */
        public void closeFile() {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    // =========================
    // FileManager
    // =========================
    /**
     * Handles saving and loading the game state to/from a text file.
     */
    static class FileManager {
        private static final String HEADER = "CONNECT5_SAVE_V1";

        /**
         * Saves the current game state to a file.
         * 
         * @param game     The game to save
         * @param file     The destination file
         * @param modeName The name/description of the game mode
         * @throws IOException If file writing fails
         */
        public static void saveGame(Game game, File file, String modeName) throws IOException {
            try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                out.println(HEADER);
                out.println("MODE_NAME:" + modeName);
                out.println("CONNECT_TARGET:" + game.connectTarget);
                out.println("MAX_MOVES:" + game.maxMoves); // Renamed in Save File
                out.println("TURN_COUNT:" + game.turnCount);
                out.println("CURRENT_PLAYER_INDEX:" + game.currentPlayerIndex);
                out.println("GAME_PHASE:" + game.gamePhase);
                out.println("NUM_PLAYERS:" + game.players.length);

                for (Player p : game.players) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(p instanceof AIPlayer ? "AI" : "HUMAN").append(";");
                    sb.append(p.name).append(";");
                    sb.append(p.symbol).append(";");
                    sb.append(p.piecesPlaced).append(";");
                    sb.append(p.movesMade); // Save movesMade
                    if (p instanceof AIPlayer) {
                        sb.append(";").append(((AIPlayer) p).difficulty);
                    }
                    out.println(sb.toString());
                }

                out.println("BOARD_START");
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        out.print(game.board.grid[r][c]);
                    }
                    out.println();
                }
                out.println("BOARD_END");
            }
        }

        /**
         * Loads a saved game from a file.
         * 
         * @param file The save file to load
         * @return The restored Game object
         * @throws Exception If file format is invalid or reading fails
         */
        public static Game loadGame(File file) throws Exception {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                if (!HEADER.equals(line)) {
                    throw new Exception("Invalid save file format (Header missing)");
                }

                String modeName = "Loaded Game";
                int connectTarget = 5;
                int maxMoves = 8;
                int turnCount = 1;
                int currIndex = 0;
                String phase = "PLACEMENT";
                int numPlayers = 0;

                ArrayList<Player> playersList = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    if (line.equals("BOARD_START"))
                        break;

                    if (line.startsWith("MODE_NAME:"))
                        modeName = line.split(":", 2)[1];
                    else if (line.startsWith("CONNECT_TARGET:"))
                        connectTarget = Integer.parseInt(line.split(":")[1]);
                    else if (line.startsWith("MAX_PIECES:")) // Legacy support
                        maxMoves = Integer.parseInt(line.split(":")[1]);
                    else if (line.startsWith("MAX_MOVES:")) // New format
                        maxMoves = Integer.parseInt(line.split(":")[1]);
                    else if (line.startsWith("TURN_COUNT:"))
                        turnCount = Integer.parseInt(line.split(":")[1]);
                    else if (line.startsWith("CURRENT_PLAYER_INDEX:"))
                        currIndex = Integer.parseInt(line.split(":")[1]);
                    else if (line.startsWith("GAME_PHASE:"))
                        phase = line.split(":")[1];
                    else if (line.startsWith("NUM_PLAYERS:"))
                        numPlayers = Integer.parseInt(line.split(":")[1]);
                    else if (line.contains(";")) {
                        // Player line
                        String[] parts = line.split(";");
                        String type = parts[0];
                        String name = parts[1];
                        char symbol = parts[2].charAt(0);
                        int placed = Integer.parseInt(parts[3]);
                        int moved = 0;
                        // Check if movesMade represents stored (new format has it at index 4)
                        // Format: TYPE;NAME;SYMBOL;PLACED;MOVED;[DIFF]
                        // Old: TYPE;NAME;SYMBOL;PLACED;[DIFF]
                        // AI OLD: AI;NAME;SYMBOL;PLACED;DIFF
                        // HUMAN OLD: HUMAN;NAME;SYMBOL;PLACED

                        // Strategy: if parts[4] is integer, it's moved. If string (BEGINNER), it's
                        // diff.
                        int nextIdx = 4;
                        if (parts.length > 4) {
                            try {
                                moved = Integer.parseInt(parts[4]);
                                nextIdx = 5;
                            } catch (NumberFormatException e) {
                                // It's likely difficulty, so moved remains 0.
                            }
                        }

                        Player p;
                        if ("AI".equals(type)) {
                            String diff = parts.length > nextIdx ? parts[nextIdx] : "BEGINNER";
                            p = new AIPlayer(name, symbol, diff);
                        } else {
                            p = new Player(name, symbol);
                        }
                        p.piecesPlaced = placed;
                        p.movesMade = moved;
                        playersList.add(p);
                    }
                }

                if (playersList.size() != numPlayers) {
                    throw new Exception("Player count mismatch in save file.");
                }

                // Create Logger (append mode)
                new File("logs").mkdirs();
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String logName = "logs/connect_log_resume_" + ts + ".txt";
                Logger logger = new Logger(logName);
                logger.writeToFile("Resumed Game Log: " + modeName);

                Player[] playersArr = playersList.toArray(new Player[0]);
                Game game = new Game(connectTarget, maxMoves, logger, playersArr, currIndex);
                game.turnCount = turnCount;
                game.gamePhase = phase;

                // Load Board
                for (int r = 0; r < 8; r++) {
                    line = br.readLine();
                    if (line == null || line.equals("BOARD_END"))
                        break;
                    for (int c = 0; c < 8 && c < line.length(); c++) {
                        game.board.grid[r][c] = line.charAt(c);
                    }
                }

                return game;
            }
        }
    }

    // =========================
    // Game
    // =========================
    /**
     * Manages the core game state, including turn management, phase transitions
     * (Placement vs Movement),
     * and victory conditions.
     */
    static class Game {
        Board board;
        Player[] players;
        int currentPlayerIndex;
        int turnCount;
        public final ArrayList<String> moveHistory = new ArrayList<>();
        int connectTarget;
        int maxMoves; // Limit for Phase 2 Movement
        final int PLACEMENT_LIMIT = 8; // Fixed limit for Phase 1
        String gamePhase;
        Player winner;
        Logger logger;

        private int[] selectedFrom = null;

        // draw only for 2-human mode
        private boolean draw = false;
        private Integer drawOfferFrom = null;

        /**
         * Constructs a new game with the specified parameters.
         * 
         * @param connectTarget Number of pieces in a row needed to win
         * @param maxMoves      Maximum number of moves per player in movement phase
         * @param logger        Logger for recording game events
         * @param players       Array of players
         * @param firstIndex    Index of the player who goes first
         */
        Game(int connectTarget, int maxMoves, Logger logger, Player[] players, int firstIndex) {
            this.connectTarget = connectTarget;
            this.maxMoves = maxMoves;
            this.logger = logger;

            this.board = new Board();
            this.board.initializeBoard();

            this.players = players;
            this.currentPlayerIndex = firstIndex;
            this.turnCount = 1;
            this.gamePhase = "PLACEMENT";
            this.winner = null;
        }

        /**
         * Gets the current player whose turn it is.
         * 
         * @return The current Player
         */
        public Player currentPlayer() {
            return players[currentPlayerIndex];
        }

        public Player getPlayerBySymbol(char sym) {
            for (Player p : players)
                if (p.symbol == sym)
                    return p;
            return null;
        }

        public char nextPlayerSymbol() {
            int nxt = (currentPlayerIndex + 1) % players.length;
            return players[nxt].symbol;
        }

        public int[] getSelectedFrom() {
            return selectedFrom;
        }

        boolean allPlayersPlaced() {
            for (Player p : players)
                if (p.piecesPlaced < PLACEMENT_LIMIT)
                    return false;
            return true;
        }

        public boolean drawAvailable() {
            return (players.length == 2
                    && !(players[0] instanceof AIPlayer)
                    && !(players[1] instanceof AIPlayer));
        }

        public String offerOrAcceptDraw() {
            if (!drawAvailable())
                return "Draw is only available in Level 1 (2  players).";
            if (winner != null)
                return "Game is already over.";
            if (draw)
                return "Game is already a draw.";

            if (drawOfferFrom == null) {
                drawOfferFrom = currentPlayerIndex;
                switchPlayer();
                selectedFrom = null;
                return players[drawOfferFrom].name
                        + " offered a draw. Other player: press Draw to accept, or make a move to decline.";
            } else {
                if (drawOfferFrom != currentPlayerIndex) {
                    draw = true;
                    return "Draw agreed by both players.";
                }
                return "Draw offer is pending. Other player must accept or decline.";
            }
        }

        /**
         * Checks if there is a winner and updates the winner field.
         */
        public void checkWinner() {
            char w = findWinnerSymbol();
            if (w == 0)
                winner = null;
            else {
                for (Player p : players)
                    if (p.symbol == w) {
                        winner = p;
                        return;
                    }
                winner = null;
            }
        }

        public boolean isGameOver() {
            checkWinner();
            if (winner != null)
                return true;
            if (draw)
                return true;
            return turnCount >= 300;
        }

        public boolean isDraw() {
            if (draw)
                return true;
            return (winner == null && turnCount >= 300);
        }

        public void switchPlayer() {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
            selectedFrom = null;
        }

        /**
         * Handles a click on a board cell during gameplay.
         * 
         * @param c Column index
         * @param r Row index
         * @return String describing the result of the action
         */
        public String handleClick(int c, int r) {
            if (isGameOver())
                return "Game is over.";

            if (drawAvailable() && drawOfferFrom != null && drawOfferFrom != currentPlayerIndex)
                drawOfferFrom = null;

            Player p = currentPlayer();
            String desc;

            if ("PLACEMENT".equals(gamePhase)) {
                if (p.piecesPlaced >= PLACEMENT_LIMIT)
                    return "No pieces left to place. Wait for movement phase.";
                boolean ok = board.placePiece(new int[] { c, r }, p.symbol);
                if (!ok)
                    return "Invalid placement. Choose an empty square.";

                p.piecesPlaced++;
                desc = p.name + " PLACE " + Board.posToLabel(c, r);

                if (allPlayersPlaced())
                    gamePhase = "MOVEMENT";

                moveHistory.add("P:" + c + "," + r); // Record
                logTurn(desc);
                advanceTurn();
                return desc;
            } else {
                if (p.movesMade >= maxMoves)
                    return "No moves remaining.";

                if (selectedFrom == null) {
                    if (board.grid[r][c] != p.symbol)
                        return "Select one of your own pieces to move.";
                    selectedFrom = new int[] { c, r };
                    return "Selected " + Board.posToLabel(c, r) + " to move.";
                } else {
                    int fromC = selectedFrom[0], fromR = selectedFrom[1];

                    if (fromC == c && fromR == r) {
                        selectedFrom = null;
                        return "Selection cleared.";
                    }

                    boolean ok = board.movePiece(new int[] { fromC, fromR }, new int[] { c, r }, p.symbol);
                    if (!ok)
                        return "Invalid move. Move 1 square to an adjacent empty space.";

                    p.movesMade++; // Increment moves
                    desc = p.name + " MOVE " + Board.posToLabel(fromC, fromR) + " -> " + Board.posToLabel(c, r);
                    selectedFrom = null;

                    moveHistory.add("M:" + fromC + "," + fromR + ":" + c + "," + r); // Record
                    logTurn(desc);
                    advanceTurn();
                    return desc;
                }
            }
        }

        /**
         * Executes an AI player's turn.
         * 
         * @return String describing the AI's move
         */
        public String performAITurn() {
            if (isGameOver())
                return "Game is over.";
            if (!(currentPlayer() instanceof AIPlayer))
                return "Not AI turn.";

            if (drawAvailable() && drawOfferFrom != null && drawOfferFrom != currentPlayerIndex)
                drawOfferFrom = null;

            AIPlayer ai = (AIPlayer) currentPlayer();
            AIPlayer.Move m = ai.pickMove(this);
            if (m == null)
                return "AI has no valid moves.";

            String desc;

            if ("PLACEMENT".equals(this.gamePhase)) {
                boolean ok = board.placePiece(new int[] { m.toC, m.toR }, ai.symbol);
                if (!ok)
                    return "AI attempted invalid placement (should not happen).";

                ai.piecesPlaced++;
                desc = ai.name + " (AI) PLACE " + Board.posToLabel(m.toC, m.toR);

                if (allPlayersPlaced())
                    this.gamePhase = "MOVEMENT";

                moveHistory.add(m.toHistoryString()); // Record
                logTurn(desc);
                advanceTurn();
                return desc;
            } else {
                boolean ok = board.movePiece(new int[] { m.fromC, m.fromR }, new int[] { m.toC, m.toR }, ai.symbol);
                if (!ok)
                    return "AI attempted invalid move (should not happen).";

                ai.movesMade++;
                desc = ai.name + " (AI) MOVE " + Board.posToLabel(m.fromC, m.fromR) + " -> "
                        + Board.posToLabel(m.toC, m.toR);

                moveHistory.add(m.toHistoryString()); // Record
                logTurn(desc);
                advanceTurn();
                return desc;
            }
        }

        void advanceTurn() {
            checkWinner();
            if (winner != null) {
                logResult("RESULT: " + winner.name + " wins by connecting " + connectTarget + "!");

                // If AI lost, learn!
                for (Player p : players) {
                    if (p instanceof AIPlayer && p != winner) {
                        AIPlayer.saveLosingGame(moveHistory);
                    }
                }
                return;
            }
            if (isDraw()) {
                logResult("RESULT: Draw.");
                return;
            }
            switchPlayer();
            turnCount++;
        }

        void logTurn(String desc) {
            if (logger == null)
                return;
            logger.writeToFile("Turn " + turnCount + ": " + desc);

        }

        private void logResult(String resultLine) {
            if (logger == null)
                return;
            logger.writeToFile("------------------------------------------------------------");
            logger.writeToFile(resultLine);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
            logger.writeToFile("Ended: " + LocalDateTime.now().format(fmt));
            logger.closeFile();
        }

        /**
         * Finds the winner symbol by checking all possible connections.
         * 
         * @return The winning player's symbol, or 0 if no winner
         */
        private char findWinnerSymbol() {
            int n = connectTarget;
            int[][] dirs = new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };

            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++) {
                    char s = board.grid[r][c];
                    if (s == EMPTY)
                        continue;

                    for (int[] d : dirs) {
                        int dc = d[0], dr = d[1];
                        int count = 1;
                        int nc = c + dc, nr = r + dr;

                        while (nc >= 0 && nc < 8 && nr >= 0 && nr < 8 && board.grid[nr][nc] == s) {
                            count++;
                            if (count >= n)
                                return s;
                            nc += dc;
                            nr += dr;
                        }
                    }
                }
            return 0;
        }
    }

    // =========================
    // GUI
    // =========================
    /**
     * The main JFrame containing the application's UI components.
     * Manages navigation between the Menu and the Game Board.
     */
    public static class ConnectFrame extends JFrame {

        private final CardLayout cards = new CardLayout();
        private final JPanel root = new JPanel(cards);

        private final MenuPanel menuPanel = new MenuPanel();
        private final GamePanel gamePanel = new GamePanel();
        private final SpinPanel spinPanel = new SpinPanel();
        private final ColorSelectPanel colorPanel = new ColorSelectPanel();

        /**
         * Constructs the main application frame and initializes all panels.
         */
        ConnectFrame() {
            setTitle("Connect 5 on 8x8 - Levels 1 to 3-4");
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // RESIZABLE ON
            setResizable(true);

            root.setBackground(APP_BG);

            JScrollPane menuScroll = new JScrollPane(menuPanel);
            menuScroll.setBorder(null);
            menuScroll.getVerticalScrollBar().setUnitIncrement(16);

            root.add(menuScroll, "MENU");
            root.add(gamePanel, "GAME");
            root.add(spinPanel, "SPIN");
            root.add(colorPanel, "COLORS");
            setContentPane(root);

            cards.show(root, "MENU");

            // Provide a sensible starting size, but still allow resizing
            pack();
            setSize(new Dimension(980, 740));
            setLocationRelativeTo(null);
        }

        /**
         * Shows a dialog to choose between Black or White pieces.
         * 
         * @param parent   Parent component for the dialog
         * @param msgTitle Dialog title
         * @param msgText  Dialog message
         * @return The chosen character (BLACK or WHITE), or null if cancelled
         */
        private static Character chooseBlackOrWhite(Component parent, String msgTitle, String msgText) {
            Object[] opts = { "Black", "White" };
            int choice = JOptionPane.showOptionDialog(
                    parent,
                    msgText,
                    msgTitle,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opts,
                    opts[0]);
            if (choice == JOptionPane.CLOSED_OPTION)
                return null;
            return (choice == 0) ? BLACK : WHITE;
        }

        /**
         * Shows the spin panel to determine who goes first.
         * 
         * @param labels     Player names to display
         * @param onComplete Callback invoked with the selected player index
         */
        public void showSpin(String[] labels, java.util.function.Consumer<Integer> onComplete) {
            spinPanel.setup(labels, onComplete);
            cards.show(root, "SPIN");
        }

        /**
         * Returns to the main menu screen.
         */
        public void showMenu() {
            cards.show(root, "MENU");
            menuPanel.revalidate();
            menuPanel.repaint();
        }

        /**
         * Shows the color selection panel for customizing player colors.
         * 
         * @param players Array of players to customize colors for
         * @param onDone  Callback invoked when color selection is complete
         */
        public void showColors(Player[] players, Runnable onDone) {
            colorPanel.setup(players, onDone);
            cards.show(root, "COLORS");
        }

        /**
         * Switches to the game board view and starts/resumes the game.
         * 
         * @param game      The Game instance to play
         * @param modeLabel Label describing the game mode
         */
        public void switchToGame(Game game, String modeLabel) {
            gamePanel.resumeGame(game, modeLabel);
            cards.show(root, "GAME");
        }

        // ---------- Integrated Spin Panel (2-5 participants) ----------
        /**
         * Panel for the "Spin" mechanic to randomly determine which player goes first.
         */
        private static class SpinPanel extends JPanel {
            private final JLabel info = new JLabel("Click a player to choose first, or press Spin Random.",
                    SwingConstants.CENTER);
            private final Random rand = new Random();

            private JPanel centerGrid;
            private JPanel cardsPanel;

            private JPanel[] cards;
            private JButton[] pickButtons;

            private boolean spinning = false;

            // Callback for when selection is made: (index) -> void
            private java.util.function.Consumer<Integer> onSelectionComplete;

            SpinPanel() {
                setLayout(new BorderLayout());
                setBackground(APP_BG);

                info.setBorder(new EmptyBorder(20, 20, 20, 20));
                info.setFont(info.getFont().deriveFont(Font.BOLD, 18f));
                info.setForeground(TEXT_LIGHT);

                add(info, BorderLayout.NORTH);

                cardsPanel = new JPanel(new GridBagLayout());
                cardsPanel.setBackground(APP_BG);
                add(cardsPanel, BorderLayout.CENTER);

                JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
                bottom.setBackground(APP_BG);

                JButton spinBtn = new JButton("Spin Random");
                styleButton(spinBtn);

                JButton backBtn = new JButton("Back");
                styleButton(backBtn);

                backBtn.addActionListener(e -> {
                    // Find parent ConnectFrame and switch to MENU
                    Container p = getParent();
                    while (p != null && !(p instanceof ConnectFrame))
                        p = p.getParent();
                    if (p != null)
                        ((ConnectFrame) p).showMenu();
                });

                spinBtn.addActionListener(e -> runSpin(spinBtn));

                bottom.add(spinBtn);
                bottom.add(backBtn);
                add(bottom, BorderLayout.SOUTH);
            }

            private void styleButton(JButton b) {
                b.setPreferredSize(new Dimension(160, 44));
                b.setFocusPainted(false);
                b.setBackground(ACCENT_GOLD);
                b.setForeground(TEXT_DARK);
                b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
            }

            /**
             * Configures the spin panel with player labels and completion callback.
             * 
             * @param labels     Player names to display on cards
             * @param onComplete Callback invoked when a player is selected
             */
            public void setup(String[] labels, java.util.function.Consumer<Integer> onComplete) {
                this.onSelectionComplete = onComplete;
                this.spinning = false;

                info.setText("Click a player to choose first, or press Spin Random.");

                cardsPanel.removeAll();

                int n = labels.length;
                cards = new JPanel[n];
                pickButtons = new JButton[n];

                int rows = (n <= 2) ? 1 : 2;
                int cols = (n <= 2) ? 2 : 3;

                centerGrid = new JPanel(new GridLayout(rows, cols, 20, 20));
                centerGrid.setOpaque(false);
                centerGrid.setBorder(new EmptyBorder(20, 20, 20, 20));

                for (int i = 0; i < n; i++) {
                    JPanel card = new JPanel(new BorderLayout());
                    card.setBackground(Color.WHITE);
                    card.setBorder(new LineBorder(BORDER_BROWN, 2));

                    JButton pick = new JButton(labels[i]);
                    pick.setFocusPainted(false);
                    pick.setBorderPainted(false);
                    pick.setContentAreaFilled(false);
                    pick.setOpaque(false);
                    pick.setFont(pick.getFont().deriveFont(Font.BOLD, 16f));
                    pick.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    final int idx = i;
                    pick.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            if (!spinning)
                                highlight(idx);
                        }

                        public void mouseExited(java.awt.event.MouseEvent e) {
                            if (!spinning)
                                clearHighlight();
                        }
                    });
                    pick.addActionListener(e -> choose(idx));

                    card.add(pick, BorderLayout.CENTER);
                    card.setPreferredSize(new Dimension(200, 100));
                    cards[i] = card;
                    pickButtons[i] = pick;
                    centerGrid.add(card);
                }

                // Fillers
                int totalCells = rows * cols;
                for (int k = n; k < totalCells; k++) {
                    JPanel filler = new JPanel();
                    filler.setOpaque(false);
                    centerGrid.add(filler);
                }

                cardsPanel.add(centerGrid);
                revalidate();
                repaint();
            }

            private void choose(int idx) {
                if (spinning)
                    return;
                info.setText("Selected: " + pickButtons[idx].getText() + " goes first.");

                // Flash verify then proceed
                highlight(idx);
                Timer t = new Timer(400, e -> {
                    if (onSelectionComplete != null)
                        onSelectionComplete.accept(idx);
                });
                t.setRepeats(false);
                t.start();
            }

            private void runSpin(JButton spinBtn) {
                if (spinning)
                    return;
                spinning = true;

                // Disable valid inputs
                for (JButton b : pickButtons)
                    b.setEnabled(false);
                spinBtn.setEnabled(false);

                info.setText("Spinning...");

                int n = cards.length;
                int finalIndex = rand.nextInt(n);
                int totalTicks = 20 + rand.nextInt(14);
                final int[] tick = { 0 };

                Timer timer = new Timer(70, null);
                timer.addActionListener(ev -> {
                    tick[0]++;
                    int c = (tick[0]) % n; // purely sequential visual spin
                    highlight(c);

                    timer.setDelay(70 + tick[0] * 10);

                    if (tick[0] >= totalTicks) {
                        timer.stop();
                        // Ensure we highlight the actual predetermined winner (visual trick: make last
                        // tick match final)
                        // Or just land on final. Let's strictly land on final.
                        highlight(finalIndex);
                        info.setText("Result: " + pickButtons[finalIndex].getText() + " goes first.");

                        Timer proceed = new Timer(1000, e2 -> {
                            spinBtn.setEnabled(true); // Restore just in case
                            if (onSelectionComplete != null)
                                onSelectionComplete.accept(finalIndex);
                        });
                        proceed.setRepeats(false);
                        proceed.start();
                    }
                });
                timer.start();
            }

            private void clearHighlight() {
                for (JPanel c : cards) {
                    c.setBackground(Color.WHITE);
                    c.setBorder(new LineBorder(BORDER_BROWN, 2));
                }
            }

            private void highlight(int idx) {
                Color on = new Color(255, 245, 180);
                Color off = Color.WHITE;
                for (int i = 0; i < cards.length; i++) {
                    if (i == idx) {
                        cards[i].setBackground(on);
                        cards[i].setBorder(new LineBorder(ACCENT_GOLD, 4));
                    } else {
                        cards[i].setBackground(off);
                        cards[i].setBorder(new LineBorder(BORDER_BROWN, 2));
                    }
                }
            }
        }

        // ---------- Menu ----------

        /**
         * The main menu panel allowing users to select game mode, number of players,
         * and enter names.
         */
        private class MenuPanel extends JPanel {

            private final JRadioButton level1Btn = new JRadioButton("Level 1: Player vs Player", true);
            private final JRadioButton level2Btn = new JRadioButton("Level 2: Player vs Computer (AI)");
            private final JRadioButton level34Btn = new JRadioButton("Level 3-4: Multi-player mode");

            private final CardLayout levelCards = new CardLayout();
            private final JPanel levelPanel = new JPanel(levelCards);

            private final JTextField l1p1 = new JTextField(22), l1p2 = new JTextField(22);
            private final JTextField l2human = new JTextField(22);
            private final JComboBox<String> l2diff = new JComboBox<>(new String[] { "BEGINNER", "MEDIUM", "SMART" });

            private final JRadioButton mpHumansOnlyBtn = new JRadioButton("Multiplayer 3-4 players)", true);
            private final JRadioButton mpVsAIBtn = new JRadioButton("Players versus AI (3-4 players vs AI)");

            private final JComboBox<Integer> mpHumanCount = new JComboBox<>(new Integer[] { 3, 4 });
            private final JTextField mpN1 = new JTextField(22), mpN2 = new JTextField(22), mpN3 = new JTextField(22),
                    mpN4 = new JTextField(22);

            private final JComboBox<Integer> vsHumanCount = new JComboBox<>(new Integer[] { 3, 4 });
            private final JTextField vsN1 = new JTextField(22), vsN2 = new JTextField(22), vsN3 = new JTextField(22),
                    vsN4 = new JTextField(22);
            private final JComboBox<String> vsDiff = new JComboBox<>(new String[] { "BEGINNER", "MEDIUM", "SMART" });

            private final JButton startBtn = new JButton("Start Game");
            private final JButton loadBtn = new JButton("Load Game");
            private final JComboBox<Integer> maxMoves = new JComboBox<>(new Integer[] { 24, 32, 40, 48 });

            /**
             * Constructs the menu panel with all game mode options and controls.
             */
            MenuPanel() {
                setBorder(new EmptyBorder(30, 20, 20, 20));
                setLayout(new GridBagLayout());
                setBackground(APP_BG);

                JLabel title = new JLabel("Connect 5 (8x8)");
                title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
                title.setForeground(TEXT_LIGHT);

                JPanel selector = new JPanel();
                selector.setLayout(new BoxLayout(selector, BoxLayout.Y_AXIS));
                selector.setBackground(CARD_BG);
                selector.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_BROWN, 2),
                        new EmptyBorder(10, 10, 10, 10)));

                ButtonGroup lv = new ButtonGroup();
                for (JRadioButton rb : new JRadioButton[] { level1Btn, level2Btn, level34Btn }) {
                    lv.add(rb);
                    styleRadio(rb);
                    selector.add(rb);
                    selector.add(Box.createVerticalStrut(6));
                }

                levelPanel.setBackground(APP_BG);
                levelPanel.add(buildLevel1Panel(), "L1");
                levelPanel.add(buildLevel2Panel(), "L2");
                levelPanel.add(buildLevel34Panel(), "L34");
                levelCards.show(levelPanel, "L1");

                for (JTextField t : new JTextField[] { l1p1, mpN1, vsN1 })
                    t.setText("Player 1");
                for (JTextField t : new JTextField[] { l1p2, mpN2, vsN2 })
                    t.setText("Player 2");
                for (JTextField t : new JTextField[] { mpN3, vsN3 })
                    t.setText("Player 3");
                for (JTextField t : new JTextField[] { mpN4, vsN4 })
                    t.setText("Player 4");
                l2human.setText("Player");

                level1Btn.addActionListener(e -> levelCards.show(levelPanel, "L1"));
                level2Btn.addActionListener(e -> levelCards.show(levelPanel, "L2"));
                level34Btn.addActionListener(e -> levelCards.show(levelPanel, "L34"));

                mpHumansOnlyBtn.addActionListener(e -> refreshLevel34Controls());
                mpVsAIBtn.addActionListener(e -> refreshLevel34Controls());
                mpHumanCount.addActionListener(e -> refreshLevel34Controls());
                vsHumanCount.addActionListener(e -> refreshLevel34Controls());

                styleMainBtn(startBtn, ACCENT_GOLD, TEXT_DARK);
                startBtn.addActionListener(e -> onStart());

                styleMainBtn(loadBtn, new Color(90, 100, 120), TEXT_LIGHT);
                loadBtn.addActionListener(e -> onLoad());

                refreshLevel34Controls();

                GridBagConstraints g = new GridBagConstraints();
                g.gridx = 0;
                g.gridy = 0;
                g.gridwidth = 2;
                g.insets = new Insets(0, 0, 20, 0);
                add(title, g);

                g.gridy++;
                g.insets = new Insets(0, 0, 20, 0);

                g.gridy++;
                add(selector, g);

                g.gridy++;
                add(levelPanel, g);

                g.gridy++;
                JPanel settingsBox = new JPanel(new FlowLayout(FlowLayout.CENTER));
                settingsBox.setOpaque(false);
                JLabel mmLabel = new JLabel("Max Moves (Pieces/Player):");
                mmLabel.setForeground(TEXT_LIGHT);
                settingsBox.add(mmLabel);
                settingsBox.add(maxMoves);
                add(settingsBox, g);

                g.gridy++;
                JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
                btnRow.setOpaque(false);
                btnRow.add(startBtn);
                btnRow.add(loadBtn);
                System.out.println("Adding startBtn to menu: " + startBtn);
                startBtn.setVisible(true);
                loadBtn.setVisible(true);
                add(btnRow, g);
            }

            private void styleMainBtn(JButton b, Color bg, Color fg) {
                b.setPreferredSize(new Dimension(200, 44));
                b.setFocusPainted(false);
                b.setBackground(bg);
                b.setForeground(fg);
                b.setFont(b.getFont().deriveFont(Font.BOLD, 16f));
                b.setBorder(new LineBorder(BORDER_BROWN, 2)); // Add border for wireframe look
            }

            private void styleRadio(JRadioButton rb) {
                rb.setOpaque(false);
                rb.setForeground(TEXT_LIGHT);
                rb.setFocusPainted(false);
                rb.setFont(rb.getFont().deriveFont(Font.BOLD, 14f));
            }

            private JPanel buildCardPanel(String titleText) {
                JPanel p = new JPanel(new GridBagLayout());
                p.setBackground(CARD_BG);
                p.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_BROWN, 2),
                        new EmptyBorder(16, 16, 16, 16)));
                JLabel t = new JLabel(titleText);
                t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
                t.setForeground(TEXT_LIGHT);

                GridBagConstraints g = new GridBagConstraints();
                g.gridx = 0;
                g.gridy = 0;
                g.gridwidth = 2;
                g.anchor = GridBagConstraints.WEST;
                g.insets = new Insets(0, 0, 14, 0);
                p.add(t, g);

                return p;
            }

            private JPanel buildLevel1Panel() {
                JPanel p = buildCardPanel("Level 1: Player vs Player (2 players)");
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(8, 8, 8, 8);
                g.anchor = GridBagConstraints.WEST;
                g.gridy = 0; // optimized start

                insertFieldRow(p, g, new JLabel("Player 1 Name:"), l1p1);
                insertFieldRow(p, g, new JLabel("Player 2 Name:"), l1p2);

                return p;
            }

            private JPanel buildLevel2Panel() {
                JPanel p = buildCardPanel("Level 2: Player vs Computer (AI)");
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(8, 8, 8, 8);
                g.anchor = GridBagConstraints.WEST;
                g.gridy = 0;

                insertFieldRow(p, g, new JLabel("Your Name:"), l2human);
                insertFieldRow(p, g, new JLabel("AI Difficulty:"), l2diff);

                return p;
            }

            private JPanel buildLevel34Panel() {
                JPanel p = buildCardPanel("Level 3-4: Multi-player mode");
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(4, 4, 4, 4); // tighter insets
                g.anchor = GridBagConstraints.WEST;
                g.gridy = 0;

                // Mode Selection
                ButtonGroup mpGroup = new ButtonGroup();
                mpGroup.add(mpHumansOnlyBtn);
                mpGroup.add(mpVsAIBtn);
                styleRadio(mpHumansOnlyBtn);
                styleRadio(mpVsAIBtn);

                g.gridwidth = 2;
                g.gridy++;
                p.add(mpHumansOnlyBtn, g);
                g.gridy++;
                p.add(mpVsAIBtn, g);
                g.gridwidth = 1;

                // Humans ONLY
                insertFieldRow(p, g, new JLabel("Players (3-4):"), mpHumanCount);
                insertFieldRow(p, g, new JLabel("Player 1 Name:"), mpN1);
                insertFieldRow(p, g, new JLabel("Player 2 Name:"), mpN2);
                insertFieldRow(p, g, new JLabel("Player 3 Name:"), mpN3);
                insertFieldRow(p, g, new JLabel("Player 4 Name:"), mpN4);

                // Separator
                g.gridy++;
                p.add(Box.createVerticalStrut(10), g);

                // Vs AI
                insertFieldRow(p, g, new JLabel("Players vs AI (3-4):"), vsHumanCount);
                insertFieldRow(p, g, new JLabel("Player 1 Name:"), vsN1);
                insertFieldRow(p, g, new JLabel("Player 2 Name:"), vsN2);
                insertFieldRow(p, g, new JLabel("Player 3 Name:"), vsN3);
                insertFieldRow(p, g, new JLabel("Player 4 Name:"), vsN4);
                insertFieldRow(p, g, new JLabel("AI Difficulty:"), vsDiff);

                return p;
            }

            private void insertFieldRow(JPanel p, GridBagConstraints g, JLabel lab, JComponent field) {
                lab.setForeground(TEXT_LIGHT);
                g.gridy++;
                g.gridx = 0;
                g.fill = GridBagConstraints.NONE;
                g.weightx = 0;
                p.add(lab, g);
                g.gridx = 1;
                g.fill = GridBagConstraints.HORIZONTAL;
                g.weightx = 1.0;
                p.add(field, g);
                if (field instanceof JTextField)
                    field.setPreferredSize(new Dimension(100, 24)); // ensure size
            }

            private void addNote(JPanel p, GridBagConstraints g, String text) {
                JLabel note = new JLabel(text);
                note.setForeground(TEXT_LIGHT);
                note.setFont(note.getFont().deriveFont(12f));
                g.gridx = 0;
                g.gridy++;
                g.gridwidth = 2;
                g.fill = GridBagConstraints.HORIZONTAL;
                p.add(note, g);
            }

            private void onLoad() {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Load Game");
                int res = fc.showOpenDialog(this);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    try {
                        Game loaded = FileManager.loadGame(file);
                        getFrame().switchToGame(loaded, "Resumed Game");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Failed to load game: " + e.getMessage(), "Load Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            private void refreshLevel34Controls() {
                boolean vsAI = mpVsAIBtn.isSelected();

                mpHumanCount.setEnabled(!vsAI);
                int humansOnlyCount = (Integer) mpHumanCount.getSelectedItem();

                mpN1.setEnabled(!vsAI);
                mpN2.setEnabled(!vsAI);
                mpN3.setEnabled(!vsAI);
                mpN4.setEnabled(!vsAI && humansOnlyCount == 4);

                vsHumanCount.setEnabled(vsAI);
                int vsCount = (Integer) vsHumanCount.getSelectedItem();

                vsN1.setEnabled(vsAI);
                vsN2.setEnabled(vsAI);
                vsN3.setEnabled(vsAI);
                vsN4.setEnabled(vsAI && vsCount == 4);
                vsDiff.setEnabled(vsAI);

                JTextField[] all = new JTextField[] { mpN1, mpN2, mpN3, mpN4, vsN1, vsN2, vsN3, vsN4 };
                for (JTextField f : all)
                    f.setBackground(f.isEnabled() ? Color.WHITE : new Color(230, 230, 230));
            }

            private void onStart() {
                if (level1Btn.isSelected())
                    startLevel1();
                else if (level2Btn.isSelected())
                    startLevel2();
                else
                    startLevel34();
            }

            // Retrieve the parent ConnectFrame to switch to SPIN
            private ConnectFrame getFrame() {
                Container p = getParent();
                while (p != null && !(p instanceof ConnectFrame))
                    p = p.getParent();
                return (ConnectFrame) p;
            }

            private void startLevel1() {
                String p1 = l1p1.getText().trim();
                String p2 = l1p2.getText().trim();
                if (p1.isEmpty() || p2.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter both player names.", "Missing Names",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String[] labels = new String[] { p1 + " (Player 1)", p2 + " (Player 2)" };

                ConnectFrame cf = getFrame();
                if (cf == null)
                    return;

                // Level 1: Show Colors -> Spin -> Game
                Player pl1 = new Player(p1, BLACK); // Temps for color setting
                Player pl2 = new Player(p2, WHITE);
                Player[] players = new Player[] { pl1, pl2 };

                cf.showColors(players, () -> {
                    cf.showSpin(labels, (firstIdx) -> {
                        // After spin, apply order
                        String firstName = (firstIdx == 0) ? p1 : p2;
                        // In Level 1, we often let them choose symbol (Color of Pieces)
                        // But here we just set "Custom Color" in showColors
                        // However, showSpin determines who moves FIRST.

                        // Wait, showColors let them pick p1/p2 colors.
                        // pl1 has custom color, pl2 has custom color.
                        // If p2 goes first, they are "Current Player"

                        // We still need distinct symbols (BLACK/WHITE chars) for logic?
                        // Actually logic uses 'B'/'W' chars. Color is visual.
                        // We should ensure symbols are assigned.

                        // Logic Update:
                        // 1. Color Screen: Sets pl1.color and pl2.color
                        // 2. Spin: Determines first player.
                        // 3. Start Game.

                        // We do NOT need "Choose Black/White" dialog if we have Color Screen.
                        // Colors are already chosen.
                        // We just need to map them to logical symbols B and W for the engine?
                        // Or can engine handle arbitrary symbols?
                        // Engine relies on BLACK='B', WHITE='W'.
                        // So we map pl1 -> B, pl2 -> W? Or allow swap?

                        // Simpler: Just start game with these players.
                        // The engine will use pl1.symbol ('B') and pl2.symbol ('W').
                        // And GamePanel will draw them with pl1.color and pl2.color.

                        int limit = (Integer) maxMoves.getSelectedItem();
                        cf.gamePanel.startNewGame(players, firstIdx, "Level 1", limit);
                        cards.show(root, "GAME");
                    });
                });
            }

            private void startLevel2() {
                String humanName = l2human.getText().trim();
                if (humanName.isEmpty())
                    humanName = "Player";
                String diff = (String) l2diff.getSelectedItem();
                if (diff == null)
                    diff = "BEGINNER"; // fallback

                Player human = new Player(humanName, BLACK);
                AIPlayer ai = new AIPlayer("Computer", WHITE, diff);
                Player[] players = new Player[] { human, ai };

                String[] labels = new String[] { human.name + " (Player)", ai.name + " (AI)" };

                ConnectFrame cf = getFrame();
                if (cf == null)
                    return;

                cf.showColors(players, () -> {
                    cf.showSpin(labels, (firstIdx) -> {
                        // Level 2: Colors set. Spin decided start.
                        // Human is players[0], AI is players[1].
                        // If firstIdx=1 (AI), AI moves first.

                        int limit = (Integer) maxMoves.getSelectedItem();
                        cf.gamePanel.startNewGame(players, firstIdx, "Level 2", limit);
                        cards.show(root, "GAME");
                    });
                });
            }

            private void startLevel34() {
                boolean vsAI = mpVsAIBtn.isSelected();
                if (!vsAI) {
                    int humans = (Integer) mpHumanCount.getSelectedItem();
                    Player[] players = new Player[humans];
                    JTextField[] fields = new JTextField[] { mpN1, mpN2, mpN3, mpN4 };

                    for (int i = 0; i < humans; i++) {
                        String nm = fields[i].getText().trim();
                        if (nm.isEmpty())
                            nm = "Player " + (i + 1);
                        players[i] = new Player(nm, COLOR_ORDER[i]);
                    }
                    // Humans only
                    String[] labels = new String[humans];
                    for (int i = 0; i < humans; i++)
                        labels[i] = players[i].name;

                    ConnectFrame cf = getFrame();
                    if (cf == null)
                        return;

                    cf.showColors(players, () -> {
                        cf.showSpin(labels, (firstIdx) -> {
                            int limit = (Integer) maxMoves.getSelectedItem();
                            cf.gamePanel.startNewGame(players, firstIdx, "Level 3-4 (Players only)", limit);
                            cards.show(root, "GAME");
                        });
                    });

                } else {
                    int humans = (Integer) vsHumanCount.getSelectedItem();
                    String diff = (String) vsDiff.getSelectedItem();
                    if (diff == null)
                        diff = "BEGINNER";

                    Player[] players = new Player[humans + 1];
                    JTextField[] fields = new JTextField[] { vsN1, vsN2, vsN3, vsN4 };
                    for (int i = 0; i < humans; i++) {
                        String nm = fields[i].getText().trim();
                        if (nm.isEmpty())
                            nm = "Player " + (i + 1);
                        players[i] = new Player(nm, COLOR_ORDER[i]);
                    }
                    char aiColor = (humans == 3) ? GREEN : RED;
                    players[humans] = new AIPlayer("Computer", aiColor, diff);

                    String[] labels = new String[humans + 1];
                    for (int i = 0; i < humans; i++)
                        labels[i] = players[i].name + " - " + colorName(players[i].symbol);
                    labels[humans] = players[humans].name + " (AI) - " + colorName(players[humans].symbol);

                    ConnectFrame cf = getFrame();
                    if (cf == null)
                        return;

                    cf.showColors(players, () -> {
                        cf.showSpin(labels, (firstIdx) -> {
                            int limit = (Integer) maxMoves.getSelectedItem();
                            cf.gamePanel.startNewGame(players, firstIdx, "Level 3-4 (" + humans + " Humans vs AI)",
                                    limit);
                            cards.show(root, "GAME");
                        });
                    });
                }
            }
        } // ---------- Game Panel (resizable) ----------

        /**
         * Panel for customizing player piece colors and selecting board themes.
         * Allows each player to choose a unique color from a palette.
         */
        private class ColorSelectPanel extends JPanel {
            private final JPanel listPanel;
            private Runnable onDoneCallback;
            private Player[] playersRef;

            // Available palette colors
            private final Color[] PALETTE = {
                    Color.BLACK, Color.WHITE, Color.RED, Color.BLUE, new Color(30, 160, 80), // Std
                    Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.YELLOW,
                    new Color(128, 0, 128), new Color(0, 128, 128), new Color(128, 128, 0), // Purple, Teal, Olive
                    new Color(139, 69, 19), new Color(75, 0, 130), new Color(255, 20, 147), // SaddleBrown, Indigo,
                                                                                            // DeepPink
                    new Color(0, 255, 127), new Color(70, 130, 180), new Color(220, 20, 60), // SpringGreen, SteelBlue,
                                                                                             // Crimson
                    new Color(255, 215, 0), new Color(47, 79, 79), new Color(240, 230, 140), // Gold, SlateGray, Khaki
                    new Color(138, 43, 226), new Color(255, 127, 80), new Color(176, 196, 222) // BlueViolet, Coral,
                                                                                               // LightSteelBlue
            };

            /**
             * Constructs the color selection panel with a color palette and theme options.
             */
            ColorSelectPanel() {
                setLayout(new BorderLayout());
                setBackground(PANEL_BG);

                JLabel title = new JLabel("Customize Piece Colors", SwingConstants.CENTER);
                title.setFont(new Font("SansSerif", Font.BOLD, 24));
                title.setForeground(ACCENT_GOLD);
                title.setBorder(new EmptyBorder(20, 0, 20, 0));
                add(title, BorderLayout.NORTH);

                listPanel = new JPanel();
                listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
                listPanel.setBackground(PANEL_BG);

                JScrollPane scroll = new JScrollPane(listPanel);
                scroll.setBorder(null);
                scroll.getViewport().setBackground(PANEL_BG);
                add(scroll, BorderLayout.CENTER);

                JButton backBtn = new JButton("Back to Menu");
                backBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
                backBtn.setBackground(ACCENT_GOLD); // Use Theme White
                backBtn.setForeground(TEXT_LIGHT); // Black Text
                backBtn.setFocusPainted(false);
                backBtn.setBorder(new LineBorder(BORDER_BROWN, 2)); // Black Border
                backBtn.addActionListener(e -> {
                    Container p = getParent();
                    while (p != null && !(p instanceof ConnectFrame))
                        p = p.getParent();
                    if (p != null)
                        ((ConnectFrame) p).showMenu();
                });

                JButton startBtn = new JButton("Start Game");
                startBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
                startBtn.setBackground(ACCENT_GOLD);
                startBtn.setForeground(TEXT_DARK);
                startBtn.setFocusPainted(false);
                startBtn.setBorder(new LineBorder(BORDER_BROWN, 2)); // Add border
                startBtn.addActionListener(e -> {
                    if (onDoneCallback != null)
                        onDoneCallback.run();
                });

                JPanel bottom = new JPanel(new BorderLayout());
                bottom.setBackground(PANEL_BG);
                bottom.setBorder(new EmptyBorder(15, 15, 15, 15));

                // --- Board Theme Selector ---
                JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
                themePanel.setBackground(PANEL_BG);
                themePanel.setBorder(new TitledBorder(new LineBorder(BORDER_BROWN, 1), "Board Style",
                        TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 14), TEXT_LIGHT));

                JLabel themeLbl = new JLabel("Select Tile Theme:");
                themeLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
                themeLbl.setForeground(TEXT_LIGHT);

                String[] themes = { "Classic Wood", "Classic", "Red", "Blue", "Black & White" };
                JComboBox<String> themeBox = new JComboBox<>(themes);
                themeBox.setBackground(CARD_BG); // White
                themeBox.setForeground(TEXT_LIGHT); // Black
                themeBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
                themeBox.setFocusable(false);

                // Preview Panel
                ThemePreviewPanel preview = new ThemePreviewPanel();

                themeBox.addActionListener(e -> {
                    String selected = (String) themeBox.getSelectedItem();
                    switch (selected) {
                        case "Classic Wood":
                            LIGHT_TILE = new Color(0xfbc273); // Custom Light Gold
                            DARK_TILE = new Color(0x95533b); // Custom Dark Brown
                            break;
                        case "Classic":
                            LIGHT_TILE = new Color(0xeeeed2); // Custom Beige
                            DARK_TILE = new Color(0x769656); // Custom Green
                            break;
                        case "Red":
                            LIGHT_TILE = new Color(0xEEEEEE); // Custom Light Gray
                            DARK_TILE = new Color(0xD25D5D); // Custom Red
                            break;
                        case "Blue":
                            LIGHT_TILE = new Color(0xBDE8F5); // Custom Light Blue
                            DARK_TILE = new Color(0x4988C4); // Custom Dark Blue
                            break;
                        case "Black & White":
                            LIGHT_TILE = Color.WHITE;
                            DARK_TILE = Color.DARK_GRAY; // Dark Gray to ensure Black pieces are visible
                            break;
                    }
                    preview.repaint(); // Update preview
                });

                themePanel.add(themeLbl);
                themePanel.add(themeBox);
                themePanel.add(preview);

                bottom.add(themePanel, BorderLayout.NORTH);

                JPanel btnPanel = new JPanel();
                btnPanel.setOpaque(false);
                btnPanel.add(backBtn);
                btnPanel.add(Box.createHorizontalStrut(20));
                btnPanel.add(startBtn);

                bottom.add(btnPanel, BorderLayout.SOUTH);

                add(bottom, BorderLayout.SOUTH);
            }

            // Mini Board Preview
            private class ThemePreviewPanel extends JPanel {
                ThemePreviewPanel() {
                    setPreferredSize(new Dimension(80, 80));
                    setBorder(new LineBorder(BORDER_BROWN, 1));
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int s = 20; // cell size
                    for (int r = 0; r < 4; r++) {
                        for (int c = 0; c < 4; c++) {
                            g.setColor((r + c) % 2 == 0 ? LIGHT_TILE : DARK_TILE);
                            g.fillRect(c * s, r * s, s, s);
                            g.setColor(Color.BLACK); // Outline
                            g.drawRect(c * s, r * s, s, s);
                        }
                    }
                }
            }

            void updateTheme() {
                setBackground(PANEL_BG);
                listPanel.setBackground(PANEL_BG);

                // Generic update for robustness
                for (Component c : getComponents()) {
                    processComponent(c);
                }

                // Update rows
                for (Component c : listPanel.getComponents()) {
                    if (c instanceof JPanel) {
                        JPanel row = (JPanel) c;
                        row.setBackground(CARD_BG);
                        row.setBorder(new LineBorder(BORDER_BROWN, 1));
                        updateRowChildren(row);
                    }
                }
            }

            private void processComponent(Component c) {
                if (c instanceof JPanel) {
                    ((JPanel) c).setBackground(PANEL_BG);
                    for (Component child : ((JPanel) c).getComponents())
                        processComponent(child);
                } else if (c instanceof JLabel) {
                    c.setForeground(TEXT_LIGHT);
                    if (((JLabel) c).getText().contains("Customize"))
                        ((JLabel) c).setForeground(ACCENT_GOLD);
                } else if (c instanceof JButton) {
                    JButton b = (JButton) c;
                    String txt = b.getText();
                    if ("Back to Menu".equals(txt)) {
                        b.setBackground(ACCENT_GOLD);
                        b.setForeground(TEXT_DARK);
                    } else if ("Start Game".equals(txt)) {
                        b.setBackground(ACCENT_GOLD);
                        b.setForeground(TEXT_DARK);
                    }
                }
            }

            private void updateRowChildren(JPanel row) {
                for (Component c : row.getComponents()) {
                    if (c instanceof JLabel) {
                        c.setForeground(TEXT_LIGHT);
                    } else if (c instanceof JButton) {
                        ((JButton) c).setBorder(new LineBorder(BORDER_BROWN, 1));
                    } else if (c instanceof JPanel) {
                        c.setBackground(CARD_BG);
                        updateRowChildren((JPanel) c);
                    }
                }
            }

            /**
             * Configures the color selection panel for the given players.
             * 
             * @param players Array of players who will choose colors
             * @param onDone  Callback invoked when color selection is complete
             */
            void setup(Player[] players, Runnable onDone) {
                this.playersRef = players;
                this.onDoneCallback = onDone;
                listPanel.removeAll();

                for (Player p : players) {
                    listPanel.add(createPlayerRow(p));
                    listPanel.add(Box.createVerticalStrut(15));
                }
                listPanel.revalidate();
                listPanel.repaint();
            }

            private JPanel createPlayerRow(Player p) {
                JPanel row = new JPanel(new BorderLayout(10, 10));
                row.setBackground(CARD_BG);
                row.setBorder(new LineBorder(BORDER_BROWN, 1));
                row.setMaximumSize(new Dimension(600, 110));

                // Left: Icon + Name
                JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
                info.setOpaque(false);
                JLabel iconLbl = new JLabel();
                iconLbl.setIcon(iconFor(p.color, 40));

                String role = (p instanceof AIPlayer) ? " (AI)" : "";
                JLabel nameLbl = new JLabel(p.name + role);
                nameLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
                nameLbl.setForeground(TEXT_LIGHT);

                info.add(iconLbl);
                info.add(nameLbl);
                row.add(info, BorderLayout.WEST);

                // Right: Palette
                JPanel palette = new JPanel(new GridLayout(3, 9, 2, 2));
                palette.setOpaque(false);
                palette.setBorder(new EmptyBorder(5, 5, 5, 5));

                for (Color c : PALETTE) {
                    JButton b = new JButton();
                    b.setPreferredSize(new Dimension(20, 20));
                    b.setBackground(c);
                    b.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

                    // Interaction: Click to set, Hover to preview
                    b.addMouseListener(new java.awt.event.MouseAdapter() {
                        Color original = p.color;

                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            // Preview
                            iconLbl.setIcon(iconFor(c, 40));
                            b.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                        }

                        public void mouseExited(java.awt.event.MouseEvent e) {
                            // Revert to SELECTED color (p.color might have changed if clicked)
                            iconLbl.setIcon(iconFor(p.color, 40));
                            b.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                        }

                        public void mousePressed(java.awt.event.MouseEvent e) {
                            p.color = c;
                            original = c; // Update 'original' so exit doesn't revert to old
                            iconLbl.setIcon(iconFor(c, 40));
                        }
                    });

                    palette.add(b);
                }
                row.add(palette, BorderLayout.CENTER);

                return row;
            }
        }

        /**
         * Main game panel displaying the board, player information, and game controls.
         * Handles user interactions and updates the UI based on game state.
         */
        private class GamePanel extends JPanel {

            private final JLabel titleLabel = new JLabel();
            private final JLabel turnLabel = new JLabel();
            private final JLabel timerLabel = new JLabel("Time: 10");
            private final JLabel phaseLabel = new JLabel();
            private final JLabel statusLabel = new JLabel(" ");

            private final JPanel playersBox = new JPanel();
            private final JLabel[] playerLabels = new JLabel[] { new JLabel(), new JLabel(), new JLabel(),
                    new JLabel(),
                    new JLabel() };

            private final JPanel grid = new JPanel(new GridLayout(9, 9, 1, 1));

            private final JButton[][] squares = new JButton[8][8];

            private Game game;
            private Timer turnLimitTimer;
            private int secondsLeft = 10;

            private final JButton drawBtn = new JButton("Offer / Accept Draw");
            private final JButton saveBtn = new JButton("Save Game");
            private final JButton loadBtn = new JButton("Load Game");
            private final JButton logBtn = new JButton("Open Log");
            private final JButton backBtn = new JButton("Back to Menu");
            private final JButton quitBtn = new JButton("Quit");

            /**
             * Constructs the game panel with board grid, status labels, and control
             * buttons.
             */
            GamePanel() {
                setLayout(new BorderLayout(12, 12));
                setBorder(new EmptyBorder(12, 12, 12, 12));
                setBackground(APP_BG);

                add(buildHeader(), BorderLayout.NORTH);
                add(buildBoard(), BorderLayout.CENTER);
                add(buildStatusBar(), BorderLayout.SOUTH);

                // Timer tick every 1 second
                turnLimitTimer = new Timer(1000, e -> {
                    if (game == null || game.isGameOver()) {
                        turnLimitTimer.stop();
                        return;
                    }
                    if (game.currentPlayer() instanceof AIPlayer) {
                        secondsLeft--;
                        if (secondsLeft < 0)
                            secondsLeft = 0;
                        updateTimerLabel();
                    } else {
                        updateTimerLabel();
                    }
                });

                // When window is resized, re-render so icons scale to new button sizes
                addComponentListener(new java.awt.event.ComponentAdapter() {
                    @Override
                    public void componentResized(java.awt.event.ComponentEvent e) {
                        render();
                    }
                });
            }

            private void updateTimerLabel() {
                if (game != null && !(game.currentPlayer() instanceof AIPlayer)) {
                    timerLabel.setText(""); // Remove redundant "Unlimited" text
                    timerLabel.setForeground(TEXT_LIGHT);
                } else {
                    timerLabel.setText("Time: " + secondsLeft + "s");
                    timerLabel.setForeground(secondsLeft <= 3 ? Color.RED : TEXT_LIGHT);
                }
            }

            private void resetTurnTimer() {
                secondsLeft = 10;
                updateTimerLabel();
                turnLimitTimer.restart();
            }

            private JPanel buildHeader() {
                JPanel header = new JPanel(new BorderLayout(12, 12));
                header.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_BROWN, 1),
                        new EmptyBorder(2, 2, 2, 2)));
                header.setBackground(PANEL_BG);

                JPanel left = new JPanel();
                left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
                left.setBackground(PANEL_BG);

                titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
                turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 15f));
                phaseLabel.setFont(phaseLabel.getFont().deriveFont(13f));
                titleLabel.setForeground(TEXT_LIGHT);
                turnLabel.setForeground(TEXT_LIGHT);
                phaseLabel.setForeground(TEXT_LIGHT);

                playersBox.setLayout(new BoxLayout(playersBox, BoxLayout.Y_AXIS));
                playersBox.setBackground(PANEL_BG);

                for (JLabel lab : playerLabels) {
                    lab.setFont(lab.getFont().deriveFont(13f));
                    lab.setForeground(TEXT_LIGHT);
                    playersBox.add(lab);
                    playersBox.add(Box.createVerticalStrut(2));
                }

                timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 22f));
                timerLabel.setForeground(TEXT_LIGHT);

                left.add(titleLabel);
                left.add(Box.createVerticalStrut(6));
                left.add(turnLabel);
                left.add(Box.createVerticalStrut(4));
                left.add(timerLabel); // Add timer to display
                left.add(Box.createVerticalStrut(4));
                left.add(phaseLabel);
                left.add(Box.createVerticalStrut(10));
                left.add(playersBox);

                JPanel right = new JPanel();
                right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
                right.setBackground(PANEL_BG);

                Dimension btnSize = new Dimension(170, 34);

                for (JButton b : new JButton[] { drawBtn, saveBtn, loadBtn, logBtn, backBtn, quitBtn }) {
                    b.setMaximumSize(btnSize);
                    b.setPreferredSize(btnSize);
                    b.setAlignmentX(Component.CENTER_ALIGNMENT);
                    b.setFocusPainted(false);
                    b.setBackground(ACCENT_GOLD);
                    b.setForeground(TEXT_DARK);
                }

                drawBtn.addActionListener(e -> onDraw());
                saveBtn.addActionListener(e -> onSave());
                loadBtn.addActionListener(e -> onLoad());

                logBtn.addActionListener(e -> onOpenLog());
                backBtn.addActionListener(e -> {
                    if (game != null && !game.isGameOver()) {
                        int ok = JOptionPane.showConfirmDialog(
                                ConnectFrame.this,
                                "Return to menu? Current game will be abandoned.",
                                "Confirm",
                                JOptionPane.YES_NO_OPTION);
                        if (ok != JOptionPane.YES_OPTION)
                            return;
                    }
                    cards.show(root, "MENU");
                });
                quitBtn.addActionListener(e -> dispose());

                right.add(drawBtn);
                right.add(Box.createVerticalStrut(8));
                right.add(saveBtn);
                right.add(Box.createVerticalStrut(8));
                right.add(loadBtn);
                right.add(Box.createVerticalStrut(8));
                right.add(logBtn);
                right.add(Box.createVerticalStrut(8));
                right.add(backBtn);
                right.add(Box.createVerticalStrut(8));
                right.add(quitBtn);

                header.add(left, BorderLayout.CENTER);
                header.add(right, BorderLayout.EAST);
                return header;
            }

            private JPanel buildBoard() {
                JPanel boardPanel = new JPanel(new BorderLayout());
                boardPanel.setBackground(APP_BG);

                grid.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_BROWN, 1),
                        new EmptyBorder(2, 2, 2, 2)));
                grid.setBackground(Color.BLACK); // Black background for gaps (Outlines)

                JLabel corner = new JLabel("");
                corner.setOpaque(true);
                corner.setBackground(PANEL_BG);
                grid.add(corner);

                for (int c = 0; c < 8; c++) {
                    JLabel l = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
                    l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
                    l.setForeground(TEXT_LIGHT);
                    l.setOpaque(true);
                    l.setBackground(PANEL_BG);
                    grid.add(l);
                }

                for (int displayRow = 0; displayRow < 8; displayRow++) {
                    int rowLabel = 8 - displayRow;
                    JLabel rl = new JLabel(String.valueOf(rowLabel), SwingConstants.CENTER);
                    rl.setFont(rl.getFont().deriveFont(Font.BOLD, 12f));
                    rl.setForeground(TEXT_LIGHT);
                    rl.setOpaque(true);
                    rl.setBackground(PANEL_BG);
                    grid.add(rl);

                    for (int c = 0; c < 8; c++) {
                        JButton btn = new JButton();
                        btn.setFocusPainted(false);
                        btn.setMargin(new Insets(0, 0, 0, 0));
                        btn.setOpaque(true);
                        btn.setContentAreaFilled(true);
                        btn.setBorder(new LineBorder(BORDER_BROWN, 1));

                        final int col = c;
                        final int dispR = displayRow;
                        btn.addActionListener(e -> onSquareClicked(col, dispR));

                        squares[displayRow][c] = btn;
                        grid.add(btn);
                    }
                }

                boardPanel.add(grid, BorderLayout.CENTER);
                return boardPanel;
            }

            private JPanel buildStatusBar() {
                JPanel status = new JPanel(new BorderLayout());
                status.setBackground(PANEL_BG);
                status.setBorder(new LineBorder(BORDER_BROWN, 2));

                statusLabel.setBorder(new EmptyBorder(8, 10, 8, 10));
                statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 12f));
                statusLabel.setForeground(TEXT_LIGHT);

                status.add(statusLabel, BorderLayout.CENTER);
                return status;
            }

            /**
             * Starts a new game with the specified players and settings.
             * 
             * @param players         Array of players
             * @param firstIndex      Index of the player who goes first
             * @param modeName        Name/description of the game mode
             * @param piecesPerPlayer Maximum pieces/moves per player
             */
            void startNewGame(Player[] players, int firstIndex, String modeName, int piecesPerPlayer) {
                new File("logs").mkdirs();

                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String logName = "logs/connect_log_" + ts + ".txt";
                Logger logger = new Logger(logName);

                logger.writeToFile("Connect on 8x8 Log");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
                logger.writeToFile("Started: " + LocalDateTime.now().format(fmt));
                logger.writeToFile("Mode: " + modeName);
                logger.writeToFile("Connect target: 5");
                logger.writeToFile("Pieces per participant: " + piecesPerPlayer);
                logger.writeToFile("Participants: " + players.length);
                for (int i = 0; i < players.length; i++) {
                    Player p = players[i];
                    logger.writeToFile("P" + (i + 1) + ": " + p.name + " (" + colorName(p.symbol) + ")"
                            + (p instanceof AIPlayer ? " AI" : ""));
                }
                logger.writeToFile("------------------------------------------------------------");

                game = new Game(5, piecesPerPlayer, logger, players, firstIndex);

                titleLabel.setText(modeName);
                statusLabel.setText("Game started. Log: " + logName + ". Click squares to place pieces.");
                drawBtn.setEnabled(game.drawAvailable());

                enableBoard();
                render();

                resetTurnTimer(); // Start timer for first turn
                triggerAIIfNeeded();
            }

            /**
             * Resumes a previously saved game.
             * 
             * @param game     The Game instance to resume
             * @param modeName Name/description of the game mode
             */
            void resumeGame(Game game, String modeName) {
                this.game = game;
                titleLabel.setText(modeName);
                statusLabel.setText("Game resumed. Log: " + game.logger.fileName);
                drawBtn.setEnabled(game.drawAvailable());

                checkGameOverPopup(); // Will disable board if over
                if (!game.isGameOver()) {
                    enableBoard();
                    resetTurnTimer();
                    triggerAIIfNeeded();
                } else {
                    disableBoard();
                }
                render();
            }

            private void onSave() {
                if (game == null)
                    return;
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Save Game");
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        String m = titleLabel.getText();
                        FileManager.saveGame(game, fc.getSelectedFile(), m);
                        statusLabel.setText("Game saved successfully.");
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "Error saving game: " + e.getMessage(), "Save Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            private void onLoad() {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Load Game");
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    try {
                        Game loaded = FileManager.loadGame(file);
                        resumeGame(loaded, "Resumed Game");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Failed to load game: " + e.getMessage(), "Load Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            private void onOpenLog() {
                if (game == null || game.logger == null || game.logger.fileName == null) {
                    JOptionPane.showMessageDialog(this, "No log file is available yet.", "Log",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                try {
                    File f = new File(game.logger.fileName);
                    // Make sure parent folder exists (in case log path changes)
                    File parent = f.getParentFile();
                    if (parent != null)
                        parent.mkdirs();

                    if (!f.exists()) {
                        // Create the file if it doesn't exist yet so Desktop.open has something to open
                        f.createNewFile();
                    }

                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(f);
                        statusLabel.setText("Opened log: " + f.getPath());
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Desktop open is not supported on this system.\nLog file: " + f.getAbsolutePath(),
                                "Log", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Could not open the log file.\n" + ex.getMessage() + "\n\nLog path: "
                                    + game.logger.fileName,
                            "Log Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            private void onDraw() {
                if (game == null)
                    return;
                if (!game.drawAvailable()) {
                    statusLabel.setText("Draw is only available in Level 1 (2 players).");
                    return;
                }
                if (game.currentPlayer() instanceof AIPlayer) {
                    statusLabel.setText("It's the computer's turn.");
                    return;
                }

                String msg = game.offerOrAcceptDraw();
                statusLabel.setText(msg);
                // If turn count changed (draw might not change turn, but here offer/accept
                // logic is complex.
                // Simplest: if draw accepted -> game over. If offer -> turn swaps?
                // Game logic says "offerOrAcceptDraw" does "switchPlayer" if offering.
                // So we should check if player index changed.)

                render();
                checkGameOverPopup();
                // If game not over, reset timer for next player?
                // Ideally track 'currentPlayerIndex' before/after.
                // For simplicity, just reset if game active.
                if (!game.isGameOver())
                    resetTurnTimer();

                triggerAIIfNeeded();
            }

            private void onSquareClicked(int col, int displayRow) {
                if (game == null)
                    return;

                if (game.currentPlayer() instanceof AIPlayer) {
                    statusLabel.setText("Computer is thinking...");
                    return;
                }

                int r = 7 - displayRow;
                int c = col;

                String msg = game.handleClick(c, r);
                statusLabel.setText(msg);

                // If valid move (contains PLACE or MOVE), turn changed.
                if (msg.contains("PLACE") || msg.contains("MOVE")) {
                    resetTurnTimer();
                }

                render();
                checkGameOverPopup();
                triggerAIIfNeeded();
            }

            private void triggerAIIfNeeded() {
                if (game == null || game.isGameOver()) {
                    turnLimitTimer.stop();
                    return;
                }
                if (!(game.currentPlayer() instanceof AIPlayer))
                    return;

                disableBoard();

                // Run AI in background thread to keep Timer animating
                new Thread(() -> {
                    // Small delay to let user see board update
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ignored) {
                    }

                    // Double check state
                    if (game == null || game.isGameOver())
                        return;

                    // Compute move (taking up to 9.5s)
                    // We must be careful not to touch Swing components here,
                    // but game.performAITurn() updates game model which IS valid if we own the
                    // lock?
                    // Swing is single separate thread. We are reader/writer here.
                    // 'game' is not synchronized. But user input is disabled.
                    // So only 'render()' (EDT) and this thread access 'game'.
                    // 'performAITurn' calculates then writes.
                    // To be safe, we calculate move, then apply on EDT?
                    // Or just invokeAndWait for the apply part?
                    // 'performAITurn' does both.
                    // Given 'pickMove' is 99% of time, let's call pickMove here (need cast).

                    AIPlayer ai = (AIPlayer) game.currentPlayer();
                    AIPlayer.Move bestCheck = ai.pickMove(game);

                    // Now apply on EDT
                    SwingUtilities.invokeLater(() -> {
                        if (game == null || game.isGameOver())
                            return;
                        // verify it's still AI turn (should be)
                        if (game.currentPlayer() != ai)
                            return;

                        String desc;
                        if (bestCheck == null) {
                            desc = "AI has no valid moves.";
                        } else {
                            if ("PLACEMENT".equals(game.gamePhase)) {
                                game.board.placePiece(new int[] { bestCheck.toC, bestCheck.toR }, ai.symbol);
                                ai.piecesPlaced++;
                                desc = ai.name + " (AI) PLACE " + Board.posToLabel(bestCheck.toC, bestCheck.toR);
                                if (game.allPlayersPlaced())
                                    game.gamePhase = "MOVEMENT";
                            } else {
                                game.board.movePiece(new int[] { bestCheck.fromC, bestCheck.fromR },
                                        new int[] { bestCheck.toC, bestCheck.toR }, ai.symbol);
                                ai.movesMade++;
                                desc = ai.name + " (AI) MOVE " + Board.posToLabel(bestCheck.fromC, bestCheck.fromR)
                                        + " -> " + Board.posToLabel(bestCheck.toC, bestCheck.toR);
                            }
                            game.logTurn(desc);
                            game.advanceTurn();
                        }

                        statusLabel.setText(desc);
                        resetTurnTimer(); // Reset for next player

                        render();
                        checkGameOverPopup();

                        if (!game.isGameOver())
                            enableBoard();
                        else
                            disableBoard();

                        // Chain reaction if next is also AI?
                        triggerAIIfNeeded();
                    });
                }).start();
            }

            private void checkGameOverPopup() {
                if (game == null)
                    return;
                game.checkWinner();

                if (game.winner != null) {
                    String msg = game.winner.name + " wins by connecting " + game.connectTarget + "!";
                    statusLabel.setText(msg);
                    JOptionPane.showMessageDialog(this, msg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    disableBoard();
                } else if (game.isDraw()) {
                    String msg = "Game ended in a draw.";
                    statusLabel.setText(msg);
                    JOptionPane.showMessageDialog(this, msg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    disableBoard();
                }
            }

            private void disableBoard() {
                for (int dr = 0; dr < 8; dr++)
                    for (int c = 0; c < 8; c++)
                        squares[dr][c].setEnabled(false);
            }

            private void enableBoard() {
                for (int dr = 0; dr < 8; dr++)
                    for (int c = 0; c < 8; c++)
                        squares[dr][c].setEnabled(true);
            }

            private int computeCellPx() {
                // Base cell size off the grid panel size (9x9 includes labels),
                // leaving some safety for borders/gaps.
                int w = grid.getWidth();
                int h = grid.getHeight();
                if (w <= 0 || h <= 0)
                    return 24;

                int cellW = w / 9;
                int cellH = h / 9;
                return Math.max(18, Math.min(cellW, cellH));
            }

            private void render() {
                if (game == null)
                    return;

                Player cur = game.currentPlayer();
                String curName = cur.name + ((cur instanceof AIPlayer) ? " (AI)" : "");
                turnLabel.setText("Turn: " + game.turnCount + " | Current: " + curName);
                phaseLabel.setText("Phase: " + game.gamePhase + " | Connect " + game.connectTarget);

                for (int i = 0; i < playerLabels.length; i++) {
                    if (i < game.players.length) {
                        Player p = game.players[i];
                        String extra = (p instanceof AIPlayer) ? (" | AI: " + ((AIPlayer) p).difficulty) : "";
                        String stats;
                        if ("PLACEMENT".equals(game.gamePhase)) {
                            int remaining = 8 - p.piecesPlaced; // PLACEMENT_LIMIT
                            stats = " | " + remaining + " pieces remaining";
                        } else {
                            int remaining = game.maxMoves - p.movesMade;
                            stats = " | " + remaining + " moves remaining";
                        }
                        playerLabels[i].setText("P" + (i + 1) + ": " + p.name + " (" + colorName(p.symbol)
                                + ")" + stats + extra);

                        // Use BOLD for everyone to prevent layout jumping ("popping")
                        playerLabels[i].setFont(playerLabels[i].getFont().deriveFont(Font.BOLD));

                        // Highlight active player with Color and Border
                        if (i == game.currentPlayerIndex) {
                            playerLabels[i].setForeground(TEXT_LIGHT); // Black
                            playerLabels[i].setBorder(new LineBorder(BORDER_BROWN, 2)); // Bold Black Border
                        } else {
                            playerLabels[i].setForeground(Color.DARK_GRAY); // Dimmed for inactive
                            playerLabels[i].setBorder(null);
                        }
                        playerLabels[i].setVisible(true);
                    } else {
                        playerLabels[i].setText("");
                        playerLabels[i].setVisible(false);
                    }
                }

                int[] sel = game.getSelectedFrom();
                int cellPx = computeCellPx();
                int iconPx = Math.max(12, (int) (cellPx * 0.62));

                for (int displayRow = 0; displayRow < 8; displayRow++) {
                    int r = 7 - displayRow;
                    for (int c = 0; c < 8; c++) {
                        JButton btn = squares[displayRow][c];

                        Color tile = ((r + c) % 2 == 0) ? LIGHT_TILE : DARK_TILE;
                        btn.setBackground(tile);

                        char v = game.board.grid[r][c];
                        if (v == EMPTY) {
                            btn.setIcon(null);
                        } else {
                            Player p = game.getPlayerBySymbol(v);
                            Color pieceColor = (p != null) ? p.color : getDefaultColor(v);
                            btn.setIcon(iconFor(pieceColor, iconPx));
                        }

                        btn.setBorder(new LineBorder(BORDER_BROWN, 1));
                        if (sel != null && sel[0] == c && sel[1] == r) {
                            btn.setBackground(new Color(255, 245, 180));
                            btn.setBorder(new LineBorder(new Color(160, 120, 0), 2));
                        }

                        btn.setEnabled(!game.isGameOver() && !(game.currentPlayer() instanceof AIPlayer));
                    }
                }

                revalidate();
                repaint();
            }
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConnectFrame f = new ConnectFrame();
            f.setVisible(true);
        });
    }
}
