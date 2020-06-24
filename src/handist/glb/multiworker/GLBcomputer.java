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

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.atomic.AtomicIntegerArray;

import apgas.GlobalRuntime;
import apgas.Place;
import apgas.util.PlaceLocalObject;
import handist.glb.multiworker.tuning.Tuner;
import handist.glb.util.Fold;
import handist.glb.util.LifelineStrategy;
import handist.glb.util.SerializableSupplier;

/**
 * Class {@link GLBcomputer} implements a lifeline-based work-stealing scheme
 * between hosts with an internal management that allows multiple workers to
 * perform computation concurrently on the same Java process.
 * <p>
 * This {@link GLBcomputer} handles distributed computation that fits the
 * {@link Bag} interface using multiple concurrent workers on the same hosts. It
 * follows the design proposed by Yamashita and Kamada in their article <a href=
 * "https://www.jstage.jst.go.jp/article/ipsjjip/24/2/24_416/_article">Introducing
 * a Multithread and Multistage Mechanism for the Global Load Balancing Library
 * of X10</a>. The current design does not implement the multi-stage mechanisms
 * described in their article.
 * <p>
 * The requirements on the kind of computation and the features available to the
 * programmer are further detailed in the documentation of classes {@link Bag}
 * and {@link Fold}. The customization possibilities of the load balance
 * algorithm are presented in class {@link Configuration}.
 * <p>
 * The work-stealing scheme is similar in spirit to the original lifeline-based
 * Global Load Balancer implemented in the X10 programming language. Inactive
 * hosts passively wait for some work to reach them through their lifelines to
 * resume computation. The major difference with the original scheme comes in
 * the fact that this implementation accommodates for several concurrent workers
 * in a single Java process. Some load balance is performed internally to keep
 * the workers occupied as much as possible while still allowing remote hosts to
 * steal work. The design relies on two {@link Bag} instances that are kept
 * aside to perform load balance. One is primarily in charge of load balance
 * operations between the workers running on the local host (member
 * {@link #intraPlaceQueue}) while the other in dedicated to steals from remote
 * hosts (member {@link #interPlaceQueue}).
 *
 *
 * @author Patrick Finnerty
 */
public class GLBcomputer extends PlaceLocalObject {

  /**
   * Utility class used to contain a bag and the id of a worker in a single
   * instance.
   *
   * @author Patrick Finnerty
   *
   */
  class WorkerBag {

    /** Bag held by worker */
    @SuppressWarnings("rawtypes")
    public Bag bag;

    /** Integer identifier of the worker */
    public int workerId;

    /**
     * Constructor
     * <p>
     * Initializes a {@link WorkerBag} instance which holds the given id and
     * {@link Bag}. This is used to identify the workers
     *
     * @param id
     *             identifier of the worker that holds the bag
     * @param b
     *             Bag instance associated to the given identifier
     */
    @SuppressWarnings("rawtypes")
    public WorkerBag(int id, Bag b) {
      workerId = id;
      bag = b;
    }
  }

  /**
   * Lock used to periodically call the method that will spread the information
   * contained in the shared memory.
   */
  TimeoutBlocker communicatorLock;

  /**
   * Boolean flag used to shut down the communicator activity when the
   * computation has completed.
   *
   * @see #communicatorThread()
   */
  volatile boolean communicatorThreadShutdown;

  /**
   * {@link Logger} instance used to gather the {@link PlaceLogger}s of each
   * place and hold runtime information common to the whole computation.
   *
   * @see #getLog()
   */
  Logger computationLog;

  /**
   * Configuration of this instance. Holds all the configuration information,
   * including the initial configuration used and the parameters currently in
   * use by the glb that can be modified by the tuning class instance.
   */
  final Configuration CONFIGURATION;

  /**
   * Array containing a flag for each worker (the worker's id is used as index
   * in the array). A {@code 1} value at index {@code i} indicates that the
   * "i"th worker is requested to send work to the {@link #interPlaceQueue}. The
   * whole array is turned to 1 values when it is discovered by a worker that
   * the {@link #interPlaceQueue} is empty. Workers put their assigned flag back
   * to 0 when they feed the {@link #interPlaceQueue} as part of their main
   * routine {@link #workerProcess(WorkerBag)}.
   */
  AtomicIntegerArray feedInterQueueRequested;

  /** Place this instance is located on */
  final Place HOME;

  /** Bag used to perform load balance between the worker within this place */
  @SuppressWarnings("rawtypes")
  Bag interPlaceQueue;

  /** Flag used to signal the fact member {@link #interPlaceQueue} is empty */
  volatile boolean interQueueEmpty;

  /**
   * Bag used to perform load-balance with remote hosts. It is also used to
   * provide the lock used when accessing members {@link #intraPlaceQueue} or
   * {@link #interPlaceQueue}.
   */
  @SuppressWarnings("rawtypes")
  Bag intraPlaceQueue;

  /** Flag used to signal the fact member {@link #intraPlaceQueue} is empty */
  volatile boolean intraQueueEmpty;

  /** Lifelines this place can establish */
  final int LIFELINE[];

  /**
   * Lock used by the {@link #lifelineAnswerThread()} to yield its thread. When
   * a lifeline answer becomes possible, a {@link #workerProcess(WorkerBag)}
   * will {@link Lock#unblock()} this lock to allow progress of the
   * {@link #lifelineAnswerThread()}.
   */
  Lock lifelineAnswerLock;

  /**
   * Flag used to confirm that the lifelineAnswerThread has exited. This
   * prevents a potential bug where a lifeline answer comes just as the old
   * lifeline answer thread is woken up for exit. When the lifeline answer calls
   * proceeds to method {@link #run(Bag)}, it can put {@link #shutdown} back to
   * {@code false} before the old thread could exit, resulting in multiple
   * lifeline answer thread running on the same place.
   * <p>
   * To solve this issue, method {@link #run(Bag)} actively waits until this
   * flag is set back to true by the exiting lifeline answer thread before
   * spawning a new one.
   */
  volatile boolean lifelineAnswerThreadExited;

