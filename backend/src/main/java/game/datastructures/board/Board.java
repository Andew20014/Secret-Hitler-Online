package game.datastructures.board;

import game.datastructures.Policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Board implements Serializable {

    final int FASCIST_POLICIES_TO_WIN = 6;
    final int LIBERAL_POLICIES_TO_WIN = 5;
    final int LIBERAL_HARDER_POLICIES_TO_WIN = 6;
    final int COMMUNIST_6_TO_7_POLICIES_TO_WIN = 5;
    final int COMMUNIST_8UP_POLICIES_TO_WIN = 6;

    // The minimum number of fascist policies required before
    // fascists can win by electing Hitler chancellor
    final int MIN_POLICIES_FOR_CHANCELLOR_VICTORY = 3;

    private int numFascistPolicies;
    private int numLiberalPolicies;
    private int numCommunistPolicies;
    private int numAnarchistPolicies;

    // The specific Policy.Type enacted into each tracker slot, in order, so
    // the board can show e.g. an anti-communist card's own art in the
    // fascist tracker slot it filled, rather than a generic fascist icon.
    private final List<Policy.Type> liberalTrackerHistory = new ArrayList<>();
    private final List<Policy.Type> fascistTrackerHistory = new ArrayList<>();
    private final List<Policy.Type> communistTrackerHistory = new ArrayList<>();

    private Policy lastEnacted;


    /** Constructs a new board.
     * @modifies this
     * @effects this is a new, empty board.
     */
    public Board() {
        numFascistPolicies = 0;
        numLiberalPolicies = 0;
        numCommunistPolicies = 0;
        numAnarchistPolicies = 0;
    }

    /**
     * Enacts the given policy.
     * @param policy the Policy to enact.
     * @throws IllegalStateException if the liberals or fascists have already won.
     *                               (isLiberalVictory() or isFascistVictory() is true)
     * @modifies this
     * @effects adds {@code policy} to the count of liberal and fascist policies.
     */
    public void enactPolicy(Policy policy) {
        if (isLiberalVictory(true) || isFascistVictory() || isCommunistVictory(8)) {
            throw new IllegalStateException("Cannot enact a policy when victory conditions were already reached.");
        }
        switch (policy.getType()) {
            case FASCIST:
            case ANTI_COMMUNIST: // Placed on the fascist tracker.
                numFascistPolicies++;
                fascistTrackerHistory.add(policy.getType());
                break;
            case COMMUNIST:
            case ANTI_FASCIST: // Placed on the communist tracker.
                numCommunistPolicies++;
                communistTrackerHistory.add(policy.getType());
                break;
            case ANARCHIST:
                numAnarchistPolicies++;
                // Anarchist policies also count toward the merged communist
                // tracker/win condition (see getNumCommunistPolicies()), so
                // they're recorded here too to keep this history's length in
                // sync with that merged count.
                communistTrackerHistory.add(policy.getType());
                break;
            case LIBERAL:
            case SOCIAL_DEMOCRATIC: // Placed on the liberal tracker.
                numLiberalPolicies++;
                liberalTrackerHistory.add(policy.getType());
                break;
            case ARTICLE_48:
            case ENABLING_ACT:
                // Emergency power cards are resolved and removed from the game;
                // they never advance a tracker.
                break;
        }
        lastEnacted = policy;
    }

    /**
     * Removes one fascist policy from the fascist tracker (used by
     * anti-fascist and social democratic policies).
     * @modifies this
     * @effects decrements the fascist policy count, floored at 0.
     */
    public void removeFascistPolicy() {
        if (numFascistPolicies > 0) {
            numFascistPolicies--;
            fascistTrackerHistory.remove(fascistTrackerHistory.size() - 1);
        }
    }

    /**
     * Removes one communist policy from the communist tracker (used by
     * anti-communist and social democratic policies).
     * @modifies this
     * @effects decrements the communist policy count, floored at 0.
     */
    public void removeCommunistPolicy() {
        if (numCommunistPolicies > 0) {
            numCommunistPolicies--;
            communistTrackerHistory.remove(communistTrackerHistory.size() - 1);
        }
    }


    /**
     * Gets the type of the last enacted policy.
     * @throws NullPointerException if no policy has been enacted yet.
     * @return the Policy.Type of the last policy enacted.
     */
    public Policy.Type getLastEnactedType() {
        if (lastEnacted == null) {
            throw new NullPointerException("No policy has been enacted yet");
        }
        return lastEnacted.getType();
    }


    /**
     * Gets the count of fascist policies.
     * @return the number of fascist policies enacted.
     */
    public int getNumFascistPolicies() {
        return numFascistPolicies;
    }


    /**
     * Gets the count of communist policies.
     * @return the number of communist policies enacted.
     */
    public int getNumCommunistPolicies() {
        return numCommunistPolicies+numAnarchistPolicies;
    }

    /**
     * Gets the count of anarchist policies, on its own (not merged with
     * communist policies as {@link #getNumCommunistPolicies()} does for
     * tracker/win-condition purposes).
     * @return the number of anarchist policies enacted.
     */
    public int getNumAnarchistPolicies() {
        return numAnarchistPolicies;
    }


    /**
     * Gets the count of liberal policies.
     * @return the number of liberal policies enacted.
     */
    public int getNumLiberalPolicies() {
        return numLiberalPolicies;
    }

    /**
     * Gets the specific Policy.Type enacted into each liberal tracker slot,
     * in order (LIBERAL or SOCIAL_DEMOCRATIC), so the board can render the
     * exact card art for each slot rather than a generic liberal icon.
     * @return an unmodifiable, ordered view of the liberal tracker's history.
     */
    public List<Policy.Type> getLiberalTrackerHistory() {
        return Collections.unmodifiableList(liberalTrackerHistory);
    }

    /**
     * Gets the specific Policy.Type enacted into each fascist tracker slot,
     * in order (FASCIST or ANTI_COMMUNIST), so the board can render the
     * exact card art for each slot rather than a generic fascist icon.
     * @return an unmodifiable, ordered view of the fascist tracker's history.
     */
    public List<Policy.Type> getFascistTrackerHistory() {
        return Collections.unmodifiableList(fascistTrackerHistory);
    }

    /**
     * Gets the specific Policy.Type enacted into each communist tracker
     * slot, in order (COMMUNIST or ANTI_FASCIST), so the board can render
     * the exact card art for each slot rather than a generic communist icon.
     * @return an unmodifiable, ordered view of the communist tracker's history.
     */
    public List<Policy.Type> getCommunistTrackerHistory() {
        return Collections.unmodifiableList(communistTrackerHistory);
    }


    /**
     * Determines whether the liberal party won by policy count.
     * @return true if the number of Liberal Policies {@literal >=} {@code LIBERAL_POLICIES_TO_WIN}
     */
    public boolean isLiberalVictory(boolean harderWinCondition) {
        if (harderWinCondition) {
            return getNumLiberalPolicies() >= LIBERAL_HARDER_POLICIES_TO_WIN;
        } else {
            return getNumLiberalPolicies() >= LIBERAL_POLICIES_TO_WIN;
        }
    }


    /**
     * Determines whether the fascist party won by policy count.
     * @return true if the number of Fascist Policies {@literal >=} {@code FASCIST_POLICIES_TO_WIN}
     */
    public boolean isFascistVictory() {
        return getNumFascistPolicies() >= FASCIST_POLICIES_TO_WIN;
    }

    /**
     * @return the number of Fascist policies needed to win by policy count.
     */
    public int getFascistPoliciesToWin() {
        return FASCIST_POLICIES_TO_WIN;
    }

    /**
     * Determines whether the communist party won by policy count.
     * @return true if the number of Communist Policies {@literal >=} {@code COMMUNIST_POLICIES_TO_WIN}
     */
    public boolean isCommunistVictory(int numPlayers) {
        return getNumCommunistPolicies() >= getCommunistPoliciesToWin(numPlayers);
    }

    /**
     * @param numPlayers the number of players in the game.
     * @return the number of Communist policies needed to win by policy
     *         count, for a game with {@code numPlayers} players.
     */
    public int getCommunistPoliciesToWin(int numPlayers) {
        if (numPlayers >= 6 && numPlayers <= 7) {
            return COMMUNIST_6_TO_7_POLICIES_TO_WIN;
        } else {
            return COMMUNIST_8UP_POLICIES_TO_WIN;
        }
    }

    public boolean isAnarchistVonWithCommunists(int numPlayers) {
        return numAnarchistPolicies >= 2;
    }


    /**
     * Gets whether the last policy activated a power.
     * @requires a policy has already been enacted.
     * @return true if the last enacted policy activated a presidential power.
     */
    public boolean hasActivatedPower() {
        return getActivatedPower() != PresidentialPower.NONE;
    }

    /**
     * Gets the presidential power (if any) that was activated by the last policy.
     * @requires a policy has already been enacted.
     * @return If no presidential power was unlocked from the last policy enacted, returns NONE. Otherwise, returns the
     *         last activated presidential power.
     */
    public PresidentialPower getActivatedPower() {
        return PresidentialPower.NONE;
    }

    public boolean fascistsCanWinByElection() {
        return (getNumFascistPolicies() >= MIN_POLICIES_FOR_CHANCELLOR_VICTORY);
    }

}
