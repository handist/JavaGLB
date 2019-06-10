/**
 *
 */
package handist.glb.examples.pentomino;

/**
 * @author Patrick Finnerty
 *
 */
public class PieceL extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth, fifth, sixth, seventh, eigth;

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#variations()
   */
  @Override
  public int variations() {
    return 8;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'L';
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
    case 4:
      return fifth;
    case 5:
      return sixth;
    case 6:
      return seventh;
    case 7:
      return eigth;
    default:
      return null;
    }
  }

  /**
   * Builds the L piece with its variations
   *
   * @param width
   *          width of the board played
   * @param height
   *          height of the board played
   */
  public PieceL(int width, int height) {
    final int[] f = { 0, 1, 2, 3, width + 3 };
    final int[] s = { 0, 1, width, 2 * width, 3 * width };

    final int[] t = { 0, width, width + 1, width + 2, width + 3 };
    final int[] fo = { 1, width + 1, 2 * width + 1, 3 * width, 3 * width + 1 };
    final int[] fi = { 3, width, width + 1, width + 2, width + 3 };
    final int[] si = { 0, width, 2 * width, 3 * width, (3 * width) + 1 };
    final int[] se = { 0, 1, 2, 3, width };
    final int[] e = { 0, 1, width + 1, (2 * width) + 1, (3 * width) + 1 };

    first = f;
    second = s;
    third = t;
    fourth = fo;
    fifth = fi;
    sixth = si;
    seventh = se;
    eigth = e;
  }

  /**
   * Displays the piece variation
   *
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    new PieceL(10, 6).printVariations(10);
  }
}
