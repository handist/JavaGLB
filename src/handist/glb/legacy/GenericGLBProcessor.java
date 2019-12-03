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

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import apgas.Place;
import apgas.util.PlaceLocalObject;
import handist.glb.util.Fold;
import handist.glb.util.HypercubeStrategy;
import handist.glb.util.LifelineStrategy;

/**
 * GenericGLBProcessor proposes a simple API to request for work to be computed
 * using the lifeline-based global load balancing scheme.
 * <p>
 * This is an implementation of {@link GLBProcessor} that can handle any kind of
 * user-defined lifeline strategy. An "hypercube" lifeline strategy
 * implementation (class {@link HypercubeStrategy}) is provided with the present
 * library but programmers are free to design their own strategies if they see
 * fit by implementing interface {@link LifelineStrategy}.
 * <p>
 * The programmer can submit his computation and an instance of the class used
 * to gather the result to method <em>compute</em>. This class will then
 * distribute the computation given across compute nodes and perform the
 * load-balancing. When the computation ends, the partial result of all remote
 * host will then be gathered back into a single instance before being returned
 * by the method. After completion of a cdistributed computation, information
 * about the performance of it can be obtained by calling method
 * {@link #getLogger()}.
 * <p>
 * An instance of this class cannot be obtained directly, the programmer will
 * have to use the {@link GLBProcessorFactory} factory methods to access the
 * distributed computation service. This is required as some setup has to be
 * done on each of the hosts of the distributed computation before the
 * computation can actually beginning.
 *
 * @author Patrick Finnerty
 * @see HypercubeStrategy
 */
