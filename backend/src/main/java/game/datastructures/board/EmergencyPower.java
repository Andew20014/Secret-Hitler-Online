package game.datastructures.board;

/**
 * The powers that may be selected when an Article 48 or Enabling Act
 * emergency power card is enacted. Article 48 is resolved by the President,
 * the Enabling Act by the Chancellor. PRESIDENTIAL_PARDON is only available
 * for Article 48, and VOTE_OF_NO_CONFIDENCE only for the Enabling Act.
 */
public enum EmergencyPower {
    PROPAGANDA,
    POLICY_PEEK,
    IMPEACHMENT,
    MARKED_FOR_EXECUTION,
    EXECUTION,
    PRESIDENTIAL_PARDON,
    VOTE_OF_NO_CONFIDENCE,
}
