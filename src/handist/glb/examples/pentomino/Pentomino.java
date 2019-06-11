/**
 *
 */
package handist.glb.examples.pentomino;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

/**
 * @author Patrick Finnerty
 *
 */
public class Pentomino {

  /**
   * Indicates the number of pieces that actually need to be placed by the
   * pentomino algorithm. As we arbitrarily place pieceX at the beginning of the
   * computation, the total number of pieces to place is reduced from 12 to 11.
   */
  public static final int NB_PIECE = 11;

  /**
   * Counter of the number of solutions found. Is incremented during the
   * computation.
   */
  transient int solutions = 0;

  /**
   * Width of the board on which the pentomino need to be placed
   */
  transient int width;

  /**
   * Height of the board on which the pentomino need to be placed
   */
  transient int height;

  /**
   * Board on which the pieces are to be placed
   */
  transient Board board;

  /**
   * Piece X kept aside as it is used to eliminate symmetry and rotations of
   * solutions in the search tree.
   */
  transient PieceX X;

  /**
   * PieceP instance which is contained in #pieces but kept as a member to allow
   * easy access in case further symmetry restrictions need to be applied to the
   * Pentomino problem
   */
  transient PieceP P;

  /**
   * Reserve of different explorations kept aside
   */
  transient Deque<Pentomino> reserve;

  /**
   * Array containing all the pieces except for piece X and whether they are
   * placed on the board or still available
   */
  PiecePlaced[] pieces;

  /**
   * Stack containing the pieces that were placed on the board
   */
  Stack<PiecePlaced> stack;

  /**
   * Indicates that 'depth' pieces have been chosen/orientated and placed on the
   * board. Used as index for arrays {@link #lowPiece}, {@link #highPiece},
   * {@link #lowPosition}, and {@link #highPosition}.
   */
  int depth;

  /**
   * Lower bound of the pieces that are left to consider in the
   * {@link #piecesLeft} collection
   */
  int[] lowPiece;
  /**
   * Upper bound of the pieces that are left to consider in the
   * {@link #piecesLeft} collection
   */
  int[] highPiece;

  /** Lower bound of the variation of the piece under consideration */
  int[] lowPosition;
  /** Upper bound of the variation of the piece under consideration */
  int[] highPosition;

  /**
   * Puts the exploration state of the given pentomino into reserve
   *
   * @param p
   *          the exploration to keep on the side
   */
  public void putInReserve(Pentomino p) {
    reserve.add(p);
  }

  /**
   * Discards the current exploration and replaces it with the last added
   * exploration available in reserve
   */
  public void takeFromReserve() {
    final Pentomino p = reserve.pop();

    pieces = p.pieces;
    stack = p.stack;
    depth = p.depth;
    lowPiece = p.lowPiece;
    highPiece = p.highPiece;
    highPosition = p.highPosition;
    lowPosition = p.lowPosition;

    // Reconstitute the board
    board.clear();
    // TODO
  }

  /**
   * Resets this instance into it's condition just after leaving the constructor
   * It will be ready to launch a new computation after being called
   */
  public void reset() {
    reserve.clear();
    solutions = 0;
    depth = 0;
    board.clear();
    lowPiece[0] = 0;
    highPiece[0] = NB_PIECE;

    lowPosition[0] = 0;
    highPosition[0] = 0;
    P.vars = 8;

  }

  /**
   * Prints the current stack status
   */
  public void printStack() {
    String s = "Stack:" + stack.size() + "[";
    for (final PiecePlaced pp : stack) {
      s += pp + " ";
    }
    System.out.println(s);
  }

  /**
   * Runs the pentomino exploration to completion
   */
  public void toCompletion() {
    for (;;) {
      while (0 <= depth) {
        step();
        // printStack();
      }
      if (!reserve.isEmpty()) {
        takeFromReserve();
        // Will restart the computation
      } else {
        break;
      }
    }
  }

