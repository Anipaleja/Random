package FinalProject;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * #####################################
 * # NAME: (Your Name)
 * # COURSE: ICS4U
 * # FILE: Connect5GUI.java
 * # PROJECT: Connect Five on 8x8 - Level 1 (P v P) with GUI
 * # DATE: (Submission Date)
 * #####################################
 *
 * Level 1 Features:
 * - Connect 5 on 8x8
 * - A1 bottom-left, H8 top-right coordinate system
 * - Choose who goes first; first player chooses black/white
 * - Each player has 8 pieces to place
 * - After both placed 8: movement phase (move to any adjacent empty square)
 * - Win detection: 5 in a row (H/V/diagonal)
 * - Draw: 32 turns per player OR mutual agreement
 * - Logs placements/moves/results to a text file
 */
public class Connect5GUI extends JFrame {

    // Rules/constants
    private static final int SIZE = 8;
    private static final int CONNECT_N = 5;
    private static final int PIECES_PER_PLAYER = 8;
    private static final int MAX_TURNS_PER_PLAYER = 32;

    // Board state (internal coordinates: r=0 is bottom, r=7 is top)
    // 0 empty, 1 P1, 2 P2
    private final int[][] board = new int[SIZE][SIZE];

    // Player info
    private String p1Name = "Player 1";
    private String p2Name = "Player 2";
    private String p1ColorName = "Black";
    private String p2ColorName = "White";

    // Turn/phase state
    private int currentToken = 1; // 1 or 2
    private int placedP1 = 0, placedP2 = 0;
    private int turnsP1 = 0, turnsP2 = 0;

    private enum Phase { PLACEMENT, MOVEMENT }
    private Phase phase = Phase.PLACEMENT;

    // Movement selection
    private Integer selectedR = null;
    private Integer selectedC = null;

    // Draw agreement
    private Integer drawRequestedBy = null; // 1 or 2

    // Logging
    private PrintWriter logOut;
    private String logFileName;

    // UI
    private JLabel statusLabel;
    private JTextArea quickLog;
    private JButton requestDrawBtn, acceptDrawBtn, declineDrawBtn, newGameBtn, helpBtn;

