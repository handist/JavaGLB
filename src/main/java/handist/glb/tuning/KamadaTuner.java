package handist.glb.tuning;

import java.io.Serializable;

import handist.glb.Configuration;
import handist.glb.PlaceLogger;

/**
 * Suggestion to use 10x smaller grain for host 0 compared to the other host
 * which use the provided value.
 *
 * @author Patrick
 *
 */
public class KamadaTuner implements Serializable, Tuner {

  /** Serial Version UID */
  private static final long serialVersionUID = 7940156771631748231L;

  @Override
  public long placeLaunched(PlaceLogger l, Configuration c) {
    final long stamp = System.nanoTime();
    c.t = 1000000000;
    if (l.place == 0) {
      c.n = c.originalN / 10;
    }
    l.NvalueTuned(stamp, c.n);
    return stamp;
  }

  @Override
  public long tune(PlaceLogger l, Configuration c) {
    return System.nanoTime();
  }
}
