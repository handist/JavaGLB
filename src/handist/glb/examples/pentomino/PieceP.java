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
 * P pentomino
 * 
 * @author Patrick Finnerty
 *
 */
public class PieceP extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece variation
   *
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    new PieceP(10, 6).printVariations(10);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth, fifth, sixth, seventh, eigth;

  /**
   * Can be modified to restrict the variations of piece P, removing potential
   * symmetries in pentomino solutions.
   */
  int vars = 8;

  /**
   * Builds the F piece with its variations
   *
   * @param width
   *          width of the board played
   * @param height
   *          height of the board played
   */
  public PieceP(int width, int height) {
    final int[] f = { 0, 1, width, width + 1, 2 * width };
    final int[] s = { 0, 1, width, width + 1, width + 2 };

    final int[] t = { 1, width, width + 1, 2 * width, 2 * width + 1 };
    final int[] fo = { 0, 1, 2, width + 1, width + 2 };
    final int[] fi = { 0, 1, width, width + 1, 2 * width + 1 };
    final int[] si = { 1, 2, width, width + 1, width + 2 };
    final int[] se = { 0, width, width + 1, 2 * width, 2 * width + 1 };
    final int[] e = { 0, 1, 2, width, width + 1 };

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
   * Copy constructor
   *
   * @param p
   *          instance to copy
   */
  public PieceP(PieceP p) {
    final int length = p.first.length;
    first = Arrays.copyOf(p.first, length);
    second = Arrays.copyOf(p.second, length);
    third = Arrays.copyOf(p.third, length);
    fourth = Arrays.copyOf(p.fourth, length);
    fifth = Arrays.copyOf(p.fifth, length);
    sixth = Arrays.copyOf(p.sixth, length);
    seventh = Arrays.copyOf(p.seventh, length);
    eigth = Arrays.copyOf(p.eigth, length);
    vars = p.vars;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#copy()
   */
  @Override
  public Piece copy() {
    return new PieceP(this);
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'P';
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
