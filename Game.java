import java.util.List;

/**
 * main game class
 * Author: Anish
 */
public class Game {
    public enum GameMode { PVP, PVA, MULTIPLAYER }
    public enum GamePhase { PLACEMENT, MOVEMENT, ENDED }
    
    private Board board;
    private Player[] players;
    private int currentPlayerIndex;
    private int turnCount;
    private int maxTurns;
    private GamePhase gamePhase;
    private GameMode gameMode;
    private Player winner;
    private Logger logger;
    private static final int CONNECT_TARGET = 5;
    private static final int MAX_PIECES = 8;
    private static final int BOARD_SIZE = 8;
    
    /**
     * Constructor for Player vs Player game
     */
    public Game(String player1Name, String player2Name) {
        initializeGame(2);
        players[0] = new Player(player1Name, 'B');
        players[1] = new Player(player2Name, 'W');
        gameMode = GameMode.PVP;
    }
    
    /**
     * Constructor for Player vs AI game
     */
    public Game(String playerName, AIPlayer.Difficulty aiDifficulty) {
        initializeGame(2);
        players[0] = new Player(playerName, 'B');
        players[1] = new AIPlayer("AI", 'W', aiDifficulty);
        gameMode = GameMode.PVA;
    }
    
    /**
     * Constructor for Multiplayer game (2-4 players)
     */
    public Game(List<String> playerNames) {
        int numPlayers = Math.min(playerNames.size(), 4);
        initializeGame(numPlayers);
        
        char[] symbols = {'B', 'W', 'U', 'G'};
        for (int i = 0; i < numPlayers; i++) {
            players[i] = new Player(playerNames.get(i), symbols[i]);
        }
        gameMode = GameMode.MULTIPLAYER;
    }
    
    /**
     * Initialize basic game components
     */
    private void initializeGame(int numPlayers) {
        board = new Board();
        players = new Player[numPlayers];
        currentPlayerIndex = 0;
        turnCount = 0;
        maxTurns = 32 + (MAX_PIECES * 2); // 8 placements + max 24 moves = 32 turns per player
        gamePhase = GamePhase.PLACEMENT;
        winner = null;
        logger = new Logger();
    }
    
    /**
     * Start the game
     */
    public void startGame() {
        logger.log("=== Connect 5 Game Started ===");
        logger.log("Game Mode: " + gameMode);
        logger.log("Board Size: " + BOARD_SIZE + "x" + BOARD_SIZE);
        logger.log("Pieces per player: " + MAX_PIECES);
        logger.log("Target: Connect " + CONNECT_TARGET);
        
        for (Player p : players) {
            logger.log("Player: " + p.getName() + " (" + p.getSymbol() + ")");
        }
        logger.log("Current Player: " + getCurrentPlayer().getName());
    }
    
    /**
     * Play a single turn
     */
    public boolean playTurn(int col, int row) {
        return playTurn(col, row, -1, -1);
    }
    
    /**
     * Play a turn with movement (from and to coordinates)
     */
    public boolean playTurn(int fromCol, int fromRow, int toCol, int toRow) {
        Player currentPlayer = getCurrentPlayer();
        
        try {
            if (gamePhase == GamePhase.PLACEMENT) {
                // Placement phase
                if (!board.placePiece(fromCol, fromRow, currentPlayer.getSymbol())) {
                    logger.logError("Cannot place piece at " + coordinateToString(fromCol, fromRow));
                    return false;
                }
                currentPlayer.addPiece();
                logger.logGameEvent(currentPlayer.getName(), 
                    "placed piece at " + coordinateToString(fromCol, fromRow));
                
                // Check if all pieces placed
                if (currentPlayer.hasPlacedAllPieces() && 
                    allPlayersCompletePlacement()) {
                    gamePhase = GamePhase.MOVEMENT;
                    logger.log("All pieces placed. Movement phase begun.");
                }
                
            } else if (gamePhase == GamePhase.MOVEMENT) {
                // Movement phase
                if (toCol == -1 || toRow == -1) {
                    logger.logError("Invalid move coordinates");
                    return false;
                }
                
                if (!board.movePiece(fromCol, fromRow, toCol, toRow)) {
                    logger.logError("Cannot move piece from " + coordinateToString(fromCol, fromRow) +
                        " to " + coordinateToString(toCol, toRow));
                    return false;
                }
                logger.logGameEvent(currentPlayer.getName(),
                    "moved piece from " + coordinateToString(fromCol, fromRow) +
                    " to " + coordinateToString(toCol, toRow));
            }
            
            // Check for win condition
            if (board.checkWinCondition(currentPlayer.getSymbol())) {
                gamePhase = GamePhase.ENDED;
                winner = currentPlayer;
                logger.log(currentPlayer.getName() + " has won the game!");
                logger.log("Player connected 5 pieces!");
                return true;
            }
            
            // Check for draw condition
            turnCount++;
            if (turnCount >= maxTurns * players.length) {
                gamePhase = GamePhase.ENDED;
                logger.log("Game ended in a draw after " + turnCount + " total turns.");
                return true;
            }
            
            // Switch to next player
            switchToNextPlayer();
            return true;
            
        } catch (Exception e) {
            logger.logError("Error during turn: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Switch to next player
     */
    private void switchToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
        logger.log("Next Player: " + getCurrentPlayer().getName());
    }
    
    /**
     * Check if all players have completed piece placement
     */
    private boolean allPlayersCompletePlacement() {
        for (Player p : players) {
            if (!p.hasPlacedAllPieces()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get current player
     */
    public Player getCurrentPlayer() {
        return players[currentPlayerIndex];
    }
    
    /**
     * Get current game phase
     */
    public GamePhase getGamePhase() {
        return gamePhase;
    }
    
    /**
     * Check if game is over
     */
    public boolean isGameOver() {
        return gamePhase == GamePhase.ENDED;
    }
    
    /**
     * Get the winner (null if draw or game not ended)
     */
    public Player getWinner() {
        return winner;
    }
    
    /**
     * Get the board
     */
    public Board getBoard() {
        return board;
    }
    
    /**
     * Get all players
     */
    public Player[] getPlayers() {
        return players;
    }
    
    /**
     * Get current turn count
     */
    public int getTurnCount() {
        return turnCount;
    }
    
    /**
     * Get game logger
     */
    public Logger getLogger() {
        return logger;
    }
    
    /**
     * Display game status
     */
    public String getGameStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(board.displayBoard()).append("\n");
        sb.append("Current Phase: ").append(gamePhase).append("\n");
        sb.append("Current Player: ").append(getCurrentPlayer().getName()).append("\n");
        sb.append("Turn Count: ").append(turnCount).append("\n");
        
        for (Player p : players) {
            sb.append(p.getName()).append(" - Pieces Placed: ")
                .append(p.getPiecesPlaced()).append("/").append(MAX_PIECES).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Convert coordinates to chess-like notation (A1-H8)
     */
    private String coordinateToString(int col, int row) {
        return "" + (char)('A' + col) + (row + 1);
    }
    
    /**
     * End the game
     */
    public void endGame() {
        gamePhase = GamePhase.ENDED;
        logger.log("Game has ended.");
        
        if (winner != null) {
            logger.saveGameSummary("Winner: " + winner.getName() + 
                "\nTotal Turns: " + turnCount);
        } else {
            logger.saveGameSummary("Result: Draw\nTotal Turns: " + turnCount);
        }
    }
}