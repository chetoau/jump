package jump61;

import ucb.gui2.Pad;
import ucb.gui2.TopLevel;
import ucb.gui2.LayoutSpec;

import java.util.concurrent.ArrayBlockingQueue;

import static jump61.Side.*;

/** The GUI controller for jump61.  To require minimal change to textual
 *  interface, we adopt the strategy of converting GUI input (mouse clicks)
 *  into textual commands that are sent to the Game object through a
 *  a Writer.  The Game object need never know where its input is coming from.
 *  A Display is an Observer of Games and Boards so that it is notified when
 *  either changes.
 *  @author Nhu Vu
 */
class Display extends TopLevel implements View, CommandSource, Reporter {

    /** Padding. */
    static final int PADDING = 35 / 2;

    /** Margin size for label placement (in pixels). */
    static final int UNIT_MARGIN = 5;

    /** A new window with given TITLE displaying GAME, and using COMMANDWRITER
     *  to send commands to the current game. */
    Display(String title) {
        super(title, true);

        addMenuButton("Game->Quit", this::quit);
        addMenuButton("Game->New Game", this::newGame);
        addMenuButton("Options->Size: 2", this::two);
        addMenuButton("Options->Size: 3", this::three);
        addMenuButton("Options->Size: 4", this::four);
        addMenuButton("Options->Size: 5", this::five);
        addMenuButton("Options->Size: 6", this::six);
        addMenuButton("Options->Size: 7", this::seven);
        addMenuButton("Options->Size: 8", this::eight);
        addMenuButton("Options->Size: 9", this::nine);
        addMenuButton("Options->Size: 10", this::ten);

        _boardWidget = new BoardWidget(_commandQueue);
        add(_boardWidget, new LayoutSpec("y", 1, "width", 2));
        display(true);
    }

    /** Response to "Quit" button click. */
    void quit(String dummy) {
        System.exit(0);
    }

    /** Response to "New Game" button click. */
    void newGame(String dummy) {
        _commandQueue.offer("new");
    }

    /** Response to "Size: 2" button click. */
    void two(String dummy) {
        _commandQueue.offer("size %d%n", 2);
    }

    /** Response to "Size: 3" button click. */
    void three(String dummy) {

    }

    /** Response to "Size: 4" button click. */
    void four(String dummy) {

    }

    /** Response to "Size: 5" button click. */
    void five(String dummy) {

    }

    /** Response to "Size: 6" button click. */
    void six(String dummy) {

    }

    /** Response to "Size: 7" button click. */
    void seven(String dummy) {

    }

    /** Response to "Size: 8" button click. */
    void eight(String dummy) {

    }

    /** Response to "Size: 9" button click. */
    void nine(String dummy) {

    }

    /** Response to "Size: 10" button click. */
    void ten(String dummy) {

    }

    @Override
    public void update(Board board) {
        if (_boardWidget == null) {
            _boardWidget = new BoardWidget(_commandQueue);
            int pad = PADDING;
            add(_boardWidget,
                    new LayoutSpec("y", 0,
                            "ileft", pad, "iright", pad,
                            "itop", pad, "ibottom", pad,
                            "height", 1, "width", 2));
            addLabel("0 R/ 0 B", "MoveLabel",
                    new LayoutSpec("y", 1, "height",
                            "REMAINDER", "x", 0, "anchor",
                            "southwest", "ileft", 4 * UNIT_MARGIN,
                            "itop", UNIT_MARGIN, "i_bottom", UNIT_MARGIN));
        }
        _boardWidget.update(board);
        pack();
        _boardWidget.repaint();
        display(true);
    }

    @Override
    public String getCommand(String ignored) {
        try {
            return _commandQueue.take();
        } catch (InterruptedException excp) {
            throw new Error("unexpected interrupt");
        }
    }

    @Override
    public void announceWin(Side side) {
        showMessage(String.format("%s wins!", side.toCapitalizedString()),
                    "Game Over", "information");
    }

    @Override
    public void announceMove(int row, int col) {
    }

    @Override
    public void msg(String format, Object... args) {
        showMessage(String.format(format, args), "", "information");
    }

    @Override
    public void err(String format, Object... args) {
        showMessage(String.format(format, args), "Error", "error");
    }

    /** Time interval in msec to wait after a board update. */
    static final long BOARD_UPDATE_INTERVAL = 50;

    /** The widget that displays the actual playing board. */
    private BoardWidget _boardWidget;
    /** Queue for commands going to the controlling Game. */
    private final ArrayBlockingQueue<String> _commandQueue =
        new ArrayBlockingQueue<>(5);

}
