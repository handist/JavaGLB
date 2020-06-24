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
 * I pentomino piece
 *
 * @author Patrick Finnerty
 *
 */
public class PieceI extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece's variations
   *
   * @param args
   *          no argument
   */
  public static void main(String[] args) {
    System.out.println("Regular board");
    new PieceI(10).printVariations(10);
    System.out.println("Shallow board");
    new PieceI(15).printVariations(15);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second;

  /** Variations of the piece I */
  int vars = 2;

  /**
   * Builds the F piece with its variations
   *
   * @param width
   *          width of the board played, including sentinels
   */
  public PieceI(int width) {
    final int[] h = { 0, 1, 2, 3, 4 };
    final int[] v = { 0, width, 2 * width, 3 * width, 4 * width };

    first = h;
    second = v;
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

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#variations()
   */
  @Override
  public int variations() {
    return vars;
  }
}
