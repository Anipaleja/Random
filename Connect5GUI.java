package FinalProject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

/**
 * Connect5GUI.java (FinalProject)
 * - Now RESIZABLE: board squares and piece icons scale with the window.
 */
public class Connect5GUI {

    private static final char EMPTY = '.';
    private static final char BLACK = 'B';
    private static final char WHITE = 'W';
    private static final char BLUE  = 'U';
    private static final char GREEN = 'G';
    private static final char RED   = 'R';

    private static final char[] COLOR_ORDER = new char[]{BLACK, WHITE, BLUE, GREEN};

    // Chessboard tiles
    private static final Color LIGHT_TILE = new Color(240, 217, 181);
    private static final Color DARK_TILE  = new Color(181, 136, 99);

    // Theme
    private static final Color APP_BG       = new Color(70, 45, 28);
    private static final Color PANEL_BG     = new Color(96, 64, 40);
    private static final Color CARD_BG      = new Color(110, 74, 47);
    private static final Color TEXT_LIGHT   = new Color(245, 236, 222);
    private static final Color TEXT_DARK    = new Color(45, 30, 20);
    private static final Color ACCENT_GOLD  = new Color(210, 170, 90);
    private static final Color BORDER_BROWN = new Color(120, 90, 60);

    // -------- scalable piece icon --------
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

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

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

    private static String colorName(char sym) {
        if (sym == BLACK) return "Black";
        if (sym == WHITE) return "White";
        if (sym == BLUE)  return "Blue";
        if (sym == GREEN) return "Green";
        if (sym == RED)   return "Red";
        return "Unknown";
    }

    // Build icon dynamically based on current button size (so it scales)
    private static Icon iconFor(char sym, int px) {
        int size = Math.max(12, px);
        if (sym == BLACK) return new CircleIcon(size, Color.BLACK, null, 0f);
        if (sym == WHITE) return new CircleIcon(size, Color.WHITE, Color.BLACK, Math.max(1f, size / 16f));
        if (sym == BLUE)  return new CircleIcon(size, new Color(30, 90, 210), Color.BLACK, Math.max(1f, size / 18f));
        if (sym == GREEN) return new CircleIcon(size, new Color(30, 160, 80), Color.BLACK, Math.max(1f, size / 18f));
        if (sym == RED)   return new CircleIcon(size, new Color(210, 50, 50), Color.BLACK, Math.max(1f, size / 18f));
        return null;
    }

    // =========================
    // Player (UML-aligned)
    // =========================
    static class Player {
        String name;
        char symbol;
        int piecesPlaced;

        Player(String name, char symbol) {
            this.name = name;
            this.symbol = symbol;
            this.piecesPlaced = 0;
        }

        public void makeMove(Board board, String phase) { }
    }

    // =========================
    // AIPlayer (UML-aligned)
    // =========================
    static class AIPlayer extends Player {
        String difficulty;
        private final Random rand = new Random();

        AIPlayer(String name, char symbol, String difficulty) {
            super(name, symbol);
            this.difficulty = difficulty;
        }

        @Override public void makeMove(Board board, String phase) { }
        public void chooseMove(Board board) { }

        static class Move {
            boolean placement;
            int fromC, fromR;
            int toC, toR;

            Move(int toC, int toR) { placement = true; this.toC = toC; this.toR = toR; }
            Move(int fromC, int fromR, int toC, int toR) {
                placement = false;
                this.fromC = fromC; this.fromR = fromR;
                this.toC = toC; this.toR = toR;
            }
        }

        Move pickMove(Game game) {
            String d = (difficulty == null) ? "BEGINNER" : difficulty.toUpperCase();

            ArrayList<Move> moves = generateMoves(game, this.symbol);
            if (moves.isEmpty()) return null;

            Move win = findWinningMove(game, this.symbol);
            if (win != null) return win;

            if (!"BEGINNER".equals(d)) {
                Move block = findBestBlockAnyOpponent(game, this.symbol);
                if (block != null) return block;
            }

            if ("BEGINNER".equals(d)) return moves.get(rand.nextInt(moves.size()));
            if ("MEDIUM".equals(d)) return pickBestHeuristic(game, this.symbol, moves);

            return pickTwoPlyVsNext(game, this.symbol, moves);
        }

