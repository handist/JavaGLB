/**
 *  This file is part of the Handy Tools for Distributed Computing project
 *  HanDist (https://github.com/handist)
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) copyright CS29 Fine 2018-2019.
 */
package handist.glb.examples.pentomino;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import handist.glb.examples.pentomino.Piece.PieceType;
import handist.glb.multiworker.Bag;

/**
 * Main class implementing the search for solutions to the pentomino problem
 * <p>
 * The implementation consists in trying to place a piece that will cover the
 * top-most, left-most tile on the board. If the attempted piece and position
 * fits, we (recursively) continue to try and place pieces on the board. If the
 * chosen piece and combination cannot fit on the board, we try its next
 * position. When we run out of positions to try for a piece, we continue with
 * the next piece. When all the pieces have been tried, we backtrack i.e. we
 * remove the last placed piece and continue the exploration.
 * <p>
 * The technique used to eliminate symmetries in the solutions consists in
 * placing {@link PieceX} arbitrarily at the start of the computation in the
 * upper left quadrant of the board. Moreover, if said place X lies on a center
 * column of a center or line of the board, we remove the flipside positions of
 * {@link PieceP} to eliminate additional symmetries. Using this technique, only
 * the 'unique' solutions to the Pentomino problem are counted.
 *
 * @author Patrick Finnerty
 *
 */
public class Pentomino implements Bag<Pentomino, Answer>, Serializable {

  /**
   * Enumerator for the type of Pentomino being computed
   *
   * @author Patrick Finnerty
   *
   */
  enum PentominoType {
    /**
     * Standard 12 pieces Pentomino
     */
    STANDARD,
    /**
     * 18 pieces Pentomino with the upside-down pieces as pieces rather than
     * variations of a piece
     */
    ONE_SIDED
  };

  /**
   * Tuple containing a Piece, an orientation (or -1 if it isn't placed) and an
   * index at which the piece was placed on the board (only relevant if
   * variation is not -1).
   *
   * @author Patrick Finnerty
   *
   */
  private class PiecePlaced implements Serializable {
    /** Serial version UID */
    private static final long serialVersionUID = 4719358467517194092L;
    /** Variation of the piece that was palced */
    int variation;
    /** index on the board at which said piece was placed */
    int index;

    public PiecePlaced() {
      variation = -1;
      index = -1;
    }

    /**
     * Copy constructor Constructs a PiecePlaced object with the same values for
     * all members
     *
     * @param piecePlaced
     *          the instance of which a copy is needed
     */
    public PiecePlaced(PiecePlaced piecePlaced) {
      index = piecePlaced.index;
      variation = piecePlaced.variation;
    }
  }

  /** Serial Version UID */
  private static final long serialVersionUID = 6033611157974969906L;

  /**
   * Launches a sequential Pentomino exploration with the board of the specified
   * width and height. If no argument is specified, W10H6 is used as the default
   * board.
   *
   * @param args
   *          first argument should be the width of the board, second argument
   *          its height
   */
  public static void main(String[] args) {
    int WIDTH = 10, HEIGHT = 6;
    boolean symmetriesOff = true;
    PentominoType type;
    try {
      WIDTH = Integer.parseInt(args[0]);
      HEIGHT = Integer.parseInt(args[1]);
      symmetriesOff = Boolean.parseBoolean(args[2]);
    } catch (final Exception e) {

      System.err.println("Error parsing arguments <W> <H> <symmetriesRemoval>");
      return;
    }

    final int surface = WIDTH * HEIGHT;
    if (surface == 60) {
      type = PentominoType.STANDARD;
    } else if (surface == 90) {
      type = PentominoType.ONE_SIDED;
    } else {
      System.err.println("Wrong board size: H=" + HEIGHT + " W=" + WIDTH);
      return;
    }

    int[] specificPositions;
    if (args.length > 3) {
      // Parse the additional arguments determining the positions of the X piece
      // to be tried
      final int nbPositions = args.length - 3;
      specificPositions = new int[nbPositions];
      for (int i = 0; i < nbPositions; i++) {
        specificPositions[i] = Integer.parseInt(args[3 + i]);
      }
    } else {
      specificPositions = null;
    }

    final Pentomino p = new Pentomino(type, WIDTH, HEIGHT);
    p.init(type, symmetriesOff, specificPositions);

    long duration = System.nanoTime();
    p.toCompletion();
    duration = System.nanoTime() - duration;

    System.out.print("ARGS: ");
    for (final String s : args) {
      System.out.print(s + " ");
    }
    System.out.println();

    System.out.println(
        "Total solutions " + p.width + "*" + p.height + "; " + p.solutions);
    System.out.println("Tree nodes; " + p.treeNode);
    System.out.println("Time (s); " + duration / 1e9);

  }

