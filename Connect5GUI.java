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
import java.util.Random;

/**
 * ICS4U Connect on 8x8 - Level 1 (Player vs Player)
 * Single-file Java program with classes/attributes aligned to UML:
 *   Player, AIPlayer, Game, Board, Logger
 *
 * File name should be: Connect5GUI.java
 * Compile + run:
 *   javac FinalProject/Connect5GUI.java
 *   java FinalProject.Connect5GUI
 */
public class Connect5GUI {

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

        public void makeMove(Board board, String phase) {
            // Level 1 uses GUI clicks.
        }
    }

    // =========================
    // AIPlayer (UML-aligned)
    // =========================
    static class AIPlayer extends Player {
        String difficulty;

        AIPlayer(String name, char symbol, String difficulty) {
            super(name, symbol);
            this.difficulty = difficulty;
        }

        @Override
        public void makeMove(Board board, String phase) { }

        public void chooseMove(Board board) { }
    }

    // =========================
    // Board (UML-aligned)
    // =========================
    static class Board {
        char[][] grid = new char[8][8]; // [row][col], row 0 bottom

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
            if (dc > 1 || dr > 1 || (dc == 0 && dr == 0)) return false;

            grid[from[1]][from[0]] = '.';
            grid[to[1]][to[0]] = symbol;
            return true;
        }

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

        public static String posToLabel(int c, int r) {
            return "" + (char)('A' + c) + (r + 1);
        }
    }

    // =========================
    // Logger (UML-aligned)
    // =========================
    static class Logger {
        String fileName;
        private PrintWriter out;

        Logger(String fileName) {
            this.fileName = fileName;
        }

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
    // Game (UML-aligned)
    // =========================
    static class Game {
        Board board;
        Player[] players;
        int currentPlayerIndex;
        int turnCount;
        int connectTarget;
        int maxPieces;
        String gamePhase; // "PLACEMENT" or "MOVEMENT"
        Player winner;
        Logger logger;

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

        public void startGame() { }
        public void playTurn() { }

        public void checkWinner() {
            char w = findWinnerSymbol();
            if (w == 0) winner = null;
            else winner = (players[0].symbol == w) ? players[0] : players[1];
        }

        public void switchPlayer() {
            currentPlayerIndex = 1 - currentPlayerIndex;
        }

        public void endGame() {
            if (logger != null) logger.closeFile();
        }

        public Player currentPlayer() { return players[currentPlayerIndex]; }
        public Player otherPlayer() { return players[1 - currentPlayerIndex]; }

        public boolean isGameOver() {
            checkWinner();
            if (winner != null) return true;
            if (draw) return true;
            return turnCount >= 65;
        }

        public boolean isDraw() {
            if (draw) return true;
            return (winner == null && turnCount >= 65);
        }

        public String offerOrAcceptDraw() {
            if (winner != null) return "Game is already over.";
            if (draw) return "Game is already a draw.";

            if (drawOfferFrom == null) {
                drawOfferFrom = currentPlayerIndex;

                switchPlayer();       // other player immediately gets the chance to accept/decline
                selectedFrom = null;  // clear selection

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

        public String handleClick(int c, int r) {
            if (isGameOver()) return "Game is over.";

            // If a draw was offered and the current player makes a move, that declines it
            if (drawOfferFrom != null && drawOfferFrom == currentPlayerIndex) {
                // Offerer got switched away; this case is rare. Keep pending.
            } else if (drawOfferFrom != null && drawOfferFrom != currentPlayerIndex) {
                // Current player is the one who can accept; any move declines.
                drawOfferFrom = null;
            }

            Player p = currentPlayer();
            String desc;

            if ("PLACEMENT".equals(gamePhase)) {
                if (p.piecesPlaced >= maxPieces) return "No pieces left to place. Wait for movement phase.";
                boolean ok = board.placePiece(new int[]{c, r}, p.symbol);
                if (!ok) return "Invalid placement. Choose an empty square.";

                p.piecesPlaced++;
                desc = p.name + " (P" + (currentPlayerIndex + 1) + ") PLACE " + Board.posToLabel(c, r);

                if (players[0].piecesPlaced >= maxPieces && players[1].piecesPlaced >= maxPieces) {
                    gamePhase = "MOVEMENT";
                }

                logTurn(desc);
                advanceTurn();
                return desc;
            } else {
                if (selectedFrom == null) {
                    if (board.grid[r][c] != p.symbol) return "Select one of your own pieces to move.";
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
    // Cleaner GUI with Menu -> Game (CardLayout)
    // =========================
    public static class ConnectFrame extends JFrame {

        private final CardLayout cards = new CardLayout();
        private final JPanel root = new JPanel(cards);

        private final MenuPanel menuPanel = new MenuPanel();
        private final GamePanel gamePanel = new GamePanel();

        ConnectFrame() {
            setTitle("Connect 5 on 8x8 - Level 1");
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setResizable(false);

            root.add(menuPanel, "MENU");
            root.add(gamePanel, "GAME");
            setContentPane(root);

            cards.show(root, "MENU");
            pack();
            setLocationRelativeTo(null);
        }

        // ---------- Menu Panel ----------
        private class MenuPanel extends JPanel {
            private final JTextField p1Field = new JTextField(16);
            private final JTextField p2Field = new JTextField(16);
            private final JButton startBtn = new JButton("Start Game");

            MenuPanel() {
                setBorder(new EmptyBorder(18, 18, 18, 18));
                setLayout(new GridBagLayout());

                JLabel title = new JLabel("Connect 5 (8x8) - Level 1");
                title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

                JLabel subtitle = new JLabel("Enter player names, then press Start.");
                subtitle.setFont(subtitle.getFont().deriveFont(13f));

                JPanel form = new JPanel(new GridBagLayout());
                form.setBorder(new EmptyBorder(10, 0, 10, 0));

                GridBagConstraints f = new GridBagConstraints();
                f.insets = new Insets(6, 6, 6, 6);
                f.anchor = GridBagConstraints.WEST;

                f.gridx = 0; f.gridy = 0;
                form.add(new JLabel("Player 1 Name:"), f);
                f.gridx = 1;
                form.add(p1Field, f);

                f.gridx = 0; f.gridy = 1;
                form.add(new JLabel("Player 2 Name:"), f);
                f.gridx = 1;
                form.add(p2Field, f);

                startBtn.setPreferredSize(new Dimension(160, 36));
                startBtn.setFocusPainted(false);

                startBtn.addActionListener(e -> onStartFromMenu());

                GridBagConstraints g = new GridBagConstraints();
                g.gridx = 0; g.gridy = 0;
                g.gridwidth = 2;
                g.insets = new Insets(0, 0, 8, 0);
                add(title, g);

                g.gridy++;
                g.insets = new Insets(0, 0, 16, 0);
                add(subtitle, g);

                g.gridy++;
                g.insets = new Insets(0, 0, 14, 0);
                add(form, g);

                g.gridy++;
                g.insets = new Insets(0, 0, 0, 0);
                add(startBtn, g);
            }

            private void onStartFromMenu() {
                String p1 = p1Field.getText().trim();
                String p2 = p2Field.getText().trim();

                if (p1.isEmpty() || p2.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            ConnectFrame.this,
                            "Please enter both player names.",
                            "Missing Names",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                // Spin dialog chooses who goes first (0 = Player 1, 1 = Player 2)
                SpinDialog spin = new SpinDialog(ConnectFrame.this, p1, p2);
                Integer firstIndex = spin.showAndGetResult();
                if (firstIndex == null) return; // cancelled/closed

                // First player chooses symbol
                String firstName = (firstIndex == 0) ? p1 : p2;
                int symChoice = JOptionPane.showConfirmDialog(
                        ConnectFrame.this,
                        firstName + " goes first.\nChoose symbol:\nYes = X (Black)\nNo = O (White)",
                        "Choose Symbol",
                        JOptionPane.YES_NO_OPTION
                );
                char firstSym = (symChoice == JOptionPane.YES_OPTION) ? 'X' : 'O';
                char secondSym = (firstSym == 'X') ? 'O' : 'X';

                // Create players in fixed order: players[0]=Player1, players[1]=Player2
                Player pl1 = new Player(p1, (firstIndex == 0) ? firstSym : secondSym);
                Player pl2 = new Player(p2, (firstIndex == 1) ? firstSym : secondSym);

                gamePanel.startNewGame(pl1, pl2, firstIndex);
                cards.show(root, "GAME");
            }
        }

        // ---------- Spin Dialog (2-player “wheel”) ----------
        private static class SpinDialog extends JDialog {
            private final JLabel info = new JLabel("Spinning to choose who goes first...", SwingConstants.CENTER);

            private final JPanel p1Card = new JPanel(new BorderLayout());
            private final JPanel p2Card = new JPanel(new BorderLayout());

            private final Random rand = new Random();
            private Integer result = null;

            SpinDialog(Frame owner, String p1Name, String p2Name) {
                super(owner, "Who Goes First?", true);
                setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                info.setBorder(new EmptyBorder(10, 12, 10, 12));
                info.setFont(info.getFont().deriveFont(Font.BOLD, 13f));

                p1Card.setBorder(new LineBorder(Color.GRAY, 2));
                p2Card.setBorder(new LineBorder(Color.GRAY, 2));
                p1Card.setBackground(Color.WHITE);
                p2Card.setBackground(Color.WHITE);

                JLabel p1 = new JLabel("Player 1: " + p1Name, SwingConstants.CENTER);
                JLabel p2 = new JLabel("Player 2: " + p2Name, SwingConstants.CENTER);
                p1.setBorder(new EmptyBorder(18, 18, 18, 18));
                p2.setBorder(new EmptyBorder(18, 18, 18, 18));
                p1.setFont(p1.getFont().deriveFont(Font.BOLD, 14f));
                p2.setFont(p2.getFont().deriveFont(Font.BOLD, 14f));

                p1Card.add(p1, BorderLayout.CENTER);
                p2Card.add(p2, BorderLayout.CENTER);

                JPanel center = new JPanel(new GridLayout(1, 2, 12, 12));
                center.setBorder(new EmptyBorder(12, 12, 12, 12));
                center.add(p1Card);
                center.add(p2Card);

                JButton spinBtn = new JButton("Spin");
                spinBtn.setFocusPainted(false);
                spinBtn.setPreferredSize(new Dimension(120, 34));

                JButton cancelBtn = new JButton("Cancel");
                cancelBtn.setFocusPainted(false);
                cancelBtn.setPreferredSize(new Dimension(120, 34));

                JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
                bottom.add(spinBtn);
                bottom.add(cancelBtn);

                spinBtn.addActionListener(e -> runSpin(spinBtn));
                cancelBtn.addActionListener(e -> {
                    result = null;
                    dispose();
                });

                setLayout(new BorderLayout());
                add(info, BorderLayout.NORTH);
                add(center, BorderLayout.CENTER);
                add(bottom, BorderLayout.SOUTH);

                pack();
                setResizable(false);
                setLocationRelativeTo(owner);
            }

            Integer showAndGetResult() {
                setVisible(true);
                return result;
            }

            private void runSpin(JButton spinBtn) {
                spinBtn.setEnabled(false);

                // Decide final result up front
                int finalIndex = rand.nextInt(2);
                int totalTicks = 18 + rand.nextInt(12); // 18..29 ticks
                int[] tick = {0};
                int[] current = {0};
                int startDelay = 70; // ms

                highlight(current[0]);

                Timer timer = new Timer(startDelay, null);
                timer.addActionListener(ev -> {
                    tick[0]++;

                    // Toggle highlight each tick
                    current[0] = 1 - current[0];
                    highlight(current[0]);

                    // Slow down as it approaches the end
                    int newDelay = 70 + (tick[0] * 12);
                    timer.setDelay(newDelay);

                    // Force it to land on finalIndex in the last tick
                    if (tick[0] >= totalTicks - 1) {
                        current[0] = finalIndex;
                        highlight(current[0]);
                    }

                    if (tick[0] >= totalTicks) {
                        timer.stop();
                        result = finalIndex;
                        info.setText("Result: " + (result == 0 ? "Player 1 goes first." : "Player 2 goes first."));
                        // short pause then close
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

                if (idx == 0) {
                    p1Card.setBackground(on);
                    p2Card.setBackground(off);
                    p1Card.setBorder(new LineBorder(new Color(160, 120, 0), 3));
                    p2Card.setBorder(new LineBorder(Color.GRAY, 2));
                } else {
                    p2Card.setBackground(on);
                    p1Card.setBackground(off);
                    p2Card.setBorder(new LineBorder(new Color(160, 120, 0), 3));
                    p1Card.setBorder(new LineBorder(Color.GRAY, 2));
                }
            }
        }

        // ---------- Game Panel ----------
        private class GamePanel extends JPanel {

            private final JLabel turnLabel = new JLabel();
            private final JLabel phaseLabel = new JLabel();
            private final JLabel p1Label = new JLabel();
            private final JLabel p2Label = new JLabel();
            private final JLabel statusLabel = new JLabel(" ");

            private final JButton[][] squares = new JButton[8][8]; // [displayRow][col]
            private Game game;

            GamePanel() {
                setLayout(new BorderLayout(12, 12));
                setBorder(new EmptyBorder(12, 12, 12, 12));

                add(buildHeader(), BorderLayout.NORTH);
                add(buildBoard(), BorderLayout.CENTER);
                add(buildStatusBar(), BorderLayout.SOUTH);
            }

            private JPanel buildHeader() {
                JPanel header = new JPanel(new BorderLayout(12, 12));
                header.setBorder(new EmptyBorder(0, 0, 8, 0));

                JPanel left = new JPanel();
                left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
                turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 15f));
                phaseLabel.setFont(phaseLabel.getFont().deriveFont(13f));

                left.add(turnLabel);
                left.add(Box.createVerticalStrut(4));
                left.add(phaseLabel);
                left.add(Box.createVerticalStrut(10));
                left.add(p1Label);
                left.add(Box.createVerticalStrut(3));
                left.add(p2Label);

                JPanel right = new JPanel();
                right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

                JButton drawBtn = new JButton("Offer / Accept Draw");
                JButton newGameBtn = new JButton("Back to Menu");
                JButton quitBtn = new JButton("Quit");

                Dimension btnSize = new Dimension(170, 34);
                for (JButton b : new JButton[]{drawBtn, newGameBtn, quitBtn}) {
                    b.setMaximumSize(btnSize);
                    b.setPreferredSize(btnSize);
                    b.setAlignmentX(Component.CENTER_ALIGNMENT);
                    b.setFocusPainted(false);
                }

                drawBtn.addActionListener(e -> onDrawButton());
                newGameBtn.addActionListener(e -> {
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
                right.add(newGameBtn);
                right.add(Box.createVerticalStrut(8));
                right.add(quitBtn);

                header.add(left, BorderLayout.CENTER);
                header.add(right, BorderLayout.EAST);
                return header;
            }

            private JPanel buildBoard() {
                JPanel boardPanel = new JPanel(new BorderLayout());
                JPanel grid = new JPanel(new GridLayout(9, 9, 3, 3));
                grid.setBorder(new EmptyBorder(6, 6, 6, 6));
                grid.setBackground(new Color(240, 240, 240));

                // Corner
                grid.add(new JLabel(""));

                // Column labels
                for (int c = 0; c < 8; c++) {
                    JLabel l = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
                    l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
                    grid.add(l);
                }

                // Rows
                for (int displayRow = 0; displayRow < 8; displayRow++) {
                    int rowLabel = 8 - displayRow;
                    JLabel rl = new JLabel(String.valueOf(rowLabel), SwingConstants.CENTER);
                    rl.setFont(rl.getFont().deriveFont(Font.BOLD, 12f));
                    grid.add(rl);

                    for (int c = 0; c < 8; c++) {
                        JButton btn = new JButton("");
                        btn.setPreferredSize(new Dimension(54, 54));
                        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 18f));
                        btn.setFocusPainted(false);
                        btn.setMargin(new Insets(0, 0, 0, 0));

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
                statusLabel.setBorder(new EmptyBorder(6, 2, 0, 2));
                statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
                status.add(statusLabel, BorderLayout.CENTER);
                return status;
            }

            void startNewGame(Player p1, Player p2, int firstIndex) {
                // Logger file
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String logName = "connect_log_" + ts + ".txt";
                Logger logger = new Logger(logName);

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

                statusLabel.setText("Game started. Log: " + logName + ". Click squares to place pieces.");
                enableBoard();
                render();
            }

            private void onDrawButton() {
                if (game == null) return;
                String msg = game.offerOrAcceptDraw();
                statusLabel.setText(msg);
                if (game.logger != null) game.logger.writeToFile("Turn " + game.turnCount + ": " + msg);
                render();
                checkGameOverPopup();
            }

            private void onSquareClicked(int col, int displayRow) {
                if (game == null) return;

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
                for (int dr = 0; dr < 8; dr++) for (int c = 0; c < 8; c++) squares[dr][c].setEnabled(false);
            }

            private void enableBoard() {
                for (int dr = 0; dr < 8; dr++) for (int c = 0; c < 8; c++) squares[dr][c].setEnabled(true);
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

                for (int displayRow = 0; displayRow < 8; displayRow++) {
                    int r = 7 - displayRow;
                    for (int c = 0; c < 8; c++) {
                        char v = game.board.grid[r][c];
                        JButton btn = squares[displayRow][c];

                        btn.setEnabled(!game.isGameOver());

                        if (v == '.') {
                            btn.setText("");
                            btn.setBackground(UIManager.getColor("Button.background"));
                            btn.setBorder(UIManager.getBorder("Button.border"));
                        } else {
                            btn.setText(String.valueOf(v));
                            btn.setBackground(v == 'X' ? new Color(220, 220, 220) : Color.WHITE);
                            btn.setBorder(new LineBorder(new Color(160, 160, 160), 1));
                        }

                        if (sel != null && sel[0] == c && sel[1] == r) {
                            btn.setBackground(new Color(255, 245, 180));
                            btn.setBorder(new LineBorder(new Color(160, 120, 0), 2));
                        }
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