final class GenericGLBProcessor extends PlaceLocalObject
    implements GLBProcessor {

  /**
   * Logger object used to store the runtime event that occur on this instance
   */
  private final Logger log;

  /**
   * Member used for the remote places to send their log instance to. Is only
   * used on the {@link GenericGLBProcessor} instance of Place 0.
   *
   * @see #getLogger()
   */
  private Logger[] logs;

  /** Bag to be processed */
  @SuppressWarnings("rawtypes")
  private ConcurrentBagQueue bagsToDo;

  /**
   * Result instance local to this place. Used to gather the results from the
   * {@link Bag} processed by this place contained in {@link #bagsToDo} when the
   * computation ends. All the remote instances are then merged back into the
   * instance of place 0.
   *
   * @see #result()
   */
  @SuppressWarnings("rawtypes")
  private Fold result;

  /** Brings the APGAS place id to the class */
  private final Place home = here();

  /**
   * Integer ({@code int}) id's of the places which are susceptible to establish
   * their lifeline on this place. There is no particular meaning to the indices
   * of the array in which this information is contained.
   */
  private final int incomingLifelines[];

  /**
   * Integer ({@code int}) id's of the places on which this place will establish
   * its lifelines when running out of work.There is no particular meaning to
   * the indices on which this information is contained.
   */
  private final int lifelines[];

  /**
   * Map indicating if the lifelines this place can establish are activated. The
   * key is the identifier of the remote place on which a lifeline can be
   * established, i.e. the keys are the content of array {@link #lifelines}. The
   * mapped boolean indicates if the lifeline is activated. For example if
   * {@code this} place is requiring work from place number 42, the mapping of
   * key 42 in this map is to {@code true}. If place 42 is not asked for work
   * from this place, the key 42 is mapped to false.
   * <p>
   * Every potential lifeline of this place always have a mapping (either true
   * or false) in this map throughout the computation.
   */
  private final Map<Integer, Boolean> lifelineActivated = new HashMap<>();

  /**
   * Collection of lifeline thieves asking for work from this place. They will
   * be answered in the {@link #distribute()} method.
   */
  private final ConcurrentLinkedQueue<Place> lifelineThieves = new ConcurrentLinkedQueue<>();

  /** Number of places available for the computation */
  private final int places = places().size();

  /**
   * Random generator used when thieving a random place.
   * <p>
   * By initializing the seed with the place id (different from all the other
   * places), we avoid having the same sequence of places to thieve from for all
   * the places.
   */
  private final Random random = new Random(home.id);

  /**
   * Number of random steal attempts performed by this place. Can be adjusted to
   * the user's convenience with the constructor.
   */
  private final int randomStealAttempts;

  /**
   * Indicates the state of the place at any given time.
   * <ul>
   * <li><em>-2</em> : inactive
   * <li><em>-1</em> : running
   * <li><em>p</em> in range [0,{@code places}] : stealing from place of id
   * <em>p</em>
   * </ul>
   * At initialization, is in inactive state. Due to race conditions, it has to
   * be protected at each read/write.
   */
  private int state = -2;

  /**
   * List of thieves that asked for work while the current place was performing
   * computation. They will be answered in the {@link #distribute()} method.
   */
  private final ConcurrentLinkedQueue<Place> thieves = new ConcurrentLinkedQueue<>();

  /** Amount of work processed by this place before dealing with thieves */
  private final int WORK_UNIT;

  /**
   * Puts this instance of {@link GenericGLBProcessor} into a ready-to-compute
   * state. Parameter {@code init} is kept in member {@link #result}. Having the
   * parameter type of the result also allows us to initialize a clean instance
   * of class {@link ConcurrentBagQueue} for member {@link #bagsToDo} with the
   * proper generic parameter type.
   *
   * @param <R>
   *          result parameter type
   * @param init
   *          neutral element of the desired result type for the computation to
   *          come
   */
  private <R extends Fold<R> & Serializable> void clear(R init) {
    log.reset();
    thieves.clear();
    lifelineThieves.clear();
    for (final int i : incomingLifelines) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }
    final boolean lifelinesAreActivated = home.id != 0;
    for (final int i : lifelines) {
      lifelineActivated.put(i, lifelinesAreActivated);
    }

    state = -2;
    bagsToDo = new ConcurrentBagQueue<R>();
    result = init;
  }

  /**
   * Yields back some work in response to a {@link #steal()}
   * <p>
   * Merges the proposed {@link Bag} {@code gift} into this place's
   * {@link #bagsToDo} instance before waking up the waiting thread in the
   * {@link #steal()} procedure. This will in turn make this place check its
   * {@link #bagsToDo} for any work and either process the given work or switch
   * to the lifeline steal procedure.
   *
   * @param <B>
   *          the type of gift given
   * @param <R>
   *          the second type parameter of the gift instance
   * @param gift
   *          the work given by place {@code p}, possibly <code>null</code>.
   * @see #steal()
   * @see #lifelineSteal()
   */
  @SuppressWarnings("unchecked")
  private synchronized <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void deal(
      B gift) {

    /*
     * If answering place couldn't share work with this place, the given q is
     * null. A check is therefore necessary.
     */
    if (gift != null) {
      bagsToDo.giveBag(gift);
      log.stealsSuccess++;
    }

    /*
     * Whichever the outcome, the worker thread blocked in method run needs to
     * be waken up
     */
    state = -1; // Switch back to 'running' state.
    notifyAll(); // Wakes up the halted thread in 'steal' procedure.
  }

  /**
   * Distributes {@link Bag}s to the random thieves and the lifeline thieves
   * asking for work from this place.
   * <p>
   * This method is part of the worker's routine {@link #run()}.
   *
   * @param <B>
   *          type of offered work given to thieves
   * @param <R>
   *          type of result type B produces
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void distribute() {
    if (places == 1) {
      return;
    }
    Place p;
    while ((p = thieves.poll()) != null) {

      final B toGive = (B) bagsToDo.split();
      if (toGive != null) {
        log.stealsSuffered++;
      }
      uncountedAsyncAt(p, () -> {
        deal(toGive);
      });
    }

    final int h = home.id;
    while ((p = lifelineThieves.poll()) != null) {
      final B toGive = (B) bagsToDo.split();
      if (toGive != null) {
        log.lifelineStealsSuffered++;
        asyncAt(p, () -> {
          lifelineDeal(toGive, h);
        });
      } else {
        /*
         * null split means no more work to share in bagsToDo. We put the thief
         * back into the collection
         */
        lifelineThieves.add(p);

        /*
         * All further calls to bagsToDo.split() would yield null at this stage,
         * it is not worth continuing
         */
        return;
      }
    }
  }

  /**
   * Computes the result from the bag processed by this place (method
   * {@link Bag#submit(Fold)}}, storing it into member {@link #result()} before
   * sending this result fragment to place 0 where it will be merged with all
   * the result fragments from every place in the computation.
   * <p>
   * For place 0, the {@link #result} instance is not sent as it is already
   * present at place 0.
   *
   * @param <R>
   *          result type
   *
   * @see #giveResult(Fold)
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <R extends Fold<R> & Serializable> void gather() {
    synchronized (result) {
      bagsToDo.result(result);
    }

    final Fold r = result;
    if (home.id != 0) {
      asyncAt(place(0), () -> {
        giveResult((R) r);
      });
    }
  }

  /**
   * Wakes up a place waiting for work on its lifeline, giving it some work
   * {@code a} on the fly in response to a {@link #lifelineSteal()}
   * <p>
   * As there are several lifelines established by this place, there will be a
   * call to this method originating from each of the lifeline, each giving some
   * work to this place. This is handled by the concurrency protection of
   * {@link ConcurrentBagQueue} which ensures mutual exclusion between the
   * processing of a bag and the addition of bags to the {@link #bagsToDo}
   * member.
   * <p>
   * Moreover, as a call to this method can happen at any time, a check on the
   * value of {@link #state} is necessary to ensure no two processes are running
   * method {@link #run()} at the same time.
   *
   * @param <B>
   *          type of the given work
   * @param <R>
   *          second parameter type of the given work
   *
   * @param q
   *          the work to be given to the place
   * @param sender
   *          place who sent the work to this place
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void lifelineDeal(
      B q, int sender) {
    bagsToDo.giveBag(q);

    log.lifelineStealsSuccess++;
    synchronized (lifelineActivated) {
      lifelineActivated.put(sender, false);
    }
    /*
     * Call to run needs to be done outside of the synchronized block. Boolean
     * toLaunch is used to carry the information outside the synchronized block.
     */
    boolean toLaunch = false;
    synchronized (this) {
      if (state == -2) {
        state = -1;
        toLaunch = true;
      }
    }

    if (toLaunch) {
      run();
    }
  }

  /**
   * Registers this {@link GenericGLBProcessor} as asking for work to its
   * (remote) lifelines. If there is only one place in the cluster, has no
   * effect.
   * <p>
   * If there is only one host in the computation cluster and that host ran out
   * of work, it means the computation has completed. There are no other
   * potential hosts that may have some computation left to do.
   *
   * @see #lifelineThieves
   */
  private void lifelineSteal() {
    if (places == 1) {
      // No other place exists, "this" is the only place.
      // Impossible to perform a steal.
      return;
    }
    final Place h = home;
    synchronized (lifelineActivated) {

      for (final int i : lifelines) {
        if (!lifelineActivated.get(i)) {
          lifelineActivated.put(i, true);
          log.lifelineStealsAttempted++;
          asyncAt(place(i), () -> {
            lifelineThieves.add(h);
            log.lifelineStealsReceived++;
          });
        }
      }
    }
  }

  /**
   * Method used to signal the fact that the {@link GenericGLBProcessor} located
   * at place {@code p} is requesting work from this place.
   * <p>
   * If this place is currently working ({@link #state} = -1), adds the place
   * asking for work to member {@link #thieves}. The answer will be provided
   * when the worker calls its {@link #distribute()} method.
   * <p>
   * If this place is not working, i.e. either trying to steal work randomly
   * ({@link #state} positive) or inactive ({@link #state} == -2), a
   * {@code null} answer is dispatched immediately to the thief.
   *
   * @param p
   *          The place asking for work
   */
  private void request(Place p) {
    synchronized (this) {
      log.stealsReceived++;
      /*
       * If the place is currently performing computation, adds the thief to its
       * list of pending thief. The work will be shared when this place stops
       * processing its tasks in the main 'run' loop by the first 'distribute'
       * call.
       */
      if (state == -1) {
        thieves.add(p);
        return;
      }
    }

    uncountedAsyncAt(p, () -> {
      deal(null);
    });
  }

  /**
   * Main computation procedure.
   * <p>
   * While this place has some work, it processes {@link #WORK_UNIT} worth of
   * tasks in its {@link #bagsToDo} before answering to potential thieves
   * (method {@link #distribute()}).
   * <p>
   * When it runs out of work, it attempts a maximum of
   * {@link #randomStealAttempts} steals on other places (method
   * {@link #steal()}). If successful in one of its steals, resumes its
   * processing/distributing routine.
   * <p>
   * If all random steals fail, establishes its lifelines (method
   * {@link #lifelineSteal()}) and stops. When a lifeline answer arrives with
   * some new work to be done by this instance (method
   * {@link #lifelineDeal(Bag, int)}), this method will be launched again.
   * <p>
   * At the end of the computation when all the {@link GenericGLBProcessor}
   * instances have run out of work and have all established their lifelines,
   * the thread that launched the {@code finish} in the
   * {@link #compute(Bag, Fold)} method will progress and start the result
   * gathering procedure.
   */
  private void run() {
    log.startLive();
    synchronized (this) {
      state = -1;
    }

    for (;;) { // Is correct, loop is exited thanks with a break later on
      while (!bagsToDo.isEmpty()) {
        bagsToDo.process(WORK_UNIT);
        distribute();
      }

      // Perform steals attempts
      int attempts = randomStealAttempts;
      while (attempts > 0 && bagsToDo.isEmpty()) {
        attempts--;
        log.stealsAttempted++;
        steal();
      }

      // Synchronized block for possible state change.
      // Necessary because of the lifelineDeal method.
      synchronized (this) {
        if (bagsToDo.isEmpty()) {
          state = -2;
          break;
        }
      }

    }

    /**
     * Sending null to thieves that have managed to ask for some work between
     * two random steals. This is absolutely necessary. The computation may not
     * end if this was not done.
     */
    Place p;
    while ((p = thieves.poll()) != null) {
      uncountedAsyncAt(p, () -> {
        deal(null);
      });
    }

    // Establishing the lifelines
    lifelineSteal();
  }

  /**
   * Attempts to steal work to a randomly chosen place. Will halt the process
   * until the target place answers (whether it indeed gave work or not).
   */
  private void steal() {
    if (places == 1) {
      // No other place exists, "this" is the only one.
      // Cannot perform a steal.
      return;
    }
    final Place h = home;

    // Selecting the random place
    int p = random.nextInt(places - 1);
    if (p >= h.id) {
      // We cannot steal on ourselves. Moreover the generated random integer has
      // a range of `places - 1`. By incrementing p we get an uniform
      // distribution for the target place.
      p++;
    }

    /*
     * Change state to 'p', i.e. thieving from place 'p' before requesting work
     * from it.
     */
    synchronized (this) {
      state = p;
    }

    log.stopLive();

    /*
     * Calls "request" at place p, passing itself as parameter. The call is
     * 'uncounted' as this asynchronous call is about program "logistics" and
     * does not need to intervene in the enclosing "finish" construct
     */
    uncountedAsyncAt(place(p), () -> {
      request(h);
    });

    synchronized (this) {
      while (state >= 0) {
        try {
          wait();
        } catch (final InterruptedException e) {
        }
      }
    }
    log.startLive();
  }

  /**
   * Merges the given parameter into member {@link #result}. Only called on
   * place 0. Mutual exclusion between several calls to this method as well as
   * the {@link #gather()} performed on place 0 is enforced.
   *
   * @param <R>
   *          type of the result
   * @param res
   *          the result instance to be merged into this place
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable> void giveResult(R res) {
    synchronized (result) {
      result.fold(res);
    }
  }

  /**
   * Clears the whole cluster of {@link GenericGLBProcessor} of all its tasks
   * and results and prepares it for a new computation. It calls for method
   * {@link #clear(Fold)} to be called on each place in the system.
   *
   * @param <R>
   *          type parameter of the next computation
   * @param init
   *          initial result instance for the next computation to take place
   */
  private <R extends Fold<R> & Serializable> void reset(R init) {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> clear(init));
      }
    });
  }

  /**
   * Gives back the result that was gathered from the {@link Bag}s contained in
   * the {@link #bagsToDo} member of all the places. This method is called in
   * the {@link #compute(Bag, Fold)} method when it is detected that the
   * computation completed.
   *
   * @param <R>
   *          type of the returned {@link Fold}
   * @return the computation's result
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable> R result() {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> gather());
      }
    });
    return (R) result;
  }

  /**
   * Sends the local log instance to place 0.
   */
  private void sendLogger() {
    final Logger l = log;
    final int placeId = home.id;
    asyncAt(place(0), () -> {
      synchronized (logs) {
        logs[placeId] = l;
      }
    });
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.GLBProcessor#compute(apgas.glb.Bag,
   * java.util.function.Supplier)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      B bag, R result) {
    synchronized (bagsToDo) {
      reset(result);
      bagsToDo.giveBag(bag);
      finish(() -> {
        run();
      });
      return result();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.GLBProcessor#getLogger()
   */
  @Override
  public Logger[] getLogger() {
    logs = new Logger[places];
    logs[0] = log;
    finish(() -> {
      for (final Place p : places()) {
        if (p.id != 0) {

          asyncAt(p, () -> {
            sendLogger();
          });
        }
      }
    });
    return logs;
  }

  /**
   * Package-private Constructor
   * <p>
   * This constructor is hidden from the programmer as some particular setup
   * needs to be done to setup the computation cluster properly. Such setup is
   * performed by class {@link GLBProcessorFactory}.
   *
   * @param workUnit
   *          the amount of work to be processed before tending to thieves
   * @param randomStealAttemptsCount
   *          number of random steals attempts before turning to the lifeline
   *          stealing scheme
   * @param s
   *          {@link LifelineStrategy} to be used for the computation
   */
  GenericGLBProcessor(int workUnit, int randomStealAttemptsCount,
      LifelineStrategy s) {
    log = new Logger();
    WORK_UNIT = workUnit;
    randomStealAttempts = randomStealAttemptsCount;

    bagsToDo = new ConcurrentBagQueue<>();

    incomingLifelines = s.reverseLifeline(home.id, places);
    lifelines = s.lifeline(home.id, places);
    for (final int i : incomingLifelines) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }
  }
}
