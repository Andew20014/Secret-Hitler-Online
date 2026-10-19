package server.util;

import game.GameState;
import game.SecretHitlerGame;
import game.datastructures.Identity;
import game.datastructures.Player;
import game.datastructures.Policy;
import org.json.JSONObject;

import java.util.List;

/**
 * Converts a SecretHitlerGame to a JSONObject that represents the game state.
 */
public class GameToJSONConverter {
    /**
     * Creates a JSON object from a SecretHitlerGame that represents its state.
     * 
     * @param game the SecretHitlerGame to convert.
     * @param name the name of the user to create JSON content for. This is used
     *             to determine what player identity information to send to the
     *             user.
     * @throws NullPointerException if {@code game} is null.
     * @return a JSONObject with the following properties:
     *         - {@code state}: the state of the game.
     *         - {@code player-order}: an array of names representing the order of
     *         the players in the game.
     *
     *         - {@code players}: a JSONObject map, with keys that are a player's
     *         {@code username}.
     *         Each {@code username} key maps to an object with the properties
     *         {@code id} (String),
     *         {@code alive} (boolean), {@code investigated} (boolean), and
     *         {@code confirmedNotHitler} (boolean, publicly known - true once
     *         this player has survived a Chancellorship that would have won
     *         the game for the Fascists had they been Hitler), to represent
     *         the player.
     *         The identity is either this.HITLER, this.FASCIST, or this.LIBERAL.
     *         Ex: {"player1":{"alive": true, "investigated": false,
     *         "confirmedNotHitler": false, "id": "LIBERAL"}}.
     *
     *         - {@code president}: the username of the current president.
     *         - {@code chancellor}: the username of the current chancellor (can be
     *         null).
     *         - {@code last-president}: The username of the last president that
     *         presided over a legislative session.
     *         - {@code last-chancellor}: The username of the last chancellor that
     *         presided over a legislative session.
     *         - {@code draw-size}: The size of the draw deck.
     *         - {@code discard-size}: The size of the discard deck.
     *         - {@code fascist-policies}: The number of passed fascist policies.
     *         - {@code liberal-policies}: The number of passed liberal policies.:
     *         - {@code user-votes}: A map from each user to their vote from the
     *         last chancellor nomination.
     *         - {@code president-choices}: The choices for the president during the
     *         legislative session (only if in
     *         game state LEGISLATIVE_PRESIDENT).
     *         - {@code chancellor-choices}: The choices for the chancellor during
     *         the legislative session (only if in
     *         game state LEGISLATIVE_CHANCELLOR).
     *         - {@code veto-occurred}: Set to true if a veto has already taken
     *         place on this legislative session.
     *         - {@code communistPolicies}: The number of passed communist (and
     *         anarchist) policies.
     *         - {@code anarchistPolicies}: The number of passed anarchist
     *         policies on their own (not merged with communist policies).
     *         - {@code remainingFascistPolicies}, {@code remainingLiberalPolicies},
     *         {@code remainingCommunistPolicies}, {@code remainingAnarchistPolicies}:
     *         the number of policies of each type still left to be enacted
     *         (currently in the draw or discard pile).
     *         - {@code communistsIncluded}, {@code anarchistIncluded},
     *         {@code capitalistIncluded}, {@code monarchistIncluded},
     *         {@code antiFascistPolicyIncluded}, {@code antiCommunistPolicyIncluded},
     *         {@code socialDemocraticPolicyIncluded},
     *         {@code longerLiberalTrackIncluded}, {@code emergencyPowersIncluded}:
     *         the Secret Hitler XL mod options in effect for this game.
     *         - {@code markedForExecution}: the username of the player marked for
     *         execution by an emergency power, if any.
     *         - {@code propagandaPeek}: the type of the top policy, only present
     *         during the Propaganda emergency power.
     */
    public static JSONObject convert(SecretHitlerGame game, String userName) {
        if (game == null) {
            throw new NullPointerException();
        }

        JSONObject out = new JSONObject();
        JSONObject playerData = new JSONObject();
        String[] playerOrder = new String[game.getPlayerList().size()];
        List<Player> playerList = game.getPlayerList();

        // Players should only be shown other players' roles under specific
        // circumstances.
        Player viewer = game.getPlayer(userName);
        Identity role = viewer.getIdentity();
        boolean gameOver = game.hasGameFinished();
        // Fascists and Monarchists know each other and Hitler (but nothing
        // about Communists, Anarchists, the Capitalist, or Liberals). Hitler
        // only shares that knowledge in small games. This only reveals the
        // identity of players who are themselves on the fascist team — it
        // must NOT reveal every player's identity.
        boolean viewerKnowsFascistTeam = (viewer.isFascist() && role != Identity.HITLER)
                || (role == Identity.HITLER && game.getPlayerList().size() <= 6);
        // Original Communists (dealt at setup) recognize each other, and any
        // later Radicalization target, from the very start - same as
        // Fascists above. A Radicalization target themselves doesn't
        // recognize the original team back until Congress resolves (its
        // whole purpose, per the rules: "the newly radicalised Communists
        // learn who the original Communists are").
        boolean viewerKnowsCommunistTeam = viewer.isCommunist()
                && (viewer.wasCommunist() || game.hasCongressHappened());

        for (int i = 0; i < playerList.size(); i++) {
            JSONObject playerObj = new JSONObject();
            Player player = playerList.get(i);

            playerObj.put("alive", player.isAlive());

            boolean selfOrGameOver = player.getUsername().equals(userName) || gameOver;
            // Former fascist-team viewers keep recognizing this player as a
            // teammate even after Radicalization converts them to Communist
            // - that knowledge, once gained, is never silently revoked.
            boolean revealAsFormerFascistTeammate = !selfOrGameOver
                    && viewerKnowsFascistTeam && player.wasFascist();
            boolean revealAsCommunistTeammate = viewerKnowsCommunistTeam && player.isCommunist();

            // Only include player role for self, once the game is over, or
            // for fellow fascist-team/communist-team members if the viewer
            // knows that team.
            if (selfOrGameOver || revealAsFormerFascistTeammate || revealAsCommunistTeammate) {
                // A former fascist teammate is shown their ORIGINAL identity
                // forever (e.g. still "FASCIST" after Radicalization), not
                // whatever they currently are - everyone else sees the
                // player's actual current identity.
                Identity shownIdentity = revealAsFormerFascistTeammate
                        ? player.getOriginalIdentity()
                        : player.getIdentity();
                playerObj.put("id", shownIdentity.toString());
            }
            playerObj.put("investigated", player.hasBeenInvestigated());
            playerObj.put("confirmedNotHitler", player.isConfirmedNotHitler());

            playerData.put(player.getUsername(), playerObj);
            playerOrder[i] = player.getUsername();
        }

        // The live communist vote tally (voter -> target) for choosing who
        // to Radicalize/Investigate, visible only to fellow communists while
        // that vote is actively happening. Note: this necessarily reveals
        // who's voting (i.e. who's a communist) to every communist
        // immediately, even ones who don't otherwise know the team yet (see
        // viewerKnowsCommunistTeam above) - coordinating the vote is itself
        // a moment of recognition. Gating this on the vote being active
        // (rather than just viewer.isCommunist()) matters for whoever the
        // vote's own target is: by the time Radicalization converts them and
        // this gets sent to them, the vote has already resolved and the
        // state has moved on - without this gate they'd otherwise learn the
        // whole communist roster despite never having participated in it.
        boolean communistVoteActive = game.getState() == GameState.PRESIDENTIAL_POWER_RADICALIZATION
                || game.getState() == GameState.PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION;
        if (viewer.isCommunist() && communistVoteActive) {
            JSONObject communistVotes = new JSONObject();
            for (java.util.Map.Entry<String, String> vote : game.getCommunistVotes().entrySet()) {
                communistVotes.put(vote.getKey(), vote.getValue());
            }
            out.put("communistVotes", communistVotes);

            // The full alive-communist roster, specifically for rendering
            // the vote UI (who still needs to vote, who's a valid target to
            // exclude). Not the same as viewerKnowsCommunistTeam's "id"
            // field above, which has its own separate (Congress-gated)
            // visibility rules elsewhere in the app.
            org.json.JSONArray aliveCommunists = new org.json.JSONArray();
            for (Player p : game.getPlayerList()) {
                if (p.isAlive() && p.isCommunist()) {
                    aliveCommunists.put(p.getUsername());
                }
            }
            out.put("aliveCommunists", aliveCommunists);
        }

        out.put("players", playerData);
        out.put("playerOrder", playerOrder);

        out.put("president", game.getCurrentPresident());
        out.put("chancellor", game.getCurrentChancellor());
        out.put("state", game.getState().toString());
        out.put("lastState", game.getLastState().toString());
        out.put("lastPresident", game.getLastPresident());
        out.put("lastChancellor", game.getLastChancellor());
        out.put("targetUser", game.getTarget());

        out.put("electionTracker", game.getElectionTracker());
        out.put("electionTrackerAdvanced", game.didElectionTrackerAdvance());

        out.put("lastPolicy", game.getLastEnactedPolicy().toString().toUpperCase());
        if (game.getAnarchistCascadeForcedPolicy() != null) {
            out.put("anarchistCascadeForcedPolicy", game.getAnarchistCascadeForcedPolicy().toString().toUpperCase());
        }

        out.put("drawSize", game.getDrawSize());
        out.put("discardSize", game.getDiscardSize());
        out.put("fascistPolicies", game.getNumFascistPolicies());
        out.put("liberalPolicies", game.getNumLiberalPolicies());
        out.put("communistPolicies", game.getNumCommunistPolicies());
        out.put("anarchistPolicies", game.getNumAnarchistPolicies());

        // The specific Policy.Type enacted into each tracker slot, in order
        // (e.g. an anti-communist policy shows up in fascistTrackerHistory
        // alongside plain fascist policies), so the board can render the
        // exact card art per slot instead of a generic per-tracker icon.
        out.put("liberalTrackerHistory", policyTypesToStrings(game.getLiberalTrackerHistory()));
        out.put("fascistTrackerHistory", policyTypesToStrings(game.getFascistTrackerHistory()));
        out.put("communistTrackerHistory", policyTypesToStrings(game.getCommunistTrackerHistory()));
        out.put("userVotes", game.getVotes());
        out.put("vetoOccurred", game.didVetoOccurThisTurn());

        // How many policies of each type are still left to be drawn/enacted
        // (currently sitting in the draw or discard pile). Computed directly
        // rather than from a fixed starting total, since the Anarchist's
        // cascading mechanic can grow its pool over the course of the game.
        out.put("remainingFascistPolicies", game.getRemainingFascistPolicies());
        out.put("remainingLiberalPolicies", game.getRemainingLiberalPolicies());
        out.put("remainingCommunistPolicies", game.getRemainingCommunistPolicies());
        out.put("remainingAnarchistPolicies", game.getRemainingAnarchistPolicies());
        out.put("remainingAntiFascistPolicies", game.getRemainingAntiFascistPolicies());
        out.put("remainingAntiCommunistPolicies", game.getRemainingAntiCommunistPolicies());
        out.put("remainingSocialDemocraticPolicies", game.getRemainingSocialDemocraticPolicies());
        out.put("remainingArticle48Policies", game.getRemainingArticle48Policies());
        out.put("remainingEnablingActPolicies", game.getRemainingEnablingActPolicies());

        // How many players (alive or dead) hold each identity over the
        // course of the game. Like the base game's public role breakdown,
        // this composition isn't secret - only who holds which role is.
        JSONObject roleCounts = new JSONObject();
        roleCounts.put("LIBERAL", game.getNumPlayersWithIdentity(Identity.LIBERAL));
        roleCounts.put("FASCIST", game.getNumPlayersWithIdentity(Identity.FASCIST));
        roleCounts.put("HITLER", game.getNumPlayersWithIdentity(Identity.HITLER));
        roleCounts.put("COMMUNIST", game.getNumPlayersWithIdentity(Identity.COMMUNIST));
        roleCounts.put("ANARCHIST", game.getNumPlayersWithIdentity(Identity.ANARCHIST));
        roleCounts.put("CAPITALIST", game.getNumPlayersWithIdentity(Identity.CAPITALIST));
        roleCounts.put("MONARCHIST", game.getNumPlayersWithIdentity(Identity.MONARCHIST));
        out.put("roleCounts", roleCounts);

        // Secret Hitler XL mod options in effect for this game.
        out.put("communistsIncluded", game.isCommunistsIncluded());
        out.put("anarchistIncluded", game.isAnarchistIncluded());
        out.put("capitalistIncluded", game.isCapitalistIncluded());
        out.put("monarchistIncluded", game.isMonarchistIncluded());
        out.put("antiFascistPolicyIncluded", game.isAntiFascistPolicyIncluded());
        out.put("antiCommunistPolicyIncluded", game.isAntiCommunistPolicyIncluded());
        out.put("socialDemocraticPolicyIncluded", game.isSocialDemocraticPolicyIncluded());
        out.put("longerLiberalTrackIncluded", game.isLongerLiberalTrackIncluded());
        out.put("emergencyPowersIncluded", game.getEmergencyPowersIncluded());

        // How many Article 48/Enabling Act cards have been enacted so far
        // (out of a max of 6), and which policy type (ARTICLE_48 or
        // ENABLING_ACT) filled each of the printed board's 6 slots, in
        // order - the power is determined automatically by slot number,
        // there is no player choice. See SecretHitlerGame.EMERGENCY_POWER_SLOT_ORDER.
        out.put("emergencyPowersEnacted", game.getEmergencyPowersEnacted());
        out.put("emergencyPowerTrackerHistory", policyTypesToStrings(game.getEmergencyPowerTrackerHistory()));

        if (game.getMarkedForExecutionUsername() != null) {
            out.put("markedForExecution", game.getMarkedForExecutionUsername());
        }
        if (game.getEmergencyPowerActingOfficer() != null) {
            out.put("emergencyPowerActingOfficer", game.getEmergencyPowerActingOfficer());
        }
        // Public for everyone, for the rest of the game - see
        // SecretHitlerGame.confession(). This never reveals the confessor's
        // true party, only which one they're (truthfully) NOT a member of.
        if (game.getConfessionUsername() != null) {
            out.put("confessionUsername", game.getConfessionUsername());
            out.put("confessionDeniedParty", game.getConfessionDeniedParty().toString());
        }

        if (game.getState() == GameState.LEGISLATIVE_PRESIDENT) {
            out.put("presidentChoices", convertPolicyListToStringArray(game.getPresidentLegislativeChoices()));
        }
        if (game.getState() == GameState.LEGISLATIVE_CHANCELLOR) {
            out.put("chancellorChoices", convertPolicyListToStringArray(game.getChancellorLegislativeChoices()));
        }
        if (game.getState() == GameState.PRESIDENTIAL_POWER_PEEK) {
            out.put("peek", convertPolicyListToStringArray(game.getPeek()));
        }
        if (game.getState() == GameState.PRESIDENTIAL_POWER_PROPAGANDA) {
            out.put("propagandaPeek", game.getPropagandaPeek().getType().toString());
        }

        return out;
    }

    /**
     * Converts a list of policies into a string array.
     * 
     * @param list the list of policies.
     * @return a string array with the same length as the list, where each index is
     *         either "FASCIST" or "LIBERAL"
     *         according to the type of the Policy at that index in the list.
     */
    public static String[] convertPolicyListToStringArray(List<Policy> list) {
        String[] out = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i).getType().toString();
        }
        return out;
    }

    /**
     * Converts a list of Policy.Types into a string array (e.g. a tracker's
     * enacted-slot history).
     *
     * @param list the list of Policy.Types.
     * @return a string array with the same length as the list, where each index is
     *         the corresponding Policy.Type's name.
     */
    private static String[] policyTypesToStrings(List<Policy.Type> list) {
        String[] out = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i).toString();
        }
        return out;
    }
}
