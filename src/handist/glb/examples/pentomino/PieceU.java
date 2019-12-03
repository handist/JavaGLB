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

/**
 * U pentomino piece
 *
 * @author Patrick Finnerty
 *
 */
public class PieceU extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece's variations
   *
   * @param args
   *          no argument
   */
  public static void main(String[] args) {
    new PieceU(10).printVariations(10);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth;

  /**
   * Builds the F piece with its variations
   *
   * @param width
   *          width of the board played
   */
  public PieceU(int width) {
    final int[] f = { 0, 2, width, width + 1, width + 2 };
    final int[] s = { 0, 1, width + 1, 2 * width, 2 * width + 1 };

    final int[] t = { 0, 1, 2, width, width + 2 };
    final int[] fo = { 0, 1, width, 2 * width, 2 * width + 1 };

    first = f;
    second = s;
    third = t;
    fourth = fo;

  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'U';
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

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#variations()
   */
  @Override
  public int variations() {
    return 4;
  }
}
