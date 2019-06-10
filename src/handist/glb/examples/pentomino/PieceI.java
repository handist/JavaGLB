/**
 *
 */
package handist.glb.examples.pentomino;

/**
 * @author Patrick Finnerty
 *
 */
public class PieceI extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second;

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#variations()
   */
  @Override
  public int variations() {
    return 2;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'I';
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getVariation(int)
   */
  @Override
  public int[] getVariation(int nb) {
    switch (nb) {
    case 0:
      return first;
    case 1:
      return second;
    default:
      return null;
    }
  }

  /**
   * Builds the F piece with its variations
   *
   * @param width
   *          width of the board played
   * @param height
   *          height of the board played
   */
  public PieceI(int width, int height) {
    final int[] f = { 0, 1, 2, 3, 4 };
    final int[] s = { 0, width, 2 * width, 3 * width, 4 * width };

    first = f;
    second = s;

  }

  /**
   * Displays the piece variation
   *
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    new PieceI(10, 6).printVariations(10);
  }
}
