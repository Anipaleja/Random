import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private char symbol;  // 'B' for Black, 'W' for White, 'U' for Blue, 'G' for Green
    private int piecesPlaced;
    private int piecesRemaining;
    private static final int MAX_PIECES = 8;
    private List<String> moveHistory;
    
    /**
     * Constructor for a player
     * @param name the player's name
     */
    public Player(String name) {
        this.name = name;
        this.piecesPlaced = 0;
        this.piecesRemaining = MAX_PIECES;
        this.moveHistory = new ArrayList<>();
    }
    
    /**
     * Constructor for a player with a specific symbol
     * @param name the player's name
     * @param symbol the piece symbol ('B', 'W', 'U', 'G')
     */
    public Player(String name, char symbol) {
        this(name);
        this.symbol = symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public char getSymbol() {
        return symbol;
    }
    
    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }
    
    public int getPiecesPlaced() {
        return piecesPlaced;
    }
    
    public void setPiecesPlaced(int count) {
        this.piecesPlaced = count;
        this.piecesRemaining = MAX_PIECES - count;
    }
    
    public int getPiecesRemaining() {
        return piecesRemaining;
    }
    
    public void addPiece() {
        if (piecesPlaced < MAX_PIECES) {
            piecesPlaced++;
            piecesRemaining--;
        }
    }
    
    public boolean hasPlacedAllPieces() {
        return piecesPlaced >= MAX_PIECES;
    }
    
    public List<String> getMoveHistory() {
        return moveHistory;
    }
    
    public void recordMove(String moveDescription) {
        moveHistory.add(moveDescription);
    }
    
    /**
     * Make a move - override in subclasses for specific behavior
     * @param board the current board state
     * @return the move coordinates as a string (e.g., "A1" or "A1-B2")
     */
    public String makeMove(Board board) {
        // Placeholder for human player - actual implementation in subclasses
        return null;
    }
    
    @Override
    public String toString() {
        return name + " (" + symbol + ")";
    }
}