  /**
   * Collection used to keep track of the lifelines this place has established
   * on other places.
   * <p>
   * The key is the integer identifying the remote place on which this place may
   * establish lifelines, the value is {@code true} when the lifeline is
   * established, {@code false}otherwise. There is always a mapping in this
   * member for every potential lifeline stablished by this place.
   *
   */
  ConcurrentHashMap<Integer, Boolean> lifelineEstablished;

  /**
   * Collection used to record the lifeline thieves that have requested some
   * work from this place but could not be given some work straight away as part
   * of method {@link #steal(int)}. The thieves stored in this member will be
   * answered by the thread running the {@link #lifelineAnswerThread()} when
   * work becomes available.
   */
  ConcurrentLinkedQueue<Integer> lifelineThieves;

  /**
   * Flag used by {@link #workerProcess(WorkerBag)} to signal that one of them
   * has unblocked the progress of the lifelineAnswer thread and that it needs
   * to answer lifelines. When a {@link #workerProcess(WorkerBag)} decided to
   * unlock the progress of the lifeline answer thread
   * ({@link #lifelineAnswerThread()}), it sets the value of
   * {@link #lifelineToAnswer} to {@code true}. The value is set back to
   * {@code false} by the {@link #lifelineAnswerThread()} when it actually
   * becomes active again.
   * <p>
   * Worker processes check the value of this boolean before considering waking
   * the lifeline answer thread. This avoids having multiple workers
   * repetitively attempting to wake up the lifeline answer thread.
   * {@link #lifelineAnswerThread()}.
   */
  volatile boolean lifelineToAnswer;

  /**
   * Logger instance used to log the runtime of the {@link GLBcomputer} instance
   * located at place {@link #HOME}.
   */
  PlaceLogger logger;

  /**
   * Indicates if the log aggregation has already being performed in method
   * {@link #getLog()}. Avoids a second aggregation of the logs if multiple
   * calls to this method are made following a computation.
   */
  boolean logsGiven;

  /**
   * ForkJoinPool of the APGAS runtime used at this place to process the
   * activities. This member is kept in order for asynchronous
   * {@link #workerProcess(WorkerBag)} activities to check if the pool has
   * pending "shorter" activities and yield if necessary.
   * <p>
   * <em>This is not safe and subject to failures if the APGAS library were to
   * evolve.</em>
   * <p>
   * The current APGAS implementation relies on a {@link ForkJoinPool} on each
   * place to keep all the asynchronous tasks submitted to a place's runtime. If
   * an other class were to be used, errors when initializing this field in the
   * constructor of {@link GLBcomputer} are likely to appear.
   *
   * @see #workerProcess(WorkerBag)
   * @see #workerLock
   * @see <a href=
   *      "https://github.com/x10-lang/apgas/blob/master/apgas/src/apgas/impl/GlobalRuntimeImpl.java">apgas/src/apgas/impl/GlobalRuntimeImpl.java</a>
   */
  final ForkJoinPool POOL;

  /**
   * Random instance used to decide the victims of random steals.
   */
  Random random;

  /**
   * Instance in which the result of the computation performed at this place is
   * going to be stored. It is initialized with the given neutral element before
   * the computation starts in method
   * {@link #reset(SerializableSupplier, SerializableSupplier,SerializableSupplier, SerializableSupplier)}.
   */
  @SuppressWarnings("rawtypes")
  Fold result;

  /** Places that can establish a lifeline on this place */
  final int REVERSE_LIFELINE[];

  /**
   * Flag used to signal the {@link #lifelineAnswerThread()} that it needs to
   * shutdown. Used when the place runs out of work and has established all its
   * lifelines.
   *
   * @see #run(Bag)
   */
  volatile boolean shutdown;

  /**
   * State of this place.
   * <ul>
   * <li>0 running
   * <li>-1 stealing
   * <li>-2 inactive
   * </ul>
   * This member accesses are protected by synchronized blocks with member
   * {@link #workerBags} as lock provider.
   */
  volatile int state;

  /**
   * Instance responsible for tuning of the {@link GLBcomputer} various
   * parameters during the computation. Is given to the {@link #logger} instance
   * as the {@link PlaceLogger} class is in charge of calling the tuning method.
   */
  final Tuner tuner;

  /**
   * {@link ManagedBlocker} implementation for the tuner thread that blocks it
   * until the tuning timeout expires.
   */
  TimeoutBlocker tunerLock;

  /** Flag raised by the tuner activity when it terminates. */
  volatile boolean tunerThreadExcited;

  /**
   * Instance used to transfer relevant data between the instances of
   * {@link #result}.
   */
  @SuppressWarnings("rawtypes")
  Whisperer whisperer;

  /**
   * Concurrent data structure for worker processes trying to yield. Each worker
   * must poll an available lock from this data structure before using it. This
   * avoids having concurrent workers attempting to yield using the same lock.
   * In practice, the current implementation has a single lock:
   * {@link #workerLock}, allowing a single worker to yield at the time.
   */
  ConcurrentLinkedQueue<Lock> workerAvailableLocks;

  /**
   * Collection of the {@link WorkerBag} of the inactive worker threads on this
   * place.
   * <p>
   * <em>Before the computation</em>, as many empty {@link WorkerBag}s as
   * concurrent workers ({@link Configuration#x}) are placed in this collection
   * as part of method
   * {@link #reset(SerializableSupplier, SerializableSupplier,SerializableSupplier, SerializableSupplier)}.
   * <p>
   * <em>During the computation</em>, this collection contains the {@link Bag}s
   * of the workers that are not active. Prior to launching a new asynchronous
   * {@link #workerProcess(WorkerBag)}, a {@link WorkerBag} is polled from this
   * collection and some computation is merged into it. When a
   * {@link #workerProcess(WorkerBag)} terminates because it has completed its
   * fraction of the computation and could not get more work from the intra
   * place load balancing mechanisms, it places its {@link WorkerBag} back into
   * this collection.
   * <p>
   * This collection is also used as the lock provider for synchronized blocks
   * when member {@link #workerCount} needs to be read or modified in a
   * protected manner. This includes segments of methods {@link #deal(int, Bag)}
   * and {@link #workerProcess(WorkerBag)}.
   * <p>
   * <em>After the computation</em>, all the {@link Bag}s processed by the
   * workers are present in this collection. This allows access for the
   * collection of each computation fragment in method {@link #collectResult()}.
   */
  ConcurrentLinkedQueue<WorkerBag> workerBags;

