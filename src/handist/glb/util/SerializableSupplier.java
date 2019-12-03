/*
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
package handist.glb.util;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Serializable {@link Supplier} interface. Used to lift some type inference
 * issues that occur with our library depending on the compiler used.
 *
 *
 * @author Patrick Finnerty
 * @param <T>
 *          Type parameter for the {@link Supplier}
 *
 */
public interface SerializableSupplier<T> extends Supplier<T>, Serializable {
}
