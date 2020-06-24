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
package handist.glb.legacy;

import java.io.Serializable;

import handist.glb.util.Fold;

/**
 * Abstraction of work to be processed by a {@link GLBProcessor}.
 * <p>
 * The requirements on the computation to be distributed are the following. We
 * see the computation as a large bag containing some computation. In the Global
 * Load Balancer scheme, a certain amount of the computation is performed before
 * checking the runtime and performing load balance operations if necessary. The
 * method responsible for the computation of a set amount of tasks is method
 * {@link #process(int)}. Load balance operations consist in splitting
 * ({@link #split()}) the bag into two instances, each containing half of the
 * computation originally present in the bag at time the method was called. One
 * bag is then transfered to a remote host and {@link #merge(Bag)} it back into
 * the bag held by that remote host to be computed.
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
 * instance of the result type are then gathered back on one host before being
 * returned. This last operation places some requirements on the kind of result
 * produced by the computation which are further discussed in the {@link Fold}
 * interface.
 * <p>
 * There are two type parameters to the interface, <em>B</em> and <em>R</em>.
 * The first parameter <em>B</em> should be the implementing class itself. The
 * second parameter <em>R</em> is the kind of result this {@link Bag} produces
 * and should be an implementation of interface {@link Fold}. In order for
 * {@link Bag} implementations to be processed successfully by the
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
 *          Type parameter used for the return type of method {@link #split()}
 *          and the parameter type of method {@link #merge(Bag)}. The programmer
 *          should choose the implementing class itself as first parameter.
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
   * Indicates if the {@link Bag} is empty, that is if there are no more work
   * tasks to perform contained by this bag.
   *
   * @return true if there are no tasks left in the {@link Bag}, false otherwise
   */
  public boolean isEmpty();

  /**
   * Merges the content of the {@link Bag} given as parameter into this
   * instance.
   * <p>
   * Unlike {@link #split()} which can return {@code null}, the provided
   * parameter will never be null.
   *
   * @param b
   *          the tasks to be merged into {@code this} instance
   */
  public void merge(B b);

  /**
   * Processes a certain amount of tasks as specified by the parameter and
   * returns. If there is less work than the given parameter to be done,
   * processes the remaining work until the {@link Bag} is empty.
   *
   * @param workAmount
   *          amount of work to process
   */
  public void process(int workAmount);

  /**
   * Creates a new instance of Bag which contains a fraction of the work to be
   * process and returns it. If no tasks can be shared, must return {@code null}
   * rather than an empty Bag.
   * <p>
   * As far as the bag splitting strategy is concerned (how much tasks are given
   * when splitting the bag), this is left to the programmer.
   * <p>
   * The programmer will be careful and remove the work placed in the returned
   * instance from {@code this} so that it is not computed twice.
   *
   * @return A new Bag containing work shared by this instance, {@code null} if
   *         no work can be shared.
   */
  public B split();

  /**
   * Asks Bag to submit its result into the user-defined instance given as
   * parameter.
   *
   * @param r
   *          the instance in which this bag's result are to be stored
   */
  public void submit(R r);

}