  /**
   * Keep tracks of the number of {@link #workerProcess(WorkerBag)} launched on
   * this place. Access is protected by synchronized blocks with
   * {@link #workerBags} as lock provider.
   * <p>
   * Note that the value carried by this variable can be different than the
   * actual number of workers working concurrently. For instance, when a new
   * asynchronous {@link #workerProcess(WorkerBag)} needs to be launched, this
   * variable is incremented <em>before</em> the asynchronous process is
   * launched. Moreover, the {@link #workerProcess(WorkerBag)} can cooperatively
   * yield its thread usage to allow some other asynchronous activities to be
   * performed. Those yields do not change the value of {@link #workerCount} but
   * are tracked separately in class member {@link #logger} with methods
   * {@link PlaceLogger#workerYieldStart()} and
   * {@link PlaceLogger#workerYieldStop()}.
   */
  int workerCount;

  /**
   * Lock instance used by {@link #workerProcess(WorkerBag)} to yield their
   * execution to allow other activities (such as remote steals or lifeline
   * answers) to take place.
   */
  Lock workerLock;

  /**
   * Constructor (package visibility)
   * <p>
   * The constructor is kept hidden as some specific setup needs to be done for
   * the distribution to take place. The class {@link GLBfactory#setupGLB()} is
   * the method the programmer should use to get a computation service instance.
   *
   * @param workUnit
   *                                   amount of work processed by workers
   *                                   before interrupting their routine
   * @param randomSteals
   *                                   maximum number of steals this place can
   *                                   perform before turning to its lifelines
   *                                   to steal some work
   * @param s
   *                                   lifeline strategy to be used by this
   *                                   place
   * @param maximumConcurrentWorkers
   *                                   number of maximum concurrent workers for
   *                                   this place
   * @param tuningTimeout
   *                                   number of nanoseconds that need to elapse
   *                                   between two calls to the tuning class
   * @param t
   *                                   implementation of interface {@link Tuner}
   *                                   in charge of adjusting the parameters
   *                                   used by the GLB during the computation.
   * @param whisperInterval
   *                                   interval at which the shared object
   *                                   contents are propagated to neighboring
   *                                   nodes
   */
  GLBcomputer(int workUnit, int randomSteals, LifelineStrategy s,
      int maximumConcurrentWorkers, long tuningTimeout, Tuner t,
      long whisperInterval) {
    String tunerClass;
    if (t != null) {
      tunerClass = t.getClass().toString();
    } else {
      tunerClass = "null";
    }

    CONFIGURATION = new Configuration(places().size(), maximumConcurrentWorkers,
        workUnit, randomSteals, s.getClass().toString(), tuningTimeout,
        tunerClass, whisperInterval);

    tunerLock = new TimeoutBlocker();
    communicatorLock = new TimeoutBlocker();
    tuner = t;
    POOL = (ForkJoinPool) GlobalRuntime.getRuntime().getExecutorService();
    HOME = here();
    LIFELINE = s.lifeline(HOME.id, CONFIGURATION.p);
    REVERSE_LIFELINE = s.reverseLifeline(HOME.id, CONFIGURATION.p);

    random = new Random(HOME.id);

    feedInterQueueRequested = new AtomicIntegerArray(CONFIGURATION.x);

    lifelineEstablished = new ConcurrentHashMap<>(LIFELINE.length);
    lifelineThieves = new ConcurrentLinkedQueue<>();
    logger = new PlaceLogger(CONFIGURATION, HOME.id);
    workerBags = new ConcurrentLinkedQueue<>();

    lifelineAnswerLock = new Lock();
    workerAvailableLocks = new ConcurrentLinkedQueue<>();
    workerLock = new Lock();
    workerAvailableLocks.add(workerLock);
  }

