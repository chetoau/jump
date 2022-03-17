package jump61;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Formatter;
import java.util.Objects;

import java.util.function.Consumer;

import static jump61.Side.*;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Nhu Vu
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _numcols = N;
        _numrows = N;
        _board = new Square[N * N];
        Arrays.fill(_board, Square.INITIAL);
        _history = new ArrayList<Board>();

    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        this.copy(board0);
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        _numrows = N;
        _numcols = N;
        _board = new Square[N * N];
        Arrays.fill(_board, Square.INITIAL);
        _history = new ArrayList<Board>();
        announce();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        int size = board._numcols * board._numrows;
        for (int i = 0; i < size; i++) {
            _board[i] = board.get(i);
        }
        _history = new ArrayList<Board>();
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        int size = board._numcols * board._numrows;
        for (int i = 0; i < size; i++) {
            _board[i] = board.get(i);
        }
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _numrows;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        return _board[n];
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int numSpots = 0;
        for (Square item: _board) {
            numSpots += item.getSpots();
        }
        return numSpots;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        return (_board[n].getSide() == WHITE || _board[n].getSide() == player);
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return (whoseMove().equals(player) && getWinner() == null);
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        boolean gameOver = false;
        ArrayList<Side> checkSide = new ArrayList<Side>();
        for (Square item: _board) {
            checkSide.add(item.getSide());
        }
        if ((checkSide.contains(RED) && !checkSide.contains(BLUE)
                && !checkSide.contains(WHITE)) || (checkSide.contains(BLUE)
                && !checkSide.contains(RED) && !checkSide.contains(WHITE))) {
            gameOver = true;
        }

        if (gameOver) {
            return _board[1].getSide();
        } else {
            return null;
        }
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int count = 0;
        for (Square square: _board) {
            if (square.getSide().equals(side)) {
                count++;
            }
        }
        return count;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        addSpot(player, sqNum(r, c));
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        int soFar = get(n).getSpots();
        _board[n] = square(player, soFar + 1);
        if (neighbors(n) < _board[n].getSpots()) {
            jump(n);
        }
        markUndo();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        if (num > 0) {
            _board[n] = square(player, num);
        } else {
            _board[n] = square(Side.WHITE, num);
        }
    }


    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        int index = _history.size() - 2;
        if (_history.size() > 1) {
            Board current = _history.get(index);
            internalCopy(current);
            _history.remove(_history.size() - 1);
        } else {
            clear(size());
        }
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        Board gameState = new Board(size());
        Board myBoard = new Board(size());
        myBoard._board = _board;
        gameState.internalCopy(myBoard);
        _history.add(gameState);
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();


    /** Helper method to find location of overfull squares.
     * @param S the square to be evaluated as overfull or not.
     * */
    private void spotOverfull(int S) {
        int neighbors = neighbors(S);
        if (_board[S].getSpots() > neighbors) {
            _workQueue.add(S);
        }
    }

    /** Helper method for jump because my function is too long.
     * @param col is for column
     * @param row is for row
     * @param p is for side color
     * @param spots is for number of spots
     */
    private void simpleHelper(int col, int row, Side p, int spots) {
        if (col == 1 && row == 1) {
            simpleAdd(p, 1, 2, spots);
            simpleAdd(p, 2, 1, spots);
            internalSet(row, col, 1, p);
        } else if (col == size() && row == 1) {
            simpleAdd(p, 1, col - 1, spots);
            simpleAdd(p, 2, col, spots);
            internalSet(row, col, 1, p);
        } else if (col == 1 && row == size()) {
            simpleAdd(p, row - 1, 1, spots);
            simpleAdd(p, row, 2, spots);
            internalSet(row, col, 1, p);
        } else if (col == size() && row == size()) {
            simpleAdd(p, row, col - 1, spots);
            simpleAdd(p, row - 1, col, spots);
            internalSet(row, col, 1, p);
        } else if (col == 1) {
            simpleAdd(p, row - 1, col, spots);
            simpleAdd(p, row, col + 1, spots);
            simpleAdd(p, row + 1, col, spots);
        } else if (col == size()) {
            simpleAdd(p, row - 1, col, spots);
            simpleAdd(p, row + 1, col, spots);
            simpleAdd(p, row, col - 1, spots);
        } else if (row == 1) {
            simpleAdd(p, row, col - 1, spots);
            simpleAdd(p, row, col + 1, spots);
            simpleAdd(p, row + 1, col, spots);
        } else if (row == size()) {
            simpleAdd(p, row, col - 1, spots);
            simpleAdd(p, row, col + 1, spots);
            simpleAdd(p, row - 1, col, spots);
        } else {
            simpleAdd(p, row, col - 1, spots);
            simpleAdd(p, row, col + 1, spots);
            simpleAdd(p, row - 1, col, spots);
            simpleAdd(p, row + 1, col, spots);
        }
    }

    /** One more helper function because my jump is still too long.
     * @param row for row
     * @param col for col
     */
    private void jumpHelper(int col, int row) {
        if (col == 1 && row == 1) {
            spotOverfull(sqNum(1, 2));
            spotOverfull(sqNum(2, 1));
        } else if (col == size() && row == 1) {
            spotOverfull(sqNum(1, col - 1));
            spotOverfull(sqNum(2, col));
        } else if (col == 1 && row == size()) {
            spotOverfull(sqNum(row - 1, 1));
            spotOverfull(sqNum(row, 2));
        } else if (col == size() && row == size()) {
            spotOverfull(sqNum(row, col - 1));
            spotOverfull(sqNum(row - 1, col));
        } else if (col == 1) {
            spotOverfull(sqNum(row - 1, col));
            spotOverfull(sqNum(row, col + 1));
            spotOverfull(sqNum(row + 1, col));
        } else if (col == size()) {
            spotOverfull(sqNum(row - 1, col));
            spotOverfull(sqNum(row + 1, col));
            spotOverfull(sqNum(row, col - 1));
        } else if (row == 1) {
            spotOverfull(sqNum(row, col - 1));
            spotOverfull(sqNum(row, col + 1));
            spotOverfull(sqNum(row + 1, col));
        } else if (row == size()) {
            spotOverfull(sqNum(row, col - 1));
            spotOverfull(sqNum(row, col + 1));
            spotOverfull(sqNum(row - 1, col));
        } else {
            spotOverfull(sqNum(row, col - 1));
            spotOverfull(sqNum(row, col + 1));
            spotOverfull(sqNum(row - 1, col));
            spotOverfull(sqNum(row + 1, col));
        }
    }

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        spotOverfull(S);
        while (!_workQueue.isEmpty()) {
            int sq = _workQueue.remove();
            int col = col(sq);
            int row = row(sq);
            Side p = _board[sq].getSide();
            if (col == 1 && row == 1) {
                simpleHelper(1, 1, p, 1);
                if (getWinner() == null) {
                    jumpHelper(1, 1);
                }
            } else if (col == size() && row == 1) {
                simpleHelper(size(), 1, p, 1);
                if (getWinner() == null) {
                    jumpHelper(size(), 1);
                }
            } else if (col == 1 && row == size()) {
                simpleHelper(1, size(), p, 1);
                if (getWinner() == null) {
                    jumpHelper(1, size());
                }
            } else if (col == size() && row == size()) {
                simpleHelper(size(), size(), p, 1);
                if (getWinner() == null) {
                    jumpHelper(size(), size());
                }
            } else if (col == 1) {
                simpleHelper(1, row, p, 1);
                internalSet(sq, 1, p);
                if (getWinner() == null) {
                    jumpHelper(1, row);
                }
            } else if (col == size()) {
                simpleHelper(size(), row, p, 1);
                internalSet(sq, 1, p);
                if (getWinner() == null) {
                    jumpHelper(size(), row);
                }
            } else if (row == 1) {
                simpleHelper(col, 1, p, 1);
                internalSet(sq, 1, p);
                if (getWinner() == null) {
                    jumpHelper(col, 1);
                }
            } else if (row == size()) {
                simpleHelper(col, size(), p, 1);
                internalSet(sq, 1, p);
                if (getWinner() == null) {
                    jumpHelper(col, size());
                }
            } else {
                simpleHelper(row, col, p, 1);
                internalSet(sq, 1, p);
                if (getWinner() == null) {
                    jumpHelper(col, row);
                }
            }
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        String out = "";
        out += "===";
        for (int i = 0; i < size() * size(); i++) {
            if (i % size() == 0) {
                out += "\n";
                out += "    ";
            }
            String square = _board[i].getSpots() + "- ";
            if (_board[i].getSide().equals(RED)) {
                square = _board[i].getSpots() + "r ";
            }
            if (_board[i].getSide().equals(BLUE)) {
                square = _board[i].getSpots() + "b ";
            }
            out += square;
        }
        out += "\n";
        out += "===";
        return out;
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            return Arrays.equals(_board, ((Board) obj)._board);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash((Object) _board);
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** Number of rows. */
    private int _numrows;

    /** Number of columns. */
    private int _numcols;

    /** Underlying array that stores the squares. */
    private Square[] _board;

    /** Stores undo history. */
    private ArrayList<Board> _history;

}