  /**
   * Returns the number of positions left to explore for the piece at the
   * current depth
   *
   * @return
   */
  private int positionsLeft() {
    return highPosition[depth] - lowPosition[depth];
  }

  /**
   * Returns the number of alternative pieces left to try at the current depth
   *
   * @return
   */
  private int piecesLeft() {
    return highPiece[depth] - lowPiece[depth];
  }

  /**
   * Retrieves the index pieceplace that remains to be placed on the board
   *
   * @param index
   * @return
   */
  private PiecePlaced getRemaining(int index) {
    PiecePlaced pp;
    int i = 0;
    do {
      pp = pieces[i];
      i++;
      if (pp.variation < 0) {
        index--;
      }
    } while (0 < index);
    return pp;
  }

  /**
   * Performs one step in the pentomino search
   */
  public void step() {
    if (positionsLeft() > 0) {
      // We try a new position of the current piece

      // Select the current piece and the position to try
      final PiecePlaced pp = getRemaining(lowPiece[depth]);
      final int position = lowPosition[depth];
      final int index = board.nextIndex;
      lowPosition[depth]++;

      if (board.placePiece(pp.piece, position)) {
        depth++;
        // place p has been placed, we remove it from the pieces left to place
        // and add it to the stack
        pp.variation = position;
        pp.index = index;
        stack.add(pp);
        if (depth == NB_PIECE) {
          // We found a solution !
          // System.out.println(this);
          // printStack();
          solutions++;

          // We need to backtrack, removing the last 2 pieces
          depth--;
          stack.pop();
          board.removePiece(pp.piece, pp.variation, pp.index);
          pp.variation = -1;

          depth--;
          final PiecePlaced oneButLast = stack.pop();
          board.removePiece(oneButLast.piece, oneButLast.variation,
              oneButLast.index);
          oneButLast.variation = -1;

          // cleanup for future exploration
          highPosition[NB_PIECE - 1] = 0;

          // lowPiece[depth]++;
        } else {
          // Prepare for the selection of the next piece
          lowPiece[depth] = 0;
          highPiece[depth] = NB_PIECE - depth;
        }
      }
    } else if (piecesLeft() > 0) {
      // We try to place a different piece
      // Depth remains unchanged

      lowPiece[depth]++; // The previous piece has been completely explored

      // Select the next piece and setup the lowPosition and highPosition arrays
      final Piece p = getRemaining(lowPiece[depth]).piece;

      lowPosition[depth] = 0;
      highPosition[depth] = p.variations();
      // The position #0 of piece p will be tried in the next call to step
    } else {
      // backtrack
      // remove the piece placed from the stack and make it available again
      depth--;
      if (depth < 0) { // Checks if the exploration is finished
        return;
      }
      final PiecePlaced pp = stack.pop();
      board.removePiece(pp.piece, pp.variation, pp.index);
      pp.variation = -1;
    }
  }

  /**
   * Constructor of a pentomino puzzle of the specified size
   *
   * @param w
   *          width of the rectangle in which to fit the pieces
   * @param h
   *          height of the rectangle in which to fit the pieces
   */
  public Pentomino(int w, int h) {
    width = w;
    height = h;

    reserve = new LinkedList<>();

    board = new Board(w, h);

    // Create the pieces
    pieces = new PiecePlaced[NB_PIECE];
    pieces[0] = new PiecePlaced(new PieceI(w + Board.SENTINEL, h));
    pieces[1] = new PiecePlaced(new PieceU(w + Board.SENTINEL, h));
    pieces[2] = new PiecePlaced(new PieceT(w + Board.SENTINEL, h));
    pieces[3] = new PiecePlaced(new PieceF(w + Board.SENTINEL, h));
    pieces[4] = new PiecePlaced(new PieceY(w + Board.SENTINEL, h));
    pieces[5] = new PiecePlaced(new PieceZ(w + Board.SENTINEL, h));
    pieces[6] = new PiecePlaced(new PieceL(w + Board.SENTINEL, h));
    pieces[7] = new PiecePlaced(new PieceN(w + Board.SENTINEL, h));
    pieces[8] = new PiecePlaced(new PieceW(w + Board.SENTINEL, h));
    pieces[9] = new PiecePlaced(new PieceV(w + Board.SENTINEL, h));
    P = new PieceP(w + Board.SENTINEL, h);
    pieces[10] = new PiecePlaced(P);

    X = new PieceX(w + Board.SENTINEL, h);

    Arrays.sort(pieces); // Not necessary but ...

    stack = new Stack<>();

    // Prepare the arrays that describe the tree
    lowPiece = new int[NB_PIECE];
    lowPosition = new int[NB_PIECE];
    highPiece = new int[NB_PIECE];
    highPosition = new int[NB_PIECE];
    highPiece[0] = NB_PIECE;
  }

