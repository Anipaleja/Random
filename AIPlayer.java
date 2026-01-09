import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIPlayer extends Player {
    public enum Difficulty { BEGINNER, MEDIUM, SMART }
    
    private Difficulty difficulty;
    private Random random;
    
    public AIPlayer(String name, char symbol, Difficulty difficulty) {
        super(name, symbol);
        this.difficulty = difficulty;
        this.random = new Random();
    }
    
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * Make a move based on AI difficulty level
     * @param board the current board state
     * @return the move as a string
     */
    @Override
    public String makeMove(Board board) {
        switch(difficulty) {
            case BEGINNER:
                return makeBeginnerMove(board);
            case MEDIUM:
                return makeMediumMove(board);
            case SMART:
                return makeSmartMove(board);
            default:
                return makeBeginnerMove(board);
        }
    }
    
    /**
     * Beginner AI: Makes random valid moves
     */
    private String makeBeginnerMove(Board board) {
        List<String> validMoves = getAllValidMoves(board);
        if (validMoves.isEmpty()) {
            return null;
        }
        return validMoves.get(random.nextInt(validMoves.size()));
    }
    
    /**
     * Medium AI: Tries to connect pieces, blocks opponent, makes random moves otherwise
     */
    private String makeMediumMove(Board board) {
        List<String> validMoves = getAllValidMoves(board);
        if (validMoves.isEmpty()) {
            return null;
        }
        
        // Try to find a move that creates connections
        for (String move : validMoves) {
            if (isWinningMove(move, board, this.getSymbol())) {
                return move;
            }
        }
        
        // Otherwise pick a random move
        return validMoves.get(random.nextInt(validMoves.size()));
    }
    
    /**
     * Smart AI: Advanced strategy considering multiple factors
     */
    private String makeSmartMove(Board board) {
        List<String> validMoves = getAllValidMoves(board);
        if (validMoves.isEmpty()) {
            return null;
        }
        
        String bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        
        for (String move : validMoves) {
            int score = evaluateMoveScore(move, board);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove != null ? bestMove : validMoves.get(0);
    }
    
    /**
     * Get all valid moves for current board state
     */
    private List<String> getAllValidMoves(Board board) {
        List<String> moves = new ArrayList<>();
        
        if (!this.hasPlacedAllPieces()) {
            // Placement phase - any empty square is valid
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (board.isEmpty(col, row)) {
                        moves.add(coordinateToString(col, row));
                    }
                }
            }
        } else {
            // Movement phase - find all pieces and their adjacent empty spaces
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (board.getPiece(col, row) == this.getSymbol()) {
                        // Check all 8 adjacent spaces
                        for (int dRow = -1; dRow <= 1; dRow++) {
                            for (int dCol = -1; dCol <= 1; dCol++) {
                                if (dRow == 0 && dCol == 0) continue;
                                int newRow = row + dRow;
                                int newCol = col + dCol;
                                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                                    if (board.isEmpty(newCol, newRow)) {
                                        String move = coordinateToString(col, row) + "-" + 
                                                    coordinateToString(newCol, newRow);
                                        if (!moves.contains(move)) {
                                            moves.add(move);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Check if a move results in a win
     */
    private boolean isWinningMove(String move, Board board, char symbol) {
        // This would need to simulate the move and check for win condition
        // Placeholder for now
        return false;
    }
    
    /**
     * Evaluate the score of a move
     */
    private int evaluateMoveScore(String move, Board board) {
        int score = 0;
        
        // Prefer center positions
        String[] parts = move.split("-");
        String targetPos = parts.length > 1 ? parts[1] : parts[0];
        int[] coords = stringToCoordinate(targetPos);
        int col = coords[0];
        int row = coords[1];
        
        // Center positions are more valuable
        int centerDist = Math.abs(col - 3) + Math.abs(row - 3);
        score += (8 - centerDist);
        
        // Check if this creates connections
        // TODO: Implement connection checking
        
        return score;
    }
    
    /**
     * Check if a move creates a connect pattern
     */
    private int countConnections(int col, int row, Board board, char symbol) {
        int connections = 0;
        
        // Check all 4 directions (horizontal, vertical, 2 diagonals)
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int count = 1; // Count the piece itself
            // Check forward
            for (int i = 1; i < 5; i++) {
                int newCol = col + (dir[0] * i);
                int newRow = row + (dir[1] * i);
                if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                    if (board.getPiece(newCol, newRow) == symbol) {
                        count++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            // Check backward
            for (int i = 1; i < 5; i++) {
                int newCol = col - (dir[0] * i);
                int newRow = row - (dir[1] * i);
                if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                    if (board.getPiece(newCol, newRow) == symbol) {
                        count++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            
            if (count >= 2) {
                connections += count;
            }
        }
        
        return connections;
    }
    
    /**
     * Convert coordinate string (e.g., "A1") to array [col, row]
     */
    private int[] stringToCoordinate(String pos) {
        if (pos.length() < 2) {
            return new int[]{0, 0};
        }
        int col = pos.charAt(0) - 'A';
        int row = Character.getNumericValue(pos.charAt(1)) - 1;
        return new int[]{col, row};
    }
    
    /**
     * Convert coordinates to string format (e.g., "A1")
     */
    private String coordinateToString(int col, int row) {
        return "" + (char)('A' + col) + (row + 1);
    }
}
