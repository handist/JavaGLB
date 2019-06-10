/**
 *
 */
package handist.glb.examples.pentomino;

/**
 * @author Patrick Finnerty
 *
 */
public class PieceV extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth;

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#variations()
   */
  @Override
  public int variations() {
    return 4;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'V';
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
    case 2:
      return third;
    case 3:
      return fourth;
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
  public PieceV(int width, int height) {
    final int[] f = { 0, 1, 2, width, 2 * width };
    final int[] s = { 0, width, 2 * width, 2 * width + 1, 2 * width + 2 };

    final int[] t = { 2, width + 2, 2 * width, 2 * width + 1, 2 * width + 2 };
    final int[] fo = { 0, 1, 2, width + 2, 2 * width + 2 };

    first = f;
    second = s;
    third = t;
    fourth = fo;

  }

  /**
   * Displays the piece variation
   *
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    new PieceV(10, 6).printVariations(10);
  }
}
