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
package handist.glb.legacy;

import java.io.Serializable;
import java.util.Arrays;

import handist.glb.util.Fold;

/**
 * {@link BagQueue} is the class used to handle the {@link Bag}s to compute in
 * the {@link LoopGLBProcessor} implementation.
 * <p>
 * Unlike its counterpart {@link ConcurrentBagQueue}, this class does not need
 * to implement any particular protections against concurrent accesses as such
 * accesses do not arise the circular loop lifeline strategy used in
 * {@link LoopGLBProcessor}.
 *
 * @param <R>
 *            The result type of the handled computation
 *
 * @author Patrick Finnerty
 * @see ConcurrentBagQueue
 */
class BagQueue<R extends Fold<R> & Serializable> {
    /*
     * The implementation has been made to accommodate for bags of different
     * types to be held simultaneously. However such feature was not made
     * accessible to the programmer and remains unused.
     */

    /** Array used to store the {@link Bag}s */
    private Object bags[] = new Bag[16];

    /** First free index in the bag queue */
    private int last = 0;

    /**
     * Index of the last Bag that was processed. Is updated by
     * {@link #process(int)} as the computation takes place and the successive
     * {@link Bag}s become empty.
     */
    private int lastPlaceWithWork = 0;

    /**
     * Constructor
     */
    public BagQueue() {
    }

    /**
     * Removes all {@link Bag}s from the {@link BagQueue}. The {@link BagQueue}
     * is empty when this method returns.
     */
    public void clear() {
        for (int i = 0; i < bags.length; i++) {
            bags[i] = null;
        }
        last = 0;
        lastPlaceWithWork = 0;
    }

    /**
     * Adds the {@link Bag} given as parameter to the {@link BagQueue}. If
     * another instance of the same class as the given parameter exists in the
     * {@link BagQueue}, it will merged into it. If there are no such existing
     * instance, the {@link Bag} is added at the last available index in the
     * array. If the array becomes full as a consequence, its size is increased.
     *
     * @param <B>
     *            type of the bag to be added
     * @param b
     *            the bag to add to the queue
     */
    public <B extends Bag<B, R> & Serializable> void giveBag(B b) {
        for (int i = 0; i < last; i++) {
            @SuppressWarnings("unchecked")
            final B a = (B) bags[i];
            if (b.getClass().getName().equals(a.getClass().getName())) {

                a.merge(b);
                return;
            }
        }
        // No existing instance of the same class in the queue, we add the work
        // at
        // the end
        bags[last] = b;
        last++;
        if (last == bags.length) {
            grow();
        }

    }

    /**
     * Doubles the capacity of the {@link #bags} array. Copying the contained
     * {@link Bag}s to the new array.
     */
    private void grow() {
        bags = Arrays.copyOf(bags, bags.length * 2);
    }

    /**
     * Indicates if this {@link BagQueue} is empty, that is that it does not
     * contain any {@link Bag} or all the {@link Bag}s it contains are empty.
     *
     * @return true if empty, false otherwise
     */
    @SuppressWarnings("rawtypes")
    public boolean isEmpty() {
        if (last == 0) {
            return true;
        }
        final int i = lastPlaceWithWork;
        Bag b = (Bag) bags[lastPlaceWithWork];
        if (b.isEmpty()) {
            do {
                lastPlaceWithWork = (lastPlaceWithWork + 1) % last;
                b = (Bag) bags[lastPlaceWithWork];
                if (!b.isEmpty()) {
                    return false;
                }
            } while (i != lastPlaceWithWork);
            return true;
        }
        return false;

    }

    /**
     * Finds a {@link Bag} in the queue with work and processes the given amount
     * of work on it.
     *
     * @param workAmount
     *            amount of work to be processed
     */
    @SuppressWarnings("rawtypes")
    public void process(int workAmount) {

        final Bag b = (Bag) bags[lastPlaceWithWork];
        b.process(workAmount);
    }

    /**
     * Gathers the result from the bags contained by this bag queue. The result
     * instance in which all the bags should store their result is given as
     * parameter.
     *
     * @param res
     *            result instance in which the results of the bags are to be
     *            stored
     * @see Bag#submit(Fold)
     */
    @SuppressWarnings("unchecked")
    public void result(R res) {
        for (int i = 0; i < last; i++) {
            ((Bag<?, R>) bags[i]).submit(res);
        }
    }

    /**
     * Tries to split the {@link Bag}s contained in the {@link BagQueue} and
     * returns the split. If no work could be be split from any {@link Bag},
     * returns null.
     *
     * @param <B>
     *            return type
     * @return some {@link Bag} instance or null is no split could be performed.
     */
    @SuppressWarnings("unchecked")
    public <B extends Bag<B, R> & Serializable> B split() {
        B split = null;
        for (int i = 0; i != last; i++) {
            split = ((B) bags[i]).split();
            if (split != null) {
                break;
            }
        }
        return split;
    }

}
