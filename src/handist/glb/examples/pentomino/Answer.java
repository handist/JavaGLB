/**
 *
 */
package handist.glb.examples.pentomino;

import java.io.Serializable;
import java.util.ArrayList;

import handist.glb.util.Fold;

/**
 * @author Patrick Finnerty
 *
 */
public class Answer implements Fold<Answer>, Serializable {

  /** Serial version UID */
  private static final long serialVersionUID = 3877827129899016979L;

  /** Collection of moves that completely cover a pentomino board */
  ArrayList<Moves> answer;

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.util.Fold#fold(handist.glb.util.Fold)
   */
  @Override
  public void fold(Answer r) {
    answer.addAll(answer);
  }

  /**
   * Constructor
   */
  public Answer() {
    answer = new ArrayList<>();
  }

}
