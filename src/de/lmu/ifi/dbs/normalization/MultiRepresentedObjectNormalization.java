package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Class to perform and undo a normalization on multi-represented objects with respect
 * to given normalizations for each representation.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MultiRepresentedObjectNormalization<O extends DatabaseObject> extends AbstractNormalization<MultiRepresentedObject<O>> {
  /**
   * Default parser.
   */
  public final static String DEFAULT_NORMALIZATION = AttributeWiseDoubleVectorNormalization.class.getName();

  /**
   * Label for parameter normalizations.
   */
  public final static String NORMALIZATION_P = "normalizations";

  /**
   * Description of parameter parser.
   */
  public final static String NORMALIZATION_D = "<classname_1,...,classname_n>a comma separated list of normalizations for each representation (default: " + DEFAULT_NORMALIZATION + ")";

  /**
   * A pattern defining a comma.
   */
  public static final Pattern SPLIT = Pattern.compile(",");

  /**
   * The normalizations for each representation.
   */
  private List<Normalization<O>> normalizations;

  /**
   * Sets normalization parameter to the optionhandler.
   */
  public MultiRepresentedObjectNormalization() {
    parameterToDescription.put(NORMALIZATION_P + OptionHandler.EXPECTS_VALUE, NORMALIZATION_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Performs a normalization on a set of feature vectors.
   *
   * @param featureVectors a set of feature vectors to be normalized
   * @return a set of normalized feature vectors corresponding to the given
   *         feature vectors
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          if feature vectors differ in length or values are not
   *          suitable to normalization
   */
  public List<MultiRepresentedObject<O>> normalize(List<MultiRepresentedObject<O>> featureVectors) throws NonNumericFeaturesException {
    if (featureVectors.size() == 0)
      return new ArrayList<MultiRepresentedObject<O>>();

    // number of representations
    int numberOfRepresentations = normalizations != null ? normalizations.size() : featureVectors.get(0).getNumberOfRepresentations();

    // init default normalizations
    if (normalizations == null) {
      normalizations = new ArrayList<Normalization<O>>(numberOfRepresentations);
      for (int r = 0; r < numberOfRepresentations; r++) {
        //noinspection unchecked
        normalizations.add(Util.instantiate(Normalization.class, DEFAULT_NORMALIZATION));
      }
    }

    // normalize each representation
    List<List<O>> objects = new ArrayList<List<O>>();
    for (int r = 0; r < numberOfRepresentations; r++) {
      List<O> objectsInRepresentation = new ArrayList<O>(featureVectors.size());
      for (MultiRepresentedObject<O> o : featureVectors) {
        if (numberOfRepresentations != o.getNumberOfRepresentations())
          throw new IllegalArgumentException("Number of representations differs!");
        objectsInRepresentation.add(o.getRepresentation(r));
      }

      Normalization<O> normalization = normalizations.get(r);
      objects.add(normalization.normalize(objectsInRepresentation));
    }

    // build the normalized multi-represented objects
    List<MultiRepresentedObject<O>> normalized = new ArrayList<MultiRepresentedObject<O>>();
    for (int i = 0; i < featureVectors.size(); i++) {
      List<O> representations = new ArrayList<O>(numberOfRepresentations);
      for (int r = 0; r < numberOfRepresentations; r++) {
        representations.add(objects.get(r).get(i));
      }
      MultiRepresentedObject<O> o = new MultiRepresentedObject<O>(representations);
      o.setID(featureVectors.get(i).getID());
      normalized.add(o);
    }

    return normalized;
  }

  /**
   * Transforms a set of feature vectores to the original attribute ranges.
   *
   * @param featureVectors a set of feature vectors to be transformed into original space
   * @return a set of feature vectors transformed into original space
   *         corresponding to the given feature vectors
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          if feature vectors differ in length or are not compatible
   *          with values initialized during normalization
   */
  public List<MultiRepresentedObject<O>> restore(List<MultiRepresentedObject<O>> featureVectors) throws NonNumericFeaturesException {
    List<MultiRepresentedObject<O>> restored = new ArrayList<MultiRepresentedObject<O>>(featureVectors.size());

    for (MultiRepresentedObject<O> o : featureVectors) {
      restored.add(restore(o));
    }

    return restored;
  }

  /**
   * Transforms a feature vector to the original attribute ranges.
   *
   * @param featureVector a feature vector to be transformed into original space
   * @return a feature vector transformed into original space corresponding to
   *         the given feature vector
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          feature vector is not compatible with values initialized
   *          during normalization
   */
  public MultiRepresentedObject<O> restore(MultiRepresentedObject<O> featureVector) throws NonNumericFeaturesException {
    List<O> restored = new ArrayList<O>();

    int r = featureVector.getNumberOfRepresentations();
    for (int i = 0; i < r; i++) {
      Normalization<O> normalization = normalizations.get(i);
      restored.add(normalization.restore(featureVector.getRepresentation(i)));
    }
    MultiRepresentedObject<O> o = new MultiRepresentedObject<O>(restored);
    o.setID(featureVector.getID());
    return o;
  }

  /**
   * Transforms a matrix describing an equation system of linear dependencies
   * derived on the normalized space to describe linear dependencies
   * quantitatively adapted to the original space.
   *
   * @param matrix the matrix to be transformed
   * @return a matrix describing an equation system of linear dependencies
   *         derived on the normalized space transformed to describe linear
   *         dependencies quantitatively adapted to the original space
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          if specified Matrix is not compatible with values initialized
   *          during normalization
   */
  public Matrix transform(Matrix matrix) throws NonNumericFeaturesException {
    throw new UnsupportedOperationException("Operation not supported!");
  }

  /**
   * Returns a string representation of this normalization. The specified prefix pre will be
   * the prefix of each new line. This method is used to write the parameters of
   * a normalization to a result of an algorithm using this normalization.
   *
   * @param pre the prefix of each new line
   * @return a string representation of this normalization
   */
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    for (Normalization<O> normalization : normalizations) {
      result.append(normalization.toString(pre));
    }

    return result.toString();
  }

  /**
   * Returns the setting of the attributes of the algorithm.
   *
   * @return the setting of the attributes of the algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings settings = result.get(0);
    settings.addSetting(NORMALIZATION_P, normalizations.toString());

    for (Normalization<O> normalization : normalizations) {
      result.addAll(normalization.getAttributeSettings());
    }

    return result;
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   *         todo
   */
  public String description() {
    return "";
  }

  /**
   * Sets the attributes of the class accordingly to the given parameters.
   * Returns a new String array containing those entries of the
   * given array that are neither expected nor used by this
   * Parameterizable.
   *
   * @param args parameters to set the attributes accordingly to
   * @return String[] an array containing the unused parameters
   * @throws IllegalArgumentException in case of wrong parameter-setting
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingOptions = super.setParameters(args);
    // normalizations
    if (optionHandler.isSet(NORMALIZATION_P)) {
      String normalizationsString = optionHandler.getOptionValue(NORMALIZATION_P);
      String[] normalizationClasses = SPLIT.split(normalizationsString);
      if (normalizationClasses.length == 0) {
        throw new IllegalArgumentException("No input files specified.");
      }
      this.normalizations = new ArrayList<Normalization<O>>(normalizationClasses.length);
      for (String normalizationClass : normalizationClasses) {
        //noinspection unchecked
        Normalization<O> n = Util.instantiate(Normalization.class, normalizationClass);
        n.setParameters(args);
        this.normalizations.add(n);
      }
    }

    return remainingOptions;
  }
}
