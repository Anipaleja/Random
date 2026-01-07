package FinalProject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * ICS4U Connect on 8x8 - Level 1 (Player vs Player)
 * Single-file Java program with classes/attributes aligned to the provided UML:
 *   Player, AIPlayer, Game, Board, Logger
 *
 * Compile + run:
 *   javac Main.java
 *   java Main
 */
public class Connect5GUI{

    // =========================
    // Player (UML-aligned)
    // =========================
    static class Player {
        // UML attributes
        String name;
        char symbol;
        int piecesPlaced;

        Player(String name, char symbol) {
            this.name = name;
            this.symbol = symbol;
            this.piecesPlaced = 0;
        }

        // UML method
        public void makeMove(Board board, String phase) {
            // Level 1 uses GUI clicks, so this method is intentionally minimal.
            // The Game/GUI orchestrate moves.
        }
    }

    // =========================
    // AIPlayer (UML-aligned)
    // (Not used in Level 1, included so your UML structure is satisfied.)
    // =========================
    static class AIPlayer extends Player {
        // UML attributes
        String difficulty;

        AIPlayer(String name, char symbol, String difficulty) {
            super(name, symbol);
            this.difficulty = difficulty;
        }

        @Override
        public void makeMove(Board board, String phase) {
            // Not used for Level 1.
        }

        // UML method
        public void chooseMove(Board board) {
            // Not used for Level 1.
        }
    }

    // =========================
    // Board (UML-aligned)
    // =========================
    static class Board {
        // UML attributes
        char[][] grid = new char[8][8]; // [row][col], row 0 = bottom (1), row 7 = top (8)

