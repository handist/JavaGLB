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

import java.util.Arrays;

/**
 * V pentomino
 *
 * @author Patrick Finnerty
 *
 */
public class PieceV extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece variation
   *
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    new PieceV(10, 6).printVariations(10);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth;

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
   * Copy constructor
   *
   * @param v
   *          instance to copy
   */
  public PieceV(PieceV v) {
    final int length = v.first.length;
    first = Arrays.copyOf(v.first, length);
    second = Arrays.copyOf(v.second, length);
    third = Arrays.copyOf(v.third, length);
    fourth = Arrays.copyOf(v.fourth, length);
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#copy()
   */
  @Override
  public Piece copy() {
    return new PieceV(this);
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
