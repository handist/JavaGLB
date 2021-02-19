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
package handist.glb.examples.uts;

import java.io.Serializable;

import handist.glb.util.Fold;

/**
 *
 * Implementation of the {@link Fold} interface that performs the addition on
 * {@code long} integers. The class also implements interface
 * {@link Serializable} in order to be used by the GLB library.
 *
 * @author Patrick Finnerty
 *
 */
public class Sum implements Fold<Sum>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 3582168956043482749L;

    /** Integer in which the sum is performed */
    public long sum;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Fold#fold(apgas.glb.Fold)
     */
    @Override
    public void fold(Sum f) {
        sum += f.sum;
    }

    /**
     * Constructor
     *
     * @param s
     *            initial value for the sum
     */
    public Sum(long s) {
        sum = s;
    }

}
