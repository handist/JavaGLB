/**
 *
 */
package handist.glb.examples.pentomino;

import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

import handist.glb.examples.Sum;
import handist.glb.multiworker.Bag;

/**
 * @author Patrick Finnerty
 *
 */
public class Pentomino implements Bag<Pentomino, Sum>, Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 6033611157974969906L;

  /**
   * Indicates the number of pieces that actually need to be placed by the
   * pentomino algorithm. As we arbitrarily place pieceX at the beginning of the
   * computation, the total number of pieces to place is reduced from 12 to 11.
   */
  public static final int NB_PIECE = 12;

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
   * Indicates of the current instance needs additional restrictions on symmetry
   */
  boolean additionalSymmetryRestriction = false;

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

    // Reconstitute the board in the state p was
    board.clear();
    for (final PiecePlaced pp : stack) {
      board.placeArbitrarily(pp.piece, pp.variation, pp.index);
    }
    P = (PieceP) pieces[10].piece;

    if (p.additionalSymmetryRestriction) {
      P.vars = 4;
    } else {
      P.vars = 8;
    }
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
   * Produces a Pentomino instance with the bare minimum of field instanciated
   *
   * @return a pentomino to be used for transfering a partial exploration of the
   *         pentomino problem
   */
  protected Pentomino getTransferPentomino() {
    final Pentomino p = new Pentomino();

    p.pieces = new PiecePlaced[NB_PIECE];
    p.pieces[0] = new PiecePlaced(new PieceI(width + Board.SENTINEL, height));
    p.pieces[1] = new PiecePlaced(new PieceU(width + Board.SENTINEL, height));
    p.pieces[2] = new PiecePlaced(new PieceT(width + Board.SENTINEL, height));
    p.pieces[3] = new PiecePlaced(new PieceF(width + Board.SENTINEL, height));
    p.pieces[4] = new PiecePlaced(new PieceY(width + Board.SENTINEL, height));
    p.pieces[5] = new PiecePlaced(new PieceZ(width + Board.SENTINEL, height));
    p.pieces[6] = new PiecePlaced(new PieceL(width + Board.SENTINEL, height));
    p.pieces[7] = new PiecePlaced(new PieceN(width + Board.SENTINEL, height));
    p.pieces[8] = new PiecePlaced(new PieceW(width + Board.SENTINEL, height));
    p.pieces[9] = new PiecePlaced(new PieceV(width + Board.SENTINEL, height));
    p.pieces[10] = new PiecePlaced(new PieceP(width + Board.SENTINEL, height));
    p.pieces[11] = new PiecePlaced(new PieceX(width + Board.SENTINEL, height));

    p.stack = new Stack<>();

    // Prepare the arrays that describe the tree
    p.lowPiece = new int[NB_PIECE];
    p.lowPosition = new int[NB_PIECE];
    p.highPiece = new int[NB_PIECE];
    p.highPosition = new int[NB_PIECE];
    p.highPiece[0] = NB_PIECE;

    return p;
  }

  /**
   * Private constructor which does not initialize any member, Used for creating
   * the only necessary members when splitting the Pentomino problem
   */
  private Pentomino() {

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
    /*
     * pieces = new PiecePlaced[NB_PIECE]; pieces[0] = new PiecePlaced(new
     * PieceI(w + Board.SENTINEL, h)); pieces[1] = new PiecePlaced(new PieceU(w
     * + Board.SENTINEL, h)); pieces[2] = new PiecePlaced(new PieceT(w +
     * Board.SENTINEL, h)); pieces[3] = new PiecePlaced(new PieceF(w +
     * Board.SENTINEL, h)); pieces[4] = new PiecePlaced(new PieceY(w +
     * Board.SENTINEL, h)); pieces[5] = new PiecePlaced(new PieceZ(w +
     * Board.SENTINEL, h)); pieces[6] = new PiecePlaced(new PieceL(w +
     * Board.SENTINEL, h)); pieces[7] = new PiecePlaced(new PieceN(w +
     * Board.SENTINEL, h)); pieces[8] = new PiecePlaced(new PieceW(w +
     * Board.SENTINEL, h)); pieces[9] = new PiecePlaced(new PieceV(w +
     * Board.SENTINEL, h)); P = new PieceP(w + Board.SENTINEL, h); pieces[10] =
     * new PiecePlaced(P);
     *
     * X = new PieceX(w + Board.SENTINEL, h); pieces[11] = new PiecePlaced(X);
     *
     * stack = new Stack<>();
     *
     * // Prepare the arrays that describe the tree lowPiece = new
     * int[NB_PIECE]; lowPosition = new int[NB_PIECE]; highPiece = new
     * int[NB_PIECE]; highPosition = new int[NB_PIECE];
     */
    depth = -1;
  }

  /**
   * Generates the various placements of PieceX as new Pentomino instances kept
   * in this instance as a reserve.
   */
  public void init() {

    for (int i = 0; i < (height - 1) / 2; i++) {
      for (int j = 0; j < (width - 1) / 2; j++) {
        // Generate Pentomino instances with pieceX in (j,i) coordinates

        final int placementIndex = i * (width + Board.SENTINEL) + j;
        if (placementIndex != 0) {
          final Pentomino p = getTransferPentomino();

          p.pieces[11].index = placementIndex;
          p.pieces[11].variation = 0;
          p.stack.push(p.pieces[11]);
          p.lowPiece[0] = 1;
          p.highPiece[0] = 1;
          p.lowPosition[0] = 1;
          p.highPosition[0] = 1;
          p.lowPiece[1] = 0;
          p.highPiece[1] = 11;
          p.depth = 1;
          // p.board.placeArbitrarily(X, 0, placementIndex);

          // Remove additional symmetry in cases where PieceX is placed on the
          // center column or the center line
          if ((height % 2 == 1 && i + 1 == (height - 1) / 2)
              || (width % 2 == 1 && j == (width - 1) / 2)) {
            p.additionalSymmetryRestriction = true;
          }

          putInReserve(p);
        }
      }
    }
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

    final Pentomino p = new Pentomino(WIDTH, HEIGHT);
    p.init();

    final Pentomino q = new Pentomino(WIDTH, HEIGHT);
    q.merge(p.split(true));

    long duration = System.nanoTime();
    p.toCompletion();
    q.toCompletion();
    duration = System.nanoTime() - duration;

    System.out.println("Total solutions " + p.width + "*" + p.height + "; "
        + (p.solutions + q.solutions));
    System.out.println("Time; " + duration / 1e9);

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

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return depth < 0 && reserve.isEmpty();
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#isSplittable()
   */
  @Override
  public boolean isSplittable() {
    return reserve != null && reserve.size() > 1;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#merge(handist.glb.multiworker.Bag)
   */
  @Override
  public void merge(Pentomino b) {
    if (b.reserve != null) {
      for (final Pentomino p : b.reserve) {
        putInReserve(p);
      }
    } else {
      putInReserve(b);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#process(int, handist.glb.util.Fold)
   */
  @Override
  public void process(int workAmount, Sum sharedObject) {
    while (workAmount > 0 && !isEmpty()) {
      if (depth < 0) {
        takeFromReserve();
      }
      step();
      workAmount--;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#split(boolean)
   */
  @Override
  public Pentomino split(boolean takeAll) {
    Pentomino p = reserve.pop();
    if (p == null) {
      p = new Pentomino(width, depth);
      System.err.println("Meh");
    }

    return p;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#submit(handist.glb.util.Fold)
   */
  @Override
  public void submit(Sum r) {
    r.sum += solutions;
  }

}
