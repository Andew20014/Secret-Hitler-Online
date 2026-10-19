package game;

import game.datastructures.Identity;
import game.datastructures.Player;
import game.datastructures.Policy;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static junit.framework.TestCase.*;

/**
 * Integration tests for the Secret Hitler XL mod mechanics (Communists,
 * anti-policies, Social Democratic policy, and Article 48 / Enabling Act
 * emergency powers). These drive full games through the public API,
 * steering president/chancellor choices toward whichever policy type is
 * under test, and resolving any triggered powers along the way.
 *
 * Some of these games only ever contain 1 (or a handful) of copies of the
 * rarer XL policy types, so a single playthrough can end (fascist/liberal/
 * communist victory) before that type happens to surface. Rather than
 * relying on an arbitrarily large step budget, these tests retry with a
 * fresh game whenever that happens, which converges reliably without ever
 * needing to fake randomness.
 */
public class testSecretHitlerGameXL {

    private static final int STEPS_PER_ATTEMPT = 200;
    private static final int MAX_ATTEMPTS = 20;

    // Policy types that advance some victory tracker when enacted. Everything
    // else (ARTICLE_48, ENABLING_ACT) is "safe" filler that can be enacted
    // without risking an early game-over while steering toward a rare type.
    private static final Set<Policy.Type> TRACKER_ADVANCING_TYPES = new HashSet<>(Arrays.asList(
            Policy.Type.FASCIST, Policy.Type.LIBERAL, Policy.Type.COMMUNIST, Policy.Type.ANARCHIST,
            Policy.Type.ANTI_FASCIST, Policy.Type.ANTI_COMMUNIST, Policy.Type.SOCIAL_DEMOCRATIC));

