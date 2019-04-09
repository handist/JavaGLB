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
package handist.glb.multiworker;

import java.io.Serializable;

import handist.glb.legacy.GLBProcessor;
import handist.glb.util.Fold;

/**
 * Computation abstraction for the multi-worker Global Load Balancer.
 * <p>
 * The requirements on the computation to be distributed are the following. We
 * see the computation as a large bag of small independent tasks that can be
 * performed in any order. In the Global Load Balancer routine, a certain number
 * of the tasks present in the bag are computed before checking the runtime and
 * performing load balance operations if necessary. The method responsible for
 * the computation of a set amount of tasks is method
 * {@link #process(int, Fold)}. Workers can also share some information with the
 * other workers present on the same host through the second parameter during
 * their computation.
 * <p>
 * Load balance operations consist in splitting (method {@link #split(boolean)})
 * the bag into two, each containing part of all the tasks present in the
 * original bag at the time of splitting. One bag is then transfered to a remote
 * host of a local worker which ran out of work and merged (method
 * {@link #merge(Bag)}) into the bag held by that remote host or worker.
 * <p>
 * To ensure that tasks can be relocated from one host to another and be
 * executed successfully, computations that require some form of access to
 * (mutable) information located on a remote host is not supported. The Global
 * Load Balancer can only handle computation that can contains in itself all the
 * information needed perform the computation.
 * <p>
 * When all the bags of all the remote hosts used in the computation have been
 * depleted, the result computation takes place. The first step of this
 * operation consists for each {@link Bag} instance to put its contribution into
 * the result-type provided instance in method {@link #submit(Fold)}. Each
 * instance of the result type (one per host) are then gathered and "folded"
 * back on one host before being returned to the user. This last operation
 * places some requirements on the kind of result produced by the computation
 * which are further discussed in the description of the {@link Fold} interface.
 * <p>
 * There are two type parameters to the interface, <em>B</em> and <em>R</em>.
 * The first parameter <em>B</em> should be the implementing class itself. We
 * use a type-parameter for methods {@link #split(boolean)} and
 * {@link #merge(Bag)} since it is the most elegant way we found to have these
 * operations operate on the implementing class itself, a sort of reflective
 * type. The second parameter <em>R</em> is the kind of result this {@link Bag}
 * produces and should be an implementation of interface {@link Fold}. In order
 * for {@link Bag} implementations to be processed successfully by the
 * {@link GLBProcessor}, they will also need to implement the
 * {@link Serializable} interface. This is required in order for instances to be
 * sent to remote hosts as part of the load balancing operations.
 * <p>
 * Example:
 *
 * <pre>
 * public class MyBag implements Bag&lt;MyBag, MyResult&gt;, Serializable {
 *
 *   private static final long serialVersionUID = 3582168956043482749L;
 *   // implementation ...
 * }
 * </pre>
 *
 *
 * @param <B>
 *          Type parameter used for the return type of method
 *          {@link #split(boolean)} and the parameter type of method
 *          {@link #merge(Bag)}. The programmer should choose the implementing
 *          class itself as first parameter.
 * @param <R>
 *          Type parameter used for method {@link #submit(Fold)}. The chosen
 *          class is "so to speak" the result produced by the implemented
 *          distributed computation. As such it is required to implement the
 *          {@link Fold} interface.
 * @author Patrick Finnerty
 * @see Fold
 *
 */
public interface Bag<B extends Bag<B, R> & Serializable, R extends Fold<R> & Serializable> {

  /**
   * Indicates if this bag has some computation left.
   *
   * @return {@code true} if there is still computation to do within this bag,
   *         {@code false} otherwise.
   */
  public boolean isEmpty();

  /**
   * Indicates if this bag can be split, i.e. if part of the computation left
   * inside the bag could be taken off of it and given to an other worker for
   * computation without depleting {@code this} instance of all its work. If
   * this bag is empty, this method should also return {@code false}.
   *
   * @return {@code true} if this bag can be split, {@code false} otherwise
   */
  public boolean isSplittable();

  /**
   * Merges the content of the bag given as parameter into this instance.
   *
   * @param b
   *          the bag to be merged into this instance
   */
  public void merge(B b);

  /**
   * Performs a fragment of the computation as indicated by the first parameter.
   * If there are less tasks in the {@link Bag} then the requested amount of
   * work, should complete all the tasks and then return.
   * <p>
   * The second parameter is the object shared with the other concurrent workers
   * on this place through which information can be shared. The programmer will
   * be careful to enforce proper synchronization on that shared object where it
   * is necessary, the library does not enforce any particular protection on
   * that object.
   *
   * @param workAmount
   *          the amount of computation to be done
   * @param sharedObject
   *          instance shared between workers of one place through which some
   *          data between workers can be shared
   */
  public void process(int workAmount, R sharedObject);

  /**
   * Takes a chunk of computation from this bag and returns it in a new
   * instance. The computation fragment this method returns will be computed by
   * a different thread.
   * <p>
   * In the case {@code this} instance is not splittable and the parameter
   * {@code takeAll} is true, the whole content of the bag should be returned in
   * a new instance, resulting in {@code this} instance being empty.
   * <p>
   * If this method is called when this instance is empty, should return an
   * empty bag instance.
   *
   *
   * @param takeAll
   *          indicates if the caller wants the whole content of this instance
   *          in the event it cannot be split. Can be ignored by the programmer
   *          in other situations.
   * @return a fragment of the computation held in this bag in a new instance
   */
  public B split(boolean takeAll);

  /**
   * Asks for the result produced by this fragment of the computation to be
   * placed in the given R instance.
   *
   * @param r
   *          the instance in which the partial result is to be stored
   */
  public void submit(R r);

}