        private ArrayList<Move> generateMoves(Game game, char sym) {
            ArrayList<Move> list = new ArrayList<>();
            Player p = game.getPlayerBySymbol(sym);
            if (p == null) return list;

            if ("PLACEMENT".equals(game.gamePhase)) {
                if (p.piecesPlaced >= game.maxPieces) return list;
                for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++)
                    if (game.board.grid[r][c] == EMPTY) list.add(new Move(c, r));
            } else {
                for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                    if (game.board.grid[r][c] != sym) continue;
                    for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (nc < 0 || nc >= 8 || nr < 0 || nr >= 8) continue;
                        if (game.board.grid[nr][nc] == EMPTY) list.add(new Move(c, r, nc, nr));
                    }
                }
            }
            return list;
        }

        private ArrayList<Move> generateMovesOnGrid(Game game, char[][] grid, char sym) {
            ArrayList<Move> list = new ArrayList<>();
            Player p = game.getPlayerBySymbol(sym);
            if (p == null) return list;

            if ("PLACEMENT".equals(game.gamePhase)) {
                if (p.piecesPlaced >= game.maxPieces) return list;
                for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++)
                    if (grid[r][c] == EMPTY) list.add(new Move(c, r));
            } else {
                for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                    if (grid[r][c] != sym) continue;
                    for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (nc < 0 || nc >= 8 || nr < 0 || nr >= 8) continue;
                        if (grid[nr][nc] == EMPTY) list.add(new Move(c, r, nc, nr));
                    }
                }
            }
            return list;
        }

        private Move findWinningMove(Game game, char sym) {
            ArrayList<Move> moves = generateMoves(game, sym);
            for (Move m : moves) {
                char[][] g2 = copyGrid(game.board.grid);
                applyMove(g2, m, sym);
                if (winnerOnGrid(g2, game.connectTarget) == sym) return m;
            }
            return null;
        }

        private Move findBestBlockAnyOpponent(Game game, char me) {
            ArrayList<Character> oppSyms = new ArrayList<>();
            for (Player p : game.players) if (p.symbol != me) oppSyms.add(p.symbol);

            boolean threat = false;
            for (char opp : oppSyms) {
                ArrayList<Move> omoves = generateMoves(game, opp);
                for (Move om : omoves) {
                    char[][] g2 = copyGrid(game.board.grid);
                    applyMove(g2, om, opp);
                    if (winnerOnGrid(g2, game.connectTarget) == opp) { threat = true; break; }
                }
                if (threat) break;
            }
            if (!threat) return null;

            ArrayList<Move> myMoves = generateMoves(game, me);
            Move best = null;
            int bestThreats = Integer.MAX_VALUE;

            for (Move mm : myMoves) {
                char[][] g2 = copyGrid(game.board.grid);
                applyMove(g2, mm, me);

                int threats = 0;
                for (char opp : oppSyms) {
                    ArrayList<Move> omoves = generateMovesOnGrid(game, g2, opp);
                    for (Move om : omoves) {
                        char[][] g3 = copyGrid(g2);
                        applyMove(g3, om, opp);
                        if (winnerOnGrid(g3, game.connectTarget) == opp) threats++;
                    }
                }

                if (threats < bestThreats) {
                    bestThreats = threats;
                    best = mm;
                    if (bestThreats == 0) break;
                }
            }
            return best;
        }

        private Move pickBestHeuristic(Game game, char me, ArrayList<Move> moves) {
            Move best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Move m : moves) {
                char[][] g2 = copyGrid(game.board.grid);
                applyMove(g2, m, me);
                int score = heuristic(game, g2, me) + centerScore(m.toC, m.toR);
                if (score > bestScore) { bestScore = score; best = m; }
            }
            return (best != null) ? best : moves.get(rand.nextInt(moves.size()));
        }

        private Move pickTwoPlyVsNext(Game game, char me, ArrayList<Move> moves) {
            char next = game.nextPlayerSymbol();
            Move best = null;
            int bestVal = Integer.MIN_VALUE;

            for (Move m : moves) {
                char[][] g2 = copyGrid(game.board.grid);
                applyMove(g2, m, me);

                if (winnerOnGrid(g2, game.connectTarget) == me) return m;

                ArrayList<Move> nextMoves = generateMovesOnGrid(game, g2, next);
                int worst = Integer.MAX_VALUE;

                if (nextMoves.isEmpty()) worst = heuristic(game, g2, me);
                else {
                    for (Move nm : nextMoves) {
                        char[][] g3 = copyGrid(g2);
                        applyMove(g3, nm, next);
                        if (winnerOnGrid(g3, game.connectTarget) == next) worst = Math.min(worst, -1_000_000);
                        else worst = Math.min(worst, heuristic(game, g3, me));
                    }
                }

                if (worst > bestVal) { bestVal = worst; best = m; }
            }
            return (best != null) ? best : moves.get(rand.nextInt(moves.size()));
        }

        private int centerScore(int c, int r) {
            int dc = Math.abs(c - 3) + Math.abs(c - 4);
            int dr = Math.abs(r - 3) + Math.abs(r - 4);
            return 50 - (dc + dr) * 5;
        }

        private int heuristic(Game game, char[][] grid, char me) {
            int score = 0;
            int n = game.connectTarget;

            for (int r = 0; r < 8; r++) for (int c = 0; c <= 8 - n; c++) score += windowScore(grid, r, c, 0, 1, me, n);
            for (int c = 0; c < 8; c++) for (int r = 0; r <= 8 - n; r++) score += windowScore(grid, r, c, 1, 0, me, n);
            for (int r = 0; r <= 8 - n; r++) for (int c = 0; c <= 8 - n; c++) score += windowScore(grid, r, c, 1, 1, me, n);
            for (int r = n - 1; r < 8; r++) for (int c = 0; c <= 8 - n; c++) score += windowScore(grid, r, c, -1, 1, me, n);

            score += centerControl(grid, me) * 4;
            return score;
        }

        private int windowScore(char[][] grid, int r0, int c0, int dr, int dc, char me, int len) {
            int meCount = 0, otherCount = 0;
            for (int i = 0; i < len; i++) {
                char v = grid[r0 + dr * i][c0 + dc * i];
                if (v == me) meCount++;
                else if (v != EMPTY) otherCount++;
            }
            if (meCount > 0 && otherCount > 0) return 0;

            if (meCount == 5) return 1_000_000;
            if (meCount == 4 && otherCount == 0) return 8000;
            if (meCount == 3 && otherCount == 0) return 900;
            if (meCount == 2 && otherCount == 0) return 120;
            if (meCount == 1 && otherCount == 0) return 15;

            if (meCount == 0 && otherCount == 0) return 1;
            if (meCount == 0 && otherCount > 0) return -otherCount * 10;
            return 0;
        }

        private int centerControl(char[][] grid, char sym) {
            int count = 0;
            for (int r = 2; r <= 5; r++) for (int c = 2; c <= 5; c++)
                if (grid[r][c] == sym) count++;
            return count;
        }

        private char[][] copyGrid(char[][] src) {
            char[][] dst = new char[8][8];
            for (int r = 0; r < 8; r++) System.arraycopy(src[r], 0, dst[r], 0, 8);
            return dst;
        }

        private void applyMove(char[][] grid, Move m, char sym) {
            if (m.placement) grid[m.toR][m.toC] = sym;
            else {
                grid[m.fromR][m.fromC] = EMPTY;
                grid[m.toR][m.toC] = sym;
            }
        }

        private char winnerOnGrid(char[][] grid, int connectTarget) {
            int n = connectTarget;
            int[][] dirs = new int[][]{{1,0},{0,1},{1,1},{1,-1}};

            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                char s = grid[r][c];
                if (s == EMPTY) continue;

                for (int[] d : dirs) {
                    int dc = d[0], dr = d[1];
                    int count = 1;
                    int nc = c + dc, nr = r + dr;
                    while (nc >= 0 && nc < 8 && nr >= 0 && nr < 8 && grid[nr][nc] == s) {
                        count++;
                        if (count >= n) return s;
                        nc += dc; nr += dr;
                    }
                }
            }
            return 0;
        }
    }

    // =========================
    // Board
    // =========================
    static class Board {
        char[][] grid = new char[8][8];

        public void initializeBoard() {
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) grid[r][c] = EMPTY;
        }

        public boolean placePiece(int[] pos, char symbol) {
            int c = pos[0], r = pos[1];
            if (c < 0 || c >= 8 || r < 0 || r >= 8) return false;
            if (grid[r][c] != EMPTY) return false;
            grid[r][c] = symbol;
            return true;
        }

        public boolean movePiece(int[] from, int[] to, char symbol) {
            int fc = from[0], fr = from[1], tc = to[0], tr = to[1];
            if (fc < 0 || fc >= 8 || fr < 0 || fr >= 8) return false;
            if (tc < 0 || tc >= 8 || tr < 0 || tr >= 8) return false;
            if (grid[fr][fc] != symbol) return false;
            if (grid[tr][tc] != EMPTY) return false;

            int dc = Math.abs(tc - fc);
            int dr = Math.abs(tr - fr);
            if (dc > 1 || dr > 1 || (dc == 0 && dr == 0)) return false;

            grid[fr][fc] = EMPTY;
            grid[tr][tc] = symbol;
            return true;
        }

        public String toConsoleString() {
            StringBuilder sb = new StringBuilder();
            sb.append("    A B C D E F G H\n");
            for (int displayRow = 7; displayRow >= 0; displayRow--) {
                sb.append(String.format("%2d  ", displayRow + 1));
                for (int c = 0; c < 8; c++) sb.append(grid[displayRow][c]).append(' ');
                sb.append('\n');
            }
            return sb.toString();
        }

        public static String posToLabel(int c, int r) {
            return "" + (char)('A' + c) + (r + 1);
        }
    }

    // =========================
    // Logger
    // =========================
    static class Logger {
        String fileName;
        private PrintWriter out;

        Logger(String fileName) { this.fileName = fileName; }

        public void writeToFile(String text) {
            try {
                if (out == null) out = new PrintWriter(new FileWriter(fileName, true));
                out.println(text);
                out.flush();
            } catch (IOException ignored) { }
        }

        public void closeFile() {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    // =========================
    // Game
    // =========================
    static class Game {
        Board board;
        Player[] players;
        int currentPlayerIndex;
        int turnCount;
        int connectTarget;
        int maxPieces;
        String gamePhase;
        Player winner;
        Logger logger;

        private int[] selectedFrom = null;

        // draw only for 2-human mode
        private boolean draw = false;
        private Integer drawOfferFrom = null;

        Game(int connectTarget, int maxPieces, Logger logger, Player[] players, int firstIndex) {
            this.connectTarget = connectTarget;
            this.maxPieces = maxPieces;
            this.logger = logger;

            this.board = new Board();
            this.board.initializeBoard();

            this.players = players;
            this.currentPlayerIndex = firstIndex;
            this.turnCount = 1;
            this.gamePhase = "PLACEMENT";
            this.winner = null;
        }

        public Player currentPlayer() { return players[currentPlayerIndex]; }

        public Player getPlayerBySymbol(char sym) {
            for (Player p : players) if (p.symbol == sym) return p;
            return null;
        }

        public char nextPlayerSymbol() {
            int nxt = (currentPlayerIndex + 1) % players.length;
            return players[nxt].symbol;
        }

        public int[] getSelectedFrom() { return selectedFrom; }

        private boolean allPlayersPlaced() {
            for (Player p : players) if (p.piecesPlaced < maxPieces) return false;
            return true;
        }

        public boolean drawAvailable() {
            return (players.length == 2
                    && !(players[0] instanceof AIPlayer)
                    && !(players[1] instanceof AIPlayer));
        }

        public String offerOrAcceptDraw() {
            if (!drawAvailable()) return "Draw is only available in Level 1 (2 human players).";
            if (winner != null) return "Game is already over.";
            if (draw) return "Game is already a draw.";

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

        public void checkWinner() {
            char w = findWinnerSymbol();
            if (w == 0) winner = null;
            else {
                for (Player p : players) if (p.symbol == w) { winner = p; return; }
                winner = null;
            }
        }

        public boolean isGameOver() {
            checkWinner();
            if (winner != null) return true;
            if (draw) return true;
            return turnCount >= 300;
        }

        public boolean isDraw() {
            if (draw) return true;
            return (winner == null && turnCount >= 300);
        }

        public void switchPlayer() {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
            selectedFrom = null;
        }

        public String handleClick(int c, int r) {
            if (isGameOver()) return "Game is over.";

            if (drawAvailable() && drawOfferFrom != null && drawOfferFrom != currentPlayerIndex) drawOfferFrom = null;

            Player p = currentPlayer();
            String desc;

            if ("PLACEMENT".equals(gamePhase)) {
                if (p.piecesPlaced >= maxPieces) return "No pieces left to place. Wait for movement phase.";
                boolean ok = board.placePiece(new int[]{c, r}, p.symbol);
                if (!ok) return "Invalid placement. Choose an empty square.";

                p.piecesPlaced++;
                desc = p.name + " PLACE " + Board.posToLabel(c, r);

                if (allPlayersPlaced()) gamePhase = "MOVEMENT";

                logTurn(desc);
                advanceTurn();
                return desc;
            } else {
                if (selectedFrom == null) {
                    if (board.grid[r][c] != p.symbol) return "Select one of your own pieces to move.";
                    selectedFrom = new int[]{c, r};
                    return "Selected " + Board.posToLabel(c, r) + " to move.";
                } else {
                    int fromC = selectedFrom[0], fromR = selectedFrom[1];

                    if (fromC == c && fromR == r) {
                        selectedFrom = null;
                        return "Selection cleared.";
                    }

                    boolean ok = board.movePiece(new int[]{fromC, fromR}, new int[]{c, r}, p.symbol);
                    if (!ok) return "Invalid move. Move 1 square to an adjacent empty space.";

                    desc = p.name + " MOVE " + Board.posToLabel(fromC, fromR) + " -> " + Board.posToLabel(c, r);
                    selectedFrom = null;

                    logTurn(desc);
                    advanceTurn();
                    return desc;
                }
            }
        }

        public String performAITurn() {
            if (isGameOver()) return "Game is over.";
            if (!(currentPlayer() instanceof AIPlayer)) return "Not AI turn.";

            if (drawAvailable() && drawOfferFrom != null && drawOfferFrom != currentPlayerIndex) drawOfferFrom = null;

            AIPlayer ai = (AIPlayer) currentPlayer();
            AIPlayer.Move m = ai.pickMove(this);
            if (m == null) return "AI has no valid moves.";

            String desc;

            if ("PLACEMENT".equals(this.gamePhase)) {
                boolean ok = board.placePiece(new int[]{m.toC, m.toR}, ai.symbol);
                if (!ok) return "AI attempted invalid placement (should not happen).";

                ai.piecesPlaced++;
                desc = ai.name + " (AI) PLACE " + Board.posToLabel(m.toC, m.toR);

                if (allPlayersPlaced()) this.gamePhase = "MOVEMENT";

                logTurn(desc);
                advanceTurn();
                return desc;
            } else {
                boolean ok = board.movePiece(new int[]{m.fromC, m.fromR}, new int[]{m.toC, m.toR}, ai.symbol);
                if (!ok) return "AI attempted invalid move (should not happen).";

                desc = ai.name + " (AI) MOVE " + Board.posToLabel(m.fromC, m.fromR) + " -> " + Board.posToLabel(m.toC, m.toR);

                logTurn(desc);
                advanceTurn();
                return desc;
            }
        }

        private void advanceTurn() {
            checkWinner();
            if (winner != null) { logResult("RESULT: " + winner.name + " wins by connecting " + connectTarget + "!"); return; }
            if (isDraw()) { logResult("RESULT: Draw."); return; }
            switchPlayer();
            turnCount++;
        }

        private void logTurn(String desc) {
            if (logger == null) return;
            logger.writeToFile("Turn " + turnCount + ": " + desc);
            logger.writeToFile(board.toConsoleString());
        }

        private void logResult(String resultLine) {
            if (logger == null) return;
            logger.writeToFile("------------------------------------------------------------");
            logger.writeToFile(resultLine);
            logger.writeToFile("Ended: " + LocalDateTime.now().withNano(0));
            logger.closeFile();
        }

        private char findWinnerSymbol() {
            int n = connectTarget;
            int[][] dirs = new int[][]{{1,0},{0,1},{1,1},{1,-1}};

            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                char s = board.grid[r][c];
                if (s == EMPTY) continue;

                for (int[] d : dirs) {
                    int dc = d[0], dr = d[1];
                    int count = 1;
                    int nc = c + dc, nr = r + dr;

                    while (nc >= 0 && nc < 8 && nr >= 0 && nr < 8 && board.grid[nr][nc] == s) {
                        count++;
                        if (count >= n) return s;
                        nc += dc; nr += dr;
                    }
                }
            }
            return 0;
        }
    }

    // =========================
    // GUI
    // =========================
    public static class ConnectFrame extends JFrame {

        private final CardLayout cards = new CardLayout();
        private final JPanel root = new JPanel(cards);

        private final MenuPanel menuPanel = new MenuPanel();
        private final GamePanel gamePanel = new GamePanel();

        ConnectFrame() {
            setTitle("Connect 5 on 8x8 - Levels 1 to 3-4");
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // RESIZABLE ON
            setResizable(true);

            root.setBackground(APP_BG);
            root.add(menuPanel, "MENU");
            root.add(gamePanel, "GAME");
            setContentPane(root);

            cards.show(root, "MENU");

            // Provide a sensible starting size, but still allow resizing
            setSize(new Dimension(980, 740));
            setLocationRelativeTo(null);
        }

        private static Character chooseBlackOrWhite(Component parent, String msgTitle, String msgText) {
            Object[] opts = {"Black", "White"};
            int choice = JOptionPane.showOptionDialog(
                    parent,
                    msgText,
                    msgTitle,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opts,
                    opts[0]
            );
            if (choice == JOptionPane.CLOSED_OPTION) return null;
            return (choice == 0) ? BLACK : WHITE;
        }

        // ---------- General Spin Dialog (2-5 participants) ----------
        private static class SpinDialogN extends JDialog {
            private final JLabel info = new JLabel("Spinning to choose who goes first...", SwingConstants.CENTER);
            private final Random rand = new Random();
            private Integer result = null;

            private final JPanel[] cards;
            private final String[] labels;
            private int current = 0;

            SpinDialogN(Frame owner, String title, String[] labels) {
                super(owner, title, true);
                this.labels = labels;

                setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                info.setBorder(new EmptyBorder(10, 12, 10, 12));
                info.setFont(info.getFont().deriveFont(Font.BOLD, 13f));

                int n = labels.length;
                cards = new JPanel[n];

                int rows = (n <= 2) ? 1 : 2;
                int cols = (n <= 2) ? 2 : 3;
                JPanel center = new JPanel(new GridLayout(rows, cols, 12, 12));
                center.setBorder(new EmptyBorder(12, 12, 12, 12));

                for (int i = 0; i < n; i++) {
                    JPanel card = new JPanel(new BorderLayout());
                    card.setBackground(Color.WHITE);
                    card.setBorder(new LineBorder(Color.GRAY, 2));
                    JLabel lab = new JLabel(labels[i], SwingConstants.CENTER);
                    lab.setBorder(new EmptyBorder(18, 18, 18, 18));
                    lab.setFont(lab.getFont().deriveFont(Font.BOLD, 14f));
                    card.add(lab, BorderLayout.CENTER);
                    cards[i] = card;
                    center.add(card);
                }

                int totalCells = rows * cols;
                for (int k = n; k < totalCells; k++) {
                    JPanel filler = new JPanel();
                    filler.setOpaque(false);
                    center.add(filler);
                }

                JButton spinBtn = new JButton("Spin");
                spinBtn.setFocusPainted(false);
                spinBtn.setPreferredSize(new Dimension(120, 34));

                JButton cancelBtn = new JButton("Cancel");
                cancelBtn.setFocusPainted(false);
                cancelBtn.setPreferredSize(new Dimension(120, 34));

                JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
                bottom.add(spinBtn);
                bottom.add(cancelBtn);

                spinBtn.addActionListener(e -> runSpin(spinBtn, n));
                cancelBtn.addActionListener(e -> { result = null; dispose(); });

                setLayout(new BorderLayout());
                add(info, BorderLayout.NORTH);
                add(center, BorderLayout.CENTER);
                add(bottom, BorderLayout.SOUTH);

                highlight(0);
                pack();
                setResizable(false);
                setLocationRelativeTo(owner);
            }

            Integer showAndGetResult() { setVisible(true); return result; }

            private void runSpin(JButton spinBtn, int n) {
                spinBtn.setEnabled(false);

                int finalIndex = rand.nextInt(n);
                int totalTicks = 20 + rand.nextInt(14);

                final int[] tick = {0};
                current = 0;
                highlight(current);

                Timer timer = new Timer(70, null);
                timer.addActionListener(ev -> {
                    tick[0]++;
                    current = (current + 1) % n;
                    highlight(current);
                    timer.setDelay(70 + tick[0] * 10);

                    if (tick[0] >= totalTicks - 1) {
                        current = finalIndex;
                        highlight(current);
                    }
                    if (tick[0] >= totalTicks) {
                        timer.stop();
                        result = finalIndex;
                        info.setText("Result: " + labels[result] + " goes first.");
                        Timer close = new Timer(650, e2 -> dispose());
                        close.setRepeats(false);
                        close.start();
                    }
                });
                timer.start();
            }

            private void highlight(int idx) {
                Color on = new Color(255, 245, 180);
                Color off = Color.WHITE;

                for (int i = 0; i < cards.length; i++) {
                    if (i == idx) {
                        cards[i].setBackground(on);
                        cards[i].setBorder(new LineBorder(new Color(160, 120, 0), 3));
                    } else {
                        cards[i].setBackground(off);
                        cards[i].setBorder(new LineBorder(Color.GRAY, 2));
                    }
                }
            }
        }

        // ---------- Menu ----------
        private class MenuPanel extends JPanel {

            private final JRadioButton level1Btn = new JRadioButton("Level 1: Player vs Player", true);
            private final JRadioButton level2Btn = new JRadioButton("Level 2: Player vs Computer (AI)");
            private final JRadioButton level34Btn = new JRadioButton("Level 3-4: Multi-player mode");

            private final CardLayout levelCards = new CardLayout();
            private final JPanel levelPanel = new JPanel(levelCards);

            private final JTextField l1p1 = new JTextField(16);
            private final JTextField l1p2 = new JTextField(16);

            private final JTextField l2human = new JTextField(16);
            private final JComboBox<String> l2diff = new JComboBox<>(new String[]{"BEGINNER", "MEDIUM", "SMART"});

            private final JRadioButton mpHumansOnlyBtn = new JRadioButton("Multiplayer (Humans only: 3–4 players)", true);
            private final JRadioButton mpVsAIBtn       = new JRadioButton("Players versus AI (3–4 players vs AI)");

            private final JComboBox<Integer> mpHumanCount = new JComboBox<>(new Integer[]{3, 4});
            private final JTextField mpN1 = new JTextField(16);
            private final JTextField mpN2 = new JTextField(16);
            private final JTextField mpN3 = new JTextField(16);
            private final JTextField mpN4 = new JTextField(16);

            private final JComboBox<Integer> vsHumanCount = new JComboBox<>(new Integer[]{3, 4});
            private final JTextField vsN1 = new JTextField(16);
            private final JTextField vsN2 = new JTextField(16);
            private final JTextField vsN3 = new JTextField(16);
            private final JTextField vsN4 = new JTextField(16);
            private final JComboBox<String> vsDiff = new JComboBox<>(new String[]{"BEGINNER", "MEDIUM", "SMART"});

            private final JButton startBtn = new JButton("Start Game");

            MenuPanel() {
                setBorder(new EmptyBorder(18, 18, 18, 18));
                setLayout(new GridBagLayout());
                setBackground(APP_BG);

                JLabel title = new JLabel("Connect 5 (8x8)");
                title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
                title.setForeground(TEXT_LIGHT);

                JPanel selector = new JPanel();
                selector.setLayout(new BoxLayout(selector, BoxLayout.Y_AXIS));
                selector.setBackground(CARD_BG);
                selector.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_BROWN, 2),
                        new EmptyBorder(10, 10, 10, 10)
                ));

                ButtonGroup lv = new ButtonGroup();
                lv.add(level1Btn);
                lv.add(level2Btn);
                lv.add(level34Btn);

                styleRadio(level1Btn);
                styleRadio(level2Btn);
                styleRadio(level34Btn);

                selector.add(level1Btn);
                selector.add(Box.createVerticalStrut(6));
                selector.add(level2Btn);
                selector.add(Box.createVerticalStrut(6));
                selector.add(level34Btn);

                levelPanel.setBackground(APP_BG);
                levelPanel.add(buildLevel1Panel(), "L1");
                levelPanel.add(buildLevel2Panel(), "L2");
                levelPanel.add(buildLevel34Panel(), "L34");
                levelCards.show(levelPanel, "L1");

                l1p1.setText("Player 1");
                l1p2.setText("Player 2");
                l2human.setText("Player");

                mpN1.setText("Player 1");
                mpN2.setText("Player 2");
                mpN3.setText("Player 3");
                mpN4.setText("Player 4");

                vsN1.setText("Player 1");
                vsN2.setText("Player 2");
                vsN3.setText("Player 3");
                vsN4.setText("Player 4");

                level1Btn.addActionListener(e -> levelCards.show(levelPanel, "L1"));
                level2Btn.addActionListener(e -> levelCards.show(levelPanel, "L2"));
                level34Btn.addActionListener(e -> levelCards.show(levelPanel, "L34"));

                mpHumansOnlyBtn.addActionListener(e -> refreshLevel34Controls());
                mpVsAIBtn.addActionListener(e -> refreshLevel34Controls());
                mpHumanCount.addActionListener(e -> refreshLevel34Controls());
                vsHumanCount.addActionListener(e -> refreshLevel34Controls());

                startBtn.setPreferredSize(new Dimension(180, 38));
                startBtn.setFocusPainted(false);
                startBtn.setBackground(ACCENT_GOLD);
                startBtn.setForeground(TEXT_DARK);
                startBtn.addActionListener(e -> onStart());

                refreshLevel34Controls();

                GridBagConstraints g = new GridBagConstraints();
                g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
                g.insets = new Insets(0, 0, 10, 0);
                add(title, g);

                g.gridy++;
                g.insets = new Insets(0, 0, 16, 0);

                g.gridy++;
                g.insets = new Insets(0, 0, 14, 0);
                add(selector, g);

                g.gridy++;
                g.insets = new Insets(0, 0, 14, 0);
                add(levelPanel, g);

                g.gridy++;
                g.insets = new Insets(0, 0, 0, 0);
                add(startBtn, g);
            }

            private void styleRadio(JRadioButton rb) {
                rb.setOpaque(false);
                rb.setForeground(TEXT_LIGHT);
                rb.setFocusPainted(false);
            }

            private JPanel buildCardPanel(String titleText) {
                JPanel p = new JPanel(new GridBagLayout());
                p.setBackground(CARD_BG);
                p.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_BROWN, 2),
                        new EmptyBorder(12, 12, 12, 12)
                ));
                JLabel t = new JLabel(titleText);
                t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));
                t.setForeground(TEXT_LIGHT);

                GridBagConstraints g = new GridBagConstraints();
                g.gridx = 0; g.gridy = 0;
                g.gridwidth = 2;
                g.anchor = GridBagConstraints.WEST;
                g.insets = new Insets(0, 0, 10, 0);
                p.add(t, g);

                return p;
            }

            private JPanel buildLevel1Panel() {
                JPanel p = buildCardPanel("Level 1: Player vs Player (2 humans)");
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(6, 6, 6, 6);
                g.anchor = GridBagConstraints.WEST;

                JLabel a = new JLabel("Player 1 Name:");
                JLabel b = new JLabel("Player 2 Name:");
                a.setForeground(TEXT_LIGHT);
                b.setForeground(TEXT_LIGHT);

                g.gridx = 0; g.gridy = 1; p.add(a, g);
                g.gridx = 1; p.add(l1p1, g);

                g.gridx = 0; g.gridy = 2; p.add(b, g);
                g.gridx = 1; p.add(l1p2, g);

                JLabel note = new JLabel("Spin chooses first. First chooses Black/White. Other gets opposite.");
                note.setForeground(TEXT_LIGHT);
                note.setFont(note.getFont().deriveFont(12f));
                g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
                p.add(note, g);
                return p;
            }

            private JPanel buildLevel2Panel() {
                JPanel p = buildCardPanel("Level 2: Player vs Computer (AI)");
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(6, 6, 6, 6);
                g.anchor = GridBagConstraints.WEST;

                JLabel a = new JLabel("Your Name:");
                JLabel b = new JLabel("AI Difficulty:");
                a.setForeground(TEXT_LIGHT);
                b.setForeground(TEXT_LIGHT);

                g.gridx = 0; g.gridy = 1; p.add(a, g);
                g.gridx = 1; p.add(l2human, g);

                g.gridx = 0; g.gridy = 2; p.add(b, g);
                g.gridx = 1; p.add(l2diff, g);

                JLabel note = new JLabel("Spin chooses first (Human or AI). Then first chooses Black/White.");
                note.setForeground(TEXT_LIGHT);
                note.setFont(note.getFont().deriveFont(12f));
                g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
                p.add(note, g);
                return p;
            }

            private JPanel buildLevel34Panel() {
                JPanel p = buildCardPanel("Level 3-4: Multi-player mode");
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(6, 6, 6, 6);
                g.anchor = GridBagConstraints.WEST;

                ButtonGroup mpGroup = new ButtonGroup();
                mpGroup.add(mpHumansOnlyBtn);
                mpGroup.add(mpVsAIBtn);
                styleRadio(mpHumansOnlyBtn);
                styleRadio(mpVsAIBtn);

                g.gridx = 0; g.gridy = 1; g.gridwidth = 2; p.add(mpHumansOnlyBtn, g);
                g.gridy++; p.add(mpVsAIBtn, g);

                JLabel hc = new JLabel("Human Players (3–4):");
                hc.setForeground(TEXT_LIGHT);
                g.gridy++; g.gridwidth = 1;
                g.gridx = 0; p.add(hc, g);
                g.gridx = 1; p.add(mpHumanCount, g);

                JLabel n1 = new JLabel("Player 1 Name:");
                JLabel n2 = new JLabel("Player 2 Name:");
                JLabel n3 = new JLabel("Player 3 Name:");
                JLabel n4 = new JLabel("Player 4 Name:");
                for (JLabel lab : new JLabel[]{n1,n2,n3,n4}) lab.setForeground(TEXT_LIGHT);

                g.gridy++; g.gridx = 0; p.add(n1, g); g.gridx = 1; p.add(mpN1, g);
                g.gridy++; g.gridx = 0; p.add(n2, g); g.gridx = 1; p.add(mpN2, g);
                g.gridy++; g.gridx = 0; p.add(n3, g); g.gridx = 1; p.add(mpN3, g);
                g.gridy++; g.gridx = 0; p.add(n4, g); g.gridx = 1; p.add(mpN4, g);

                JLabel sep = new JLabel(" ");
                sep.setForeground(TEXT_LIGHT);
                g.gridy++; g.gridx = 0; g.gridwidth = 2; p.add(sep, g);
                g.gridwidth = 1;

                JLabel vhc = new JLabel("Human Players vs AI (3–4):");
                vhc.setForeground(TEXT_LIGHT);
                g.gridy++; g.gridx = 0; p.add(vhc, g);
                g.gridx = 1; p.add(vsHumanCount, g);

                JLabel vn1 = new JLabel("Human 1 Name:");
                JLabel vn2 = new JLabel("Human 2 Name:");
                JLabel vn3 = new JLabel("Human 3 Name:");
                JLabel vn4 = new JLabel("Human 4 Name:");
                JLabel vd  = new JLabel("AI Difficulty:");
                for (JLabel lab : new JLabel[]{vn1,vn2,vn3,vn4,vd}) lab.setForeground(TEXT_LIGHT);

                g.gridy++; g.gridx = 0; p.add(vn1, g); g.gridx = 1; p.add(vsN1, g);
                g.gridy++; g.gridx = 0; p.add(vn2, g); g.gridx = 1; p.add(vsN2, g);
                g.gridy++; g.gridx = 0; p.add(vn3, g); g.gridx = 1; p.add(vsN3, g);
                g.gridy++; g.gridx = 0; p.add(vn4, g); g.gridx = 1; p.add(vsN4, g);

                g.gridy++; g.gridx = 0; p.add(vd, g);  g.gridx = 1; p.add(vsDiff, g);

                JLabel note = new JLabel("Spin chooses first. Humans use Black/White/Blue/Green. AI uses Green (3 humans) or Red (4 humans).");
                note.setForeground(TEXT_LIGHT);
                note.setFont(note.getFont().deriveFont(12f));
                g.gridy++; g.gridx = 0; g.gridwidth = 2; p.add(note, g);

                return p;
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

                JTextField[] all = new JTextField[]{mpN1, mpN2, mpN3, mpN4, vsN1, vsN2, vsN3, vsN4};
                for (JTextField f : all) f.setBackground(f.isEnabled() ? Color.WHITE : new Color(230, 230, 230));
            }

            private void onStart() {
                if (level1Btn.isSelected()) startLevel1();
                else if (level2Btn.isSelected()) startLevel2();
                else startLevel34();
            }

            private void startLevel1() {
                String p1 = l1p1.getText().trim();
                String p2 = l1p2.getText().trim();
                if (p1.isEmpty() || p2.isEmpty()) {
                    JOptionPane.showMessageDialog(ConnectFrame.this, "Please enter both player names.", "Missing Names", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String[] labels = new String[]{ p1 + " (Player 1)", p2 + " (Player 2)" };
                SpinDialogN spin = new SpinDialogN(ConnectFrame.this, "Who Goes First?", labels);
                Integer firstIdx = spin.showAndGetResult();
                if (firstIdx == null) return;

                String firstName = (firstIdx == 0) ? p1 : p2;

                Character firstColor = chooseBlackOrWhite(
                        ConnectFrame.this,
                        "Choose Color",
                        firstName + " goes first.\nChoose your piece color:"
                );
                if (firstColor == null) return;

                char secondColor = (firstColor == BLACK) ? WHITE : BLACK;

                Player pl1 = new Player(p1, (firstIdx == 0) ? firstColor : secondColor);
                Player pl2 = new Player(p2, (firstIdx == 1) ? firstColor : secondColor);

                gamePanel.startNewGame(new Player[]{pl1, pl2}, firstIdx, "Level 1");
                cards.show(root, "GAME");
            }

            private void startLevel2() {
                String humanName = l2human.getText().trim();
                if (humanName.isEmpty()) humanName = "Player";

                String diff = (String) l2diff.getSelectedItem();
                if (diff == null) diff = "BEGINNER";

                Player human = new Player(humanName, BLACK);
                AIPlayer ai  = new AIPlayer("Computer", WHITE, diff);
                Player[] players = new Player[]{human, ai};

                String[] labels = new String[]{ human.name + " (Human)", ai.name + " (AI)" };
                SpinDialogN spin = new SpinDialogN(ConnectFrame.this, "Who Goes First? (Level 2)", labels);
                Integer firstIdx = spin.showAndGetResult();
                if (firstIdx == null) return;

                if (firstIdx == 0) {
                    Character humanColor = chooseBlackOrWhite(
                            ConnectFrame.this,
                            "Choose Color",
                            human.name + " goes first.\nChoose your piece color:"
                    );
                    if (humanColor == null) return;

                    human.symbol = humanColor;
                    ai.symbol = (humanColor == BLACK) ? WHITE : BLACK;
                } else {
                    Character aiColor = chooseBlackOrWhite(
                            ConnectFrame.this,
                            "Choose AI Color",
                            "Computer goes first.\nChoose the Computer's piece color:"
                    );
                    if (aiColor == null) return;

                    ai.symbol = aiColor;
                    human.symbol = (aiColor == BLACK) ? WHITE : BLACK;
                }

                gamePanel.startNewGame(players, firstIdx, "Level 2");
                cards.show(root, "GAME");
            }

            private void startLevel34() {
                boolean vsAI = mpVsAIBtn.isSelected();

                if (!vsAI) {
                    int humans = (Integer) mpHumanCount.getSelectedItem();

                    Player[] players = new Player[humans];
                    JTextField[] fields = new JTextField[]{mpN1, mpN2, mpN3, mpN4};

                    for (int i = 0; i < humans; i++) {
                        String nm = fields[i].getText().trim();
                        if (nm.isEmpty()) nm = "Player " + (i + 1);
                        players[i] = new Player(nm, COLOR_ORDER[i]); // B,W,U,G
                    }

                    String[] labels = new String[humans];
                    for (int i = 0; i < humans; i++) labels[i] = players[i].name + " - " + colorName(players[i].symbol);

                    SpinDialogN spin = new SpinDialogN(ConnectFrame.this, "Who Goes First? (Level 3-4)", labels);
                    Integer firstIdx = spin.showAndGetResult();
                    if (firstIdx == null) return;

                    gamePanel.startNewGame(players, firstIdx, "Level 3-4 (Humans only)");
                    cards.show(root, "GAME");

                } else {
                    int humans = (Integer) vsHumanCount.getSelectedItem();
                    String diff = (String) vsDiff.getSelectedItem();
                    if (diff == null) diff = "BEGINNER";

                    Player[] players = new Player[humans + 1];
                    JTextField[] fields = new JTextField[]{vsN1, vsN2, vsN3, vsN4};

                    for (int i = 0; i < humans; i++) {
                        String nm = fields[i].getText().trim();
                        if (nm.isEmpty()) nm = "Player " + (i + 1);
                        players[i] = new Player(nm, COLOR_ORDER[i]); // B,W,U,(G)
                    }

                    char aiColor = (humans == 3) ? GREEN : RED; // keep AI distinct
                    players[humans] = new AIPlayer("Computer", aiColor, diff);

                    String[] labels = new String[humans + 1];
                    for (int i = 0; i < humans; i++) labels[i] = players[i].name + " - " + colorName(players[i].symbol);
                    labels[humans] = players[humans].name + " (AI) - " + colorName(players[humans].symbol);

                    SpinDialogN spin = new SpinDialogN(ConnectFrame.this, "Who Goes First? (Level 3-4 vs AI)", labels);
                    Integer firstIdx = spin.showAndGetResult();
                    if (firstIdx == null) return;

                    gamePanel.startNewGame(players, firstIdx, "Level 3-4 (" + humans + " Humans vs AI)");
                    cards.show(root, "GAME");
                }
            }
        }

        // ---------- Game Panel (resizable) ----------
        private class GamePanel extends JPanel {

            private final JLabel titleLabel = new JLabel();
            private final JLabel turnLabel = new JLabel();
            private final JLabel phaseLabel = new JLabel();
            private final JLabel statusLabel = new JLabel(" ");

            private final JPanel playersBox = new JPanel();
            private final JLabel[] playerLabels = new JLabel[]{ new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };

            private final JPanel grid = new JPanel(new GridLayout(9, 9, 3, 3));
            private final JButton[][] squares = new JButton[8][8];

            private Game game;

            private final JButton drawBtn = new JButton("Offer / Accept Draw");
            private final JButton backBtn = new JButton("Back to Menu");
            private final JButton quitBtn = new JButton("Quit");

            GamePanel() {
                setLayout(new BorderLayout(12, 12));
                setBorder(new EmptyBorder(12, 12, 12, 12));
                setBackground(APP_BG);

                add(buildHeader(), BorderLayout.NORTH);
                add(buildBoard(), BorderLayout.CENTER);
                add(buildStatusBar(), BorderLayout.SOUTH);

                // When window is resized, re-render so icons scale to new button sizes
                addComponentListener(new java.awt.event.ComponentAdapter() {
                    @Override public void componentResized(java.awt.event.ComponentEvent e) { render(); }
                });
            }

            private JPanel buildHeader() {
                JPanel header = new JPanel(new BorderLayout(12, 12));
                header.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_BROWN, 2),
                        new EmptyBorder(10, 10, 10, 10)
                ));
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

                left.add(titleLabel);
                left.add(Box.createVerticalStrut(6));
                left.add(turnLabel);
                left.add(Box.createVerticalStrut(4));
                left.add(phaseLabel);
                left.add(Box.createVerticalStrut(10));
                left.add(playersBox);

                JPanel right = new JPanel();
                right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
                right.setBackground(PANEL_BG);

                Dimension btnSize = new Dimension(170, 34);
                for (JButton b : new JButton[]{drawBtn, backBtn, quitBtn}) {
                    b.setMaximumSize(btnSize);
                    b.setPreferredSize(btnSize);
                    b.setAlignmentX(Component.CENTER_ALIGNMENT);
                    b.setFocusPainted(false);
                    b.setBackground(ACCENT_GOLD);
                    b.setForeground(TEXT_DARK);
                }

                drawBtn.addActionListener(e -> onDraw());
                backBtn.addActionListener(e -> {
                    if (game != null && !game.isGameOver()) {
                        int ok = JOptionPane.showConfirmDialog(
                                ConnectFrame.this,
                                "Return to menu? Current game will be abandoned.",
                                "Confirm",
                                JOptionPane.YES_NO_OPTION
                        );
                        if (ok != JOptionPane.YES_OPTION) return;
                    }
                    cards.show(root, "MENU");
                });
                quitBtn.addActionListener(e -> dispose());

                right.add(drawBtn);
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
                        new LineBorder(BORDER_BROWN, 2),
                        new EmptyBorder(10, 10, 10, 10)
                ));
                grid.setBackground(PANEL_BG);

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

            void startNewGame(Player[] players, int firstIndex, String modeName) {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String logName = "logs/connect_log_" + ts + ".txt";
                Logger logger = new Logger(logName);

                logger.writeToFile("Connect on 8x8 Log");
                logger.writeToFile("Started: " + LocalDateTime.now().withNano(0));
                logger.writeToFile("Mode: " + modeName);
                logger.writeToFile("Connect target: 5");
                logger.writeToFile("Pieces per participant: 8");
                logger.writeToFile("Participants: " + players.length);
                for (int i = 0; i < players.length; i++) {
                    Player p = players[i];
                    logger.writeToFile("P" + (i + 1) + ": " + p.name + " (" + colorName(p.symbol) + ")" + (p instanceof AIPlayer ? " AI" : ""));
                }
                logger.writeToFile("------------------------------------------------------------");

                game = new Game(5, 8, logger, players, firstIndex);

                titleLabel.setText(modeName);
                statusLabel.setText("Game started. Log: " + logName + ". Click squares to place pieces.");
                drawBtn.setEnabled(game.drawAvailable());

                enableBoard();
                render();
                triggerAIIfNeeded();
            }

            private void onDraw() {
                if (game == null) return;
                if (!game.drawAvailable()) { statusLabel.setText("Draw is only available in Level 1 (2 human players)."); return; }
                if (game.currentPlayer() instanceof AIPlayer) { statusLabel.setText("It's the computer's turn."); return; }

                String msg = game.offerOrAcceptDraw();
                statusLabel.setText(msg);
                render();
                checkGameOverPopup();
                triggerAIIfNeeded();
            }

            private void onSquareClicked(int col, int displayRow) {
                if (game == null) return;
                if (game.currentPlayer() instanceof AIPlayer) { statusLabel.setText("Computer is thinking..."); return; }

                int r = 7 - displayRow;
                int c = col;

                String msg = game.handleClick(c, r);
                statusLabel.setText(msg);

                render();
                checkGameOverPopup();
                triggerAIIfNeeded();
            }

            private void triggerAIIfNeeded() {
                if (game == null || game.isGameOver()) return;
                if (!(game.currentPlayer() instanceof AIPlayer)) return;

                disableBoard();

                Timer t = new Timer(350, null);
                t.addActionListener(e -> {
                    ((Timer) e.getSource()).stop();

                    if (game == null || game.isGameOver()) { disableBoard(); return; }
                    if (!(game.currentPlayer() instanceof AIPlayer)) { enableBoard(); render(); return; }

                    String msg = game.performAITurn();
                    statusLabel.setText(msg);

                    render();
                    checkGameOverPopup();

                    if (!game.isGameOver()) enableBoard();
                    else disableBoard();
                });
                t.setRepeats(false);
                t.start();
            }

            private void checkGameOverPopup() {
                if (game == null) return;
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
                for (int dr = 0; dr < 8; dr++) for (int c = 0; c < 8; c++) squares[dr][c].setEnabled(false);
            }

            private void enableBoard() {
                for (int dr = 0; dr < 8; dr++) for (int c = 0; c < 8; c++) squares[dr][c].setEnabled(true);
            }

            private int computeCellPx() {
                // Base cell size off the grid panel size (9x9 includes labels),
                // leaving some safety for borders/gaps.
                int w = grid.getWidth();
                int h = grid.getHeight();
                if (w <= 0 || h <= 0) return 24;

                int cellW = w / 9;
                int cellH = h / 9;
                return Math.max(18, Math.min(cellW, cellH));
            }

            private void render() {
                if (game == null) return;

                Player cur = game.currentPlayer();
                String curName = cur.name + ((cur instanceof AIPlayer) ? " (AI)" : "");
                turnLabel.setText("Turn: " + game.turnCount + " | Current: " + curName);
                phaseLabel.setText("Phase: " + game.gamePhase + " | Connect " + game.connectTarget);

                for (int i = 0; i < playerLabels.length; i++) {
                    if (i < game.players.length) {
                        Player p = game.players[i];
                        String extra = (p instanceof AIPlayer) ? (" | AI: " + ((AIPlayer) p).difficulty) : "";
                        playerLabels[i].setText("P" + (i + 1) + ": " + p.name + " (" + colorName(p.symbol) + ") | placed: " + p.piecesPlaced + extra);
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
                        btn.setIcon(v == EMPTY ? null : iconFor(v, iconPx));

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