  /**
   * Computes the number of unique solutions to the pentomino problem by
   * arbitrarily placing piece X in some predetermined position on the board.
   */
  public void computeSolutions() {
    long durationSum = 0;
    int solutionSum = 0;

    for (int i = 0; i < (height - 1) / 2; i++) {
      for (int j = 0; j < (width + 1) / 2; j++) {
        // Place pieceX in (j,i) coordinates
        final int placementIndex = i * (width + Board.SENTINEL) + j;
        board.nextIndex = placementIndex;
        if (placementIndex != 1 && board.placePiece(X, 0)) {

          // Remove additional symmetry in cases where PieceX is placed on the
          // center axis
          if ((height % 2 == 1 && i + 1 == (height - 1) / 2)
              || (width % 2 == 1 && j == (width - 1) / 2)) {
            System.err.println(
                "Additional symmetry removal for (" + j + "," + i + ")");
            // Remove variations of PieceP to rule out symmetries
            P.vars = 4;
          }
          board.nextIndex = 0;

          long duration = System.nanoTime();
          toCompletion();
          duration = System.nanoTime() - duration;
          System.out.println("(" + j + "," + i + ") Total solutions: "
              + solutions + ":Elapsed time(s): " + duration / 1e9);
          durationSum += duration;
          solutionSum += solutions;

          reset();
        }
      }
    }

    System.out.println(
        "Total solutions " + width + "*" + height + "; " + solutionSum);
    System.out.println("Time; " + durationSum / 1e9);
  }

  /**
   * Builds and displays the board with its sentinel on stdout
   *
   * @param args
   *          unused
   */
  public static void main(String[] args) {
    int WIDTH = 10, HEIGHT = 6;
    try {
      WIDTH = Integer.parseInt(args[0]);
      HEIGHT = Integer.parseInt(args[1]);
    } catch (final Exception e) {
      System.err.println(
          "Error parsing arguments, using H=" + HEIGHT + " W=" + WIDTH);
    }

    if (WIDTH * HEIGHT != 60 || WIDTH < HEIGHT) {
      System.err.println("Wrong board size: H=" + HEIGHT + " W=" + WIDTH);
      return;
    }

    new Pentomino(WIDTH, HEIGHT).computeSolutions();

  }

  /**
   * Tuple containing a Piece, an orientation (or -1 if it isn't placed) and an
   * index at which the piece was placed on the board (only relevant if
   * variation is not -1).
   *
   * @author Patrick Finnerty
   *
   */
  private class PiecePlaced implements Comparable<PiecePlaced>, Serializable {
    /** Serial version UID */
    private static final long serialVersionUID = 4719358467517194092L;
    int variation;
    Piece piece;
    int index;

    public PiecePlaced(Piece p) {
      piece = p;
      variation = -1;
      index = -1;
    }

    @Override
    public String toString() {
      return "" + piece + variation + "/" + piece.variations();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(PiecePlaced o) {
      return o.piece.compareTo(piece);
    }
  }

}