  /**
   * Indicates the number of pieces that actually need to be placed by the
   * pentomino algorithm. As we arbitrarily place pieceX at the beginning of the
   * computation, the total number of pieces to place is reduced from 12 to 11.
   */
  public final int NB_PIECE;

  /** Counter for the number of nodes in the search tree */
  transient long treeNode = 0;

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
   * PieceP instance which is kept as a member for easy access when removing
   * variations of the piece when extra symmetries is needed.
   */
  transient PieceV V;

  /**
   * Reserve of different explorations kept aside
   */
  Deque<Pentomino> reserve;

  /**
   * Indicates of the current instance needs additional restrictions on symmetry
   * In the case of {@value PentominoType#ONE_SIDED}, value 0 indicates no need
   * for extra symmetry, -1 indicates need for removal of horizontal symmetries
   * and 1 indicates need for removal of vertical symmetries.
   */
  int additionalSymmetryRestriction = 0;

  /** Indicates what kind of problem this instance is */
  PentominoType pentominoType;

  /**
   * Array containing all the pieces that we try to place on the board
   */
  transient Piece[] pieces;

  /**
   * Array containing the information of the pieces and in which position they
   * were placed. Each element of this array contains information about the
   * piece located in array {@link #pieces} at the same index.
   */
  PiecePlaced[] placement;

  /**
   * Stack containing the indeces of the pieces in array pieces that were placed
   * on the board
   */
  int[] stack;
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
   * Private constructor which does not initialize any member, Used for creating
   * the only necessary members when splitting the Pentomino problem
   *
   * @param t
   *          type of the computation at hand
   */
  public Pentomino(PentominoType t) {
    pentominoType = t;
    reserve = new LinkedList<>();
    switch (t) {
    case STANDARD:
      NB_PIECE = 12;
      break;
    case ONE_SIDED:
      NB_PIECE = 18;
      break;
    default:
      NB_PIECE = 0;
    }
    depth = -1;
  }

  /**
   * Constructor of a pentomino puzzle of the specified size
   *
   * @param type
   *          type of pentoino problem
   *
   * @param w
   *          width of the rectangle in which to fit the pieces
   * @param h
   *          height of the rectangle in which to fit the pieces
   */
  public Pentomino(PentominoType type, int w, int h) {
    pentominoType = type;
    width = w;
    height = h;

    reserve = new LinkedList<>();

    board = new Board(w, h);
    depth = -1;
    switch (type) {
    case STANDARD:
      NB_PIECE = 12;
      break;
    case ONE_SIDED:
      NB_PIECE = 18;
      break;
    default:
      NB_PIECE = 0;
    }

    initPieces(type);
  }

  /**
   * Retrieves the index in array {@link PiecePlaced} of the n'th piece that
   * remains to be placed on the board
   *
   * @param n
   *          the number of the piece among those that remain to be found
   * @return the index of the n'th piece in array {@link #placement} and
   *         {@link #pieces}
   */
  private int getRemaining(int index) {
    PiecePlaced pp;
    int i = 0;
    do {
      pp = placement[i];
      i++;
      if (pp.variation < 0) {
        index--;
      }
    } while (0 < index);
    return i - 1;
  }

  /**
   * Produces a Pentomino instance with the bare minimum of field instanciated
   *
   * @return a pentomino to be used for transfering a partial exploration of the
   *         pentomino problem
   */
  protected Pentomino getInitPentomino() {
    final Pentomino p = new Pentomino(pentominoType);

    p.placement = new PiecePlaced[NB_PIECE];
    for (int i = 0; i < NB_PIECE; i++) {
      p.placement[i] = new PiecePlaced();
    }

    p.stack = new int[NB_PIECE];

    // Prepare the arrays that describe the tree
    p.lowPiece = new int[NB_PIECE];
    p.lowPosition = new int[NB_PIECE];
    p.highPiece = new int[NB_PIECE];
    p.highPosition = new int[NB_PIECE];
    p.highPiece[0] = NB_PIECE;

    return p;
  }

