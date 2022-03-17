package jump61;

import antlr.debug.SemanticPredicateEvent;
import ucb.gui2.Pad;

import java.awt.*;
import java.awt.event.MouseEvent;

import java.util.concurrent.ArrayBlockingQueue;


import static jump61.Side.*;

/** A GUI component that displays a Jump61 board, and converts mouse clicks
 *  on that board to commands that are sent to the current Game.
 *  @author Nhu Vu
 */
class BoardWidget extends Pad {

    /** Length of the side of one square in pixels. */
    private static final int SQUARE_SIZE = 50;
    /** Width and height of a spot. */
    private static final int SPOT_DIM = 8;
    /** Minimum separation of center of a spot from a side of a square. */
    private static final int SPOT_MARGIN = 10;
    /** Width of the bars separating squares in pixels. */
    private static final int SEPARATOR_SIZE = 3;
    /** Width of square plus one separator. */
    private static final int SQUARE_SEP = SQUARE_SIZE + SEPARATOR_SIZE;

    /** Colors of various parts of the displayed board. */
    private static final Color
        NEUTRAL = Color.WHITE,
        SEPARATOR_COLOR = Color.BLACK,
        SPOT_COLOR = Color.BLACK,
        RED_TINT = new Color(255, 200, 200),
        BLUE_TINT = new Color(200, 200, 255);

    /** Strokes for ordinary grid lines. */
    static final BasicStroke GRIDLINE_STROKE = new BasicStroke(2);

    /** A new BoardWidget that monitors and displays a game Board, and
     *  converts mouse clicks to commands to COMMANDQUEUE. */
    BoardWidget(ArrayBlockingQueue<String> commandQueue) {
        _commandQueue = commandQueue;
        _side = 6 * SQUARE_SEP + SEPARATOR_SIZE;
        setMouseHandler("click", this::doClick);
    }

    /* .update and .paintComponent are synchronized because they are called
     *  by three different threads (the main thread, the thread that
     *  responds to events, and the display thread).  We don't want the
     *  saved copy of our Board to change while it is being displayed. */

    /** Update my display to show BOARD.  Here, we save a copy of
     *  BOARD (so that we can deal with changes to it only when we are ready
     *  for them), and recompute the size of the displayed board. */
    synchronized void update(Board board) {
        if (board.equals(_board)) {
            return;
        }
        if (_board != null && _board.size() != board.size()) {
            invalidate();
        }
        _board = new Board(board);
        _side = _board.size() * SQUARE_SEP + SEPARATOR_SIZE;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(_side, _side);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(_side, _side);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(_side, _side);
    }

    /** Fills the cells of the model. */
    private void drawCells(Graphics2D g) {
        for (int r = 1; r <= _board.size(); r++) {
            for (int c = 1; c <= _board.size(); c++) {
                if (_board.get(r, c).getSide() == RED) {
                    g.setColor(RED_TINT);
                    g.fillRect(SQUARE_SEP + SEPARATOR_SIZE,
                            SQUARE_SEP + SEPARATOR_SIZE,
                            SQUARE_SIZE, SQUARE_SIZE);
                } else if (_board.get(r, c).getSide() == BLUE) {
                    g.setColor(BLUE_TINT);
                    g.fillRect(SQUARE_SEP + SEPARATOR_SIZE,
                            SQUARE_SEP + SEPARATOR_SIZE,
                            SQUARE_SIZE, SQUARE_SIZE);
                } else {
                    g.setColor(NEUTRAL);
                    g.fillRect(SQUARE_SEP + SEPARATOR_SIZE,
                            SQUARE_SEP + SEPARATOR_SIZE,
                            SQUARE_SIZE, SQUARE_SIZE);
                }
                displaySpots(g, r, c);
            }
        }
    }

