package jump61;

import java.util.ArrayList;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    /** Winning number for static evaluation. */
    static final int WINNING_NUMBER = 15;

    @Override
    String getMove() {
        Board board = getGame().getBoard();

        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        assert getSide() == work.whoseMove();
        _foundMove = -1;
        if (getSide() == RED) {
            minMax(work, 4, true, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } else {
            minMax(work, 4, true, -1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return _foundMove;
    }

    /** Helper function to help find next possible moves.
     *
     * @param b for Board.
     * @param p for Side p.
     * @return Arraylist containing next possible moves.
     */
    private ArrayList<Integer> nextMoves(Board b, Side p) {
        ArrayList<Integer> moves = new ArrayList<Integer>();
        int size = b.size() * b.size();
        for (int i = 0; i < size; i++) {
            if (b.isLegal(p, i)) {
                moves.add(i);
            }
        }
        return moves;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        if (depth == 0 || !getGame().gameInProgress()) {
            return staticEval(board, WINNING_NUMBER);
        } else if (sense == 1) {
            int bestVal = Integer.MIN_VALUE;
            ArrayList<Integer> possMoves = nextMoves(board, RED);
            for (int move: possMoves) {
                Board nextState = new Board(board.size());
                nextState.copy(board);
                int response = minMax(nextState, depth - 1,
                        true, -1, alpha, beta);
                _foundMove = move;
                if (response > bestVal) {
                    bestVal = response;
                    alpha = Integer.max(alpha, bestVal);
                    if (alpha >= beta) {
                        return bestVal;
                    }
                }
            }
            return bestVal;
        } else {
            int bestVal = Integer.MAX_VALUE;
            ArrayList<Integer> possMoves = nextMoves(board, BLUE);
            for (int move: possMoves) {
                Board nextState = new Board(board.size());
                nextState.copy(board);
                int response = minMax(nextState, depth - 1,
                        true, 1, alpha, beta);
                _foundMove = move;
                if (response < bestVal) {
                    bestVal = response;
                    beta = Integer.min(beta, bestVal);
                    if (alpha >= beta) {
                        return bestVal;
                    }
                }
            }
            return bestVal;
        }
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        int size = getBoard().size() * getBoard().size();
        int redSquares = b.numOfSide(RED);
        int blueSquares = b.numOfSide(BLUE);
        if (redSquares == size) {
            return winningValue;
        } else if (blueSquares == size) {
            return -winningValue;
        } else {
            return redSquares - blueSquares;
        }
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;

}
