/**
 *
 */
package handist.glb.examples.pentomino;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Stack;

import handist.glb.multiworker.Bag;

/**
 * @author Patrick Finnerty
 *
 */
public class Board implements Bag<Board, Answer>, Serializable {

  /** Serial version UID */
  private static final long serialVersionUID = 9003560227802428650L;

  /** Character used in the board array to signal that a tile is empty */
  public static final char EMPTY = ' ';

  /** Margin to the right side of the board */
  public static final int SENTINEL = 4;

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
  int solutions = 0;

  /**
   * Array containing all the pieces and whether they are placed on the board or
   * still available
   */
  PiecePlaced[] pieces;

  /**
   * Stack containing the pieces that were placed on the board
   */
  Stack<PiecePlaced> piecesPlaced;

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
   * 1D array containing the lines of the board with some sentinel
   */
  char board[];

  /** Index of the next empty tile on the board */
  int nextIndex;

  /** Width of the board on which the pentomino pieces need to be placed */
  int width;
  /** Height of the board on which the pentomino pieces need to be placed */
  int height;

  /**
   * Prints the current stack status
   */
  public void printStack() {
    String s = "Stack:" + piecesPlaced.size() + "[";
    for (final PiecePlaced pp : piecesPlaced) {
      s += pp + " ";
    }
    System.out.println(s);
  }

  /**
   * Runs the pentomino exploration to completion
   */
  public void toCompletion() {
    while (!isEmpty()) {
      step();
      // printStack();
    }
  }

  /**
   * Formats the board into a several line String fit for display on a terminal
   *
   * @return String presenting the board
   */
  public String boardToString() {
    String s = "";
    for (int i = 0; i < height + 1; i++) {
      for (int j = 0; j < width + SENTINEL; j++) {
        s += board[i * (width + SENTINEL) + j];
      }
      s += "\r\n";
    }

    return s;
  }

  /**
   * Attempts to place the given piece in the given orientation on the board,
   * filling the topmost, leftmost tile of the board
   *
   * @param p
   *          the piece to place
   * @param variation
   *          the variation of piece p to place
   * @return {@value true} if the piece can be placed on the board at the
   *         top/left-most position, false otherwise
   */
  public boolean placePiece(Piece p, int variation) {
    final int[] toPlace = p.getVariation(variation);

    /** See if the piece can fit on the board */
    final int shift = toPlace[0]; // the top left tile of the piece may not be
                                  // described with 0, in which case the whole
                                  // piece needs to be translated towards the
                                  // left.
    for (int i = 0; i < toPlace.length; i++) {
      final int tileIndex = nextIndex + toPlace[i] - shift;

      if (board[tileIndex] != EMPTY) {
        // The piece cannot be placed here, restore the board and return false
        for (int j = i - 1; 0 <= j; j--) {
          board[nextIndex + toPlace[j] - shift] = EMPTY;
        }
        return false;
      } else {
        // The current tile if fine, we color the board accordingly
        board[tileIndex] = p.getChar();
      }
    }

    while (nextIndex < board.length && board[nextIndex] != EMPTY) {
      nextIndex++;
    }
    return true;
  }