    /** Draws the grid of the model. */
    private void drawGrid(Graphics2D g) {
        g.setColor(NEUTRAL);
        g.setStroke(GRIDLINE_STROKE);
        g.fillRect(0, 0, _side, _side);
        g.setColor(SEPARATOR_COLOR);
        for (int i = 0; i <= _side; i += SQUARE_SEP) {
            g.fillRect(0, i, _side, SEPARATOR_SIZE);
            g.fillRect(i, 0, SEPARATOR_SIZE, _side);
            int r = i / _board.size() + 1;
            int c = i & _board.size() + 1;
        }
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        if (_board == null) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(NEUTRAL);
        g.fillRect(0, 0, _side, _side);

        drawGrid(g);
        drawCells(g);
    }

    /** Color and display the spots on the square at row R and column C
     *  on G.  (Used by paintComponent). */
    private void displaySpots(Graphics2D g, int r, int c) {
        int numSpots = _board.get(r, c).getSpots();
        if (numSpots == 1) {
            int x = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            int y = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            spot(g, x, y);
        } else if (numSpots == 2) {
            int x = (SQUARE_SIZE / 3) + SQUARE_SEP + SEPARATOR_SIZE;
            int x1 = (int) (SQUARE_SIZE / 1.5) + SQUARE_SEP + SEPARATOR_SIZE;
            int y = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            int y1 = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            spot(g, x, y);
            spot(g, x1, y1);
        } else if (numSpots == 3) {
            int x = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            int x1 = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            int x2 = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            int y = (SQUARE_SIZE / 4) + SQUARE_SEP + SEPARATOR_SIZE;
            int y1 = (SQUARE_SIZE / 2) + SQUARE_SEP + SEPARATOR_SIZE;
            int y2 = (int) (SQUARE_SIZE / 1.35) + SQUARE_SEP + SEPARATOR_SIZE;
            spot(g, x, y);
            spot(g, x1, y1);
            spot(g, x2, y2);
        } else {
            int x = (SQUARE_SIZE / 4) + SQUARE_SEP + SEPARATOR_SIZE;
            int x1 = (int) (SQUARE_SIZE / 1.35) + SQUARE_SEP + SEPARATOR_SIZE;
            int x2 = (SQUARE_SIZE / 4) + SQUARE_SEP + SEPARATOR_SIZE;
            int x3 = (int) (SQUARE_SIZE / 1.35) + SQUARE_SEP + SEPARATOR_SIZE;
            int y = (SQUARE_SIZE / 4) + SQUARE_SEP + SEPARATOR_SIZE;
            int y1 = (SQUARE_SIZE / 4) + SQUARE_SEP + SEPARATOR_SIZE;
            int y2 = (int) (SQUARE_SIZE / 1.35) + SQUARE_SEP + SEPARATOR_SIZE;
            int y3 = (int) (SQUARE_SIZE / 1.35) + SQUARE_SEP + SEPARATOR_SIZE;
            spot(g, x, y);
            spot(g, x1, y1);
            spot(g, x2, y2);
            spot(g, x3, y3);
        }
    }

    /** Draw one spot centered at position (X, Y) on G. */
    private void spot(Graphics2D g, int x, int y) {
        g.setColor(SPOT_COLOR);
        g.fillOval(x - SPOT_DIM / 2, y - SPOT_DIM / 2, SPOT_DIM, SPOT_DIM);
    }

    /** Respond to the mouse click depicted by EVENT. */
    public void doClick(String dummy, MouseEvent event) {
        int x = event.getX() - SEPARATOR_SIZE,
            y = event.getY() - SEPARATOR_SIZE;
        int r = y / SQUARE_SEP + 1;
        int c = x / SQUARE_SEP + 1;
        _commandQueue.offer(String.format("%d %d", r, c));
    }

    /** The Board I am displaying. */
    private Board _board;
    /** Dimension in pixels of one side of the board. */
    private int _side;
    /** Destination for commands derived from mouse clicks. */
    private ArrayBlockingQueue<String> _commandQueue;


}
