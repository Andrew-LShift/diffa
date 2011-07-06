package net.lshift.diffa.participant.scanning;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Helper for building constraints from a web request.
 */
public class ConstraintsBuilder {
  private final HttpServletRequest req;
  private final List<ScanConstraint> result;

  public ConstraintsBuilder(HttpServletRequest req) {
    this.req = req;
    this.result = new ArrayList<ScanConstraint>();
  }

  /**
   * Transforms the builder into a list of constraints.
   * @return the constraint list.
   */
  public List<ScanConstraint> toList() {
    return result;
  }

  /**
   * Attempt to add a date range constraint for the given attribute. The constraint will be added if
   * one or both of [attrName]-start, [attrName]-end are present in the request.
   * @param attrName the name of the attribute
   */
  public void maybeAddDateRangeConstraint(String attrName) {
    String startVal = req.getParameter(attrName + "-start");
    String endVal = req.getParameter(attrName + "-end");

    if (startVal != null || endVal != null) {
      result.add(new DateRangeConstraint(attrName, startVal, endVal));
    }
  }

  /**
   * Attempt to add a time range constraint for the given attribute. The constraint will be added if
   * one or both of [attrName]-start, [attrName]-end are present in the request.
   * @param attrName the name of the attribute
   */
  public void maybeAddTimeRangeConstraint(String attrName) {
    String startVal = req.getParameter(attrName + "-start");
    String endVal = req.getParameter(attrName + "-end");

    if (startVal != null || endVal != null) {
      result.add(new TimeRangeConstraint(attrName, startVal, endVal));
    }
  }

  /**
   * Attempt to add a set constraint for the given attribute. The constraint will be added if there are
   * any arguments in the request with the given attribute name.
   * @param attrName the name of the attribute
   */
  public void maybeAddSetConstraint(String attrName) {
    String[] values = req.getParameterValues(attrName);

    if (values != null && values.length > 0) {
      HashSet<String> set = new HashSet<String>();
      Collections.addAll(set,values);
      result.add(new SetConstraint(attrName,set));
    }
  }

  /**
   * Attempt to add a integer range constraint for the given attribute. The constraint will be added if
   * one or both of [attrName]-start, [attrName]-end are present in the request.
   * @param attrName the name of the attribute
   */
  public void maybeAddIntegerRangeConstraint(String attrName) {
    String startVal = req.getParameter(attrName + "-start");
    String endVal = req.getParameter(attrName + "-end");

    if (startVal != null || endVal != null) {
      result.add(new IntegerRangeConstraint(attrName, startVal, endVal));
    }
  }

  /**
   * Attempt to add a string prefix constraint for the given attribute. The constraint will be added if
   * [attrName]-prefix is present in the request.
   * @param attrName the name of the attribute
   */
  public void maybeAddStringPrefixConstraint(String attrName) {
    String prefix = req.getParameter(attrName + "-prefix");

    if (prefix != null) {
      result.add(new StringPrefixConstraint(attrName, prefix));
    }
  }
}
