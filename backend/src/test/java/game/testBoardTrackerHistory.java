package game;

import game.datastructures.Policy;
import game.datastructures.board.FiveToSixPlayerBoard;
import game.datastructures.board.Board;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for Board's per-slot tracker history (which specific
 * Policy.Type filled each tracker slot), used so the frontend can render
 * e.g. an anti-communist policy's own art in the fascist tracker slot it
 * filled, instead of a generic fascist icon.
 */
public class testBoardTrackerHistory {

    @Test
    public void testFascistTrackerHistoryRecordsFascistAndAntiCommunist() {
        Board board = new FiveToSixPlayerBoard();
        board.enactPolicy(new Policy(Policy.Type.FASCIST));
        board.enactPolicy(new Policy(Policy.Type.ANTI_COMMUNIST));
        board.enactPolicy(new Policy(Policy.Type.FASCIST));

        List<Policy.Type> history = board.getFascistTrackerHistory();
        assertEquals(3, history.size());
        assertEquals(Policy.Type.FASCIST, history.get(0));
        assertEquals(Policy.Type.ANTI_COMMUNIST, history.get(1));
        assertEquals(Policy.Type.FASCIST, history.get(2));
        assertEquals(3, board.getNumFascistPolicies());
    }

    @Test
    public void testCommunistTrackerHistoryRecordsCommunistAntiFascistAndAnarchist() {
        Board board = new FiveToSixPlayerBoard();
        board.enactPolicy(new Policy(Policy.Type.COMMUNIST));
        board.enactPolicy(new Policy(Policy.Type.ANTI_FASCIST));
        board.enactPolicy(new Policy(Policy.Type.ANARCHIST));

        List<Policy.Type> history = board.getCommunistTrackerHistory();
        assertEquals(3, history.size());
        assertEquals(Policy.Type.COMMUNIST, history.get(0));
        assertEquals(Policy.Type.ANTI_FASCIST, history.get(1));
        assertEquals(Policy.Type.ANARCHIST, history.get(2));
        // getNumCommunistPolicies() merges communist and anarchist counts;
        // the history's length must stay in sync with that merged count so
        // the frontend never sees more filled slots than it has art for.
        assertEquals(3, board.getNumCommunistPolicies());
    }

    @Test
    public void testLiberalTrackerHistoryRecordsLiberalAndSocialDemocratic() {
        Board board = new FiveToSixPlayerBoard();
        board.enactPolicy(new Policy(Policy.Type.LIBERAL));
        board.enactPolicy(new Policy(Policy.Type.SOCIAL_DEMOCRATIC));

        List<Policy.Type> history = board.getLiberalTrackerHistory();
        assertEquals(2, history.size());
        assertEquals(Policy.Type.LIBERAL, history.get(0));
        assertEquals(Policy.Type.SOCIAL_DEMOCRATIC, history.get(1));
        assertEquals(2, board.getNumLiberalPolicies());
    }

    @Test
    public void testEmergencyPowersDoNotAppearInAnyTrackerHistory() {
        Board board = new FiveToSixPlayerBoard();
        board.enactPolicy(new Policy(Policy.Type.ARTICLE_48));
        board.enactPolicy(new Policy(Policy.Type.ENABLING_ACT));

        assertEquals(0, board.getLiberalTrackerHistory().size());
        assertEquals(0, board.getFascistTrackerHistory().size());
        assertEquals(0, board.getCommunistTrackerHistory().size());
    }

    @Test
    public void testRemoveFascistPolicyPopsHistoryInSyncWithCount() {
        Board board = new FiveToSixPlayerBoard();
        board.enactPolicy(new Policy(Policy.Type.FASCIST));
        board.enactPolicy(new Policy(Policy.Type.ANTI_COMMUNIST));

        board.removeFascistPolicy();

        assertEquals(1, board.getNumFascistPolicies());
        List<Policy.Type> history = board.getFascistTrackerHistory();
        assertEquals(1, history.size());
        assertEquals(Policy.Type.FASCIST, history.get(0));
    }

    @Test
    public void testRemoveCommunistPolicyPopsHistoryInSyncWithCount() {
        Board board = new FiveToSixPlayerBoard();
        board.enactPolicy(new Policy(Policy.Type.COMMUNIST));
        board.enactPolicy(new Policy(Policy.Type.ANTI_FASCIST));

        board.removeCommunistPolicy();

        assertEquals(1, board.getNumCommunistPolicies());
        List<Policy.Type> history = board.getCommunistTrackerHistory();
        assertEquals(1, history.size());
        assertEquals(Policy.Type.COMMUNIST, history.get(0));
    }
}
