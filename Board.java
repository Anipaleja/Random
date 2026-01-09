/**
 * Board class represents the 8x8 game board for Connect 5
 * Coordinates are labeled A-H (columns) and 1-8 (rows)
 * Grid uses 0-7 indexing internally
 */
public class Board {
    private char[][] grid = new char[8][8];
    private static final char EMPTY = '-';
    private static final int BOARD_SIZE = 8;
    private static final int CONNECT_TARGET = 5;
    
    public Board() {
        initializeBoard();
    }
    
    /**
     * Initializes the board with all empty spaces
     */
    public void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                grid[i][j] = EMPTY;
            }
        }
    }
    
    /**
     * Place a piece on the board at specified coordinates
     * @param col column index (0-7)
     * @param row row index (0-7)
     * @param symbol the piece symbol ('B', 'W', 'U', 'G')
     * @return true if placement successful, false if space occupied
     */
    public boolean placePiece(int col, int row, char symbol) {
        if (!isValidCoordinate(col, row)) {
            return false;
        }
        if (grid[row][col] != EMPTY) {
            return false;
        }
        grid[row][col] = symbol;
        return true;
    }

    /**
     * Move a piece from one position to another
     * @return true if move successful, false otherwise
     */
    public boolean movePiece(int fromCol, int fromRow, int toCol, int toRow) {
        if (!isValidCoordinate(fromCol, fromRow) || !isValidCoordinate(toCol, toRow)) {
            return false;
        }
        
        // Check if source has a piece
        if (grid[fromRow][fromCol] == EMPTY) {
            return false;
        }
        
        // Check if destination is empty
        if (grid[toRow][toCol] != EMPTY) {
            return false;
        }
        
        // Check if move is only one space away
        if (!isAdjacentSpace(fromCol, fromRow, toCol, toRow)) {
            return false;
        }
        
        char piece = grid[fromRow][fromCol];
        grid[fromRow][fromCol] = EMPTY;
        grid[toRow][toCol] = piece;
        return true;
    }
    
    /**
     * Check if two positions are adjacent (1 space away in any direction)
     */
    private boolean isAdjacentSpace(int col1, int row1, int col2, int row2) {
        int colDiff = Math.abs(col2 - col1);
        int rowDiff = Math.abs(row2 - row1);
        return colDiff <= 1 && rowDiff <= 1 && !(colDiff == 0 && rowDiff == 0);
    }
    
    /**
     * Check if the specified player has won (connected 5 pieces)
     * @return true if player has connected 5 pieces
     */
    public boolean checkWinCondition(char playerSymbol) {
        // Check horizontal
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col <= BOARD_SIZE - CONNECT_TARGET; col++) {
                if (checkLine(col, row, 1, 0, playerSymbol)) {
                    return true;
                }
            }
        }
        
        // Check vertical
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row <= BOARD_SIZE - CONNECT_TARGET; row++) {
                if (checkLine(col, row, 0, 1, playerSymbol)) {
                    return true;
                }
            }
        }
        
        // Check diagonal (top-left to bottom-right)
        for (int row = 0; row <= BOARD_SIZE - CONNECT_TARGET; row++) {
            for (int col = 0; col <= BOARD_SIZE - CONNECT_TARGET; col++) {
                if (checkLine(col, row, 1, 1, playerSymbol)) {
                    return true;
                }
            }
        }
        
        // Check diagonal (top-right to bottom-left)
        for (int row = 0; row <= BOARD_SIZE - CONNECT_TARGET; row++) {
            for (int col = CONNECT_TARGET - 1; col < BOARD_SIZE; col++) {
                if (checkLine(col, row, -1, 1, playerSymbol)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a line of 5 consecutive pieces exists starting at given position
     * @param col starting column
     * @param row starting row
     * @param colDir column direction (-1, 0, or 1)
     * @param rowDir row direction (-1, 0, or 1)
     * @param symbol the symbol to check for
     */
    private boolean checkLine(int col, int row, int colDir, int rowDir, char symbol) {
        for (int i = 0; i < CONNECT_TARGET; i++) {
            int currCol = col + (i * colDir);
            int currRow = row + (i * rowDir);
            if (!isValidCoordinate(currCol, currRow) || grid[currRow][currCol] != symbol) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if coordinates are within board bounds
     */
    private boolean isValidCoordinate(int col, int row) {
        return col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE;
    }
    
    /**
     * Get the piece at specified coordinates
     */
    public char getPiece(int col, int row) {
        if (isValidCoordinate(col, row)) {
            return grid[row][col];
        }
        return EMPTY;
    }
    
    /**
     * Check if a position is empty
     */
    public boolean isEmpty(int col, int row) {
        return getPiece(col, row) == EMPTY;
    }
    
    /**
     * Check if board is full
     */
    public boolean isFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (grid[i][j] == EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Get a copy of the board grid
     */
    public char[][] getGrid() {
        char[][] copy = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, BOARD_SIZE);
        }
        return copy;
    }
    
    /**
     * Display the board with coordinate labels
     */
    public String displayBoard() {
        StringBuilder sb = new StringBuilder();
        
        // Column headers (A-H)
        sb.append("   ");
        for (int col = 0; col < BOARD_SIZE; col++) {
            sb.append((char) ('A' + col)).append(" ");
        }
        sb.append("\n");
        
        // Board rows (8-1, top to bottom)
        for (int row = BOARD_SIZE - 1; row >= 0; row--) {
            sb.append(row + 1).append("  ");
            for (int col = 0; col < BOARD_SIZE; col++) {
                sb.append(grid[row][col]).append(" ");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