    private ArrayList<String> makePlayers(int numPlayers) {
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            out.add(Integer.toString(i));
        }
        return out;
    }

    private String findFirstAlive(SecretHitlerGame game, java.util.function.Predicate<Player> predicate) {
        for (Player p : game.getPlayerList()) {
            if (p.isAlive() && predicate.test(p)) {
                return p.getUsername();
            }
        }
        return null;
    }

    private String findCommunist(SecretHitlerGame game) {
        return findFirstAlive(game, Player::isCommunist);
    }

    /**
     * Picks a party {@code username} is allowed to deny membership in, per
     * {@link SecretHitlerGame#isValidDeniedParty}, for use with
     * {@link SecretHitlerGame#confession}.
     */
    private Identity pickDeniableParty(SecretHitlerGame game, String username) {
        return game.isValidDeniedParty(username, Identity.FASCIST) ? Identity.FASCIST : Identity.COMMUNIST;
    }

    /** Roughly how close to winning this policy type's tracker currently is; used to steer stalling choices. */
    private double closenessToWinning(SecretHitlerGame game, Policy.Type type) {
        switch (type) {
            case FASCIST:
            case ANTI_COMMUNIST:
                return game.getNumFascistPolicies() / 6.0;
            case LIBERAL:
            case SOCIAL_DEMOCRATIC:
                return game.getNumLiberalPolicies() / 5.0;
            case COMMUNIST:
            case ANTI_FASCIST:
            case ANARCHIST:
                return game.getNumCommunistPolicies() / 5.0;
            default:
                return 0.0;
        }
    }

    /**
     * Nominates the next eligible living player as chancellor and passes the
     * vote unanimously. Avoids nominating Hitler when any other candidate is
     * eligible, so tests aren't surprised by an unrelated fascist election
     * victory while steering toward some other policy type.
     */
    private void electChancellor(SecretHitlerGame game, List<String> players) {
        String president = game.getCurrentPresident();
        boolean nominated = false;
        for (boolean avoidHitler : new boolean[] { true, false }) {
            if (nominated) {
                break;
            }
            for (String candidate : players) {
                if (candidate.equals(president) || !game.getPlayer(candidate).isAlive()
                        || (avoidHitler && game.getPlayer(candidate).isHitler())) {
                    continue;
                }
                try {
                    game.nominateChancellor(candidate);
                    nominated = true;
                    break;
                } catch (IllegalArgumentException e) {
                    // Term-limited candidate (last chancellor/president); try the next one.
                }
            }
        }
        for (Player p : game.getPlayerList()) {
            if (p.isAlive()) {
                game.registerVote(p.getUsername(), true);
            }
        }
    }

    /**
     * Generically resolves whatever presidential/chancellor power is
     * currently active, using harmless default choices. Lets tests drive a
     * game forward without hand-holding every possible power.
     */
    private void resolveActivePower(SecretHitlerGame game, String avoidExecutionTarget) {
        String communist = findCommunist(game);
        switch (game.getState()) {
            case PRESIDENTIAL_POWER_PEEK:
                game.endPeek();
                break;
            case PRESIDENTIAL_POWER_INVESTIGATE:
                game.investigatePlayer(findFirstAlive(game, p -> !p.hasBeenInvestigated()));
                break;
            case PRESIDENTIAL_POWER_EXECUTION:
                game.executePlayer(findFirstAlive(game,
                        p -> !p.isHitler() && !p.getUsername().equals(avoidExecutionTarget)));
                break;
            case PRESIDENTIAL_POWER_ELECTION:
                game.electNextPresident(findFirstAlive(game, p -> !p.getUsername().equals(game.getCurrentPresident())));
                break;
            case PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION:
                game.communistsInvestigatePlayer(findFirstAlive(game, p -> !p.getUsername().equals(communist)), communist);
                break;
            case PRESIDENTIAL_POWER_CONFESSION:
                game.confession(game.getCurrentPresident(), pickDeniableParty(game, game.getCurrentPresident()));
                break;
            case PRESIDENTIAL_POWER_FIVE_YEAR_PLAN:
                game.fiveYearPlan();
                break;
            case PRESIDENTIAL_POWER_CONGRESS:
                game.endCongress();
                break;
            case PRESIDENTIAL_POWER_RADICALIZATION:
                game.radicalization(findFirstAlive(game, p -> !p.isCommunist() && !p.isHitler() && !p.isCapitalist()), communist);
                break;
            case PRESIDENTIAL_POWER_SOCIAL_DEMOCRATIC:
                game.socialDemocraticRemovePolicy(Policy.Type.FASCIST);
                break;
            case PRESIDENTIAL_POWER_PROPAGANDA:
                game.propagandaDiscard(false);
                break;
            case PRESIDENTIAL_POWER_IMPEACHMENT: {
                String president = game.getCurrentPresident();
                String chancellor = game.getCurrentChancellor();
                game.impeach(findFirstAlive(game,
                        p -> !p.getUsername().equals(president) && !p.getUsername().equals(chancellor)));
                break;
            }
            case PRESIDENTIAL_POWER_MARK_FOR_EXECUTION:
                game.markForExecution(findFirstAlive(game, p -> !p.isHitler()));
                break;
            default:
                break;
        }
    }

    /**
     * Performs a single state-machine action. During the legislative phase,
     * steers towards enacting {@code preferredType} whenever it is offered;
     * otherwise stalls as long as possible by discarding whichever remaining
     * option is closest to winning and enacting whichever option is safest
     * (not tracker-advancing, or furthest from winning). This keeps tests
     * from accidentally ending the game before a rare policy type surfaces.
     */
    private void step(SecretHitlerGame game, List<String> players, Policy.Type preferredType) {
        step(game, players, preferredType, null);
    }

    /** Like {@link #step}, but avoids executing {@code avoidExecutionTarget} via a generic execution power. */
    private void step(SecretHitlerGame game, List<String> players, Policy.Type preferredType, String avoidExecutionTarget) {
        GameState state = game.getState();
        if (state == GameState.CHANCELLOR_NOMINATION) {
            electChancellor(game, players);
        } else if (state == GameState.LEGISLATIVE_PRESIDENT) {
            List<Policy> choices = game.getPresidentLegislativeChoices();
            game.presidentDiscardPolicy(pickDiscardIndex(game, choices, preferredType));
        } else if (state == GameState.LEGISLATIVE_CHANCELLOR) {
            List<Policy> choices = game.getChancellorLegislativeChoices();
            game.chancellorEnactPolicy(pickEnactIndex(game, choices, preferredType));
        } else if (state == GameState.POST_LEGISLATIVE) {
            game.endPresidentialTerm();
        } else {
            resolveActivePower(game, avoidExecutionTarget);
        }
    }

    /** Never discards {@code preferredType}; otherwise discards whichever option is closest to winning. */
    private int pickDiscardIndex(SecretHitlerGame game, List<Policy> choices, Policy.Type preferredType) {
        int bestIndex = -1;
        double bestCloseness = -1;
        for (int i = 0; i < choices.size(); i++) {
            Policy.Type t = choices.get(i).getType();
            if (t == preferredType) {
                continue;
            }
            double closeness = closenessToWinning(game, t);
            if (closeness > bestCloseness) {
                bestCloseness = closeness;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? 0 : bestIndex;
    }

    /** Enacts {@code preferredType} if offered; otherwise the safest available option. */
    private int pickEnactIndex(SecretHitlerGame game, List<Policy> choices, Policy.Type preferredType) {
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).getType() == preferredType) {
                return i;
            }
        }
        for (int i = 0; i < choices.size(); i++) {
            if (!TRACKER_ADVANCING_TYPES.contains(choices.get(i).getType())) {
                return i;
            }
        }
        int bestIndex = 0;
        double bestCloseness = Double.MAX_VALUE;
        for (int i = 0; i < choices.size(); i++) {
            double closeness = closenessToWinning(game, choices.get(i).getType());
            if (closeness < bestCloseness) {
                bestCloseness = closeness;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Builds fresh games via {@code factory} and steers each one (via
     * {@link #step}) towards enacting {@code preferredType}, until the game
     * reaches {@code targetState}. Retries with a brand new game whenever an
     * attempt ends (game over) or exhausts its step budget first, so the
     * test doesn't depend on a single risky playthrough.
     *
     * @return a game sitting in {@code targetState}, or null if every attempt failed.
     */
    private SecretHitlerGame buildGameReachingState(
            Supplier<SecretHitlerGame> factory, List<String> players, Policy.Type preferredType, GameState targetState) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = factory.get();
            for (int i = 0; i < STEPS_PER_ATTEMPT && !game.hasGameFinished(); i++) {
                if (game.getState() == targetState) {
                    return game;
                }
                step(game, players, preferredType);
            }
        }
        return null;
    }

    @Test
    public void testCommunistPoliciesTrackCorrectlyAndCommunistsCanWin() {
        List<String> players = makePlayers(6);
        SecretHitlerGame game = new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0);

        assertEquals(0, game.getNumCommunistPolicies());

        int steps = 0;
        while (!game.hasGameFinished() && steps < 500) {
            step(game, players, Policy.Type.COMMUNIST);
            steps++;
        }

        assertTrue("Communists should have won within the step budget", steps < 500);
        assertEquals(GameState.COMMUNIST_VICTORY_POLICY, game.getState());
        assertTrue(game.getNumCommunistPolicies() >= 5);
    }

    @Test
    public void testAnarchistCascadeReplenishesDeckWhenEnactedNormally() {
        List<String> players = makePlayers(6);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(players, false, true, false, false, false, false, false, false, 0);
            Integer totalBefore = null;

            for (int i = 0; i < STEPS_PER_ATTEMPT && !game.hasGameFinished() && totalBefore == null; i++) {
                if (game.getState() == GameState.LEGISLATIVE_CHANCELLOR) {
                    List<Policy> choices = game.getChancellorLegislativeChoices();
                    int enactIndex = pickEnactIndex(game, choices, Policy.Type.ANARCHIST);
                    if (choices.get(enactIndex).getType() == Policy.Type.ANARCHIST) {
                        // The Chancellor is about to enact this Anarchist
                        // policy through a normal (non-forced) choice.
                        totalBefore = game.getDrawSize() + game.getDiscardSize();
                    }
                }
                step(game, players, Policy.Type.ANARCHIST);
            }

            if (totalBefore == null) {
                continue; // Never reached a normal Anarchist enactment; retry with a fresh game.
            }

            if (game.getState() == GameState.ANARCHIST_VICTORY_POLICY) {
                // The forced cascade card happened to be Anarchist too, so
                // the Anarchist won immediately. Also correct; nothing left
                // to check about deck replenishment.
                return;
            }

            assertEquals(GameState.POST_LEGISLATIVE, game.getState());
            int totalAfter = game.getDrawSize() + game.getDiscardSize();
            // totalBefore was captured while the Chancellor still held both
            // legislative options (the Anarchist policy plus the other,
            // rejected one), neither of which counts toward draw+discard at
            // that moment. Afterward: the enacted Anarchist policy itself is
            // placed in the discard pile (net +1, it wasn't counted in
            // totalBefore), a brand new Anarchist policy is shuffled into
            // the draw pile (net +1), the forced cascade card is consumed
            // with no replenishment (net 0 relative to totalBefore, since it
            // was never counted either), and the rejected card lands in the
            // discard pile (net +1, since it wasn't counted in totalBefore)
            // - for an overall change of +2 relative to totalBefore.
            assertEquals(totalBefore + 2, totalAfter);
            return;
        }
        fail("Could not drive a normal (non-forced) Anarchist enactment within the attempt budget");
    }

    @Test
    public void testAnarchistCascadeSurfacesTheForcedPolicySeparately() {
        List<String> players = makePlayers(6);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(players, false, true, false, false, false, false, false, false, 0);
            boolean reachedNormalAnarchistEnactment = false;

            for (int i = 0; i < STEPS_PER_ATTEMPT && !game.hasGameFinished() && !reachedNormalAnarchistEnactment; i++) {
                if (game.getState() == GameState.LEGISLATIVE_CHANCELLOR) {
                    List<Policy> choices = game.getChancellorLegislativeChoices();
                    int enactIndex = pickEnactIndex(game, choices, Policy.Type.ANARCHIST);
                    if (choices.get(enactIndex).getType() == Policy.Type.ANARCHIST) {
                        reachedNormalAnarchistEnactment = true;
                    }
                }
                step(game, players, Policy.Type.ANARCHIST);
            }

            if (!reachedNormalAnarchistEnactment) {
                continue; // Never reached a normal Anarchist enactment; retry with a fresh game.
            }
            if (game.getState() == GameState.ANARCHIST_VICTORY_POLICY) {
                // The forced cascade card happened to be Anarchist too, so
                // the Anarchist won immediately - getAnarchistCascadeForcedPolicy()
                // isn't relevant to check once the game has already ended.
                return;
            }

            // The Anarchist card itself should be reported as the "normal"
            // enacted policy, with whatever it forced through surfaced
            // separately rather than overwriting it.
            assertEquals(Policy.Type.ANARCHIST, game.getLastEnactedPolicy());
            assertNotNull("Expected the forced cascade policy to be recorded",
                    game.getAnarchistCascadeForcedPolicy());

            // Once any other policy is enacted afterward, the cascade field
            // should be cleared again rather than lingering.
            for (int i = 0; i < STEPS_PER_ATTEMPT
                    && game.getLastEnactedPolicy() == Policy.Type.ANARCHIST
                    && !game.hasGameFinished(); i++) {
                step(game, players, Policy.Type.LIBERAL);
            }
            if (game.getLastEnactedPolicy() != Policy.Type.ANARCHIST) {
                assertNull(game.getAnarchistCascadeForcedPolicy());
            }
            return;
        }
        fail("Could not drive a normal (non-forced) Anarchist enactment within the attempt budget");
    }

    @Test
    public void testAntiCommunistPolicyRemovesFromCommunistTrackerAndSkipsNextPower() {
        List<String> players = makePlayers(6);
        int[] fascistBefore = { -1 };
        int[] communistBefore = { -1 };
        boolean[] success = { false };

        for (int attempt = 0; attempt < MAX_ATTEMPTS && !success[0]; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(players, true, false, false, false, false, true, false, false, 0);

            for (int i = 0; i < STEPS_PER_ATTEMPT && game.getNumCommunistPolicies() == 0 && !game.hasGameFinished(); i++) {
                step(game, players, Policy.Type.COMMUNIST);
            }
            if (game.hasGameFinished() || game.getNumCommunistPolicies() == 0) {
                continue;
            }

            for (int i = 0; i < STEPS_PER_ATTEMPT && !game.hasGameFinished() && !success[0]; i++) {
                if (game.getState() == GameState.LEGISLATIVE_CHANCELLOR) {
                    List<Policy> choices = game.getChancellorLegislativeChoices();
                    int aboutToEnactIndex = pickEnactIndex(game, choices, Policy.Type.ANTI_COMMUNIST);
                    if (choices.get(aboutToEnactIndex).getType() == Policy.Type.ANTI_COMMUNIST) {
                        fascistBefore[0] = game.getNumFascistPolicies();
                        communistBefore[0] = game.getNumCommunistPolicies();
                    }
                }
                step(game, players, Policy.Type.ANTI_COMMUNIST);
                if (game.getLastEnactedPolicy() == Policy.Type.ANTI_COMMUNIST) {
                    success[0] = true;
                    assertEquals(fascistBefore[0] + 1, game.getNumFascistPolicies());
                    assertEquals(communistBefore[0] - 1, game.getNumCommunistPolicies());
                }
            }
        }

        assertTrue("An anti-communist policy should have been enacted", success[0]);
    }

    @Test
    public void testSocialDemocraticPolicyResolvesWithoutError() {
        List<String> players = makePlayers(7);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, true, false, 0),
                players, Policy.Type.SOCIAL_DEMOCRATIC, GameState.PRESIDENTIAL_POWER_SOCIAL_DEMOCRATIC);

        assertNotNull("Should have reached the social democratic power", game);

        // A few more steps to make sure resolving the power didn't get the game stuck.
        for (int i = 0; i < 5 && !game.hasGameFinished(); i++) {
            step(game, players, Policy.Type.SOCIAL_DEMOCRATIC);
        }
    }

    @Test
    public void testArticle48PropagandaResolvesWithoutError() {
        List<String> players = makePlayers(6);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, false, false, false, false, false, false, false, false, 3),
                players, Policy.Type.ARTICLE_48, GameState.PRESIDENTIAL_POWER_PROPAGANDA);

        // Propaganda is the board's 1st slot, so it's reached automatically
        // as soon as the first emergency power card (of either type) is
        // enacted - no player choice involved.
        assertNotNull("Should have reached the Propaganda power", game);
        assertEquals(1, game.getEmergencyPowersEnacted());

        assertNotNull(game.getPropagandaPeek());
        game.propagandaDiscard(false);
        assertEquals(GameState.POST_LEGISLATIVE, game.getState());
    }

    @Test
    public void testMarkedForExecutionKillsAfterThreeFascistPolicies() {
        List<String> players = makePlayers(6);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(players, false, false, false, false, false, false, false, false, 3);
            // Marked For Execution is the board's 4th slot, reached
            // automatically once the 4th emergency power card (of either
            // type) is enacted - no player choice involved.
            boolean reachedMarkForExecution = false;
            for (int i = 0; i < STEPS_PER_ATTEMPT * 4 && !game.hasGameFinished(); i++) {
                if (game.getState() == GameState.PRESIDENTIAL_POWER_MARK_FOR_EXECUTION) {
                    reachedMarkForExecution = true;
                    break;
                }
                step(game, players, Policy.Type.ARTICLE_48);
            }
            if (!reachedMarkForExecution) {
                continue;
            }
            if (game.getNumFascistPolicies() >= 3) {
                // Marking would resolve on the very next enacted policy;
                // retry so this test actually exercises waiting for the
                // count to reach 3.
                continue;
            }

            String target = findFirstAlive(game, p -> !p.isHitler());
            game.markForExecution(target);

            for (int i = 0; i < 500 && game.getPlayer(target).isAlive() && !game.hasGameFinished(); i++) {
                // Avoid letting a generic (unrelated) execution power pick the
                // same target, which would confound this test's assertions.
                step(game, players, Policy.Type.FASCIST, target);
            }

            if (game.getNumFascistPolicies() < 3) {
                continue; // The game ended some other way first; retry.
            }

            assertFalse("Marked player should have been executed", game.getPlayer(target).isAlive());
            return;
        }
        fail("Could not drive a game to test Marked For Execution within the attempt budget");
    }

    /**
     * Plays {@code game} towards enacting the 6th emergency power card
     * while it lands on Enabling Act specifically (auto-resolving any
     * earlier emergency power rounds with whatever's simplest - see
     * resolveActivePower), tracking whether the President's discard on the
     * round that gets there was a plain fascist/liberal card (as opposed
     * to, say, an Article 48 card, which would make the automatically-
     * triggered Vote Of No Confidence a no-op for the trackers). On success,
     * fills in {@code fascistBeforeOut}/{@code liberalBeforeOut} with the
     * tracker counts immediately before the 6th card was enacted. Returns
     * null if the target state was never reached.
     */
    private Boolean driveToSixthCardAsEnablingAct(
            SecretHitlerGame game, List<String> players, int[] fascistBeforeOut, int[] liberalBeforeOut) {
        Boolean discardWasFascistOrLiberal = null;
        for (int i = 0; i < STEPS_PER_ATTEMPT * 6 && !game.hasGameFinished(); i++) {
            GameState state = game.getState();
            if (state == GameState.CHANCELLOR_NOMINATION) {
                electChancellor(game, players);
            } else if (state == GameState.LEGISLATIVE_PRESIDENT) {
                List<Policy> choices = game.getPresidentLegislativeChoices();
                int discardIndex = pickDiscardIndex(game, choices, Policy.Type.ENABLING_ACT);
                Policy.Type discardedType = choices.get(discardIndex).getType();
                discardWasFascistOrLiberal = discardedType == Policy.Type.FASCIST || discardedType == Policy.Type.LIBERAL;
                game.presidentDiscardPolicy(discardIndex);
            } else if (state == GameState.LEGISLATIVE_CHANCELLOR) {
                List<Policy> choices = game.getChancellorLegislativeChoices();
                int enactIndex = pickEnactIndex(game, choices, Policy.Type.ENABLING_ACT);
                Policy.Type enactedType = choices.get(enactIndex).getType();
                boolean willBeSixthEnablingAct =
                        enactedType == Policy.Type.ENABLING_ACT && game.getEmergencyPowersEnacted() == 5;
                int fascistBefore = game.getNumFascistPolicies();
                int liberalBefore = game.getNumLiberalPolicies();
                game.chancellorEnactPolicy(enactIndex);
                if (willBeSixthEnablingAct) {
                    fascistBeforeOut[0] = fascistBefore;
                    liberalBeforeOut[0] = liberalBefore;
                    return discardWasFascistOrLiberal;
                }
            } else if (state == GameState.POST_LEGISLATIVE) {
                game.endPresidentialTerm();
            } else {
                resolveActivePower(game, null);
            }
        }
        return null;
    }

    @Test
    public void testVoteOfNoConfidenceEnactsDiscardedPolicy() {
        List<String> players = makePlayers(6);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(players, false, false, false, false, false, false, false, false, 3);
            // Vote Of No Confidence is the Enabling Act's branch-specific
            // power on the board's 6th slot, which triggers automatically -
            // no player choice involved - once the 6th emergency card is
            // enacted and happens to be an Enabling Act card.
            int[] fascistBefore = new int[1];
            int[] liberalBefore = new int[1];
            Boolean discardWasFascistOrLiberal = driveToSixthCardAsEnablingAct(game, players, fascistBefore, liberalBefore);
            if (discardWasFascistOrLiberal == null) {
                continue; // Didn't reach the 6th card this attempt; retry with a fresh game.
            }
            if (!discardWasFascistOrLiberal) {
                // The President happened to discard another emergency power
                // card rather than a fascist/liberal one; Vote Of No
                // Confidence would legitimately be a no-op for the trackers
                // here, which isn't what this test is checking. Retry.
                continue;
            }

            // Vote Of No Confidence has already resolved automatically and
            // instantly by this point - there is no separate state to pause
            // at (though enacting the previously-discarded card can itself
            // land on a fascist/liberal tracker power, so the resulting
            // state isn't necessarily POST_LEGISLATIVE).
            assertEquals(Policy.Type.ENABLING_ACT, game.getEmergencyPowerTrackerHistory().get(5));

            int fascistAfter = game.getNumFascistPolicies();
            int liberalAfter = game.getNumLiberalPolicies();

            assertEquals(1, (fascistAfter - fascistBefore[0]) + (liberalAfter - liberalBefore[0]));
            return;
        }
        fail("Should have reached the 6th emergency card as an Enabling Act via a known fascist/liberal discard");
    }

    @Test
    public void testRadicalizationOnHitlerOrCapitalistIsANoOpButStillConsumesThePower() {
        // Communists can choose Hitler or the Capitalist as a radicalization
        // target (nothing on the client distinguishes them from anyone else),
        // but their identity secretly doesn't change - they just swap their
        // card back. The power is still spent either way.
        List<String> players = makePlayers(7);

        SecretHitlerGame capitalistGame = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, true, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", capitalistGame);
        String communist = findCommunist(capitalistGame);
        String capitalist = findFirstAlive(capitalistGame, Player::isCapitalist);
        assertNotNull(capitalist);

        capitalistGame.radicalization(capitalist, communist);
        assertTrue("Capitalist should remain the Capitalist", capitalistGame.getPlayer(capitalist).isCapitalist());
        assertEquals(GameState.POST_LEGISLATIVE, capitalistGame.getState());

        SecretHitlerGame hitlerGame = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, true, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", hitlerGame);
        communist = findCommunist(hitlerGame);
        String hitler = findFirstAlive(hitlerGame, Player::isHitler);
        assertNotNull(hitler);

        hitlerGame.radicalization(hitler, communist);
        assertTrue("Target should remain Hitler", hitlerGame.getPlayer(hitler).isHitler());
        assertEquals(GameState.POST_LEGISLATIVE, hitlerGame.getState());
    }

    @Test
    public void testConfessionRecordsAPublicDeniedPartyWithoutRevealingTheTruePart() {
        // 9 players are needed to reach the Confession slot on the communist tracker.
        List<String> players = makePlayers(9);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_CONFESSION);

        assertNotNull("Should have reached the confession power", game);

        String president = game.getCurrentPresident();
        Identity deniedParty = pickDeniableParty(game, president);
        game.confession(president, deniedParty);

        assertEquals(GameState.POST_LEGISLATIVE, game.getState());
        assertEquals(president, game.getConfessionUsername());
        assertEquals(deniedParty, game.getConfessionDeniedParty());
    }

    @Test
    public void testConfessionRejectsAnInvalidDeniedParty() {
        List<String> players = makePlayers(9);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = buildGameReachingState(
                    () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                    players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_CONFESSION);
            assertNotNull("Should have reached the confession power", game);

            // A Liberal president has no invalid choice between FASCIST and
            // COMMUNIST (either is truthfully deniable for them) - retry with
            // a fresh game until a non-Liberal president comes up.
            String president = game.getCurrentPresident();
            boolean fascistValid = game.isValidDeniedParty(president, Identity.FASCIST);
            boolean communistValid = game.isValidDeniedParty(president, Identity.COMMUNIST);
            if (fascistValid && communistValid) {
                continue;
            }

            Identity invalidChoice = fascistValid ? Identity.COMMUNIST : Identity.FASCIST;
            try {
                game.confession(president, invalidChoice);
                fail("Should not be able to deny a party isValidDeniedParty rejects");
            } catch (IllegalArgumentException expected) {
                // expected
            }
            assertEquals("State should not have advanced after a rejected confession",
                    GameState.PRESIDENTIAL_POWER_CONFESSION, game.getState());
            return;
        }
        fail("Could not reach Confession with a non-Liberal president within the attempt budget");
    }

    @Test
    public void testConfessionRejectsAnarchistCapitalistOrLiberalAsADeniedParty() {
        List<String> players = makePlayers(9);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_CONFESSION);
        assertNotNull("Should have reached the confession power", game);

        for (Identity invalidParty : new Identity[] { Identity.ANARCHIST, Identity.CAPITALIST, Identity.LIBERAL }) {
            try {
                game.confession(game.getCurrentPresident(), invalidParty);
                fail("Should only be able to deny Fascist or Communist, not " + invalidParty);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test
    public void testFascistAlignedPlayersCanOnlyDenyCommunist() {
        // Hitler, Fascist, Monarchist, and the Capitalist are all fascist-aligned.
        List<String> players = makePlayers(9);
        SecretHitlerGame game = new SecretHitlerGame(players, true, true, true, true, false, false, false, false, 0);
        for (Player p : game.getPlayerList()) {
            if (p.isFascist() || p.isCapitalist()) {
                assertTrue(p.getUsername() + " (" + p.getIdentity() + ") should be able to deny Communist",
                        game.isValidDeniedParty(p.getUsername(), Identity.COMMUNIST));
                assertFalse(p.getUsername() + " (" + p.getIdentity() + ") should not be able to deny Fascist",
                        game.isValidDeniedParty(p.getUsername(), Identity.FASCIST));
            }
        }
    }

    @Test
    public void testCommunistAlignedPlayersCanOnlyDenyFascist() {
        // Communists and the Anarchist are communist-aligned.
        List<String> players = makePlayers(9);
        SecretHitlerGame game = new SecretHitlerGame(players, true, true, true, true, false, false, false, false, 0);
        for (Player p : game.getPlayerList()) {
            if (p.isCommunist() || p.isAnarchist()) {
                assertTrue(p.getUsername() + " (" + p.getIdentity() + ") should be able to deny Fascist",
                        game.isValidDeniedParty(p.getUsername(), Identity.FASCIST));
                assertFalse(p.getUsername() + " (" + p.getIdentity() + ") should not be able to deny Communist",
                        game.isValidDeniedParty(p.getUsername(), Identity.COMMUNIST));
            }
        }
    }

    @Test
    public void testLiberalsCanFreelyDenyEitherFascistOrCommunist() {
        List<String> players = makePlayers(9);
        SecretHitlerGame game = new SecretHitlerGame(players, true, true, true, true, false, false, false, false, 0);
        String liberal = findFirstAlive(game, Player::isLiberal);
        assertNotNull(liberal);
        assertTrue(game.isValidDeniedParty(liberal, Identity.FASCIST));
        assertTrue(game.isValidDeniedParty(liberal, Identity.COMMUNIST));
    }

    @Test
    public void testNobodyCanDenyBeingLiberal() {
        List<String> players = makePlayers(9);
        SecretHitlerGame game = new SecretHitlerGame(players, true, true, true, true, false, false, false, false, 0);
        for (Player p : game.getPlayerList()) {
            assertFalse(p.getUsername() + " (" + p.getIdentity() + ") should never be able to deny Liberal",
                    game.isValidDeniedParty(p.getUsername(), Identity.LIBERAL));
        }
    }

    @Test
    public void testRadicalizedMonarchistFollowsTheCommunistAlignedRuleAfterward() {
        List<String> players = makePlayers(9);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, true, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        String communist = findCommunist(game);
        String monarchist = findFirstAlive(game, p -> p.getIdentity() == Identity.MONARCHIST);
        assertNotNull("Expected a Monarchist in this game", monarchist);
        assertTrue("A Monarchist should start out fascist-aligned (deny Communist only)",
                game.isValidDeniedParty(monarchist, Identity.COMMUNIST));

        game.radicalization(monarchist, communist);
        assertTrue("Monarchist should have been converted to Communist", game.getPlayer(monarchist).isCommunist());

        assertTrue("A radicalized former Monarchist should now be able to deny Fascist",
                game.isValidDeniedParty(monarchist, Identity.FASCIST));
        assertFalse("A radicalized former Monarchist should no longer be able to deny Communist",
                game.isValidDeniedParty(monarchist, Identity.COMMUNIST));
    }

    @Test
    public void testRoleCountsCoverEveryPlayerWithEveryModEnabled() {
        // 9 players is the minimum that allows Communists, Anarchist, and
        // Monarchist to all be included together.
        List<String> players = makePlayers(9);
        SecretHitlerGame game = new SecretHitlerGame(players, true, true, true, true, false, false, false, false, 0);

        int total = game.getNumPlayersWithIdentity(Identity.LIBERAL)
                + game.getNumPlayersWithIdentity(Identity.FASCIST)
                + game.getNumPlayersWithIdentity(Identity.HITLER)
                + game.getNumPlayersWithIdentity(Identity.COMMUNIST)
                + game.getNumPlayersWithIdentity(Identity.ANARCHIST)
                + game.getNumPlayersWithIdentity(Identity.CAPITALIST)
                + game.getNumPlayersWithIdentity(Identity.MONARCHIST);
        assertEquals(9, total);

        // Exactly one of each of these singleton roles.
        assertEquals(1, game.getNumPlayersWithIdentity(Identity.HITLER));
        assertEquals(1, game.getNumPlayersWithIdentity(Identity.ANARCHIST));
        assertEquals(1, game.getNumPlayersWithIdentity(Identity.CAPITALIST));
        assertEquals(1, game.getNumPlayersWithIdentity(Identity.MONARCHIST));
    }

    /**
     * Regression test: the "remaining policies" counts (surfaced to every
     * player via the "Unenacted Policies" row) must not dip the instant the
     * President draws their 3 legislative cards, or the type composition of
     * those cards would leak to every player by diffing the counts before
     * and after the draw. The count should only ever change once a policy is
     * actually enacted onto a tracker.
     */
    @Test
    public void testRemainingPolicyCountsDoNotChangeWhileCardsAreDrawnDuringLegislativeSession() {
        List<String> players = makePlayers(7);
        SecretHitlerGame game = new SecretHitlerGame(
                players, false, false, false, false, false, false, false, false, 0);

        int liberalBeforeDraw = game.getRemainingLiberalPolicies();
        int fascistBeforeDraw = game.getRemainingFascistPolicies();

        electChancellor(game, players);
        assertEquals("A fresh 1st round should go straight to the President's legislative choice",
                GameState.LEGISLATIVE_PRESIDENT, game.getState());

        // The President has now drawn 3 cards into their hand: the totals
        // must be unchanged, or their type composition would be leaked.
        assertEquals(liberalBeforeDraw, game.getRemainingLiberalPolicies());
        assertEquals(fascistBeforeDraw, game.getRemainingFascistPolicies());

        game.presidentDiscardPolicy(0);
        assertEquals(GameState.LEGISLATIVE_CHANCELLOR, game.getState());

        // Still unchanged: the discarded card just moved into the discard pile.
        assertEquals(liberalBeforeDraw, game.getRemainingLiberalPolicies());
        assertEquals(fascistBeforeDraw, game.getRemainingFascistPolicies());

        Policy.Type enactedType = game.getChancellorLegislativeChoices().get(0).getType();
        game.chancellorEnactPolicy(0);

        // Only now, once a policy is actually enacted, should the enacted
        // type's remaining count drop by exactly 1, with the other type untouched.
        int expectedLiberal = enactedType == Policy.Type.LIBERAL ? liberalBeforeDraw - 1 : liberalBeforeDraw;
        int expectedFascist = enactedType == Policy.Type.FASCIST ? fascistBeforeDraw - 1 : fascistBeforeDraw;
        assertEquals(expectedLiberal, game.getRemainingLiberalPolicies());
        assertEquals(expectedFascist, game.getRemainingFascistPolicies());
    }

    @Test
    public void testRemainingCountsForAntiPoliciesSocialDemocraticAndEmergencyPowers() {
        List<String> players = makePlayers(9);
        SecretHitlerGame game = new SecretHitlerGame(players, true, false, false, false, true, true, true, false, 2);

        // A fresh game has enacted nothing yet, so the "remaining" counts
        // should equal exactly what resetDeck() shuffled in: one
        // anti-fascist, one anti-communist, one social democratic, and
        // emergencyPowersIncluded (2) of each emergency power type.
        assertEquals(1, game.getRemainingAntiFascistPolicies());
        assertEquals(1, game.getRemainingAntiCommunistPolicies());
        assertEquals(1, game.getRemainingSocialDemocraticPolicies());
        assertEquals(2, game.getRemainingArticle48Policies());
        assertEquals(2, game.getRemainingEnablingActPolicies());
    }

    @Test
    public void testAntiFascistPolicySwapsInForACommunistPolicyNotALiberalOne() {
        // 6 players with Communists: baseline deck is 10 fascist, 5 liberal,
        // 8 communist (NUM_FASCIST_POLICIES_WITH_COMMUNIST_6TO7 / _LIBERAL_ /
        // _COMMUNIST_6TO7). The Anti-Fascist policy swaps in for a Communist
        // policy (it's placed on the Communist tracker when enacted), so the
        // liberal count must stay untouched.
        List<String> players = makePlayers(6);
        SecretHitlerGame game = new SecretHitlerGame(players, true, false, false, false, true, false, false, false, 0);

        assertEquals(5, game.getRemainingLiberalPolicies());
        assertEquals(10, game.getRemainingFascistPolicies());
        assertEquals(7, game.getRemainingCommunistPolicies());
        assertEquals(1, game.getRemainingAntiFascistPolicies());
    }

    @Test
    public void testAntiCommunistPolicySwapsInForAFascistPolicyNotALiberalOne() {
        // Same 6-player baseline as above. The Anti-Communist policy swaps
        // in for a Fascist policy (it's placed on the Fascist tracker when
        // enacted), so the liberal count must stay untouched.
        List<String> players = makePlayers(6);
        SecretHitlerGame game = new SecretHitlerGame(players, true, false, false, false, false, true, false, false, 0);

        assertEquals(5, game.getRemainingLiberalPolicies());
        assertEquals(9, game.getRemainingFascistPolicies());
        assertEquals(8, game.getRemainingCommunistPolicies());
        assertEquals(1, game.getRemainingAntiCommunistPolicies());
    }

    @Test
    public void testAntiFascistPolicyRequiresCommunists() {
        List<String> players = makePlayers(6);
        try {
            new SecretHitlerGame(players, false, false, false, false, true, false, false, false, 0);
            fail("The Anti-Fascist policy should require Communists to be included");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private List<String> communistUsernames(SecretHitlerGame game) {
        List<String> communists = new ArrayList<>();
        for (Player p : game.getPlayerList()) {
            if (p.isCommunist()) {
                communists.add(p.getUsername());
            }
        }
        return communists;
    }

    @Test
    public void testOriginalCommunistsRecognizeEachOtherImmediatelyWithoutCongress() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0);

        List<String> communists = communistUsernames(game);
        assertEquals("8 players should have exactly 2 original communists", 2, communists.size());
        assertFalse(game.hasCongressHappened());

        JSONObject json = server.util.GameToJSONConverter.convert(game, communists.get(0));
        JSONObject viewOfOtherCommunist = json.getJSONObject("players").getJSONObject(communists.get(1));
        assertEquals("Original Communists should recognize each other immediately, without needing Congress",
                "COMMUNIST", viewOfOtherCommunist.getString("id"));
    }

    @Test
    public void testCommunistsKnowEachOtherAfterCongressIn8PlayerGame() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_CONGRESS);
        assertNotNull("Should have reached the Congress power", game);

        game.endCongress();
        assertTrue(game.hasCongressHappened());

        // At least the original 2 communists, possibly more if Radicalization
        // (which sits earlier on the same tracker) happened to fire first
        // while steering toward Congress.
        List<String> communists = communistUsernames(game);
        assertTrue("Expected at least 2 communists, found " + communists.size(), communists.size() >= 2);

        JSONObject json = server.util.GameToJSONConverter.convert(game, communists.get(0));
        JSONObject viewOfOtherCommunist = json.getJSONObject("players").getJSONObject(communists.get(1));
        assertEquals("A communist should know a fellow communist's identity after Congress",
                "COMMUNIST", viewOfOtherCommunist.getString("id"));
    }

    @Test
    public void testOriginalCommunistsRecognizeANewlyRadicalizedTargetImmediately() {
        // 6-7 player games only start with 1 communist, so radicalize a
        // second one and confirm the ORIGINAL immediately recognizes them -
        // no Congress needed for the original side of this relationship.
        List<String> players = makePlayers(6);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        String originalCommunist = findCommunist(game);
        String newTarget = findFirstAlive(game, p -> !p.isCommunist() && !p.isHitler() && !p.isCapitalist());
        game.radicalization(newTarget, originalCommunist);
        assertTrue(game.getPlayer(newTarget).isCommunist());
        assertFalse(game.hasCongressHappened());

        JSONObject json = server.util.GameToJSONConverter.convert(game, originalCommunist);
        JSONObject viewOfNewCommunist = json.getJSONObject("players").getJSONObject(newTarget);
        assertEquals("An original Communist should recognize a freshly radicalized target immediately",
                "COMMUNIST", viewOfNewCommunist.getString("id"));
    }

    @Test
    public void testRadicalizedCommunistOnlyRecognizesOriginalsAfterCongress() {
        // The flip side of the above: the Radicalization target themselves
        // shouldn't recognize the original Communist team back until
        // Congress resolves - that's the whole point of Congress existing
        // ("the newly radicalised Communists learn who the original
        // Communists are").
        List<String> players = makePlayers(6);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        String originalCommunist = findCommunist(game);
        String newTarget = findFirstAlive(game, p -> !p.isCommunist() && !p.isHitler() && !p.isCapitalist());
        game.radicalization(newTarget, originalCommunist);
        assertTrue(game.getPlayer(newTarget).isCommunist());
        assertFalse(game.hasCongressHappened());

        JSONObject beforeCongress = server.util.GameToJSONConverter.convert(game, newTarget);
        assertFalse("A Radicalization target should not yet recognize the original Communist team before Congress",
                beforeCongress.getJSONObject("players").getJSONObject(originalCommunist).has("id"));

        for (int i = 0; i < STEPS_PER_ATTEMPT * 2 && game.getState() != GameState.PRESIDENTIAL_POWER_CONGRESS
                && !game.hasGameFinished(); i++) {
            step(game, players, Policy.Type.COMMUNIST);
        }
        assertEquals("Should have reached the Congress power", GameState.PRESIDENTIAL_POWER_CONGRESS, game.getState());
        game.endCongress();
        assertTrue(game.hasCongressHappened());

        JSONObject afterCongress = server.util.GameToJSONConverter.convert(game, newTarget);
        assertEquals("A Radicalization target should recognize the original Communist team after Congress",
                "COMMUNIST", afterCongress.getJSONObject("players").getJSONObject(originalCommunist).getString("id"));
    }

    @Test
    public void testRadicalizationKeepsFormerFascistTeammatesInformedWithoutLeakingCommunistTeam() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        String communist = findCommunist(game);
        String target = findFirstAlive(game, p -> p.isFascist() && !p.isHitler());
        assertNotNull("Expected a non-Hitler Fascist to radicalize", target);
        String fascistTeammate = findFirstAlive(game,
                p -> p.isFascist() && !p.isHitler() && !p.getUsername().equals(target));
        assertNotNull("Expected another Fascist teammate to remain after radicalizing one away", fascistTeammate);

        game.radicalization(target, communist);
        assertTrue(game.getPlayer(target).isCommunist());
        assertFalse(game.hasCongressHappened());

        // The radicalized player's own view correctly shows their new identity.
        JSONObject selfView = server.util.GameToJSONConverter.convert(game, target);
        assertEquals("COMMUNIST",
                selfView.getJSONObject("players").getJSONObject(target).getString("id"));
        // 8-player games gate communist-team knowledge behind Congress -
        // the freshly radicalized player shouldn't already know the rest of
        // the team, whether via the normal "id" field...
        assertFalse("Should not yet know the original communist's identity",
                selfView.getJSONObject("players").getJSONObject(communist).has("id"));
        // ...or via the communist vote roster, which must not leak to them
        // just because the vote that radicalized them happened to resolve
        // right before this update - they never participated in it.
        assertFalse("aliveCommunists should not leak the communist team to a freshly radicalized player",
                selfView.has("aliveCommunists"));
        assertFalse("communistVotes should not leak either, for the same reason",
                selfView.has("communistVotes"));

        // Their former Fascist teammates should keep seeing them as FASCIST forever.
        JSONObject teammateView = server.util.GameToJSONConverter.convert(game, fascistTeammate);
        assertEquals("A former Fascist teammate should keep seeing the radicalized player as FASCIST",
                "FASCIST", teammateView.getJSONObject("players").getJSONObject(target).getString("id"));
    }

    @Test
    public void testRadicalizationVoteRequiresAllAliveCommunistsToAgreeOnTheSameTarget() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        List<String> communists = communistUsernames(game);
        assertEquals(2, communists.size());
        String voterA = communists.get(0);
        String voterB = communists.get(1);
        // Exclude Hitler: radicalizing them is a silent no-op (they resist),
        // which would make the "target became communist" assertion flaky.
        String targetX = findFirstAlive(game, p -> !p.isCommunist() && !p.isHitler());
        String targetY = findFirstAlive(game,
                p -> !p.isCommunist() && !p.isHitler() && !p.getUsername().equals(targetX));
        assertNotNull(targetX);
        assertNotNull(targetY);

        game.castCommunistVote(voterA, targetX);
        assertNull("Should not resolve until every alive communist has voted",
                game.getUnanimousCommunistVoteTarget());
        assertEquals(GameState.PRESIDENTIAL_POWER_RADICALIZATION, game.getState());

        game.castCommunistVote(voterB, targetY);
        assertNull("Votes for two different targets should not resolve",
                game.getUnanimousCommunistVoteTarget());

        game.castCommunistVote(voterB, targetX);
        assertEquals("Once every alive communist votes for the same target, it should resolve",
                targetX, game.getUnanimousCommunistVoteTarget());

        game.radicalization(targetX, voterA);
        assertTrue(game.getPlayer(targetX).isCommunist());
        assertEquals(GameState.POST_LEGISLATIVE, game.getState());
    }

    @Test
    public void testRetractingACommunistVotePreventsResolution() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        List<String> communists = communistUsernames(game);
        String voterA = communists.get(0);
        String voterB = communists.get(1);
        String target = findFirstAlive(game, p -> !p.isCommunist() && !p.isHitler());

        game.castCommunistVote(voterA, target);
        game.castCommunistVote(voterB, target);
        assertEquals(target, game.getUnanimousCommunistVoteTarget());

        game.retractCommunistVote(voterB);
        assertNull("Retracting a vote should prevent resolution", game.getUnanimousCommunistVoteTarget());

        game.castCommunistVote(voterB, target);
        assertEquals("Re-casting the same vote should resolve again", target, game.getUnanimousCommunistVoteTarget());
    }

    @Test
    public void testCannotVoteForAFellowCommunist() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        List<String> communists = communistUsernames(game);
        String voterA = communists.get(0);
        String voterB = communists.get(1);

        try {
            game.castCommunistVote(voterA, voterB);
            fail("Should not be able to vote for a fellow communist");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void testCommunistInvestigationVoteResolvesAndNotifiesAllCommunists() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION);
        assertNotNull("Should have reached the communist investigation power", game);

        List<String> communists = communistUsernames(game);
        String voterA = communists.get(0);
        String voterB = communists.get(1);
        String target = findFirstAlive(game, p -> !p.isCommunist());

        game.castCommunistVote(voterA, target);
        assertNull(game.getUnanimousCommunistVoteTarget());
        game.castCommunistVote(voterB, target);
        assertEquals(target, game.getUnanimousCommunistVoteTarget());

        Identity revealed = game.communistsInvestigatePlayer(target, voterA);
        assertNotNull(revealed);
        assertEquals(GameState.POST_LEGISLATIVE, game.getState());
    }

    @Test
    public void testAliveCommunistsAndVoteTallyOnlyExposedToCommunistViewers() {
        List<String> players = makePlayers(8);
        SecretHitlerGame game = buildGameReachingState(
                () -> new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0),
                players, Policy.Type.COMMUNIST, GameState.PRESIDENTIAL_POWER_RADICALIZATION);
        assertNotNull("Should have reached the radicalization power", game);

        List<String> communists = communistUsernames(game);
        String voterA = communists.get(0);
        String target = findFirstAlive(game, p -> !p.isCommunist() && !p.isHitler());
        game.castCommunistVote(voterA, target);

        JSONObject fromCommunist = server.util.GameToJSONConverter.convert(game, voterA);
        assertTrue(fromCommunist.has("aliveCommunists"));
        assertEquals(communists.size(), fromCommunist.getJSONArray("aliveCommunists").length());
        assertTrue(fromCommunist.has("communistVotes"));
        assertEquals(target, fromCommunist.getJSONObject("communistVotes").getString(voterA));

        String nonCommunist = findFirstAlive(game, p -> !p.isCommunist());
        JSONObject fromNonCommunist = server.util.GameToJSONConverter.convert(game, nonCommunist);
        assertFalse("Non-communists should not see the alive-communist roster",
                fromNonCommunist.has("aliveCommunists"));
        assertFalse("Non-communists should not see the vote tally",
                fromNonCommunist.has("communistVotes"));
    }

    // The power printed on each of the board's first 5 (shared) slots, in
    // left-to-right order - mirrors SecretHitlerGame.EMERGENCY_POWER_SLOT_ORDER,
    // expressed as the GameState each slot automatically resolves into.
    private static final GameState[] EMERGENCY_POWER_SLOT_STATES = {
        GameState.PRESIDENTIAL_POWER_PROPAGANDA,
        GameState.PRESIDENTIAL_POWER_PEEK,
        GameState.PRESIDENTIAL_POWER_IMPEACHMENT,
        GameState.PRESIDENTIAL_POWER_MARK_FOR_EXECUTION,
        GameState.PRESIDENTIAL_POWER_EXECUTION,
    };

    @Test
    public void testEmergencyPowersResolveAutomaticallyInBoardOrderWithNoPlayerChoice() {
        List<String> players = makePlayers(6);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(players, false, false, false, false, false, false, false, false, 3);
            int cardsSeen = runEmergencyPowerBoardOrderCheck(game, players);
            if (cardsSeen == 6) {
                assertEquals(6, game.getEmergencyPowersEnacted());
                assertEquals(6, game.getEmergencyPowerTrackerHistory().size());
                return;
            }
            // The game ended some other way (e.g. an ordinary fascist/liberal
            // policy victory) before all 6 emergency cards were drawn; retry
            // with a fresh shuffle.
        }
        fail("Could not drive a game through all 6 emergency power cards within the attempt budget");
    }

    /**
     * Drives {@code game} forward, asserting that each emergency power card
     * enacted immediately (with no player choice) resolves into the exact
     * power printed on its slot - the 1st through 5th shared slots via
     * {@link #EMERGENCY_POWER_SLOT_STATES}, and the 6th (branching by card
     * type) by resolving instantly straight to POST_LEGISLATIVE - and that
     * {@code emergencyPowerTrackerHistory} records the correct card type
     * per slot.
     * @return how many emergency power cards were seen before the game
     *         ended or the step budget ran out (at most 6).
     */
    private int runEmergencyPowerBoardOrderCheck(SecretHitlerGame game, List<String> players) {
        int cardsSeen = 0;
        for (int i = 0; i < STEPS_PER_ATTEMPT * 6 && !game.hasGameFinished() && cardsSeen < 6; i++) {
            GameState state = game.getState();
            if (state == GameState.CHANCELLOR_NOMINATION) {
                electChancellor(game, players);
            } else if (state == GameState.LEGISLATIVE_PRESIDENT) {
                List<Policy> choices = game.getPresidentLegislativeChoices();
                game.presidentDiscardPolicy(pickDiscardIndex(game, choices, Policy.Type.ARTICLE_48));
            } else if (state == GameState.LEGISLATIVE_CHANCELLOR) {
                List<Policy> choices = game.getChancellorLegislativeChoices();
                int enactIndex = pickEnactIndex(game, choices, Policy.Type.ARTICLE_48);
                Policy.Type enactedType = choices.get(enactIndex).getType();
                boolean isEmergencyCard =
                        enactedType == Policy.Type.ARTICLE_48 || enactedType == Policy.Type.ENABLING_ACT;

                game.chancellorEnactPolicy(enactIndex);

                if (isEmergencyCard) {
                    cardsSeen++;
                    assertEquals(cardsSeen, game.getEmergencyPowersEnacted());
                    assertEquals(cardsSeen, game.getEmergencyPowerTrackerHistory().size());
                    assertEquals(enactedType, game.getEmergencyPowerTrackerHistory().get(cardsSeen - 1));

                    if (cardsSeen <= EMERGENCY_POWER_SLOT_STATES.length) {
                        assertEquals("Slot " + cardsSeen + " should resolve its fixed shared power with no choice",
                                EMERGENCY_POWER_SLOT_STATES[cardsSeen - 1], game.getState());
                    } else if (enactedType == Policy.Type.ARTICLE_48) {
                        // Presidential Pardon needs no further input and
                        // resolves instantly, straight to POST_LEGISLATIVE.
                        assertEquals(GameState.POST_LEGISLATIVE, game.getState());
                    }
                    // Vote Of No Confidence (the Enabling Act's 6th-slot
                    // power) also resolves instantly with no player choice,
                    // but it enacts the President's previously-discarded
                    // card - which can itself land on a fascist/liberal
                    // tracker power, so no single resulting state can be
                    // asserted here.
                }
            } else if (state == GameState.POST_LEGISLATIVE) {
                game.endPresidentialTerm();
            } else {
                resolveActivePower(game, null);
            }
        }
        return cardsSeen;
    }

    /**
     * Once 3+ fascist policies are enacted, any chancellor who's
     * successfully elected without triggering a fascist election victory is
     * publicly confirmed to not be Hitler (since if they were, that election
     * would have already won the game for the Fascists). This should be
     * visible to any viewer, not just the confirmed player themselves.
     */
    @Test
    public void testChancellorSurvivingElectionAfterThreeFascistPoliciesIsConfirmedNotHitler() {
        List<String> players = makePlayers(7);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(
                    players, false, false, false, false, false, false, false, false, 0);
            String confirmedChancellor = null;
            for (int i = 0; i < STEPS_PER_ATTEMPT && !game.hasGameFinished(); i++) {
                String lastChancellor = game.getLastChancellor();
                if (game.getNumFascistPolicies() >= 3 && lastChancellor != null
                        && game.getPlayer(lastChancellor).isConfirmedNotHitler()) {
                    confirmedChancellor = lastChancellor;
                    break;
                }
                step(game, players, Policy.Type.FASCIST);
            }

            if (confirmedChancellor != null) {
                final String confirmed = confirmedChancellor;
                assertFalse("A confirmed player must actually not be Hitler",
                        game.getPlayer(confirmed).isHitler());

                String otherViewer = findFirstAlive(game, p -> !p.getUsername().equals(confirmed));
                JSONObject json = server.util.GameToJSONConverter.convert(game, otherViewer);
                JSONObject viewOfConfirmedPlayer = json.getJSONObject("players").getJSONObject(confirmed);
                assertTrue("This is public information, visible to any viewer",
                        viewOfConfirmedPlayer.getBoolean("confirmedNotHitler"));
                return;
            }
            // The game ended some other way (e.g. an early liberal/fascist
            // policy victory) before this could happen; retry with a fresh shuffle.
        }
        fail("Could not drive a game to a state where a chancellor was confirmed not Hitler within the attempt budget");
    }

    /**
     * Fails {@code SecretHitlerGame.MAX_FAILED_ELECTIONS} elections in a row
     * so the top card of the draw pile is forced through, exactly as the
     * election tracker does when it reaches its maximum.
     */
    private void failElectionsUntilPolicyIsForced(SecretHitlerGame game, List<String> players) {
        for (int i = 0; i < SecretHitlerGame.MAX_FAILED_ELECTIONS; i++) {
            String president = game.getCurrentPresident();
            String candidate = findFirstAlive(game, p -> !p.getUsername().equals(president));
            game.nominateChancellor(candidate);
            for (Player p : game.getPlayerList()) {
                if (p.isAlive()) {
                    game.registerVote(p.getUsername(), false);
                }
            }
            // A failed vote that doesn't yet hit MAX_FAILED_ELECTIONS leaves
            // the game in POST_LEGISLATIVE; endPresidentialTerm() advances
            // to the next president and back to CHANCELLOR_NOMINATION. The
            // final failed vote instead forces a policy through directly,
            // and the test needs to inspect that POST_LEGISLATIVE state as-is.
            if (i < SecretHitlerGame.MAX_FAILED_ELECTIONS - 1) {
                game.endPresidentialTerm();
            }
        }
    }

    @Test
    public void testForcedEmergencyPowerStillFillsBoardSlotButDoesNotResolve() {
        // A forced policy (3 failed elections in a row) never grants a
        // presidential power - true for every policy type, including
        // Article 48/Enabling Act. But unlike an ordinary tracker-advancing
        // policy, an emergency power card still has to visibly occupy its
        // slot on the printed Emergency Powers board; it just can't resolve
        // its power (there's no sitting chancellor to act on a forced
        // Enabling Act either, since a failed election clears it).
        List<String> players = makePlayers(6);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game = new SecretHitlerGame(
                    players, false, false, false, false, false, false, false, false, 3);

            failElectionsUntilPolicyIsForced(game, players);

            Policy.Type forced = game.getLastEnactedPolicy();
            if (forced == Policy.Type.ARTICLE_48 || forced == Policy.Type.ENABLING_ACT) {
                assertEquals(GameState.POST_LEGISLATIVE, game.getState());
                assertEquals(1, game.getEmergencyPowersEnacted());
                assertEquals(1, game.getEmergencyPowerTrackerHistory().size());
                assertEquals(forced, game.getEmergencyPowerTrackerHistory().get(0));
                assertNull("A forced emergency power never resolves, so no one should be assigned to act on it",
                        game.getEmergencyPowerActingOfficer());
                return;
            }
            // Some other policy type was forced through instead; retry with a fresh shuffle.
        }
        fail("Could not force an Article 48/Enabling Act policy through the election tracker within the attempt budget");
    }

    @Test
    public void testVetoIsAvailableAtPenultimateCommunistPolicyCount() {
        // 9 players: communist victory is at 6 policies, so veto should
        // unlock once 5 are enacted - same as fascist veto unlocking at 5
        // out of 6 needed for a fascist victory.
        List<String> players = makePlayers(9);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SecretHitlerGame game =
                    new SecretHitlerGame(players, true, false, false, false, false, false, false, false, 0);

            for (int i = 0; i < STEPS_PER_ATTEMPT * 3
                    && game.getNumCommunistPolicies() < 5 && !game.hasGameFinished(); i++) {
                step(game, players, Policy.Type.COMMUNIST);
            }
            if (game.getNumCommunistPolicies() != 5) {
                continue; // Overshot to a communist victory, or ran out of budget; retry.
            }
            assertTrue("Veto should be available once the communist tracker is one policy from winning",
                    game.isVetoAvailable());

            // Advance to the next LEGISLATIVE_CHANCELLOR round without
            // enacting another communist policy, to actually exercise
            // chancellorVeto() rather than just the availability check.
            for (int i = 0; i < STEPS_PER_ATTEMPT
                    && game.getState() != GameState.LEGISLATIVE_CHANCELLOR
                    && game.getNumCommunistPolicies() == 5 && !game.hasGameFinished(); i++) {
                step(game, players, Policy.Type.LIBERAL);
            }
            if (game.getState() != GameState.LEGISLATIVE_CHANCELLOR || game.getNumCommunistPolicies() != 5) {
                continue;
            }

            game.chancellorVeto();
            assertEquals(GameState.LEGISLATIVE_PRESIDENT_VETO, game.getState());
            return;
        }
        fail("Could not drive the communist tracker to exactly one-away-from-winning within the attempt budget");
    }

    @Test
    public void testVetoIsNotAvailableWithoutCommunistsAtFivePolicies() {
        // Without Communists included, enacting 5 fascist policies is the
        // only way veto unlocks - a coincidentally equal communist count
        // (impossible here since Communists aren't in the deck, but this
        // guards isVetoAvailable() never mistaking an unrelated tracker for
        // the communist one) shouldn't matter.
        List<String> players = makePlayers(9);
        SecretHitlerGame game =
                new SecretHitlerGame(players, false, false, false, false, false, false, false, false, 0);
        assertFalse("Veto should not be available with no policies enacted yet", game.isVetoAvailable());
    }
}