    private CellButton[][] cellButtons = new CellButton[SIZE][SIZE];

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Connect5GUI app = new Connect5GUI();
            app.setVisible(true);
            app.startNewGame();
        });
    }

    public Connect5GUI() {
        super("Connect 5 on 8x8 - Level 1 (Player vs Player)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        buildUI();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                closeLogger();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        // Board panel with coordinates:
        // We'll build a 9x9 grid:
        // row 0: blank + letters A..H
        // rows 1..8: number (8..1) + 8 cells
        JPanel boardPanel = new JPanel(new GridLayout(SIZE + 1, SIZE + 1, 2, 2));
        boardPanel.setBackground(Color.DARK_GRAY);

        // Top-left blank
        boardPanel.add(coordLabel(""));

        // Top letters A..H
        for (int c = 0; c < SIZE; c++) {
            char letter = (char) ('A' + c);
            boardPanel.add(coordLabel(String.valueOf(letter)));
        }

        // Rows (top to bottom): 8..1
        for (int displayRow = 0; displayRow < SIZE; displayRow++) {
            int rowNumber = SIZE - displayRow; // 8..1
            boardPanel.add(coordLabel(String.valueOf(rowNumber)));

            for (int c = 0; c < SIZE; c++) {
                int r = internalRowFromDisplay(displayRow); // internal r (0 bottom)
                CellButton btn = new CellButton(r, c);
                btn.setPreferredSize(new Dimension(60, 60));
                btn.setFocusPainted(false);
                btn.setBorder(new LineBorder(Color.BLACK, 1));
                btn.addActionListener(e -> onCellClicked(btn.r, btn.c));
                cellButtons[r][c] = btn;
                boardPanel.add(btn);
            }
        }

        // Right panel controls
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        newGameBtn = new JButton("New Game");
        newGameBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        newGameBtn.addActionListener(e -> startNewGame());

        requestDrawBtn = new JButton("Request Draw");
        requestDrawBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        requestDrawBtn.addActionListener(e -> requestDraw());

        acceptDrawBtn = new JButton("Accept Draw");
        acceptDrawBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        acceptDrawBtn.addActionListener(e -> acceptDraw());

        declineDrawBtn = new JButton("Decline Draw");
        declineDrawBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        declineDrawBtn.addActionListener(e -> declineDraw());

        helpBtn = new JButton("Help / Rules");
        helpBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpBtn.addActionListener(e -> showHelp());

        quickLog = new JTextArea(14, 28);
        quickLog.setEditable(false);
        quickLog.setLineWrap(true);
        quickLog.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(quickLog);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        right.add(statusLabel);
        right.add(Box.createVerticalStrut(10));
        right.add(newGameBtn);
        right.add(Box.createVerticalStrut(6));
        right.add(requestDrawBtn);
        right.add(Box.createVerticalStrut(6));
        right.add(acceptDrawBtn);
        right.add(Box.createVerticalStrut(6));
        right.add(declineDrawBtn);
        right.add(Box.createVerticalStrut(12));
        right.add(helpBtn);
        right.add(Box.createVerticalStrut(12));
        right.add(new JLabel("Quick Log:"));
        right.add(Box.createVerticalStrut(4));
        right.add(scroll);

        root.add(boardPanel, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);

        updateDrawButtons();
    }

    private JLabel coordLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setOpaque(true);
        lbl.setBackground(new Color(240, 240, 240));
        lbl.setBorder(new LineBorder(Color.GRAY, 1));
        return lbl;
    }

    // displayRow: 0..7 top->bottom, internal r: 7..0
    private int internalRowFromDisplay(int displayRow) {
        return (SIZE - 1) - displayRow;
    }

    private void startNewGame() {
        // Close previous log (if any)
        closeLogger();

        // Reset board and state
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) board[r][c] = 0;
        }
        selectedR = null;
        selectedC = null;
        drawRequestedBy = null;

        placedP1 = placedP2 = 0;
        turnsP1 = turnsP2 = 0;
        phase = Phase.PLACEMENT;

        // Setup dialog (names, first player, first color)
        if (!runSetupDialog()) {
            // User canceled: keep window open, but no game
            quickLog.setText("Setup canceled.\n");
            repaintBoard();
            updateStatus();
            return;
        }

        // Create new log file
        openLogger();

        // Determine who starts
        // (setup dialog sets currentToken accordingly)
        logLine("=== Connect 5 (8x8) Level 1 Log ===");
        logLine("Start: " + nowStamp());
        logLine("Player 1: " + p1Name + " (" + p1ColorName + ")");
        logLine("Player 2: " + p2Name + " (" + p2ColorName + ")");
        logLine("First turn: " + currentPlayerName());
        logLine("");

        quickLog.setText("");
        appendQuick("Log file: " + logFileName);
        appendQuick("Started: " + nowStamp());
        appendQuick(p1Name + " = " + p1ColorName + ", " + p2Name + " = " + p2ColorName);
        appendQuick("First: " + currentPlayerName());

        repaintBoard();
        updateStatus();
        updateDrawButtons();
    }

    private boolean runSetupDialog() {
        JTextField p1Field = new JTextField(p1Name);
        JTextField p2Field = new JTextField(p2Name);

        String[] firstOptions = {"Player 1", "Player 2"};
        JComboBox<String> firstBox = new JComboBox<>(firstOptions);

        String[] colorOptions = {"Black", "White"};
        JComboBox<String> colorBox = new JComboBox<>(colorOptions);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Player 1 name:"));
        panel.add(p1Field);
        panel.add(new JLabel("Player 2 name:"));
        panel.add(p2Field);
        panel.add(new JLabel("Who goes first?"));
        panel.add(firstBox);
        panel.add(new JLabel("First player chooses:"));
        panel.add(colorBox);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Game Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return false;

        p1Name = p1Field.getText().trim().isEmpty() ? "Player 1" : p1Field.getText().trim();
        p2Name = p2Field.getText().trim().isEmpty() ? "Player 2" : p2Field.getText().trim();

        int firstIdx = firstBox.getSelectedIndex(); // 0 => P1, 1 => P2
        String firstColor = (String) colorBox.getSelectedItem();

        // Assign colors so the first player gets chosen color
        if (firstIdx == 0) {
            p1ColorName = firstColor;
            p2ColorName = firstColor.equals("Black") ? "White" : "Black";
            currentToken = 1;
        } else {
            p2ColorName = firstColor;
            p1ColorName = firstColor.equals("Black") ? "White" : "Black";
            currentToken = 2;
        }

        return true;
    }

    private void onCellClicked(int r, int c) {
        if (isGameOver()) return;

        if (phase == Phase.PLACEMENT) {
            handlePlacement(r, c);
        } else {
            handleMovement(r, c);
        }

        repaintBoard();
        updateStatus();
        updateDrawButtons();

        // If game ended on this action, show dialog
        if (isGameOver()) {
            int winner = findWinner();
            if (winner != 0) {
                JOptionPane.showMessageDialog(this,
                        "Winner: " + playerName(winner) + " (" + playerColor(winner) + ")",
                        "Game Over", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Draw.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void handlePlacement(int r, int c) {
        if (board[r][c] != 0) {
            warn("You cannot place on an occupied square.");
            return;
        }

        if (currentToken == 1 && placedP1 >= PIECES_PER_PLAYER) {
            warn(p1Name + " has already placed all pieces.");
            return;
        }
        if (currentToken == 2 && placedP2 >= PIECES_PER_PLAYER) {
            warn(p2Name + " has already placed all pieces.");
            return;
        }

        // Place
        board[r][c] = currentToken;
        if (currentToken == 1) { placedP1++; turnsP1++; }
        else { placedP2++; turnsP2++; }

        logLine("[" + nowStamp() + "] " + currentPlayerName() + " PLACE " + squareLabel(r, c));
        appendQuick(currentPlayerName() + " placed at " + squareLabel(r, c));

        // Decline opponent draw request implicitly by playing
        if (drawRequestedBy != null && drawRequestedBy != currentToken) {
            declineDrawInternal("Draw request declined by play.");
        }

        // Check win/draw, then advance
        if (postActionCheckEnd()) return;

        // Transition to movement when both have 8 placed
        if (placedP1 >= PIECES_PER_PLAYER && placedP2 >= PIECES_PER_PLAYER) {
            phase = Phase.MOVEMENT;
            logLine("PHASE CHANGE: movement mode begins (all pieces placed).");
            appendQuick("Movement phase begins.");
        }

        advanceTurn();
    }

    private void handleMovement(int r, int c) {
        // selecting a piece
        if (selectedR == null) {
            if (board[r][c] != currentToken) {
                warn("Select one of your own pieces.");
                return;
            }
            selectedR = r;
            selectedC = c;
            return;
        }

        // if click another own piece, reselect
        if (board[r][c] == currentToken) {
            selectedR = r;
            selectedC = c;
            return;
        }

        // attempt move to empty adjacent
        if (board[r][c] != 0) {
            warn("Destination must be empty.");
            return;
        }

        int rf = selectedR, cf = selectedC;
        if (!isAdjacent(rf, cf, r, c)) {
            warn("Move must be to an adjacent square (8 directions).");
            return;
        }

        // Move
        board[rf][cf] = 0;
        board[r][c] = currentToken;
        if (currentToken == 1) turnsP1++;
        else turnsP2++;

        logLine("[" + nowStamp() + "] " + currentPlayerName() + " MOVE " + squareLabel(rf, cf) + " -> " + squareLabel(r, c));
        appendQuick(currentPlayerName() + " moved " + squareLabel(rf, cf) + " -> " + squareLabel(r, c));

        selectedR = null;
        selectedC = null;

        // Decline opponent draw request implicitly by playing
        if (drawRequestedBy != null && drawRequestedBy != currentToken) {
            declineDrawInternal("Draw request declined by play.");
        }

        if (postActionCheckEnd()) return;

        advanceTurn();
    }

    private boolean isAdjacent(int r1, int c1, int r2, int c2) {
        int dr = Math.abs(r2 - r1);
        int dc = Math.abs(c2 - c1);
        return (dr == 0 && dc == 0) ? false : (dr <= 1 && dc <= 1);
    }

    private void advanceTurn() {
        currentToken = (currentToken == 1) ? 2 : 1;
    }

    private boolean postActionCheckEnd() {
        int winner = findWinner();
        if (winner != 0) {
            logLine("RESULT: WINNER = " + playerName(winner) + " (" + playerColor(winner) + ")");
            return true;
        }

        // Turn-limit draw: 32 turns each
        if (turnsP1 >= MAX_TURNS_PER_PLAYER && turnsP2 >= MAX_TURNS_PER_PLAYER) {
            logLine("RESULT: DRAW (turn limit reached)");
            return true;
        }

        return false;
    }

    private boolean isGameOver() {
        if (findWinner() != 0) return true;
        return (turnsP1 >= MAX_TURNS_PER_PLAYER && turnsP2 >= MAX_TURNS_PER_PLAYER);
    }

    private int findWinner() {
        // directions: up, right, up-right, down-right (in internal coords: up is +1 row)
        int[][] dirs = {{1,0},{0,1},{1,1},{-1,1}};

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int token = board[r][c];
                if (token == 0) continue;

                for (int[] d : dirs) {
                    if (hasNFrom(r, c, d[0], d[1], token, CONNECT_N)) return token;
                }
            }
        }
        return 0;
    }

    private boolean hasNFrom(int r, int c, int dr, int dc, int token, int n) {
        for (int k = 1; k < n; k++) {
            int rr = r + dr * k;
            int cc = c + dc * k;
            if (!inBounds(rr, cc) || board[rr][cc] != token) return false;
        }
        return true;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    // Coordinate label (A1 bottom-left)
    private String squareLabel(int r, int c) {
        char col = (char) ('A' + c);
        int row = r + 1; // internal r=0 is row 1
        return "" + col + row;
    }

    private void repaintBoard() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cellButtons[r][c].setToken(board[r][c]);
                boolean selected = (selectedR != null && selectedR == r && selectedC == c);
                cellButtons[r][c].setSelectedHighlight(selected);
            }
        }
        repaint();
    }

    private void updateStatus() {
        String phaseText = (phase == Phase.PLACEMENT) ? "PLACEMENT" : "MOVEMENT";
        String turnText = "Turn: " + currentPlayerName() + " (" + playerColor(currentToken) + ")";
        String piecesText = "Pieces placed: " + p1Name + "=" + placedP1 + "/" + PIECES_PER_PLAYER
                + ", " + p2Name + "=" + placedP2 + "/" + PIECES_PER_PLAYER;
        String turnsText = "Turns taken: " + p1Name + "=" + turnsP1 + "/" + MAX_TURNS_PER_PLAYER
                + ", " + p2Name + "=" + turnsP2 + "/" + MAX_TURNS_PER_PLAYER;

        String instruction;
        if (isGameOver()) {
            int winner = findWinner();
            if (winner != 0) instruction = "Game Over: WINNER = " + playerName(winner) + " (" + playerColor(winner) + ")";
            else instruction = "Game Over: DRAW";
        } else {
            if (phase == Phase.PLACEMENT) {
                instruction = "Click an empty square to place a piece.";
            } else {
                if (selectedR == null) instruction = "Click one of your pieces to select it.";
                else instruction = "Selected " + squareLabel(selectedR, selectedC) + ". Click an adjacent empty square to move.";
            }
        }

        String drawText = "";
        if (drawRequestedBy != null && !isGameOver()) {
            drawText = "Draw requested by: " + playerName(drawRequestedBy);
        }

        statusLabel.setText("<html>"
                + "Phase: <b>" + phaseText + "</b><br>"
                + turnText + "<br>"
                + piecesText + "<br>"
                + turnsText + "<br>"
                + instruction + (drawText.isEmpty() ? "" : "<br>" + drawText)
                + "</html>");
    }

    // Draw controls
    private void requestDraw() {
        if (isGameOver()) return;

        drawRequestedBy = currentToken;
        logLine("[" + nowStamp() + "] " + currentPlayerName() + " requested a draw.");
        appendQuick(currentPlayerName() + " requested a draw.");
        updateDrawButtons();
        updateStatus();
    }

    private void acceptDraw() {
        if (isGameOver()) return;
        if (drawRequestedBy == null) return;
        if (drawRequestedBy == currentToken) return; // requester cannot accept

        logLine("[" + nowStamp() + "] Draw accepted by " + currentPlayerName() + ".");
        logLine("RESULT: DRAW (mutual agreement)");
        appendQuick("Draw accepted. Game over.");
        repaintBoard();
        updateStatus();
        updateDrawButtons();
    }

    private void declineDraw() {
        if (drawRequestedBy == null || isGameOver()) return;
        declineDrawInternal("Draw request declined.");
        repaintBoard();
        updateStatus();
        updateDrawButtons();
    }

    private void declineDrawInternal(String reason) {
        logLine("[" + nowStamp() + "] " + reason);
        drawRequestedBy = null;
    }

    private void updateDrawButtons() {
        boolean active = !isGameOver();
        requestDrawBtn.setEnabled(active);

        if (!active || drawRequestedBy == null) {
            acceptDrawBtn.setEnabled(false);
            declineDrawBtn.setEnabled(false);
            return;
        }

        // only non-requesting player can accept/decline
        boolean canRespond = (drawRequestedBy != currentToken);
        acceptDrawBtn.setEnabled(canRespond);
        declineDrawBtn.setEnabled(canRespond);
    }

    private void showHelp() {
        String msg =
                "Connect 5 on 8x8 (Level 1)\n\n" +
                "Placement phase:\n" +
                "- Each player places 8 pieces by clicking an empty square.\n\n" +
                "Movement phase:\n" +
                "- Click one of your pieces, then click an adjacent empty square (8 directions).\n\n" +
                "Win:\n" +
                "- Connect 5 in a row (horizontal, vertical, diagonal).\n\n" +
                "Draw:\n" +
                "- If no winner after 32 turns per player (64 total actions), or both agree.\n\n" +
                "Coordinates:\n" +
                "- A1 is bottom-left, H8 is top-right.";
        JOptionPane.showMessageDialog(this, msg, "Help / Rules", JOptionPane.INFORMATION_MESSAGE);
    }

    // Utilities
    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Invalid", JOptionPane.WARNING_MESSAGE);
    }

    private String currentPlayerName() {
        return (currentToken == 1) ? p1Name : p2Name;
    }

    private String playerName(int token) {
        return (token == 1) ? p1Name : p2Name;
    }

    private String playerColor(int token) {
        return (token == 1) ? p1ColorName : p2ColorName;
    }

    // Logging
    private void openLogger() {
        try {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            logFileName = "connect5_log_" + stamp + ".txt";
            Path path = Path.of(logFileName);
            logOut = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
        } catch (Exception e) {
            logOut = null;
            logFileName = "(logging unavailable)";
            appendQuick("WARNING: Could not create log file: " + e.getMessage());
        }
    }

    private void closeLogger() {
        try {
            if (logOut != null) {
                logLine("");
                logLine("End: " + nowStamp());
                logOut.flush();
                logOut.close();
            }
        } catch (Exception ignored) {}
        logOut = null;
    }

    private void logLine(String line) {
        if (logOut != null) {
            logOut.println(line);
            logOut.flush();
        }
    }

    private void appendQuick(String line) {
        quickLog.append(line + "\n");
        quickLog.setCaretPosition(quickLog.getDocument().getLength());
    }

    private String nowStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Custom button that draws a piece (black/white) based on token
    private class CellButton extends JButton {
        private final int r, c;
        private int token = 0;
        private boolean selected = false;

        CellButton(int r, int c) {
            this.r = r;
            this.c = c;
            setBackground(((r + c) % 2 == 0) ? new Color(240, 217, 181) : new Color(181, 136, 99));
        }

        void setToken(int token) {
            this.token = token;
            repaint();
        }

        void setSelectedHighlight(boolean selected) {
            this.selected = selected;
            setBorder(new LineBorder(selected ? Color.RED : Color.BLACK, selected ? 3 : 1));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (token == 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                String colorName = playerColor(token);
                Color fill = colorName.equals("Black") ? Color.BLACK : Color.WHITE;

                int pad = 10;
                int x = pad;
                int y = pad;
                int w = getWidth() - 2 * pad;
                int h = getHeight() - 2 * pad;

                g2.setColor(fill);
                g2.fillOval(x, y, w, h);

                g2.setColor(Color.GRAY);
                g2.drawOval(x, y, w, h);
            } finally {
                g2.dispose();
            }
        }
    }
}
