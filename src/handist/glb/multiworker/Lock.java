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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.Semaphore;

/**
 * {@link ManagedBlocker} implementation relying on a Semaphore.
 * <p>
 * This class is used to make threads yield to one another in the
 * {@link GLBcomputer}.
 *
 * @author Patrick Finnerty
 *
 */
public class Lock implements ForkJoinPool.ManagedBlocker, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -3222675796580210125L;

  /** Semaphore used for this lock implementation */
  final Semaphore lock;

  /**
   * Constructor Initializes a lock with no permits.
   */
  public Lock() {
    lock = new Semaphore(0);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.concurrent.ForkJoinPool.ManagedBlocker#block()
   */
  @Override
  public boolean block() {
    try {
      lock.acquire();
    } catch (final InterruptedException e) {
      e.printStackTrace();
      block();
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.concurrent.ForkJoinPool.ManagedBlocker#isReleasable()
   */
  @Override
  public boolean isReleasable() {
    return lock.tryAcquire();
  }

  /**
   * Called to unblock the thread that is blocked using this {@link Lock}.
   */
  public void unblock() {
    lock.drainPermits(); // Avoids unnecessary accumulation of permits in the
                         // Lock. In our situation, a maximum of one permit is
                         // sufficient.
    lock.release();
  }

  /**
   * Drains all the permits in this lock.
   */
  public void reset() {
    lock.drainPermits();
  }
}
