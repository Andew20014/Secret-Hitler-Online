package game.datastructures.board;

/**
 * Presidential powers are triggered when a set number of fascist policies have been enacted.
 * These vary between each {@code Board}.
 */
public enum PresidentialPower {
    NONE,
    PEEK,
    INVESTIGATE,
    EXECUTION,
    ELECTION,
    COMMUNISTS_INVESTIGATION,
    CONFESSION,
    FIVE_YEAR_PLAN,
    CONGRESS,
    RADICALIZATION,
}