  /**
   * Removes the last placed piece from the board
   *
   * @param p
   *          the piece that was last placed on the board
   * @param variation
   *          variation from the piece that was placed on the board
   * @param placedIndex
   *          index at which the piece was placed on the board
   */
  public void removePiece(Piece p, int variation, int placedIndex) {
    final int[] pieceIndeces = p.getVariation(variation);
    nextIndex = placedIndex;
    final int shift = pieceIndeces[0];
    for (final int toEmpty : pieceIndeces) {
      board[nextIndex - shift + toEmpty] = EMPTY;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return boardToString();
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return depth < 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#isSplittable()
   */
  @Override
  public boolean isSplittable() {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#merge(handist.glb.multiworker.Bag)
   */
  @Override
  public void merge(Board b) {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#process(int, handist.glb.util.Fold)
   */
  @Override
  public void process(int workAmount, Answer sharedObject) {
    while (0 <= depth && 0 < workAmount) {
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
  public Board split(boolean takeAll) {
    // TODO Auto-generated method stub
    return null;
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
      if (pp.placed < 0) {
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
      final int index = nextIndex;
      lowPosition[depth]++;

      if (placePiece(pp.piece, position)) {
        depth++;
        // place p has been placed, we remove it from the pieces left to place
        // and add it to the stack
        pp.placed = position;
        pp.index = index;
        piecesPlaced.add(pp);
        if (depth == NB_PIECE) {
          // We found a solution !
          // System.out.println(this);
          // printStack();
          solutions++;

          // We need to backtrack, removing the last 2 pieces
          depth--;
          piecesPlaced.pop();
          removePiece(pp.piece, pp.placed, pp.index);
          pp.placed = -1;

          depth--;
          final PiecePlaced oneButLast = piecesPlaced.pop();
          removePiece(oneButLast.piece, oneButLast.placed, oneButLast.index);
          oneButLast.placed = -1;

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
      final PiecePlaced pp = piecesPlaced.pop();
      removePiece(pp.piece, pp.placed, pp.index);
      pp.placed = -1;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#submit(handist.glb.util.Fold)
   */
  @Override
  public void submit(Answer r) {
    // TODO Auto-generated method stub
  }

  /**
   * Initializes a board with the given size. The product of the width and
   * height should be equal to 60.
   *
   * @param w
   *          width of the pentomino board
   * @param h
   *          height of the pentomino board
   */
  public Board(int w, int h) {
    // Prepare the board
    width = w;
    height = h;
    board = new char[width * height + SENTINEL * height + width + SENTINEL];
    Arrays.fill(board, EMPTY);
    for (int i = 0; i < height; i++) {
      final int sentinelStart = i * (width + SENTINEL) + width;
      final int sentinelStop = i * (width + SENTINEL) + SENTINEL + width;
      Arrays.fill(board, sentinelStart, sentinelStop, 's');
    }
    Arrays.fill(board, (width + SENTINEL) * height, board.length, 's');

    // Create the pieces
    pieces = new PiecePlaced[NB_PIECE];
    pieces[0] = new PiecePlaced(new PieceI(w + SENTINEL, h));
    pieces[1] = new PiecePlaced(new PieceU(w + SENTINEL, h));
    pieces[2] = new PiecePlaced(new PieceT(w + SENTINEL, h));
    pieces[3] = new PiecePlaced(new PieceF(w + SENTINEL, h));
    pieces[4] = new PiecePlaced(new PieceY(w + SENTINEL, h));
    pieces[5] = new PiecePlaced(new PieceZ(w + SENTINEL, h));
    pieces[6] = new PiecePlaced(new PieceL(w + SENTINEL, h));
    pieces[7] = new PiecePlaced(new PieceN(w + SENTINEL, h));
    pieces[8] = new PiecePlaced(new PieceW(w + SENTINEL, h));
    pieces[9] = new PiecePlaced(new PieceV(w + SENTINEL, h));
    pieces[10] = new PiecePlaced(new PieceP(w + SENTINEL, h));
    // pieces[11] = new PiecePlaced(new PieceX(w + SENTINEL, h));

    Arrays.sort(pieces); // Not necessary but ...

    piecesPlaced = new Stack<>();

    // Prepare the arrays that describe the tree
    lowPiece = new int[NB_PIECE];
    lowPosition = new int[NB_PIECE];
    highPiece = new int[NB_PIECE];
    highPosition = new int[NB_PIECE];
    highPiece[0] = NB_PIECE;
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

    long durationSum = 0;
    int solutionSum = 0;

    Board b = new Board(WIDTH, HEIGHT);
    final PieceX X = new PieceX(WIDTH + SENTINEL, HEIGHT);

    for (int i = 0; i < (HEIGHT - 1) / 2; i++) {
      for (int j = 0; j < (WIDTH + 1) / 2; j++) {
        // Place pieceX in (j,i) coordinates
        final int placementIndex = i * (WIDTH + SENTINEL) + j;
        b.nextIndex = placementIndex;
        if (placementIndex != 1 && b.placePiece(X, 0)) {

          // Remove additional symmetry in cases where PieceX is placed on the
          // center axis
          if ((HEIGHT % 2 == 1 && i + 1 == (HEIGHT - 1) / 2)
              || (WIDTH % 2 == 1 && j == (WIDTH - 1) / 2)) {
            System.err.println(
                "Additional symmetry removal for (" + j + "," + i + ")");
            // Remove variations of PieceP
            for (int p = 0; p < b.pieces.length; p++) {
              if (b.pieces[p].piece instanceof PieceP) {
                ((PieceP) b.pieces[p].piece).vars = 4;
              }
            }
          }
          b.nextIndex = 0;

          long duration = System.nanoTime();
          b.toCompletion();
          duration = System.nanoTime() - duration;
          System.out.println("(" + j + "," + i + ") Total solutions: "
              + b.solutions + ":Elapsed time(s): " + duration / 1e9);
          durationSum += duration;
          solutionSum += b.solutions;

          b = new Board(WIDTH, HEIGHT);
        }
      }
    }

    System.out.println(
        "Total solutions " + WIDTH + "*" + HEIGHT + "; " + solutionSum);
    System.out.println("Time; " + durationSum / 1e9);
  }

  /**
   * Pair of a Piece and a boolean value
   *
   * @author Patrick Finnerty
   *
   */
  private class PiecePlaced implements Comparable<PiecePlaced>, Serializable {
    /** Serial version UID */
    private static final long serialVersionUID = 4719358467517194092L;
    int placed;
    Piece piece;
    int index;

    public PiecePlaced(Piece p) {
      piece = p;
      placed = -1;
      index = -1;
    }

    @Override
    public String toString() {
      return "" + piece + placed + "/" + piece.variations();
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
