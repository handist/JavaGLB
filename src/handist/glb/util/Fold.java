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

import handist.glb.legacy.Bag;
import handist.glb.legacy.GLBProcessor;

/**
 * Abstraction of a result computed by the {@link GLBProcessor}. The programmer
 * can implement their own data structure implementing this interface and use it
 * as the {@link Bag}'s second parameter type.
 * <p>
 * The {@link Fold} interface can be seen as a binary operation whose operands
 * are two instances of the implementing class and whose result is also a
 * instance of the implementing class. This operation is embodied by the
 * {@link #fold(Fold)} method: the operands are the given parameter {@code r}
 * and {@code this} and the result is stored in {@code this}.
 * <p>
 * When the {@link GLBProcessor} computation ends, there will be as many
 * {@link Fold} implementation instances as there were places used for the
 * computation. There is no guarantee as to the order in which these
 * (potentially many) instances will be folded into a single instance. Therefore
 * the {@link #fold(Fold)} implementation has to be symmetric in order for
 * results to be consistent from a computation to an other.
 * <p>
 * Implementation classes should implement the interface with themselves as
 * parameter type as well as the {@link Serializable} interface to ensure proper
 * transfer from a place to an other.
 *
 * <p>
 * Below is a simple example of a potential {@link Fold} implementation of a Sum
 * of integers:
 *
 * <pre>
 * public class Sum implements Fold&lt;Sum&gt;, Serializable {
 *
 *   private static final long serialVersionUID = 3582168956043482749L;
 *
 *   public int sum;
 *
 *   &#64;Override
 *   public void fold(Sum r) {
 *     sum += r.sum;
 *   }
 *
 *   public Sum(int s) {
 *     sum = s;
 *   }
 * }
 * </pre>
 *
 * @param <R>
 *          implementing class itself (reflective-type method implementation)
 *
 * @author Patrick Finnerty
 */
public interface Fold<R extends Fold<?> & Serializable> {

  /**
   * Folds (merges) the given parameter's result into this instance.
   *
   * @param r
   *          the Fold to be folded into {@code this}.
   */
  public void fold(R r);

}