        // UML methods
        public void initializeBoard() {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    grid[r][c] = '.';
                }
            }
        }

        public boolean isValidPosition(int[] pos) {
            if (pos == null || pos.length != 2) return false;
            int c = pos[0], r = pos[1];
            return c >= 0 && c < 8 && r >= 0 && r < 8;
        }

        public boolean isEmpty(int[] pos) {
            if (!isValidPosition(pos)) return false;
            return grid[pos[1]][pos[0]] == '.';
        }

        public boolean placePiece(int[] pos, char symbol) {
            if (!isValidPosition(pos)) return false;
            if (!isEmpty(pos)) return false;
            grid[pos[1]][pos[0]] = symbol;
            return true;
        }

        public boolean movePiece(int[] from, int[] to, char symbol) {
            if (!isValidPosition(from) || !isValidPosition(to)) return false;
            if (grid[from[1]][from[0]] != symbol) return false;
            if (!isEmpty(to)) return false;

            int dc = Math.abs(to[0] - from[0]);
            int dr = Math.abs(to[1] - from[1]);
            if (dc > 1 || dr > 1 || (dc == 0 && dr == 0)) return false; // adjacent only

            grid[from[1]][from[0]] = '.';
            grid[to[1]][to[0]] = symbol;
            return true;
        }

        public void displayBoard() {
            // Console display (optional). GUI is the primary display.
            System.out.println(toConsoleString());
        }

        // Helper (not in UML) for logging/console
        public String toConsoleString() {
            StringBuilder sb = new StringBuilder();
            sb.append("    A B C D E F G H\n");
            for (int displayRow = 7; displayRow >= 0; displayRow--) {
                sb.append(String.format("%2d  ", displayRow + 1));
                for (int c = 0; c < 8; c++) {
                    sb.append(grid[displayRow][c]).append(' ');
                }
                sb.append('\n');
            }
            return sb.toString();
        }

        // Helper: label like "A1" => int[]{col,row}
        public static int[] labelToPos(String label) {
            if (label == null) throw new IllegalArgumentException("Invalid position.");
            String s = label.trim().toUpperCase();
            if (s.length() < 2) throw new IllegalArgumentException("Invalid position.");
            char colCh = s.charAt(0);
            int col = colCh - 'A';
            int row;
            try {
                row = Integer.parseInt(s.substring(1)) - 1;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid position.");
            }
            if (col < 0 || col > 7 || row < 0 || row > 7) throw new IllegalArgumentException("Invalid position.");
            return new int[]{col, row};
        }

        public static String posToLabel(int c, int r) {
            return "" + (char)('A' + c) + (r + 1);
        }
    }

    // =========================
    // Logger (UML-aligned)
    // =========================
    static class Logger {
        // UML attribute
        String fileName;

        private PrintWriter out; // helper

        Logger(String fileName) {
            this.fileName = fileName;
        }

        // UML methods
        public void writeToFile(String text) {
            try {
                if (out == null) {
                    out = new PrintWriter(new FileWriter(fileName, true));
                }
                out.println(text);
                out.flush();
            } catch (IOException ignored) {
            }
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
    // Game (UML-aligned)
    // =========================
    static class Game {
        // UML attributes
        Board board;
        Player[] players;
        int currentPlayerIndex;
        int turnCount;
        int connectTarget;
        int maxPieces;
        String gamePhase; // "PLACEMENT" or "MOVEMENT"
        Player winner;
        Logger logger;

        // Helpers (not in UML)
        private int[] selectedFrom = null;
        private boolean draw = false;
        private Integer drawOfferFrom = null;

        Game(int connectTarget, int maxPieces, Logger logger) {
            this.connectTarget = connectTarget;
            this.maxPieces = maxPieces;
            this.logger = logger;

            this.board = new Board();
            this.board.initializeBoard();

            this.players = new Player[2];
            this.currentPlayerIndex = 0;
            this.turnCount = 1;
            this.gamePhase = "PLACEMENT";
            this.winner = null;
        }

        // UML methods
        public void startGame() {
            // GUI triggers this flow; this method exists to match UML.
        }

        public void playTurn() {
            // GUI triggers this flow; this method exists to match UML.
        }

        public void checkWinner() {
            char w = findWinnerSymbol();
            if (w == 0) {
                winner = null;
            } else {
                winner = (players[0].symbol == w) ? players[0] : players[1];
            }
        }

        public void switchPlayer() {
            currentPlayerIndex = 1 - currentPlayerIndex;
        }

        public void endGame() {
            // GUI will show message; this method exists to match UML.
            if (logger != null) logger.closeFile();
        }

        // Gameplay API for GUI
        public Player currentPlayer() { return players[currentPlayerIndex]; }
        public Player otherPlayer() { return players[1 - currentPlayerIndex]; }

        public boolean isGameOver() {
            checkWinner();
            if (winner != null) return true;
            if (draw) return true;
            // Draw by turn count (approx. 32 turns each => 64 total turns).
            return turnCount >= 65; // after 64 turns have completed, next would be 65
        }

        public boolean isDraw() {
            if (draw) return true;
            return (winner == null && turnCount >= 65);
        }

        public String offerOrAcceptDraw() {
            if (winner != null) return "Game is already over.";
            if (draw) return "Game is already a draw.";

            int offeringIndex = currentPlayerIndex;
            if (drawOfferFrom == null) {
                drawOfferFrom = offeringIndex;
                return currentPlayer().name + " offered a draw. Other player must accept on their turn.";
            } else {
                if (drawOfferFrom == offeringIndex) {
                    return "Draw offer is pending from " + currentPlayer().name + ". Other player must accept.";
                } else {
                    draw = true;
                    return "Draw agreed by both players.";
                }
            }
        }

        public String handleClick(int c, int r) {
            if (isGameOver()) return "Game is over.";

            Player p = currentPlayer();
            String desc;

            if ("PLACEMENT".equals(gamePhase)) {
                if (p.piecesPlaced >= maxPieces) {
                    return "No pieces left to place. Wait for movement phase.";
                }
                boolean ok = board.placePiece(new int[]{c, r}, p.symbol);
                if (!ok) return "Invalid placement. Choose an empty square.";

                p.piecesPlaced++;
                desc = p.name + " (P" + (currentPlayerIndex + 1) + ") PLACE " + Board.posToLabel(c, r);

                // If both placed all pieces, switch phase
                if (players[0].piecesPlaced >= maxPieces && players[1].piecesPlaced >= maxPieces) {
                    gamePhase = "MOVEMENT";
                }

                // End turn
                logTurn(desc);
                advanceTurn();
                return desc;
            } else {
                // MOVEMENT
                if (selectedFrom == null) {
                    if (board.grid[r][c] != p.symbol) {
                        return "Select one of your own pieces to move.";
                    }
                    selectedFrom = new int[]{c, r};
                    return "Selected " + Board.posToLabel(c, r) + " to move.";
                } else {
                    int fromC = selectedFrom[0];
                    int fromR = selectedFrom[1];

                    if (fromC == c && fromR == r) {
                        selectedFrom = null;
                        return "Selection cleared.";
                    }

                    boolean ok = board.movePiece(new int[]{fromC, fromR}, new int[]{c, r}, p.symbol);
                    if (!ok) return "Invalid move. Move 1 square to an adjacent empty space.";

                    desc = p.name + " (P" + (currentPlayerIndex + 1) + ") MOVE " +
                            Board.posToLabel(fromC, fromR) + " -> " + Board.posToLabel(c, r);
                    selectedFrom = null;

                    logTurn(desc);
                    advanceTurn();
                    return desc;
                }
            }
        }

        public int[] getSelectedFrom() { return selectedFrom; }

        private void advanceTurn() {
            // check win after a completed action
            checkWinner();
            if (winner != null) {
                logResult("RESULT: " + winner.name + " wins by connecting " + connectTarget + "!");
                return;
            }
            if (isDraw()) {
                logResult("RESULT: Draw.");
                return;
            }

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

        // Winner detection for connectTarget
        private char findWinnerSymbol() {
            int n = connectTarget;
            int[][] dirs = new int[][]{{1,0},{0,1},{1,1},{1,-1}};

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    char s = board.grid[r][c];
                    if (s == '.') continue;

                    for (int[] d : dirs) {
                        int dc = d[0], dr = d[1];
                        int count = 1;
                        int nc = c + dc, nr = r + dr;

                        while (nc >= 0 && nc < 8 && nr >= 0 && nr < 8 && board.grid[nr][nc] == s) {
                            count++;
                            if (count >= n) return s;
                            nc += dc;
                            nr += dr;
                        }
                    }
                }
            }
            return 0;
        }
    }

    // =========================
    // Swing GUI (Level 1)
    // =========================
    public static class ConnectFrame extends JFrame {
        private final JLabel turnLabel = new JLabel();
        private final JLabel phaseLabel = new JLabel();
        private final JLabel p1Label = new JLabel();
        private final JLabel p2Label = new JLabel();
        private final JLabel statusLabel = new JLabel(" ");

        private final JButton[][] squares = new JButton[8][8]; // [displayRow][col], displayRow 0 = top

        private Game game;

        ConnectFrame() {
            setTitle("Connect on 8x8 - Level 1 (One File)");
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setResizable(false);

            buildLayout();
            newGameDialog();

            pack();
            setLocationRelativeTo(null);
        }

        private void buildLayout() {
            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(new EmptyBorder(10, 10, 10, 10));
            setContentPane(root);

            // Left info
            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 14f));
            info.add(turnLabel);
            info.add(Box.createVerticalStrut(4));
            info.add(phaseLabel);
            info.add(Box.createVerticalStrut(8));
            info.add(p1Label);
            info.add(Box.createVerticalStrut(4));
            info.add(p2Label);

            // Right controls
            JPanel controls = new JPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            JButton newGameBtn = new JButton("New Game");
            JButton drawBtn = new JButton("Offer / Accept Draw");
            JButton quitBtn = new JButton("Quit");

            newGameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            drawBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            quitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

            controls.add(newGameBtn);
            controls.add(Box.createVerticalStrut(8));
            controls.add(drawBtn);
            controls.add(Box.createVerticalStrut(8));
            controls.add(quitBtn);

            newGameBtn.addActionListener(e -> newGameDialog());
            drawBtn.addActionListener(e -> onDrawButton());
            quitBtn.addActionListener(e -> dispose());

            // Top panel
            JPanel top = new JPanel(new BorderLayout(10, 10));
            top.add(info, BorderLayout.CENTER);
            top.add(controls, BorderLayout.EAST);

            root.add(top, BorderLayout.NORTH);

            // Board panel with coordinates
            JPanel boardPanel = new JPanel(new BorderLayout());
            JPanel gridPanel = new JPanel(new GridLayout(9, 9, 2, 2));

            // Top-left empty corner
            gridPanel.add(new JLabel(""));

            // Column labels A..H
            for (int c = 0; c < 8; c++) {
                JLabel l = new JLabel(String.valueOf((char)('A' + c)), SwingConstants.CENTER);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                gridPanel.add(l);
            }

            // 8 rows: display row 0 = row 8
            for (int displayRow = 0; displayRow < 8; displayRow++) {
                int rowLabel = 8 - displayRow;
                JLabel rl = new JLabel(String.valueOf(rowLabel), SwingConstants.CENTER);
                rl.setFont(rl.getFont().deriveFont(Font.BOLD));
                gridPanel.add(rl);

                for (int c = 0; c < 8; c++) {
                    JButton btn = new JButton("");
                    btn.setPreferredSize(new Dimension(54, 54));
                    btn.setFont(btn.getFont().deriveFont(Font.BOLD, 18f));
                    final int col = c;
                    final int dispR = displayRow;
                    btn.addActionListener(e -> onSquareClicked(col, dispR));
                    squares[displayRow][c] = btn;
                    gridPanel.add(btn);
                }
            }

            boardPanel.add(gridPanel, BorderLayout.CENTER);

            root.add(boardPanel, BorderLayout.CENTER);

            // Status line
            statusLabel.setBorder(new EmptyBorder(8, 2, 0, 2));
            root.add(statusLabel, BorderLayout.SOUTH);
        }

        private void newGameDialog() {
            String p1Name = JOptionPane.showInputDialog(this, "Enter Player 1 name:");
            if (p1Name == null || p1Name.trim().isEmpty()) return;

            String p2Name = JOptionPane.showInputDialog(this, "Enter Player 2 name:");
            if (p2Name == null || p2Name.trim().isEmpty()) return;

            // Choose who goes first
            int first = JOptionPane.showConfirmDialog(
                    this,
                    "Who goes first?\nYes = Player 1\nNo = Player 2\nCancel = Random",
                    "First Player",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );

            int firstIndex;
            if (first == JOptionPane.YES_OPTION) firstIndex = 0;
            else if (first == JOptionPane.NO_OPTION) firstIndex = 1;
            else firstIndex = new Random().nextInt(2);

            // First player chooses symbol (as "color" proxy)
            int symChoice = JOptionPane.showConfirmDialog(
                    this,
                    "First player chooses symbol:\nYes = X (Black)\nNo = O (White)",
                    "Choose Symbol",
                    JOptionPane.YES_NO_OPTION
            );

            char firstSym = (symChoice == JOptionPane.YES_OPTION) ? 'X' : 'O';
            char secondSym = (firstSym == 'X') ? 'O' : 'X';

            Player p1, p2;
            if (firstIndex == 0) {
                p1 = new Player(p1Name.trim(), firstSym);
                p2 = new Player(p2Name.trim(), secondSym);
            } else {
                p1 = new Player(p1Name.trim(), secondSym);
                p2 = new Player(p2Name.trim(), firstSym);
            }

            // Logger file
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String logName = "connect_log_" + ts + ".txt";
            Logger logger = new Logger(logName);

            // Header
            logger.writeToFile("Connect on 8x8 - Level 1 Log");
            logger.writeToFile("Started: " + LocalDateTime.now().withNano(0));
            logger.writeToFile("Connect target: 5");
            logger.writeToFile("Pieces per player (placement phase): 8");
            logger.writeToFile("------------------------------------------------------------");

            game = new Game(5, 8, logger);
            game.players[0] = p1;
            game.players[1] = p2;
            game.currentPlayerIndex = firstIndex;
            game.turnCount = 1;
            game.gamePhase = "PLACEMENT";
            game.winner = null;

            statusLabel.setText("New game started. Log: " + logName + ". Click squares to place pieces.");
            render();
        }

        private void onDrawButton() {
            if (game == null) return;
            String msg = game.offerOrAcceptDraw();
            statusLabel.setText(msg);
            if (game.logger != null) {
                game.logger.writeToFile("Turn " + game.turnCount + ": " + msg);
            }
            render();
            checkGameOverPopup();
        }

        private void onSquareClicked(int col, int displayRow) {
            if (game == null) return;

            // displayRow 0..7 (top..bottom) => internal row 7..0
            int r = 7 - displayRow;
            int c = col;

            String msg = game.handleClick(c, r);
            statusLabel.setText(msg);

            render();
            checkGameOverPopup();
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
            for (int dr = 0; dr < 8; dr++) {
                for (int c = 0; c < 8; c++) {
                    squares[dr][c].setEnabled(false);
                }
            }
        }

        private void render() {
            if (game == null) return;

            Player p1 = game.players[0];
            Player p2 = game.players[1];

            turnLabel.setText("Turn: " + game.turnCount + " | Current: " + game.currentPlayer().name);
            phaseLabel.setText("Phase: " + game.gamePhase + " | Connect " + game.connectTarget);

            p1Label.setText("Player 1: " + p1.name + " (" + p1.symbol + ") | piecesPlaced: " + p1.piecesPlaced);
            p2Label.setText("Player 2: " + p2.name + " (" + p2.symbol + ") | piecesPlaced: " + p2.piecesPlaced);

            int[] sel = game.getSelectedFrom();

            // Draw board: internal row 7..0 => display row 0..7
            for (int displayRow = 0; displayRow < 8; displayRow++) {
                int r = 7 - displayRow;
                for (int c = 0; c < 8; c++) {
                    char v = game.board.grid[r][c];
                    JButton btn = squares[displayRow][c];

                    btn.setEnabled(!game.isGameOver());

                    if (v == '.') {
                        btn.setText("");
                        btn.setBackground(UIManager.getColor("Button.background"));
                    } else {
                        btn.setText(String.valueOf(v));
                        btn.setBackground(v == 'X' ? Color.LIGHT_GRAY : Color.WHITE);
                    }

                    // Highlight selection (movement phase)
                    if (sel != null && sel[0] == c && sel[1] == r) {
                        btn.setBackground(new Color(255, 255, 170));
                    }
                }
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
