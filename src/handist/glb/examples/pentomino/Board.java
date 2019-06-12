/**
 *
 */
package handist.glb.examples.pentomino;

import java.util.Arrays;

/**
 * @author Patrick Finnerty
 *
 */
public class Board {

  /** Character used in the board array to signal that a tile is empty */
  public static final char EMPTY = ' ';

  /** Margin to the right side of the board */
  public static final int SENTINEL = 4;

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
    final char c = p.getChar();

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
        board[tileIndex] = c;
      }
    }

    while (nextIndex < board.length && board[nextIndex] != EMPTY) {
      nextIndex++;
    }

    return true;
  }

  /**
   * Removes the given last piece from the board
   *
   * @param p
   *          the last piece that was successfully placed on the board by
   *          calling {@link #placePiece(Piece, int)}.
   * @param variation
   *          orientation in which the last piece was placed
   * @param index
   *          index in the array at which the piece was placed
   */
  public void removePiece(Piece p, int variation, int index) {
    final int[] pieceIndeces = p.getVariation(variation);
    nextIndex = index;
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

  }

  /**
   * Removes all the pieces from the board, restoring the instance to the
   * condition it was in when created.
   */
  public void clear() {
    Arrays.fill(board, EMPTY);
    for (int i = 0; i < height; i++) {
      final int sentinelStart = i * (width + SENTINEL) + width;
      final int sentinelStop = i * (width + SENTINEL) + SENTINEL + width;
      Arrays.fill(board, sentinelStart, sentinelStop, 's');
    }
    Arrays.fill(board, (width + SENTINEL) * height, board.length, 's');
    nextIndex = 0;
  }

  /**
   * Places a piece arbitrarily on the board at the specified index. No checks
   * are made on the validity of such placement. It is assumed to be valid.
   *
   * @param piece
   *          The piece to place on the board
   * @param variation
   *          the orientation of the piece to place
   * @param index
   *          the index at which the piece needs to be placed in array
   *          {@link #board}
   */
  public void placeArbitrarily(Piece piece, int variation, int index) {
    final int[] toPlace = piece.getVariation(variation);
    final char c = piece.getChar();

    for (int i = 0; i < toPlace.length; i++) {
      final int tileIndex = index + toPlace[i];
      board[tileIndex] = c;
    }

    while (nextIndex < board.length && board[nextIndex] != EMPTY) {
      nextIndex++;
    }
  }

}
