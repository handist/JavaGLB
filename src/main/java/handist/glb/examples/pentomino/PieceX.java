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
 * X pentomino piece
 *
 * @author Patrick Finnerty
 *
 */
public class PieceX extends Piece {

    /** Serial version UID */
    private static final long serialVersionUID = -5244591650743453450L;

    /**
     * Displays the piece's variations
     *
     * @param args
     *            no argument
     */
    public static void main(String[] args) {
        new PieceX(10).printVariations(10);
    }

    /** Variations of this piece */
    int[] first;

    /**
     * Builds the F piece with its variations
     *
     * @param width
     *            width of the board played
     */
    public PieceX(int width) {
        final int[] f = { 1, width, width + 1, width + 2, 2 * width + 1 };

        first = f;

    }

    /*
     * (non-Javadoc)
     *
     * @see handist.glb.examples.pentomino.Piece#getChar()
     */
    @Override
    public char getChar() {
        return 'X';
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
        return 1;
    }
}
