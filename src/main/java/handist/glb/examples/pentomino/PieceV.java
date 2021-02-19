/*******************************************************************************
 * This file is part of the Handy Tools for Distributed Computing project
 * HanDist (https:/github.com/handist)
 *
 * This file is licensed to You under the Eclipse Public License (EPL);
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 	https://www.opensource.org/licenses/eclipse-1.0.php
 *
 * (C) copyright CS29 Fine 2018-2021
 ******************************************************************************/
package handist.glb.examples.pentomino;

/**
 * V pentomino piece
 *
 * @author Patrick Finnerty
 *
 */
public class PieceV extends Piece {

    /** Serial version UID */
    private static final long serialVersionUID = -5244591650743453450L;

    /**
     * Displays the piece's variations
     *
     * @param args
     *            no argument
     */
    public static void main(String[] args) {
        final PieceV V = new PieceV(10);
        V.printVariations(10);
        V.removeHorizontalSymmetry();
        V.printVariations(10);
        V.removeVerticalSymmetry();
        V.printVariations(10);
        V.reset();
        V.printVariations(10);
    }

    /** Variations of this piece: pointing in the upper left corner */
    final int[] UL;
    /** Variations of this piece: pointing in the downward left corner */
    final int[] DL;
    /** Variations of this piece: pointing in the upper right corner */
    final int[] UR;
    /** Variations of this piece: pointing in the downward right corner */
    final int[] DR;

    /**
     * First variation of the V piece actually used in the Pentomino exploration
     */
    int[] first;
    /**
     * Second variation of the V piece actually used in the Pentomino
     * exploration
     */
    int[] second;
    /**
     * Third variation of the V piece actually used in the Pentomino
     * exploration. May actually be unused if symmetry elimination is desired
     * for a particular exploration.
     *
     * @see #removeHorizontalSymmetry()
     * @see PieceV#removeVerticalSymmetry()
     */
    int[] third;
    /**
     * Fourth variation of the V piece actually used in the Pentomino
     * exploration. May actually be unused if symmetry elimination is desired
     * for a particular exploration.
     *
     * @see #removeHorizontalSymmetry()
     * @see PieceV#removeVerticalSymmetry()
     */
    int[] fourth;

    /** Number of variations of the piece */
    int vars = 4;

    /**
     * Builds the F piece with its variations
     *
     * @param width
     *            width of the board played
     */
    public PieceV(int width) {
        final int[] f = { 0, 1, 2, width, 2 * width };
        final int[] s = { 0, width, 2 * width, 2 * width + 1, 2 * width + 2 };

        final int[] t = { 0, 1, 2, width + 2, 2 * width + 2 };
        final int[] fo = { 2, width + 2, 2 * width, 2 * width + 1,
                2 * width + 2 };

        UL = f;
        DL = s;
        UR = t;
        DR = fo;
        first = UL;
        second = DL;
        third = UR;
        fourth = DR;

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
        return vars;
    }

    /**
     * Removes symmetries of the piece along the vertical axis
     */
    public void removeVerticalSymmetry() {
        first = UL;
        second = DL;
        vars = 2;
    }

    /**
     * Removes symmetries of the piece along the horizontal axis
     */
    public void removeHorizontalSymmetry() {
        vars = 2;
        first = UL;
        second = UR;
    }

    /**
     * Puts all the variations back into the piece after a potential call to
     * method {@link #removeHorizontalSymmetry()} or
     * {@link #removeVerticalSymmetry()}.
     */
    public void reset() {
        first = UL;
        second = DL;
        third = UR;
        fourth = DR;

        vars = 4;
    }
}