  /**
   * Generates the various placements of PieceX as new Pentomino instances kept
   * in this instance as a reserve.
   *
   * @param type
   *          type of the pentomino at hand
   * @param noSymmetries
   *          indicates if the symmetry removal should be applied or not
   * @param specificPositions
   *          indicates which initial positions of piece X should be included in
   *          the search. Ignored if the symmetries are not removed. If null,
   *          all possible initial positions of Piece X are included
   */
  public void init(PentominoType type, boolean noSymmetries,
      int[] specificPositions) {
    if (noSymmetries) {
      if (type == PentominoType.STANDARD) {
        int nextP = 0; // index in array specificPositions
        int pos = 0; // position available for trial
        for (int i = 0; i < (height - 1) / 2; i++) {
          for (int j = 0; j < (width - 1) / 2; j++) {

            // Generate Pentomino instances with pieceX in (j,i) coordinates

            final int placementIndex = i * (width + Board.SENTINEL) + j;
            if (placementIndex != 0) {
              if (specificPositions == null || (specificPositions != null
                  && nextP < specificPositions.length
                  && specificPositions[nextP] == pos)) {
                final Pentomino p = getInitPentomino();

                final PiecePlaced Xplacement = p.placement[11];

                Xplacement.index = placementIndex + 1;
                Xplacement.variation = 0;
                p.stack[0] = 11;
                p.depth = 1;
                p.lowPiece[0] = 1;
                p.highPiece[0] = 1;
                p.lowPosition[0] = 1;
                p.highPosition[0] = 1;
                p.lowPiece[1] = 0;
                p.highPiece[1] = 11;

                // Remove additional symmetry in cases where PieceX is placed on
                // the
                // center column or the center line
                if ((height % 2 == 1 && i + 1 == (height - 1) / 2)
                    || (width % 2 == 1 && j + 1 == (width - 1) / 2)) {
                  p.additionalSymmetryRestriction = 1;
                }

                putInReserve(p);
                nextP++;
              }
              pos++;
            }
          }
        }

      } else if (type == PentominoType.ONE_SIDED) {
        int nextP = 0; // index in array specificPositions
        int pos = 0; // position available for trial
        for (int i = 0; i < (height - 1) / 2; i++) {
          for (int j = 0; j < (width - 1) / 2; j++) {
            // Generate Pentomino instances with pieceX in (j,i) coordinates

            final int placementIndex = i * (width + Board.SENTINEL) + j;
            if (placementIndex != 0) {
              if (specificPositions == null || (specificPositions != null
                  && nextP < specificPositions.length
                  && specificPositions[nextP] == pos)) {
                final Pentomino p = getInitPentomino();

                final PiecePlaced Xplacement = p.placement[2];

                Xplacement.index = placementIndex + 1;
                Xplacement.variation = 0;
                p.stack[0] = 2;
                p.depth = 1;
                p.lowPiece[0] = 1;
                p.highPiece[0] = 1;
                p.lowPosition[0] = 1;
                p.highPosition[0] = 1;
                p.lowPiece[1] = 0;
                p.highPiece[1] = 17;

                // Remove additional symmetry in cases where PieceX is placed on
                // the center column or the center line, not that the actual
                // symmetry removal is made when taking a subproblem from the
                // reserve.
                if (height % 2 == 1 && i + 1 == (height - 1) / 2) {
                  // Horizontal symmetry needs to be removed
                  p.additionalSymmetryRestriction = 1;
                } else if ((width % 2 == 1 && j + 1 == (width - 1) / 2)) {
                  // Vertical symmetry needs to be removed
                  p.additionalSymmetryRestriction = -1;
                } else {
                  p.additionalSymmetryRestriction = 0;
                }

                putInReserve(p);
                nextP++;
              }
              pos++;
            }
          }
        }

      }
    } else {
      // Does the full exploration
      final Pentomino p = getInitPentomino();
      p.highPiece[0] = NB_PIECE;
      p.additionalSymmetryRestriction = 0;
      putInReserve(p);
    }
  }

