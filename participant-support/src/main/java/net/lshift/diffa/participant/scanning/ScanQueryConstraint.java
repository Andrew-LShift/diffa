package net.lshift.diffa.participant.scanning;

/**
 * Describes a constraint applied to a Scan Query.
 */
public interface ScanQueryConstraint {
  /**
   * Retrieves the name of the attribute being constrained.
   * @return the name.
   */
  String getAttributeName();

}
