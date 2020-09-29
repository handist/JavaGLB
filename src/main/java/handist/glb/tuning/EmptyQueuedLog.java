package handist.glb.tuning;

import java.io.Serializable;

import handist.glb.Configuration;
import handist.glb.GLBcomputer;
import handist.glb.PlaceLogger;

/**
 * This is not a tuner as originally intended by this mechanism of the Global
 * Load Balancer. Instead of analyzing the logs of the computation in progress
 * to adjust some parameters, this class logs the number of times the intra- bag
 * has been emptied.
 *
 * The logging mechanism for the tuner is abused to log not the new value taken
 * by the grain size but rather the number of times the intra-bag of
 * {@link GLBcomputer} was emptied during the last tuning interval.
 *
 * @author Patrick
 *
 */
public class EmptyQueuedLog implements Tuner, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -7136650908831277345L;
  /** Last value of {@link PlaceLogger#intraQueueEmptied} read by this class */
  long lastEmptyCount;

  @Override
  public long placeLaunched(PlaceLogger l, Configuration c) {
    final long stamp = System.nanoTime();
    lastEmptyCount = l.intraQueueEmptied;
    return stamp;
  }

  @Override
  public long tune(PlaceLogger l, Configuration c) {
    final long stamp = System.nanoTime();
    final long newCount = l.intraQueueEmptied;
    l.NvalueTuned(stamp, (int) (newCount - lastEmptyCount));
    lastEmptyCount = newCount;
    return stamp;
  }

}