  /**
   * Sends the order to all places to gather their results in their
   * {@link #result} member before sending it to place 0. This is done
   * asynchronously, this method will block until all places have completed
   * their {@link #collectResult} method.
   */
  void collectAllResult() {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> collectResult());
      }
    });
  }

  /**
   * Makes the place gather the results contained by all its bags into its
   * member {@link #result}. If this place ({@link #HOME}) is not place 0, sends
   * the content of the result to place 0 to be merged with all the other
   * results there.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void collectResult() {
    synchronized (result) { // Synchronized in case this is place 0 and remote
                            // results are going to merge in
      for (final WorkerBag wb : workerBags) {
        wb.bag.submit(result);
      }
    }

    final Fold r = result;
    if (HOME.id != 0) {
      asyncAt(place(0), () -> {
        synchronized (result) { // Synchronized to avoid concurrent
                                // merging/gathering on place 0
          result.fold(r);
        }
      });
    }
  }

  /**
   * Communication task that relies on a whispering scheme to propagate
   * information. Is shut down by setting the flag
   * {@link #communicatorThreadShutdown} to {@code true}.
   */
  @SuppressWarnings("unchecked")
  void communicatorThread() {
    while (!communicatorThreadShutdown) {
      final long lastCall = System.nanoTime();
      communicatorLock.setNextWakeup(lastCall + CONFIGURATION.whisperInterval);
      try {
        ForkJoinPool.managedBlock(tunerLock);
      } catch (final InterruptedException e) {
        // Ignore
      }
      workerLock.unblock();
      if (whisperer.hasValueToShare(result)) {
        logger.communicationSent++;
        final Serializable s = whisperer.getInformation(result);
        for (final int l : LIFELINE) {
          uncountedAsyncAt(place(l), () -> {
            workerLock.unblock();
            logger.communicationReceived.incrementAndGet();
            whisperer.integrateInformation(s, result);
          });
        }
      }
    }
  }

  /**
   * Computes the given bag and returns the aggregated result of this
   * computation.
   *
   * @param  <R>
   *                              type of the result produced by the computation
   * @param  <B>
   *                              type of the computation bag
   * @param  bag
   *                              the computation to be performed
   * @param  initResultSupplier
   *                              function that provides new empty result
   *                              instances
   * @param  emptyBagSupplier
   *                              function that provides new empty computation
   *                              bag instances
   * @return                    aggregated result of the computation
   */
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      B bag, SerializableSupplier<R> initResultSupplier,
      SerializableSupplier<B> emptyBagSupplier) {
    return compute(bag, initResultSupplier, emptyBagSupplier, emptyBagSupplier);
  }

  /**
   * Computes the given bag and returns the result of the distributed
   * computation.
   *
   * @param  <R>
   *                             type parameter for the result produced by the
   *                             computation
   * @param  <B>
   *                             type parameter for the computation to perform
   * @param  work
   *                             initial work to be processed
   * @param  resultInitializer
   *                             initializer for the result instance
   * @param  queueInitializer
   *                             initializer for the queue used for load
   *                             balancing purposes
   * @param  workerInitializer
   *                             initializer for the workers bag
   * @return                   instance of type R containing the result of the
   *                           distributed computation
   */
  @SuppressWarnings("unchecked")
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      B work, SerializableSupplier<R> resultInitializer,
      SerializableSupplier<B> queueInitializer,
      SerializableSupplier<B> workerInitializer) {
    // We reset every place
    final long initStart = System.nanoTime();
    resetAll(resultInitializer, queueInitializer, workerInitializer, null);

    // We launch the computation
    final long start = System.nanoTime();
    workerCount = 1;
    state = 0;
    finish(() -> run(work));
    final long computationFinish = System.nanoTime();
    // We gather the result back into place 0
    collectAllResult();
    final long resultGathering = System.nanoTime();

    // Preparation for method getLog if it is called
    computationLog = new Logger(initStart, start, computationFinish,
        resultGathering, CONFIGURATION.p);
    return (R) result;
  }

  /**
   * Launches the computation with a periodic whisper based information sharing
   * between the distributed processes. This method allows for differentiated
   * implementations of the splitting methods between the workers and the queues
   * used at each place for load balancing.
   *
   * @param  <R>
   *                              Type of the result produced by the computation
   * @param  <B>
   *                              Type of the computation
   * @param  <W>
   *                              Type in charge of performing the communication
   *                              between the hosts
   * @param  work
   *                              initial fragment of computation
   * @param  initResultSupplier
   *                              initializer for the result instance contained
   *                              by each process
   * @param  emptyQueueSupplier
   *                              initializer for the queues used to balance the
   *                              work between the workers and between processes
   * @param  emptyBagSupplier
   *                              initializer for the worker's bag
   * @param  whispererSupplier
   *                              initializer for the instance in charge of
   *                              handling the communication of information
   *                              during the computation
   * @return                    instance of type <em>R</em> containing the
   *                            result of the computation
   */
  @SuppressWarnings("unchecked")
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable, W extends Whisperer<? extends Serializable, R>> R computeWhisperedResult(
      B work, SerializableSupplier<R> initResultSupplier,
      SerializableSupplier<B> emptyQueueSupplier,
      SerializableSupplier<B> emptyBagSupplier,
      SerializableSupplier<W> whispererSupplier) {
    // We reset every place
    final long initStart = System.nanoTime();
    resetAll(initResultSupplier, emptyQueueSupplier, emptyBagSupplier,
        whispererSupplier);

    // We launch the computation
    final long start = System.nanoTime();

    finish(() -> {
      // First we launch a whisperer task on each host
      for (final Place p : places()) {
        asyncAt(p, () -> communicatorThread());
      }

      // Second we launch the actual computation
      workerCount = 1;
      state = 0;
      finish(() -> run(work));

      // The computation has completed, we shut down the whisperer tasks
      for (final Place p : places()) {
        uncountedAsyncAt(p, () -> {
          communicatorThreadShutdown = true;
          communicatorLock.unblock();
        });
      }
    });
    final long computationFinish = System.nanoTime();
    // We gather the result back into place 0
    collectAllResult();
    final long resultGathering = System.nanoTime();

    // Preparation for method getLog if it is called
    computationLog = new Logger(initStart, start, computationFinish,
        resultGathering, CONFIGURATION.p);
    return (R) result;
  }

  /**
   * Launches the computation with a periodic whisper based information sharing
   * between the distributed processes.
   *
   * @param  <R>
   *                              Type of the result produced by the computation
   * @param  <B>
   *                              Type of the computation
   * @param  <W>
   *                              Type in charge of performing the communication
   *                              between the hosts
   * @param  bag
   *                              initial fragment of computation
   * @param  initResultSupplier
   *                              initializer for the result instance contained
   *                              by each process
   * @param  emptyBagSupplier
   *                              initializer for the worker's bag and the two
   *                              queues used to balance the work
   * @param  whispererSupplier
   *                              initializer for the instance in charge of
   *                              handling the communication of information
   *                              during the computation
   * @return                    instance of type <em>R</em> containing the
   *                            result of the computation
   */
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable, W extends Whisperer<? extends Serializable, R> & Serializable> R computeWhisperedResult(
      B bag, SerializableSupplier<R> initResultSupplier,
      SerializableSupplier<B> emptyBagSupplier,
      SerializableSupplier<W> whispererSupplier) {
    return computeWhisperedResult(bag, initResultSupplier, emptyBagSupplier,
        emptyBagSupplier, whispererSupplier);
  }

  /**
   * Method called on this place when a victim of steal is answering and
   * providing some loot.
   * <p>
   * This method checks if the current place is "alive", meaning if it has any
   * workers.
   * <ul>
   * <li>If there are active workers, the loot is merged into the
   * {@link #intraPlaceQueue}.
   * <li>If no workers exist and the place is performing some steals, the loot
   * is placed in the first workerBag of collection {@link #workerBags} before
   * unlocking the "main" {@link #run(Bag)} thread progress which is either
   * stealing from random victims or stealing from lifelines. This will cause it
   * to resume computation by spawning a first worker (with the merged loot) as
   * part of the {@link #run(Bag)} routine.
   * <li>If the place is inactive, method {@link #run(Bag)} is launched with the
   * loot as parameter.
   * </ul>
   *
   * @param victim
   *                 the id from place sending the loot or {@code -1} if it is a
   *                 random steal
   * @param loot
   *                 the work that was stolen by this place
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void deal(int victim, Bag loot) {
    workerLock.unblock();
    if (victim < 0) {
      logger.stealsSuccess.incrementAndGet();
    } else {
      logger.lifelineStealsSuccess.incrementAndGet();
      lifelineEstablished.put(victim, false);
    }

    synchronized (workerBags) {
      switch (state) {
      case 0:

        /*
         * There are workers on the place -> we merge the loot into the
         * intra-place queue
         */
        synchronized (intraPlaceQueue) {
          intraPlaceQueue.merge(loot);
          logger.intraQueueFed.incrementAndGet();
          intraQueueEmpty = false;
        }

        return; // Placing this return instruction allows us to put the run call
                // out of the synchronized block without having to use an extra
                // condition.

      case -1:

        /*
         * If the place is currently stealing, the bag is given to the head of
         * collection workerBags. This head is the one which is going to be run
         * when the stealing stops and a new workerProcess is spawned in method
         * run
         */
        workerBags.peek().bag.merge(loot);
        state = 0; // Back into a running state
        workerCount = 1;
        synchronized (this) {
          notifyAll();
        }

        return;
      case -2:
        // There are no workers on this place, it needs to be waken up
        workerCount = 1;
        state = 0; // Possible concurrent lifeline answers will not spawn
                   // a new run method as this signals that this place is
                   // now "alive"
      }
    }

    // There were no workers, method run is launched.
    run(loot);
  }

  /**
   * Returns a Configuration instance showing the configuration of this GLB
   * instance.
   *
   * @return Configuration instance containing the configuration of this GLB
   *         instance.
   */
  public Configuration getConfiguration() {
    return CONFIGURATION;
  }

  /**
   * Gives back the log of the previous computation.
   *
   * @return the {@link PlaceLogger} instance of this place
   */
  public Logger getLog() {
    if (!logsGiven) {

      finish(() -> {
        for (final Place p : places()) {

          asyncAt(p, () -> {
            final PlaceLogger l = logger;
            asyncAt(place(0), () -> {
              computationLog.addPlaceLogger(l);
            });
          });
        }
      });
      logsGiven = true;
    }
    return computationLog;
  }

  /**
   * Activity spawned by method {@link #run(Bag)} to answer lifelines that were
   * not able to be answered straight away.
   * <p>
   * This process yields until a lifeline answer is signaled as possible or the
   * <em>shutdown</em> is activated by method {@link #run(Bag)}.
   */
  @SuppressWarnings("rawtypes")
  void lifelineAnswerThread() {
    logger.lifelineAnswerThreadStarted();

    do {

      /*
       * 1. Yield
       */
      logger.lifelineAnswerThreadInactive();
      try {
        ForkJoinPool.managedBlock(lifelineAnswerLock);
      } catch (final InterruptedException e) {
        // Should not happen since the implementation of Lock does not throw
        // InterruptedException
        e.printStackTrace();
      }
      lifelineToAnswer = false;
      workerLock.unblock();
      logger.lifelineThreadWokenUp++;

      logger.lifelineAnswerThreadActive();

      /*
       * 2. Answer lifelines
       */
      while (!lifelineThieves.isEmpty()) {
        Bag loot;
        synchronized (intraPlaceQueue) {
          if (interQueueEmpty) {
            break;
          }
          loot = interPlaceQueue.split(true);
          logger.interQueueSplit.incrementAndGet();
          interQueueEmpty = interPlaceQueue.isEmpty();
        }
        // Send the loot
        final int h = HOME.id;
        asyncAt(place(lifelineThieves.poll()), () -> deal(h, loot));
        logger.lifelineStealsSuffered.incrementAndGet();
      }
      if (interQueueEmpty) {
        requestInterQueueFeed();
      }

      /*
       * 3. Until "shutdown" is activated, repeat from step 1.
       */
    } while (!shutdown);

    logger.lifelineAnswerThreadEnded();
    lifelineAnswerThreadExited = true;
  }

  /**
   * Sub-routine of methods {@link #performRandomSteals()} and
   * {@link #performLifelineSteals()}. Gets some loot from the inter/intra place
   * queues and performs the status updates on these queues as necessary.
   *
   * @return some loot to be sent to thieves
   */
  @SuppressWarnings("rawtypes")
  Bag loot() {
    Bag loot = null;
    // Quick check on the other queue
    if (!interQueueEmpty) {
      synchronized (intraPlaceQueue) {
        if (!interQueueEmpty) {
          // if (intraQueueEmpty && interPlaceQueue.isSplittable()) {
          // intraPlaceQueue.merge(interPlaceQueue.split(false));
          // logger.interQueueSplit.incrementAndGet();
          // logger.intraQueueFed.incrementAndGet();
          // intraQueueEmpty = intraPlaceQueue.isEmpty(); // Update flag
          // }
          loot = interPlaceQueue.split(true);
          logger.interQueueSplit.incrementAndGet();
          interQueueEmpty = interPlaceQueue.isEmpty(); // Update flag
        }
      }
      if (interQueueEmpty) {
        requestInterQueueFeed();
      }
    }

    return loot;
  }

  /**
   * Part of the {@link #run(Bag)} procedure. Performs lifeline steals until
   * either of two things happen:
   * <ul>
   * <li>Some work is received through a lifeline
   * <li>All lifelines have been established
   * </ul>
   *
   * @return {@code true} if some work is received during the method's
   *         execution, {@code false} otherwise
   */
  boolean performLifelineSteals() {
    for (int i = 0; i < LIFELINE.length; i++) {
      final int lifeline = LIFELINE[i];
      if (!lifelineEstablished.get(lifeline)) { // We check if the lifeline was
                                                // previously established or not

        logger.lifelineStealsAttempted.incrementAndGet();
        lifelineEstablished.put(lifeline, true);

        final int h = HOME.id;
        asyncAt(place(lifeline), () -> steal(h));
      }

      synchronized (this) {
        try {
          wait(5); // Wait a while for an answer
        } catch (final InterruptedException e) {
          // If an Interrupted exception is thrown, it does not hurt the program
          // to have the thread resume its progress
        }
      }

      // Checks if some work was received
      synchronized (workerBags) {
        if (state == 0) { // State is put back to 0 in lifelineDeal when an
                          // answer is received
          return true;
        } else if (i == LIFELINE.length - 1) {
          // If all lifelines were established and still no positive answer was
          // received
          state = -2;
        }
      }
    }

    return false;
  }

  /**
   * Part of the {@link #run(Bag)} procedure. Performs random steals until one
   * of two things happen:
   * <ul>
   * <li>Some work is received by this place, either by a previously established
   * lifeline or through a random steal initiated by this method
   * <li>The maximum number of steals on a random place is reached
   * </ul>
   * These two events are not mutually exclusive, it can happen that the maximum
   * number of random steals was reached and that some work was received by this
   * place concurrently.
   *
   * @return {@code true} if some work is received during the method's
   *         execution, {@code false} otherwise
   */
  boolean performRandomSteals() {
    if (CONFIGURATION.p < 2) {
      return false;
    }
    for (int i = 0; i < CONFIGURATION.w; i++) {
      logger.stealsAttempted.incrementAndGet();
      // Choose a victim
      int victim = random.nextInt(CONFIGURATION.p - 1);
      if (victim >= HOME.id) {
        victim++;
      }

      final int h = HOME.id;
      asyncAt(place(victim), () -> steal(-h - 1));

      synchronized (this) {
        try {
          wait(5); // Wait a while for an answer
        } catch (final InterruptedException e) {
          // If an Interrupted exception is thrown, it does not hurt the program
          // to have the thread resume its progress
        }
      }

      // Checks if some work was received
      synchronized (workerBags) {
        if (state == 0) { // State is put back to 0 when an answer is received
          return true;
        }
      }
    }

    return false;

  }

  /**
   * Sets all the boolean in array {@link #feedInterQueueRequested} to
   * {@code true}. Is called when a it is noticed that member
   * {@link #interPlaceQueue} is empty. This will make the workers send part of
   * their work this queue.
   */
  void requestInterQueueFeed() {
    for (int i = 0; i < feedInterQueueRequested.length(); i++) {
      feedInterQueueRequested.set(i, 1);
    }
  }

  /**
   * Resets the local GLBcomputer instance to a ready to compute state in
   * preparation for the next computation. An instance of the result which the
   * computation is going to produce is kept aside for later use.
   * <p>
   *
   * @param <R>
   *                             type parameter of the result the computation to
   *                             come will produce
   * @param <B>
   *                             type parameter of the computation
   * @param <W>
   *                             type of the whisperer in charge of propagating
   *                             shared information between hosts
   *
   * @param resultInitSupplier
   *                             supplier of empty result instance
   * @param queueInitializer
   *                             supplier of empty queues for load balancing
   *                             purposes
   * @param workerInitializer
   *                             supplier of empty bag for workers
   * @param whispererSupplier
   *                             supplier of instances in charge of propagating
   *                             the shared information across hosts. May be
   *                             left {@code null} if this communication feature
   *                             is not desired.
   */
  <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable, W extends Whisperer<? extends Serializable, R>> void reset(
      SerializableSupplier<R> resultInitSupplier,
      SerializableSupplier<B> queueInitializer,
      SerializableSupplier<B> workerInitializer,
      SerializableSupplier<W> whispererSupplier) {

    // Resetting the configuration to the initial values
    CONFIGURATION.reset();

    // Resetting the logger
    logger = new PlaceLogger(CONFIGURATION, HOME.id);
    logsGiven = false;

    // Resetting the field used to keep the result
    result = resultInitSupplier.get();

    // Resetting the queues
    interPlaceQueue = queueInitializer.get();
    intraPlaceQueue = queueInitializer.get();

    // Resetting flags
    lifelineAnswerLock.reset();
    workerLock.reset();
    interQueueEmpty = true;
    intraQueueEmpty = true;
    lifelineAnswerThreadExited = true;
    tunerThreadExcited = true;
    state = -2;
    shutdown = false;

    // Removing old bags and getting some new ones
    workerBags.clear();

    for (int i = 0; i < CONFIGURATION.x; i++) {// We put as many new
                                               // empty bags as there are
                                               // possible concurrent
                                               // workers
      workerBags.add(new WorkerBag(i, workerInitializer.get()));
      feedInterQueueRequested.set(i, 1);
    }

    // We reset the established lifelines trackers
    final boolean lifelinesOn = HOME.id != 0;
    for (final int i : LIFELINE) {
      lifelineEstablished.put(i, lifelinesOn);
    }

    // We establish lifelines on this place for initial work-stealing conditions
    for (final int i : REVERSE_LIFELINE) {
      if (i != 0) {
        lifelineThieves.add(i);
      }
    }

    if (whispererSupplier != null) {
      whisperer = whispererSupplier.get();
      communicatorThreadShutdown = false;
    } else {
      whisperer = null;
      communicatorThreadShutdown = true; // Not necessary as this member will
                                         // not be used if there is no
                                         // whisperer. However, setting the
                                         // value to true maintains a certain
                                         // consistency
    }

  }

  /**
   * Resets all instances of GLBcomputer in the system.
   * <p>
   * Calls method
   * {@link #reset(SerializableSupplier, SerializableSupplier,SerializableSupplier, SerializableSupplier)}
   * on all places in the system. The tasks are performed asynchronously. The
   * method returns when all the instances on each place have completed their
   * reset.
   *
   * @param <R>
   *                             type parameter for result
   * @param <B>
   *                             type parameter for computation bag
   * @param <W>
   *                             type of the whisperer in charge of propagating
   *                             shared information between hosts
   *
   * @param resultInitSupplier
   *                             supplier of empty result instance
   * @param queueInitializer
   *                             supplier of empty queues for load balancing
   *                             purposes
   * @param workerInitializer
   *                             supplier of empty bag for workers
   * @param whispererSupplier
   *                             supplier of instances in charge of propagating
   *                             the shared information across hosts
   */
  <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable, W extends Whisperer<? extends Serializable, R>> void resetAll(
      SerializableSupplier<R> resultInitSupplier,
      SerializableSupplier<B> queueInitializer,
      SerializableSupplier<B> workerInitializer,
      SerializableSupplier<W> whispererSupplier) {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> reset(resultInitSupplier, queueInitializer,
            workerInitializer, whispererSupplier));
      }
    });

  }

  /**
   * Main procedure of a place.
   * <p>
   * Spawns the first worker thread (which will in turn recursively spawn other
   * worker threads). When all workers run out of work, attempts to steal work
   * from remote hosts.
   *
   * @param b
   *            the initial bag to compute.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void run(Bag b) {

    // Wait until the previous lifeline and tuner threads exited
    while (!lifelineAnswerThreadExited || !tunerThreadExcited) {
    }

    // Reset the flags and the locks
    lifelineAnswerThreadExited = false;
    shutdown = false;
    workerLock.reset();
    lifelineAnswerLock.reset();
    tunerLock.reset();

    // Spawn the lifeline answer and the tuner thread activities
    async(() -> lifelineAnswerThread());
    if (tuner != null) {
      tunerThreadExcited = false;
      uncountedAsyncAt(here(), () -> tunerThread());
    }

    // Prepare the first worker to process the work given as parameter
    workerBags.peek().bag.merge(b);

    do {
      do {
        // Spawn a first worker (which will spawn the others)
        final WorkerBag workerBag = workerBags.poll();
        finish(() -> workerProcess(workerBag)); // Working

        // All the workers have stopped, this place does not have any work and
        // will now attempt to steal some from other places
      } while (performRandomSteals());
    } while (performLifelineSteals());

    // Shutdown the lifelineAnswerThread and the tuner thread
    shutdown = true; // Flag used to signal to the activities they need to
                     // terminate
    logger.lifelineAnswerThreadHold();
    lifelineAnswerLock.unblock(); // Unblocks the progress of the lifeline
                                  // answer thread
    tunerLock.unblock(); // Unblocks the progress of the tuner thread
  }

  /**
   * Method called asynchronously by a thief to steal work from this place.
   * <p>
   *
   * @param thief
   *                the integer id of the place performing the steal, or `(-id -
   *                1)` if this is a random steal
   */
  @SuppressWarnings("rawtypes")
  synchronized void steal(int thief) {
    workerLock.unblock();

    final int h = HOME.id;
    final Bag loot = loot();

    if (thief >= 0) {
      // A lifeline is trying to steal some work
      logger.lifelineStealsReceived.incrementAndGet();

      if (loot == null) {
        // Steal does not immediately succeeds
        // The lifeline is registered to answer it later.
        lifelineThieves.offer(thief);
      } else {
        logger.lifelineStealsSuffered.incrementAndGet();
        asyncAt(place(thief), () -> deal(h, loot));
      }
    } else {
      // A random thief is trying to steal some work
      logger.stealsReceived.incrementAndGet();
      if (loot != null) {
        logger.stealsSuffered.incrementAndGet();
        asyncAt(place(-thief - 1), () -> deal(-1, loot));
      }
    }
  }

  /**
   * Activity in charge of the tuning mechanism
   */
  void tunerThread() {
    long lastCall = tuner.placeLaunched(logger, CONFIGURATION);

    for (;;) {
      tunerLock.setNextWakeup(lastCall + CONFIGURATION.t);
      try {
        ForkJoinPool.managedBlock(tunerLock);
      } catch (final InterruptedException e) {
        // Ignore
      }
      workerLock.unblock();
      if (shutdown) {
        tunerThreadExcited = true;
        return;
      }
      lastCall = tuner.tune(logger, CONFIGURATION);
    }
  }

  /**
   * Launches an distributed warm-up on each process in the distributed cluster.
   * <p>
   * For some computations, it can be beneficial to launch a smaller problem
   * instance to let the Java Virtual Machine perform some optimizations that
   * will yield better performance when the larger "real" computation is later
   * launched. This method launches the provided computation on each host of the
   * cluster. No load balance between the compute nodes in the cluster is
   * performed for the warm-up, only the load balance operations between the
   * workers of each host is performed.
   *
   * @param  <R>
   *                             type of the result produced by the warm-up
   *                             computation
   * @param  <B>
   *                             type of the computation at hand
   * @param  warmupBagSupplier
   *                             supplier of the work sample which will be
   *                             computed at each place independently as a
   *                             warm-up
   * @param  resultInitializer
   *                             initializer for the result instance of each
   *                             place
   * @param  queueInitializer
   *                             initializer for the two queues used at each
   *                             place to perform load balancing operations
   * @param  workerInitializer
   *                             initializer for the bags held by each worker
   * @return                   logger instance containing information about the
   *                           warm-up execution
   */
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> Logger warmup(
      SerializableSupplier<B> warmupBagSupplier,
      SerializableSupplier<R> resultInitializer,
      SerializableSupplier<B> queueInitializer,
      SerializableSupplier<B> workerInitializer) {
    final long reset = System.nanoTime();
    final long start = System.nanoTime();
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> {
          reset(resultInitializer, queueInitializer, workerInitializer, null);
          lifelineThieves.clear();
          for (final int i : LIFELINE) {
            lifelineEstablished.put(i, true);
          }
          deal(-1, warmupBagSupplier.get());
        });
      }
    });
    final long end = System.nanoTime();

    computationLog = new Logger(reset, start, end, end, CONFIGURATION.p);

    return getLog();
  }

  /**
   * Main procedure of a worker thread in a place
   * <p>
   * A worker is a thread that processes the computation on a place. It has a
   * {@link Bag} instance to process and an identifier.
   * <p>
   * Each worker follows the following routine:
   * <ol>
   * <li>Spawns a new {@link #workerProcess(WorkerBag)} if the bag it is
   * currently processing can be split and the {@link Configuration#x} limit was
   * not reached (meaning there are {@link Bag} instances left in member
   * {@link #workerBags})
   * <li>Checks if the {@link #intraPlaceQueue} bag is empty. If so and the
   * currently held bag can be split ({@link Bag#isSplittable()}), splits its
   * bag and merges the split content into {@link #intraPlaceQueue}.
   * <li>Checks if feeding the {@link #interPlaceQueue} was requested. If the
   * value for this worker in array {@link #feedInterQueueRequested} is
   * {@code true} and this worker can split its bag, the worker sends half of
   * the work it holds into the {@link #interPlaceQueue}.
   * <li>Check if there are pending lifeline answers that can be answered. If
   * so, unblocks the {@link #lifelineAnswerThread()}'s progress by unlocking
   * the {@link #lifelineAnswerLock}.
   * <li>If there are activities that are waiting for execution and the number
   * of active workers has reached the number of available cores on the system,
   * yields its execution to allow execution of other activities.
   * <li>Processes a chunk of its bag
   * <li>Repeat steps 1. to 6. until the {@link Bag} of which this worker is in
   * charge becomes empty.
   * <li>When the bag becomes empty as a result of splitting and processing it,
   * the worker attempts to get some more work from the {@link #intraPlaceQueue}
   * and the {@link #interPlaceQueue}. If successful in acquiring some work,
   * resume its routine from step 1. If unsuccessful, stops operating.
   * </ol>
   *
   * @param workerBag
   *                    computation to process along with an identifier for this
   *                    worker process
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void workerProcess(WorkerBag workerBag) {
    logger.workerStarted();
    final Bag bag = workerBag.bag; // Makes later accesses more compact

    for (;;) { // Infinite loop, not a mistake
      do {
        /*
         * 1. Checking if a new worker can be spawned
         */
        if (!workerBags.isEmpty() && bag.isSplittable()) {
          final WorkerBag wb = workerBags.poll();
          // polling of workerBags may yield null if a concurrent worker polled
          // the last bag, check is necessary.
          if (wb != null) {
            // We can spawn a new worker
            synchronized (workerBags) {
              workerCount++;
            }
            wb.bag.merge(bag.split(false));
            async(() -> workerProcess(wb));
          }
        }

        /*
         * 2. Checking the status of the Bag used for intra place load balancing
         */
        if (intraQueueEmpty) {
          if (bag.isSplittable()) {
            synchronized (intraPlaceQueue) {
              intraPlaceQueue.merge(bag.split(false));
              logger.intraQueueFed.incrementAndGet();
              intraQueueEmpty = intraPlaceQueue.isEmpty();
            }
          }
        }

        /*
         * 3. Checking if interQueue feeding was requested
         */
        if (feedInterQueueRequested.get(workerBag.workerId) == 1) {
          if (bag.isSplittable()) {
            synchronized (intraPlaceQueue) {
              interPlaceQueue.merge(bag.split(false));
              logger.interQueueFed.incrementAndGet();
              interQueueEmpty = interPlaceQueue.isEmpty();
            }

            feedInterQueueRequested.set(workerBag.workerId, 0);
          }
        }

        /*
         * 4. Checking if waiting lifelines can be answered
         */
        if (!lifelineThieves.isEmpty() && !interQueueEmpty) {
          logger.lifelineAnswerThreadHold();
          lifelineAnswerLock.unblock(); // unblocking lifeline answer thread,
          lifelineToAnswer = true;
        }

        /*
         * 5. Yield if need be
         */
        if (workerCount == CONFIGURATION.x
            && (POOL.hasQueuedSubmissions() || lifelineToAnswer)) {
          final Lock l = workerAvailableLocks.poll();
          if (l != null) {
            logger.workerYieldStart();
            try {
              ForkJoinPool.managedBlock(l);
            } catch (final InterruptedException e) {
              // Should not happen in practice as the implementation Lock does
              // not throw the InterruptedException
              e.printStackTrace();
            }
            logger.workerYieldStop();

            l.reset(); // Reset the lock after usage
            workerAvailableLocks.add(l);
          }
        }

        /*
         * 6. Process its bag
         */
        bag.process(CONFIGURATION.n, result);

      } while (!bag.isEmpty());// 7. Repeat previous steps until the bag becomes
                               // empty.

      logger.workerStealing(); // The worker is now stealing

      /*
       * 8. Intra-place load balancing
       */
      synchronized (workerBags) { // Decision on whether this worker is going to
                                  // continue is made here. This decision needs
                                  // to be done in a synchronized block to
                                  // guarantee mutual exclusion with method
                                  // lifelineDeal.

        // Attempt to steal some work from the intra-place bag
        if (!intraQueueEmpty) {
          Bag loot = null;
          synchronized (intraPlaceQueue) {
            if (!intraQueueEmpty) {
              loot = intraPlaceQueue.split(true); // If only a fragment can't
                                                  // be taken, we take the whole
                                                  // content of the
                                                  // intraPlaceQueue
              intraQueueEmpty = intraPlaceQueue.isEmpty(); // Flag update
              logger.intraQueueSplit.incrementAndGet();
            }
          }
          if (loot != null) {
            bag.merge(loot);
          }

        } else if (!interQueueEmpty) { // Couldn't steal from intraQueue, try on
                                       // interQueue
          Bag loot = null;
          synchronized (intraPlaceQueue) {
            if (!interQueueEmpty) {
              loot = interPlaceQueue.split(true); // Take from interplace
              logger.interQueueSplit.incrementAndGet();
              interQueueEmpty = interPlaceQueue.isEmpty(); // Update the flag
              /*
               * if (loot.isSplittable()) { // Put some work back into the intra
               * queue intraPlaceQueue.merge(loot.split(false));
               * logger.intraQueueFed.incrementAndGet(); intraQueueEmpty =
               * intraPlaceQueue.isEmpty(); // Update the flag }
               */
            }
          }
          if (interQueueEmpty) {
            requestInterQueueFeed();
          }
          if (loot != null) {
            bag.merge(loot);
          }

        } else {// Both queues were empty. The worker stops.
          workerBags.add(workerBag);
          workerCount--;
          if (workerCount == 0) {
            state = -1; // No more workers, we are now in stealing mode
          }
          logger.workerStopped();
          workerLock.unblock(); // A yielding worker can be unlocked.
                                // As this worker is terminating, its thread
                                // will be available for computation.
          return;
        }
      } // synchronized stealing block

      // Stealing from the queues in the place was successful. The worker goes
      // back to processing its fraction of the work.
      logger.workerResumed();

    } // Enclosing infinite for loop. Exit is done with the "return;" 7 lines
      // above.
  }
}
