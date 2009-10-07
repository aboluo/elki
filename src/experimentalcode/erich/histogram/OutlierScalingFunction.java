package experimentalcode.erich.histogram;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for scaling functions used by Outlier evaluation such as Histograms
 * and visualization.
 * 
 * When the function needs extra parameters, it should also implement {@link Parameterizable}.
 * 
 * @author Erich Schubert
 * 
 */
public interface OutlierScalingFunction {
  /**
   * Prepare is called once for each data set, before getScaled() will be called.
   * This function can be used to extract global parameters such as means, minimums
   * or maximums from the Database, Result or Annotation.
   * 
   * @param db Database used
   * @param result Result object
   * @param ann Annotation that is being used
   */
  public void prepare(Database<?> db, Result result, AnnotationResult<Double> ann);

  /**
   * Transform a given value using the scaling function.
   * 
   * @param value Original value
   * @return Scaled value
   */
  public double getScaled(double value);
}