  /**
   * Initializes the {@link #pieces} array with the pieces that are relevant to
   * the pentomino problem
   *
   * @param type
   *          type of the pentomino to build
   */
  private void initPieces(PentominoType type) {
    pieces = new Piece[NB_PIECE]; // NB_PIECE is assumed to be set to the
                                  // correct value
    switch (type) {
    case STANDARD:
      pieces[0] = new PieceI(width + Board.SENTINEL, height);
      pieces[1] = new PieceU(width + Board.SENTINEL);
      pieces[2] = new PieceT(width + Board.SENTINEL);
      pieces[3] = new PieceF(PieceType.STANDARD, width + Board.SENTINEL);
      pieces[4] = new PieceY(PieceType.STANDARD, width + Board.SENTINEL,
          height);
      pieces[5] = new PieceZ(PieceType.STANDARD, width + Board.SENTINEL);
      pieces[6] = new PieceL(PieceType.STANDARD, width + Board.SENTINEL,
          height);
      pieces[7] = new PieceN(PieceType.STANDARD, width + Board.SENTINEL,
          height);
      pieces[8] = new PieceW(width + Board.SENTINEL);
      pieces[9] = new PieceV(width + Board.SENTINEL);
      P = new PieceP(PieceType.STANDARD, width + Board.SENTINEL);
      pieces[10] = P;
      pieces[11] = new PieceX(width + Board.SENTINEL);
      break;
    case ONE_SIDED:
      // Symmetric pieces wrt flip
      pieces[0] = new PieceI(width + Board.SENTINEL, height);
      pieces[1] = new PieceU(width + Board.SENTINEL);
      pieces[2] = new PieceX(width + Board.SENTINEL);
      pieces[3] = new PieceT(width + Board.SENTINEL);
      pieces[4] = new PieceW(width + Board.SENTINEL);
      V = new PieceV(width + Board.SENTINEL);
      pieces[5] = V;

      // upside pieces
      pieces[6] = new PieceP(PieceType.UPSIDE, width + Board.SENTINEL);
      pieces[7] = new PieceF(PieceType.UPSIDE, width + Board.SENTINEL);
      pieces[8] = new PieceY(PieceType.UPSIDE, width + Board.SENTINEL, height);
      pieces[9] = new PieceZ(PieceType.UPSIDE, width + Board.SENTINEL);
      pieces[10] = new PieceL(PieceType.UPSIDE, width + Board.SENTINEL, height);
      pieces[11] = new PieceN(PieceType.UPSIDE, width + Board.SENTINEL, height);

      // flipside pieces
      pieces[12] = new PieceF(PieceType.FLIPSIDE, width + Board.SENTINEL);
      pieces[13] = new PieceY(PieceType.FLIPSIDE, width + Board.SENTINEL,
          height);
      pieces[14] = new PieceZ(PieceType.FLIPSIDE, width + Board.SENTINEL);
      pieces[15] = new PieceL(PieceType.FLIPSIDE, width + Board.SENTINEL,
          height);
      pieces[16] = new PieceN(PieceType.FLIPSIDE, width + Board.SENTINEL,
          height);
      pieces[17] = new PieceP(PieceType.FLIPSIDE, width + Board.SENTINEL);
      break;
    default:
      break;

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
    if (reserve != null && reserve.size() >= 2) {
      return true;
    } else {
      return (lowPiece != null && treeSplittable());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#merge(handist.glb.multiworker.Bag)
   */
  @Override
  public void merge(Pentomino b) {
    reserve.addAll(b.reserve);
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
   * Returns the number of pieces left to explore at the specified index
   *
   * @param index
   *          level of exploration in the tree to check
   * @return the number of pieces left to explore at the specified level
   */
  private int piecesLeft(int index) {
    return highPiece[index] - lowPiece[index];
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
   * Returns the number of positions left to explore the piece located at the
   * specified level in the exploration tree
   *
   * @param index
   *          tree depth to check
   * @return the number of positions left to try for the piece at the pecified
   *         level of the tree exploration
   */
  private int positionsLeft(int index) {
    return highPosition[index] - lowPosition[index];
  }

  /**
   * Prints the current stack status
   */
  public void printStack() {
    String s = "Stack:" + depth + "[";
    for (int i = 0; i < depth; i++) {
      final PiecePlaced pp = placement[stack[i]];
      s += pp + " ";
    }
    System.out.println(s);
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#process(int, handist.glb.util.Fold)
   */
  @Override
  public void process(int workAmount, Answer sharedObject) {
    while (workAmount > 0 && !isEmpty()) {
      if (depth < 0) {
        takeFromReserve();
      }
      step();
      workAmount--;
    }
  }

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

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#split(boolean)
   */
  @Override
  public Pentomino split(boolean takeAll) {
    final Pentomino toReturn = new Pentomino(pentominoType);

    if (reserve.size() >= 2 || takeAll) {
      int qtt = (reserve.size() + 1) / 2;
      while (qtt > 0) {
        toReturn.reserve.addLast(reserve.pollFirst());
        qtt--;
      }
    } else {
      // We need to split the current exploration tree
      final Pentomino p = new Pentomino(pentominoType);
      p.depth = depth;

      // Copy the stack
      p.stack = Arrays.copyOf(stack, NB_PIECE);
      // Copy the placement of the pieces
      p.placement = new PiecePlaced[NB_PIECE];
      for (int i = 0; i < NB_PIECE; i++) {
        p.placement[i] = new PiecePlaced(placement[i]);
      }
      p.highPiece = Arrays.copyOf(highPiece, NB_PIECE);
      p.lowPiece = Arrays.copyOf(lowPiece, NB_PIECE);
      p.highPosition = Arrays.copyOf(highPosition, NB_PIECE);
      p.lowPosition = Arrays.copyOf(lowPosition, NB_PIECE);
      for (int i = 0; i <= depth; i++) {
        final int piecesLeft = piecesLeft(i);
        final int positionsLeft = positionsLeft(i);
        if (piecesLeft >= 1) {
          if (takeAll && piecesLeft == 1) {
            lowPiece[i]++;
          } else {
            p.lowPiece[i] = highPiece[i] -= piecesLeft / 2;
          }
          p.lowPosition[i] = p.highPosition[i];
        } else {
          // We cannot split by giving away pieces
          // Give half the final remaining positions
          if (positionsLeft == 1 && takeAll) {
            lowPosition[i]++;
          } else {
            p.lowPosition[i] = highPosition[i] -= positionsLeft / 2;
          }
        }
      }

      p.additionalSymmetryRestriction = additionalSymmetryRestriction;
      toReturn.reserve.addLast(p);
    }

    return toReturn;
  }

  /**
   * Performs one step in the pentomino search
   */
  public void step() {
    if (positionsLeft() > 0) {
      treeNode++;
      // We try a new position of the current piece

      // Select the current piece and the position to try
      final int ppIndex = getRemaining(lowPiece[depth]);
      final PiecePlaced pp = placement[ppIndex];
      final Piece piece = pieces[ppIndex];
      final int position = lowPosition[depth];
      final int index = board.nextIndex;
      lowPosition[depth]++;

      if (board.placePiece(piece, position)) {
        // place p has been placed, we remove it from the pieces left to place
        // and add it to the stack
        pp.variation = position;
        pp.index = index;
        stack[depth] = ppIndex;
        depth++;
        if (depth == NB_PIECE) {
          // We found a solution !
          // System.out.println(board);
          // printStack();
          solutions++;

          // We need to backtrack, removing the last 2 pieces
          depth--;
          board.removePiece(piece, pp.variation, pp.index);
          pp.variation = -1;

          depth--;
          final int oneButLastIndex = stack[depth];
          final PiecePlaced oneButLast = placement[oneButLastIndex];
          final Piece oneButLastPiece = pieces[oneButLastIndex];
          board.removePiece(oneButLastPiece, oneButLast.variation,
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
      final Piece p = pieces[getRemaining(lowPiece[depth])];

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
      final int lastPiecePlacedIndex = stack[depth];
      final PiecePlaced pp = placement[lastPiecePlacedIndex];
      final Piece pieceToRemove = pieces[lastPiecePlacedIndex];
      board.removePiece(pieceToRemove, pp.variation, pp.index);
      pp.variation = -1;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#submit(handist.glb.util.Fold)
   */
  @Override
  public void submit(Answer r) {
    r.solutions += solutions;
    r.nodes += treeNode;
  }

  /**
   * Discards the current exploration and replaces it with the last added
   * exploration available in reserve
   */
  public void takeFromReserve() {
    final Pentomino p = reserve.pop();

    placement = p.placement;
    stack = p.stack;
    depth = p.depth;
    lowPiece = p.lowPiece;
    highPiece = p.highPiece;
    highPosition = p.highPosition;
    lowPosition = p.lowPosition;
    additionalSymmetryRestriction = p.additionalSymmetryRestriction;
    // Reconstitutes the board in the state p was
    board.clear();
    for (int i = 0; i < depth; i++) {
      final PiecePlaced pp = placement[stack[i]];
      final Piece pieceToPlace = pieces[stack[i]];
      board.placeArbitrarily(pieceToPlace, pp.variation, pp.index);
    }

    if (pentominoType == PentominoType.STANDARD) {
      if (additionalSymmetryRestriction == 1) {
        P.vars = 4;
      } else {
        P.vars = 8;
      }
    } else if (pentominoType == PentominoType.ONE_SIDED) {
      if (additionalSymmetryRestriction > 0) {
        V.removeHorizontalSymmetry();
      } else if (additionalSymmetryRestriction < 0) {
        V.removeVerticalSymmetry();
      } else {
        V.reset();
      }
    }
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
   * @return
   */
  private boolean treeSplittable() {
    for (int i = 0; i <= depth; i++) {
      final int pieces = piecesLeft(i);
      final int positions = positionsLeft(i);

      if (pieces >= 2 || positions >= 2) {
        return true;
      }
    }

    return false;
  }

}
