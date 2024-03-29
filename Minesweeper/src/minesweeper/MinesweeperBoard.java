package minesweeper;

import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * <p>A concrete subclass of {@link TrustableReadOnlyMinesweeperBoard}.</p>
 * 
 * <p>Provides the additional {@link #uncover(int, int)}, {@link #flag(int, int)}, {@link #unflag(int, int)},
 * and {@link #toggleFlag(int, int)} methods to facilitate interaction with the board.</p>
 * 
 * <p>Each MinesweeperBoard additionally has a <i>mine percentage</i>, which is the percentage of its tile that are
 * mines. The exact number of mines is given by:<br>
 * floor({@link #getRows()} * {@link #getColumns()} * {@link #getMinePercent()})</p>
 * 
 * <p>A MinesweeperBoard whose mine locations are randomly generated will be guaranteed to have an "easy start"
 * if created with {@link #newBoardEasyStart(int, int, float)}.
 * This guarantee is not made for boards created using {@link #newBoard(int, int)} or
 * {@link #newBoard(int, int, float)}. The first tile uncovered on a board with an "easy start" will have zero adjacent
 * mines, if that is possible given the board size and mine percentage. Boards created with the all three of
 * those methods are guaranteed not to have a mine on the first tile uncovered.</p>
 * 
 * @author Sam Hooper
 *
 */
public class MinesweeperBoard extends TrustableReadOnlyMinesweeperBoard {
	
	/**
	 * The default mine percent used by a MinesweeperBoard if none is specified.
	 */
	public static final float DEFAULT_MINE_PERCENT = 0.15f;
	
	/**
	 * The maximum number of rows in any MinesweeperBoard. A MinesweeperBoard cannot have greater than MAX_ROWS rows.
	 */
	public static final int MAX_ROWS = 100;
	
	/**
	 * The maximum number of columns in any MinesweeperBoard.
	 * A MinesweeperBoard cannot have greater than MAX_COLS columns.
	 */
	public static final int MAX_COLS = 100;
	
	/**
	 * The minimum number of rows in any MinesweeperBoard. A MinesweeperBoard cannot have fewer than MIN_ROWS rows.
	 */
	public static final int MIN_ROWS = 3;
	
	/**
	 * The minimum number of columns in any MinesweeperBoard.
	 * A MinesweeperBoard cannot have fewer than MIN_COLS columns.
	 */
	public static final int MIN_COLS = 3;
	
	/**
	 * A <b>negative</b> constant value used by {@link #board} to indicate that there is a mine on that tile.
	 */
	private static final byte MINE = -1;
	/**
	 * A <b>negative</b> costant used by {@link #visibleBoard} to indicate that a tile has been flagged
	 */
	private static final byte FLAGGED = -1;
	
	/**
	 * Used by {@link #visibleBoard} to indicate that a tile is undecided; that is, it has not been uncovered and is
	 * not currently flagged.
	 */
	private static final byte UNDECIDED = -2;
	
	/**
	 * Used by {@link #visibleBoard} to indicate that a tile was a mine and caused the game to end when it was
	 * uncovered.
	 */
	private static final byte EXPLODED = -3;
	
	/**
	 * Returns a new MinesweeperBoard with {@code rows} rows and {@code cols} columns. The mine
	 * percentage is set to {@link #DEFAULT_MINE_PERCENT}. The first tile uncovered on the returned board is
	 * guaranteed not to be a mine, but may be adjacent to one.<br>
	 * Throws an {@link IllegalArgumentException} if {@code rows} or {@code cols} is too large or too small.
	 * @param rows the number of rows in the board
	 * @param cols the number of columns in the board.
	 * @throws IllegalArgumentException if and only if 
	 * {@code (rows < MIN_ROWS || row > MAX_ROWS || cols < MIN_COLS || rows > MAX_COLS)}
	 */
	public static MinesweeperBoard newBoard(final int rows, final int cols) {
		return new MinesweeperBoard(rows, cols, DEFAULT_MINE_PERCENT, false);
	}
	/**
	 * Returns a MinesweeperBoard with {@code rows} rows and {@code cols} columns. The mine percentage is set to
	 * {@code minePercent}. The first tile uncovered on the returned board is guaranteed not to be a mine, but
	 * may be adjacent to one.<br>Throws an {@link IllegalArgumentException} if and only if
	 * {@code (rows < MIN_ROWS || row > MAX_ROWS || cols < MIN_COLS || rows > MAX_COLS || minePercent <= 0.0f ||
	 * minePercent >= 1.0f)}.
	 * @param rows the number of rows in the board
	 * @param cols the number of columns in the board
	 * @param minePercent the percentage of the board's tiles that will be mines.
	 * @throws IllegalArgumentException for the reasons above.
	 */
	public static MinesweeperBoard newBoard(final int rows, final int cols, final float minePercent) {
		return new MinesweeperBoard(rows, cols, minePercent, false);
	}
	/**
	 * Returns a MinesweeperBoard with {@code rows} rows and {@code cols} columns. The mine percentage is set to
	 * {@code minePercent}. The first tile uncovered on the returned board will have zero adjacent mines, if that
	 * is possible given the board size and mine percentage.<br>Throws an {@link IllegalArgumentException} if and only if
	 * {@code (rows < MIN_ROWS || row > MAX_ROWS || cols < MIN_COLS || rows > MAX_COLS || minePercent <= 0.0f ||
	 * minePercent >= 1.0f)}.
	 * @param rows the number of rows in the board
	 * @param cols the number of columns in the board
	 * @param minePercent the percentage of the board's tiles that will be mines.
	 * @throws IllegalArgumentException for the reasons above.
	 */
	public static MinesweeperBoard newBoardEasyStart(final int rows, final int cols, final float minePercent) {
		return new MinesweeperBoard(rows, cols, minePercent, true);
	}
	/**
	 * Converts a rectangular grid of {@code boolean}s to a MinesweeperBoard. A value of {@code true} in {@code grid}
	 * indicates a mine, while a value of {@code false} indicates that there is not a mine. Each {@code boolean[]}
	 * in {@code grid} must have the same length. The number of rows and columns in grid must be within appropriate
	 * ranges for a MinesweeperBoard. 
	 * @param grid a rectangular {@code boolean[][]} representing the board.
	 * @return a new MinesweeperBoard containing the tiles represented by {@code grid}.
	 */
	static MinesweeperBoard from(final boolean[][] grid) {
		return new MinesweeperBoard(grid);
	}
	/**
	 * Throws an {@link IllegalArgumentException} if and only if {@code (minePercent <= 0.0f || minePercent >= 1.0f)}.
	 * Has no effect otherwise.
	 * @param minePercent
	 * @throws an IllegalArgumentException for the reason given above.
	 */
	private static void checkMinePercentInput(final float minePercent) {
		if(minePercent <= 0.0f || minePercent >= 1.0f) {
			throw new IllegalArgumentException(String.format("minePercent must be between 0.0f and 1.0f (exclusive)",
					minePercent));
		}
	}
	/**
	 * Throws a descriptive {@link IllegalArgumentException} if a board with {@code rows} rows and {@code cols}
	 * columns is not valid. Has no effect otherwise.
	 * @param rows the number of rows in a board
	 * @param cols the number of columns in a board.
	 * @throws an IllegalArgumentException for the reason given above.
	 */
	private static void checkSizeInput(final int rows, final int cols) {
		if(rows > MAX_ROWS || rows < MIN_ROWS) {
			throw new IllegalArgumentException(String.format("Number of rows must be between %d and %d (inclusive),"
					+ " was: %d", MIN_ROWS, MAX_ROWS, rows));
		}
		else if(cols > MAX_COLS || cols < MIN_COLS) {
			throw new IllegalArgumentException(String.format("Number of columns must be between %d and %d (inclusive),"
					+ " was: %d", MIN_COLS, MAX_COLS, rows));
		}
	}
	/**
	 * The number of rows in this board.
	 */
	private final int ROWS;
	/**
	 * The number of columns in this board.
	 */
	private final int COLS;
	/**
	 * The percentage of this board's tiles that are mines. The exact number of mines is given by:<br>
	 * floor(ROWS * COLS * MINE_PERCENT)
	 */
	private final float MINE_PERCENT;
	/**
	 * The exact number of mines on this board. Equal to floor(ROWS * COLS * MINE_PERCENT).
	 */
	private final int MINE_COUNT;
	
	/**
	 * A boolean indicating whether this board had an "easy start." If {@code true}, the first tile uncovered
	 * will have 0 adjacent flags (if that is possible given the board size and mine percentage). If {@code false},
	 * the first tile uncovered may have a nonzero number of adjacent flags. Note that the first tile uncovered
	 * will never be a mine.
	 */
	private final boolean EASY_START;
	
	/**
	 * {@code board[i][j]} represents the tile at row {@code i} and column {@code j}.<br>
	 * This is the internal board that stores the locations of all the mines and the number of mines next to each tile.
	 * Each location holds {@link #MINE} for a mine, and otherwise holds the number of adjacent flags (as given by
	 * {@link #countNearbyMines(int, int)}).
	 */
	private final byte[][] board;
	
	/**
	 * {@code visibleBoard[i][j]} represents the tile at row {@code i} and column {@code j}.<br>
	 * It stores {@link #UNDECIDED} if the tile is undecided (see {@link #isUndecided(int, int)}),
	 * {@link #FLAGGED} if the tile currently has a flag on it (see {@link #isFlagged(int, int)}), and
	 * otherwise holds the number of adjacent flags, as stored in {@link #board}.
	 */
	private final byte[][] visibleBoard;
	
	/**
	 * The source of random used to generate the locations of mines in {@link #initialize(int, int)}.
	 */
	private final Random random = new Random();
	/**
	 * The number of non-mine tiles the user has to uncover. It is set in {@link #finishInit()}.
	 */
	private int tilesRemaining;
	
	/**
	 * The number of usable flags remaining. It is set to {@link #MINE_COUNT} in {@link #finishInit()}.
	 * The number is decremented by one every time a flag is placed in
	 * {@link #flag(int, int)} and decremented by one every time a flag is removed in {@link #unflag(int, int)}.
	 */
	private int flagsRemaining;
	
	/**
	 * The state of this MinesweeperBoard. Set to {@link MinesweeperGameState#NOT_STARTED} until
	 * {@link #initialize(int, int)} is called, when it is the nset to {@link MinesweeperGameState#ONGOING}. When
	 * the last non-mine tile is uncovered, it is set to {@link MinesweeperGameState#WIN}. If a tile with a mine on it
	 * is uncovered, it is set to {@link MinesweeperGameState#LOSS}.
	 */
	private MinesweeperGameState gameState = MinesweeperGameState.NOT_STARTED;
	
	/**
	 * the row of the tile that exploded, or -1 if no tile has exploded.
	 */
	private int explodedRow = -1;
	
	/**
	 * the column of the tile that exploded, or -1 if no tile has exploded.
	 */
	private int explodedCol = -1;
	
	/**
	 * {@code boolean} indicating whether or not this board has been initialized. A board is initialized once the
	 * mines have been placed in {@link #board}.
	 */
	private boolean initialized = false;

	/**
	 * Constructs a MinesweeperBoard as defined by {@link #newBoard(int, int, float)}.
	 * @param rows the number of rows in the board
	 * @param cols the number of columns in the board
	 * @param minePercent the percentage of the board's tiles that will be mines.
	 * @throws IllegalArgumentException for the reasons stated in {@link #newBoard(int, int, float)}.
	 */
	private MinesweeperBoard(final int rows, final int cols, final float minePercent, boolean easyStart) {
		checkSizeInput(rows, cols);
		checkMinePercentInput(minePercent);
		this.ROWS = rows;
		this.COLS = cols;
		this.MINE_PERCENT = minePercent;
		this.EASY_START = easyStart;
		MINE_COUNT = (int) (ROWS * COLS * minePercent);
		board = new byte[ROWS][COLS];
		visibleBoard = new byte[ROWS][COLS];
		finishInit();
	}
	
	/**
	 * Constructs a new MinesweeperBoard as defined by {@link #from(boolean[][])}.
	 * @param grid a rectangular {@code boolean[][]} representing the board
	 */
	private MinesweeperBoard(final boolean[][] grid) {
		checkGridInput(grid);
		ROWS = grid.length;
		COLS = grid[0].length;
		board = new byte[ROWS][COLS];
		visibleBoard = new byte[ROWS][COLS];
		for(int i = 0; i < grid.length; i++) {
			for(int j = 0; j < grid[i].length; j++) {
				board[i][j] = grid[i][j] ? MINE : 0;
			}
		}
		putNumbers();
		int mineCount = 0;
		for(int i = 0; i < ROWS; i++) {
			for(int j = 0; j < COLS; j++) {
				if(isMine(i, j)) {
					mineCount++;
				}
			}
		}
		MINE_COUNT = mineCount;
		MINE_PERCENT = (float) (((double) MINE_COUNT) / (ROWS * COLS));
		EASY_START = false;
		finishInit();
		initialized = true;
	}
	
	/**
	 * Places a flag on the tile indicated by {@code row} and {@code col}, if possible. Placing a flag is not possible
	 * if any of the following are true:
	 * <ul>
     * <li> The tile has been uncovered.
     * <li> The tile already has a flag on it
     * <li> There are no flags remaining.
     * </ul>
	 * Returns {@code true} if and only if the board was changed by this call, {@code false otherwise}. If
	 * this method returns {@code true}, the number of flags remaining (as given by {@link #getFlagsRemaining()}) is
	 * decremented by one.
	 * @param row the row of the tile to set to a flag.
	 * @param col the column of the tile to set to a flag.
	 * @return Returns {@code true} if and only if the board was changed by this call, {@code false otherwise}.
	 * @throws IllegalArgumentException if {@code row} or {@code col} are out of bounds for this board
	 * @throws IllegalStateExcepton if the board has ended (See {@link #isEnded()}).
	 */
	public boolean flag(final int row, final int col) {
		checkNotEnded();
		checkBounds(row, col);
		if(flagsRemaining >= 1 && visibleBoard[row][col] == UNDECIDED) {
			visibleBoard[row][col] = FLAGGED;
			flagsRemaining--;
			return true;
		}
		return false;
	}
	
	@Override
	public int getColumns() {
		return COLS;
	}

	@Override
	public int getDisplayedNumber(final int row, final int col) {
		checkBounds(row, col);
		if(visibleBoard[row][col] >= 0)
			return visibleBoard[row][col];
		else
			return -1;
	}
	
	@Override
	public int[] getExplodedTile() {
		if(isEndedWithLoss()) {
			return new int[] {explodedRow, explodedCol};
		}
		else {
			throw new IllegalStateException("Cannot get exploded tile unless the game has ended with a loss");
		}
	}
	
	@Override
	public int getFlagsRemaining() {
		return flagsRemaining;
	}
	
	/**
	 * @return the mine percentage for this board.
	 */
	public float getMinePercent() {
		return MINE_PERCENT;
	}
	
	@Override
	public int getRows() {
		return ROWS;
	}
	
	/**
	 * {@inheritDoc}
	 * When a board has ended, attempting to invoke {@link #uncover(int, int)}, {@link #flag(int, int)} or
	 * {@link #unflag(int, int)} will cause an {@link IllegalStateException} because no more actions can occur on
	 * the board.
	 * @return {@code true} iff this board has ended, {@code false} otherwise.
	 */
	@Override
	public boolean isEnded() {
		return gameState == MinesweeperGameState.WIN || gameState == MinesweeperGameState.LOSS;
	}

	@Override
	public boolean isEndedWithLoss() {
		return gameState == MinesweeperGameState.LOSS;
	}
	
	@Override
	public boolean isEndedWithWin() {
		return gameState == MinesweeperGameState.WIN;
	}

	@Override
	public boolean isExploded(final int row, final int col) {
		checkBounds(row, col);
		return visibleBoard[row][col] == EXPLODED;
	}
	
	/**
	 * Returns {@code true} if any only if the tile indicated by {@code row} and {@code col} currently has a flag
	 * on it. See {@link #flag(int, int)}, {@link #unflag(int, int)} and {@link #toggleFlag(int, int)}.
	 * 
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return {@code true} iff the tile indicated by {@code row} and {@code col} currently has a flag on it
	 */
	@Override
	public boolean isFlagged(final int row, final int col) {
		checkBounds(row, col);
		return isFlaggedTrusted(row, col);
	}

	@Override
	public boolean isInBounds(final int row, final int col) {
		return row >= 0 && row < ROWS && col >= 0 && col < COLS;
	}
	
	/**
	 * Returns {@code true} if and only if the tile indicated by {@code row} and {@code col} has been uncovered,
	 * {@code false} otherwise (see {@link #uncover(int, int)}).<br>
	 * Note that a tile that was uncovered and had a mine underneath; that is,
	 * a tile for which {@link #isExploded(int, int)} returns true, is still considered uncovered.
	 * 
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return {@code true} iff the tile indicated by {@code row} and {@code col} has been uncovered, {@code false}
	 * otherwise.
	 * @throws IllegalArgumentException if the tile indicated by {@code row} and {@code col} is out of bounds for this
	 * board.
	 */
	@Override
	public boolean isUncovered(final int row, final int col) {
		checkBounds(row, col);
		return isUncoveredTrusted(row, col);
	}
	
	/**
	 * Returns {@code true} if and only if the tile indicated by {@code row} and {@code col} is undecided (that is, it
	 * is not currently flagged and has not been uncovered), {@code false} otherwise.
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return {@code true} iff the tile indicated by {@code row} and {@code col} is undecided, {@code false} otherwise.
	 * @throws IllegalArgumentException if the tile indicated by {@code row} and {@code col} is out of bounds for
	 * this board.
	 */
	@Override
	public boolean isUndecided(final int row, final int col) {
		checkBounds(row, col);
		return isUndecidedTrusted(row, col);
	}
	
	/**
	 * Toggles whether or not there is a flag on the tile indicated by {@code row} and {@code col}, if possible.
	 * Returns {@code true} if and only if the board was changed by this call, {@code false} otherwise.
	 * The behavior of this method is identical to:
	 * <blockquote><pre>
	 * if(isUndecided(row, col)) {
	 * 	return flag(row, col);
	 * }
	 * else if(isFlagged(row, col)) {
	 * 	return unflag(row, col);
	 * }
	 * return false;
	 * </pre></blockquote>
	 * @param row the row of the tile to toggle the flag of
	 * @param col the column of the tile to toggle the flag of
	 * @return {@code true} if and only if the board was changed by this call, {@code false} otherwise.
	 * @throws IllegalArgumentException if {@code row} or {@code col} are out of bounds for this board
	 * @throws IllegalStateExcepton if the board has ended (See {@link #isEnded()}).
	 */
	public boolean toggleFlag(final int row, final int col) {
		if(isUndecided(row, col)) {
			return flag(row, col);
		}
		else if(isFlagged(row, col)) {
			return unflag(row, col);
		}
		return false;
	}

	/**
     * Returns a String representation of this MinesweeperBoard containing the number of rows, the number of columns,
     * and the mine percentage (rounded to 3 decimal places).
     * @return a String representation of this MinesweeperBoard
     */
	@Override
	public String toString() {
		return String.format("MinesweeperBoard[rows=%d,columns=%d,minePercent=%.3f]", ROWS, COLS, MINE_PERCENT);
	}
	
	/**
	 * Uncovers the tile indicated by {@code row} and {@code col}. <b>If the tile is has already been uncovered, this
	 * method returns silently.</b> Calling this method for the first time on a MinesweeperBoard object will cause it
	 * to be initialized. This method throws an {@link IllegalStateException} if the board has ended
	 * (see {@link #isEnded()})
	 * @param row the row of the tile to uncover
	 * @param col the column of the tile to uncover
	 * @throws IllegalArgumentException if the tile indicated by {@code row} and {@code col} is out bounds for this
	 * board
	 * @throws IllegalStateExcepton if and only if the board has ended
	 */
	public void uncover(final int row, final int col) {
		checkNotEnded();
		checkBounds(row, col);
		if(gameState == MinesweeperGameState.NOT_STARTED) {
			if(!initialized) {
				initialize(row, col);
			}
			gameState = MinesweeperGameState.ONGOING;
		}
		uncoverTrustedAndAlreadyInitialized(row, col);
	}
	
	/**
	 * Removes the flag on the tile indicated by {@code row} and {@code col}, if possible. Removing a flag is not
	 * possible if the tile does not have a flag on it. Returns {@code true} if and only if the board was changed
	 * by this call, {@code false} otherwise. If this method returns {@code true}, the number of flags remaining
	 * (as given by {@link #getFlagsRemaining()}) is incremented by one.
	 * @param row the row of the tile to set to a flag.
	 * @param col the column of the tile to set to a flag.
	 * @return {@code true} iff a flag was successfully removed from the board, {@code false} otherwise.
	 * @throws IllegalArgumentException if {@code row} or {@code col} are out of bounds for this board
	 * @throws IllegalStateExcepton if and only if the board has ended (See {@link #isEnded()})
	 */
	public boolean unflag(final int row, final int col) {
		checkNotEnded();
		checkBounds(row, col);
		if(visibleBoard[row][col] == FLAGGED) {
			visibleBoard[row][col] = UNDECIDED;
			flagsRemaining++;
			return true;
		}
		return false;
	}
	
	/**
	 * Returns the number of adjacent tiles to the tile indicated by {@code row} and {@code col} that are
	 * flagged.
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return the number of tiles that are adjacent to the tile indicated by {@code row} and {@code col} that are
	 * flagged.
	 */
	int countAdjacentFlaggedTiles(final int row, final int col) {
		return countAdjacentsSatisfying(row, col, this::isFlaggedTrusted);
	}
	
	/**
	 * Returns the number of adjacent tiles to the tile indicated by {@code row} and {@code col} that are
	 * uncovered.
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return the number of tiles that are adjacent to the tile indicated by {@code row} and {@code col} that are
	 * uncovered.
	 */
	int countAdjacentUncoveredTiles(final int row, final int col) {
		return countAdjacentsSatisfying(row, col, this::isUncoveredTrusted);
	}
	
	/** 
	 * Returns true if and only if the tile indicated by {@code row} and {@code col} has a mine, false otherwise.
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return true iff the tile indicated by {@code row} and {@code col} has a mine, false otherwise.
	 */
	@Override
	boolean isFlaggedTrusted(final int row, final int col) {
		return visibleBoard[row][col] == FLAGGED;
	}
	
	/**
	 * Returns true if and only if the tile indicated by {@code row} and {@code col} has been uncovered,
	 * false otherwise (see {@link #uncover(int, int)} and {@link #uncoverNearby(int, int)}).
	 * 
	 * This method does not perform bounds checking. It will throw an {@link ArrayIndexOutOfBoundsException}
	 * if the tile indicated by {@code row} and {@code col} is out of bounds.
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return {@code true} iff the tile indicated by {@code row} and {@code col} has been uncovered, {@code false}
	 * otherwise.
	 */
	@Override
	boolean isUncoveredTrusted(final int row, final int col) {
		return visibleBoard[row][col] >= 0 || visibleBoard[row][col] == EXPLODED;
	}

	/**
	 * Returns {@code true} if and only if the tile indicated by {@code row} and {@code col} is undecided (that is, it
	 * is not currently flagged and has not been uncovered), {@code false} otherwise. This method does not perform
	 * bounds checking and therefore throws and ArrayIndexOutOfBoundsException if the tile is out of bounds.
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return {@code true} iff the tile indicated by {@code row} and {@code col} is undecided, {@code false} otherwise.
	 */
	@Override
	boolean isUndecidedTrusted(final int row, final int col) {
		return visibleBoard[row][col] == UNDECIDED;
	}
	
	/**
	 * Throws a descriptive {@link IllegalArgumentException} if the tile indicated by {@code row} and {@code col} is out of
	 * bounds for this board. The tile is out of bounds if and only if
	 * {@code (row < 0 || row >= ROWS || col < 0 || col >= COLS)}.
	 * @param row the row of the tile to be checked.
	 * @param col the column of the tile to be checked.
	 * @throws IllegalArgumentException if the tile indicated by {@code row} and {@code col} is out bounds for this
	 * board.
	 */
	private void checkBounds(final int row, final int col) {
		if(row < 0 | row >= ROWS || col < 0 || col >= COLS) {
			throw new IllegalArgumentException(String.format("row '%d' and col '%d' is out of bounds for this"
					+ " board having %d rows and %d columns", row, col, ROWS, COLS));
		}
	}
	
	/**
	 * Throws an {@link IllegalArgumentExceptio} if {@code grid} does not represent a valid board. A
	 * {@code boolean[][]} does not represent a valid board if and only if one or more of the following is true:
	 * <ul>
	 * <li> It has fewer than {@link MIN_ROWS} or greater than {@link #MAX_ROWS} rows
	 * <li> It has fewer than {@link MIN_COLS} or greater than {@link #MAX_COLS} columns
	 * <li> Any pair of {@code boolean[]} components have different lengths.
	 * </ul>
	 * @param grid the {@code boolean[][]} to be checked
	 */
	private void checkGridInput(final boolean[][] grid) {
		Objects.requireNonNull(grid);
		final int rows = grid.length;
		if(rows < MIN_ROWS || rows > MAX_ROWS) {
			throw new IllegalArgumentException(String.format(
					"grid.length must be between MIN_ROWS and MAX_ROWS (inclusive), was: %d", rows));
		}
		final int cols = grid[0].length;
		if(rows < MIN_ROWS || rows > MAX_ROWS) {
			throw new IllegalArgumentException(String.format(
					"The length of each row in must be between MIN_COLS and MAX_COLS (inclusive); row 0 was: %d",
					cols));
		}
		for(int i = 1; i < grid.length; i++) {
			if(grid[i].length != cols) {
				throw new IllegalArgumentException(String.format("The length of each row must be the same. Row 0 was"
						+ " %d while row %d was %d", cols, i, grid[i].length));
			}
		}
	}
	
	/**
	 * Throws an {@link IllegalStateException} if this board has ended, as determined by {@link #isEnded()}. Has no
	 * effect otherwise.
	 * @throws IllegalStateException for the reason given above.
	 */
	private void checkNotEnded() {
		if(isEnded()) {
			throw new IllegalStateException("The game has already ended.");
		}
	}
	
	/**
	 * Returns the number of flags adjacent to the tile indicated by {@code row} and {@code col}. "Adjacent" flags are
	 * those in the 8 nearby tiles.<br>
	 * {@code row} and {@code col} are assumed to be in bounds. This method throws and ArrayIndexOutOfBoundsException
	 * if they are not. If there is a flag on the tile indicated by {@code row} and {@code col}, it is <b>not</b>
	 * counted.
	 * @param row the row of the spot to count nearby flags
	 * @param col the column of the spot to count nearby flags.
	 * @throws ArrayIndexOutOfBoundsException if {@code row} is out of bounds for this board or {@code col} is out of
	 * bounds for this board.
	 * @return the number of flags adjacent to the tile indicated {@code row} and {@code col}. The value returned is
	 * guaranteed to be between 0 and 8 (inclusive).
	 */
	private byte countNearbyMines(final int row, final int col) {
		return (byte) countAdjacentsSatisfying(row, col, this::isMine);
	}
	
	private void finishInit() {
		flagsRemaining = MINE_COUNT;
		tilesRemaining = ROWS * COLS - MINE_COUNT;
		initVisibleBoard();
	}

	/**
	 * Initializes this board. Fills each tile in {@link #board} with the appropriate values: {@link #MINE} if the
	 * tile is a mine, and a non-negative number indicating the number of nearby flags otherwise.
	 * @param firstRow
	 * @param firstCol
	 */
	private void initialize(final int firstRow, final int firstCol) {
		putMines(firstRow, firstCol);
		putNumbers();
		initialized = true;
	}
	
	/**
	 * Sets all values in {@link #visibleBoard} to {@link #UNDECIDED}.
	 */
	private void initVisibleBoard() {
		for(int i = 0; i < visibleBoard.length; i++) {
			for(int j = 0; j < visibleBoard[i].length; j++) {
				visibleBoard[i][j] = UNDECIDED;
			}
		}
	}
	
	/**
	 * Returns true if the tile indicated by {@code row} and {@code col} is a mine, {@code false} otherwise
	 * @param row the row of the tile
	 * @param col the column of the tile
	 * @return true if the tile indicated by {@code row} and {@code col} is a mine, {@code false} otherwise
	 */
	private boolean isMine(final int row, final int col) {
		return board[row][col] == MINE;
	}
	
	/**
	 * Places {@link #MINE_COUNT} mines on the board. A mine will not
	 * be placed on the tile indicated by {@code firstRow} and {@code firstCol}. This method
	 * should only be called from the private {@code initialize} method.
	 * If {@link #EASY_START} is {@code true} and if it is possible fiven the board size and mine percentage,
	 * the tile on {@code firstRow} and {@code firstCol} will have zero adjacent mines.
	 * @param firstRow the row of the tile of the initial uncover action.
	 * @param firstCol the column of the tile of the initial uncover action.
	 */
	private void putMines(final int firstRow, final int firstCol) {
		int[][] mineLocations;
		System.out.printf("ROWS = %d, COLS = %d, MINE_COUNT = %d, EASY_START = %b, DIFF = %d%n", ROWS, COLS,
				MINE_COUNT, EASY_START, ROWS * COLS - MINE_COUNT);
		IntStream ints = random.ints(0, ROWS * COLS).distinct();
		if(!EASY_START || ROWS * COLS - MINE_COUNT < 9) {
			System.out.println("\tNoneasy init");
			ints = ints.filter(i -> i != COLS * firstRow + firstCol);
		}
		else {
			System.out.println("\tEasy init");
			ints = ints.filter(i -> {
				int row = i / COLS, col = i % COLS;
				return Math.abs(row - firstRow) > 1 || Math.abs(col - firstCol) > 1;
			});
					
		}
		mineLocations = ints.limit(MINE_COUNT)
				.mapToObj(x -> new int[] {x / COLS, x % COLS})
				.toArray(int[][]::new);
		for(int[] spot : mineLocations) {
			board[spot[0]][spot[1]] = MINE;
		}
	}
	
	/**
	 * Sets each tile in that is not a flag (see {@link #isFlagTrusted(int, int)}) in {@link board} to the number
	 * of nearby flags as given by {@link #countNearbyMines(int, int)}.
	 */
	private void putNumbers() {
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				if(!isMine(i, j)) {
					board[i][j] = countNearbyMines(i, j);
				}
			}
		}
	}
	
	/**
	 * Uncovers the tile indicated by {@code row} and {@code col}.<br>
	 * This method assumes that this board has been initialized (the mines have been placed),
	 * {@code row} and {@code col} are in bounds, and the game has not ended.<br>
	 * <b>This method returns silently if the tile indicated by {@code row} and {@code col}
	 * has already been uncovered.</b>
	 * @param row the row of the tile to uncover.
	 * @param col the column of the tile to uncover.
	 */
	private void uncoverTrustedAndAlreadyInitialized(final int row, final int col) {
		if(isUncoveredTrusted(row, col)) {
			return;
		}
		if(board[row][col] == MINE) {
			visibleBoard[row][col] = EXPLODED;
			explodedRow = row;
			explodedCol = col;
			gameState = MinesweeperGameState.LOSS;
			return;
		}
		else {
			unflag(row, col);
			visibleBoard[row][col] = board[row][col];
			tilesRemaining--;
			if(board[row][col] == 0) {
				uncoverNearby(row, col);
			}
		}
		if(tilesRemaining == 0) {
			System.out.println("Board ended with win");
			gameState = MinesweeperGameState.WIN;
		}
	}
	
	/**
	 * Uncovers all adjacent tiles to the tile indicated by {@code row} and {@code col} that are in bounds by
	 * invoking {@link #uncoverTrustedAndAlreadyInitialized(int, int)} on each.
	 * @param row the row of the tile to uncover the nearby tiles of.
	 * @param col the column of the tile to uncover the nearby tiles of.
	 */
	private void uncoverNearby(final int row, final int col) {
		forEachAdjacent(row, col, this::uncoverTrustedAndAlreadyInitialized);
	}
}
