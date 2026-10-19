package game;

import game.datastructures.Deck;
import game.datastructures.Identity;
import game.datastructures.Player;
import game.datastructures.Policy;
import game.datastructures.board.Board;
import game.datastructures.board.EmergencyPower;
import game.datastructures.board.FiveToSixPlayerBoard;
import game.datastructures.board.NineToTenPlayerBoard;
import game.datastructures.board.PresidentialPower;
import game.datastructures.board.SevenToEightPlayerBoard;
import game.datastructures.board.SixPlayerBoardCommunist;
import game.datastructures.board.SevenPlayerBoardCommunist;
import game.datastructures.board.EightPlayerBoardCommunist;
import game.datastructures.board.NinePlayerBoardCommunist;
import game.datastructures.board.TenPlayerBoardCommunist;

import java.io.Serializable;
import java.util.*;

/**
 * Keeps track of the state of a game of Secret Hitler.
 *
 * Secret Hitler is a social deduction game for 5-10 players, originally
 * created by Goat, Wolf {@literal &} Cabbage (c) 2016. You can find more
 * details on their
 * website, or order the physical game, at https://www.secrethitler.com/.
 *
 * Secret Hitler is licensed through Creative Commons.
 */
public class SecretHitlerGame implements Serializable {

    /////////////////// Static Fields
    // <editor-fold desc="Static Fields">

    // Keeps track of the number of fascists that should be in the game for a given
    // number
    // of players. - - - - - 5 6 7 8 9 10
    public static final int[] NUM_FASCISTS_FOR_PLAYERS = { 
        -1, -1, -1, -1, -1, 1, 1, 2, 2, 3, 3 
    };
    public static final int[] NUM_FASCISTS_FOR_PLAYERS_WITH_COMMUNISTS = { 
        -1, -1, -1, -1, -1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4 
    };
    public static final int[] NUM_COMMUNISTS_FOR_PLAYERS = { 
        -1, -1, -1, -1, -1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4 
    };

    // The number of fascist and liberal policies in a standard deck.
    public static final int NUM_FASCIST_POLICIES = 11;
    public static final int NUM_LIBERAL_POLICIES = 6;
    public static final int NUM_FASCIST_POLICIES_WITH_COMMUNIST_6TO7 = 10;
    public static final int NUM_LIBERAL_POLICIES_WITH_COMMUNIST_6TO7 = 5;
    public static final int NUM_COMMUNIST_POLICIES_6TO7 = 8;
    public static final int NUM_FASCIST_POLICIES_WITH_COMMUNIST_8UP = 9;
    public static final int NUM_LIBERAL_POLICIES_WITH_COMMUNIST_8UP = 6;
    public static final int NUM_COMMUNIST_POLICIES_8UP = 8;
    public static final int NUM_COMMUNIST_POLICIES_WITH_ANARCHIST = 6;
    public static final int NUM_ANARCHIST_POLICIES = 3;
    public static final int NUM_ANARCHIST_POLICIES_WITH_COMMUNIST = 2;

    public static final int MIN_PLAYERS = 5;
    public static final int MAX_PLAYERS = 19;
    public static final int MIN_PLAYERS_WITH_COMMUNIST = 6;
    public static final int MIN_PLAYERS_WITH_COMMUNIST_AND_ANARCHIST = 9;
    public static final int MIN_PLAYERS_WITH_MONARCHIST = 7;
    public static final int MIN_PLAYERS_WITH_COMMUNIST_AND_MONARCHIST = 8;
    public static final int MAX_EMERGENCY_POWER_POLICIES = 3;

    public static final int MAX_FAILED_ELECTIONS = 3;
    private static final float VOTING_CUTOFF = 0.5000001f;
    private static final int MIN_DRAW_DECK_SIZE = 3;

    public static final int PRESIDENT_DRAW_SIZE = 3;
    public static final int CHANCELLOR_DRAW_SIZE = 2;

    // </editor-fold>

    /////////////////// Private Fields
    // <editor-fold desc="Private Fields">

    private List<Player> playerList;
    private Board board;
    private Deck discard;
    private Deck draw;

    private boolean communistsIncluded;
    private boolean anarchistIncluded;
    private boolean capitalistIncluded;
    private boolean monarchistIncluded;
    private boolean antiFascistPolicyIncluded;
    private boolean antiCommunistPolicyIncluded;
    private boolean socialDemocraticPolicyIncluded;
    private boolean longerLiberalTrackIncluded;
    private int emergencyPowersIncluded;

    private int electionTracker;

    private GameState state;
    private GameState lastState = GameState.SETUP;
    private int round;

    private Random random;

    // The last president and chancellor that were successfully voted into office.
    private String lastPresident;
    private String lastChancellor;
    private Policy.Type lastEnactedPolicy = Policy.Type.FASCIST;
    // Set only for the single broadcast right after the Anarchist's cascade
    // mechanic forces a second policy through (see onEnactPolicy()); cleared
    // at the top of every subsequent enactment, so the frontend can treat a
    // non-null value as "this is what the Anarchist card just forced".
    private Policy.Type anarchistCascadeForcedPolicy = null;

    private String currentPresident;
    private String currentChancellor;

    // Used during a session with the PRESIDENTIAL_POWER_ELECTION power active to
    // remember the next president.
    // The default state should be null.
    private String nextPresident;
    // The president that was elected to take power next (due to the
    // PRESIDENTIAL_POWER_ELECTION power being active).
    private String electedPresident;

    // The player that was targeted with the last presidential power.
    private String target;

    // The options available to either the President or the Chancellor during the
    // legislative session
    private List<Policy> legislativePolicies;

    private boolean didElectionTrackerAdvance = false;
    private boolean didVetoOccurThisTurn = false;

    private HashMap<String, Boolean> voteMap;

    // Set when an anti-communist/anti-fascist/social democratic policy removes a
    // policy from the fascist/communist tracker: the next policy to land on that
    // tracker's slot does not re-trigger its presidential power.
    private boolean skipNextFascistPower = false;
    private boolean skipNextCommunistPower = false;

    // Set once the Congress power has resolved: in 8+ player games,
    // Communists only learn each other's identities after this happens
    // (in 6-7 player games they always know each other, regardless).
    private boolean congressHappened = false;

    // Tracks each alive communist's current vote (voter username -> target
    // username) for choosing who to radicalize or investigate. Cleared
    // whenever a new presidential power is entered, and whenever a vote
    // resolves. A communist may change or retract their vote freely; the
    // target is chosen once every alive communist's vote points at the
    // same player.
    private final Map<String, String> communistVotes = new HashMap<>();

    // Tracks the "Marked For Execution" emergency power: the marked player is
    // executed once 3 fascist policies have been enacted in total (not just
    // since the mark).
    private String markedForExecutionUsername = null;

    // Tracks the Communist "Confession" power's public claim: the username
    // of whoever confessed, and the party (FASCIST/COMMUNIST - never
    // LIBERAL, see isValidDeniedParty()) they publicly denied being a
    // member of. Both null until Confession is used.
    private String confessionUsername = null;
    private Identity confessionDeniedParty = null;

    // The policy the President discarded this legislative round, kept around in
    // case the Enabling Act's "Vote Of No Confidence" power is used.
    private Policy lastPresidentDiscardedPolicy = null;

    // Whether the emergency power currently being resolved came from an
    // Article 48 (true, President acts) or Enabling Act (false, Chancellor acts).
    private boolean currentEmergencyPowerIsArticle48 = false;

    // The username of the officer (President for Article 48, Chancellor for
    // the Enabling Act) currently authorized to act while an emergency
    // sub-power is being resolved. Null outside of that flow, including
    // when Execution/Peek are reached normally via the fascist tracker.
    private String emergencyPowerActingOfficer = null;

    // How many Article 48/Enabling Act cards have been enacted so far
    // (shared count, regardless of which of the two). Matches the printed
    // board's slot progression (max 6 slots, since at most 6 emergency
    // cards are ever included in a game): each card enacted fills the next
    // slot left-to-right, and that slot's printed power is what
    // automatically happens - see EMERGENCY_POWER_SLOT_ORDER.
    private int emergencyPowersEnacted = 0;

    // The Article 48/Enabling Act policy type that filled each of the 6
    // printed slots, in order - lets the board render which card type
    // occupies each slot, mirroring Board's *TrackerHistory fields.
    private final List<Policy.Type> emergencyPowerTrackerHistory = new ArrayList<>();

    // The power printed on each of the board's first 5 (shared) slots, in
    // left-to-right order. There is no player choice involved: whichever
    // slot number a newly-enacted emergency card fills (emergencyPowersEnacted,
    // 1-indexed) automatically determines its power. The 6th slot is split
    // into an Article-48-specific box (Presidential Pardon) and an
    // Enabling-Act-specific box (Vote Of No Confidence) - see
    // getEmergencyPowerForSlot().
    private static final EmergencyPower[] EMERGENCY_POWER_SLOT_ORDER = {
        EmergencyPower.PROPAGANDA,
        EmergencyPower.POLICY_PEEK,
        EmergencyPower.IMPEACHMENT,
        EmergencyPower.MARKED_FOR_EXECUTION,
        EmergencyPower.EXECUTION,
    };

    // </editor-fold>

    /////////////////// Public Observers
    // <editor-fold desc="Public Observers">

    public GameState getLastState() {
        return lastState;
    }

    public String getTarget() {
        return target;
    }

    public String getCurrentPresident() {
        return currentPresident;
    }

    public String getCurrentChancellor() {
        return currentChancellor;
    }

    public String getLastPresident() {
        return lastPresident;
    }

    public String getLastChancellor() {
        return lastChancellor;
    }

    public Policy.Type getLastEnactedPolicy() {
        return lastEnactedPolicy;
    }

    /**
     * @return the Policy.Type forced through by the Anarchist's cascading
     *         mechanic, if the most recently enacted policy was an Anarchist
     *         card that triggered it. Returns null otherwise (including once
     *         any other policy has since been enacted).
     */
    public Policy.Type getAnarchistCascadeForcedPolicy() {
        return anarchistCascadeForcedPolicy;
    }

    public int getDrawSize() {
        return draw.getSize();
    }

    public int getDiscardSize() {
        return discard.getSize();
    }

    public int getElectionTracker() {
        return electionTracker;
    }

    public int getNumFascistPolicies() {
        return board.getNumFascistPolicies();
    }

    public int getNumCommunistPolicies() {
        return board.getNumCommunistPolicies();
    }

    /**
     * @return the number of anarchist policies enacted, on its own (not
     *         merged with communist policies).
     */
    public int getNumAnarchistPolicies() {
        return board.getNumAnarchistPolicies();
    }

    public int getNumLiberalPolicies() {
        return board.getNumLiberalPolicies();
    }

    /**
     * @return the specific Policy.Type enacted into each liberal tracker
     *         slot, in order, for rendering the exact card art per slot.
     */
    public List<Policy.Type> getLiberalTrackerHistory() {
        return board.getLiberalTrackerHistory();
    }

    /**
     * @return the specific Policy.Type enacted into each fascist tracker
     *         slot, in order, for rendering the exact card art per slot.
     */
    public List<Policy.Type> getFascistTrackerHistory() {
        return board.getFascistTrackerHistory();
    }

    /**
     * @return the specific Policy.Type enacted into each communist tracker
     *         slot, in order, for rendering the exact card art per slot.
     */
    public List<Policy.Type> getCommunistTrackerHistory() {
        return board.getCommunistTrackerHistory();
    }

    /**
     * @param type the Policy.Type to count.
     * @return the number of policies of that type still left to be
     *         drawn/enacted, i.e. currently sitting in the draw pile, the
     *         discard pile, or drawn into the current legislative session
     *         but not yet resolved (not yet placed on a tracker). Counting
     *         {@code legislativePolicies} here too (rather than only draw +
     *         discard) keeps this total from dipping the instant the
     *         President draws their 3 cards, which would otherwise leak the
     *         type composition of the drawn cards to every player. For the
     *         Anarchist, this can grow over the course of the game: the
     *         standalone Anarchist-without-Communists variant shuffles
     *         additional anarchist policies into circulation each time one
     *         is enacted normally.
     */
    public int getRemainingPolicies(Policy.Type type) {
        int legislativeCount = 0;
        if (legislativePolicies != null) {
            for (Policy policy : legislativePolicies) {
                if (policy.getType() == type) {
                    legislativeCount++;
                }
            }
        }
        return draw.countByType(type) + discard.countByType(type) + legislativeCount;
    }

    /**
     * @return the number of fascist policies still left to be drawn/enacted.
     */
    public int getRemainingFascistPolicies() {
        return getRemainingPolicies(Policy.Type.FASCIST);
    }

    /**
     * @return the number of liberal policies still left to be drawn/enacted.
     */
    public int getRemainingLiberalPolicies() {
        return getRemainingPolicies(Policy.Type.LIBERAL);
    }

    /**
     * @return the number of communist policies still left to be
     *         drawn/enacted (0 if communists are not included).
     */
    public int getRemainingCommunistPolicies() {
        return getRemainingPolicies(Policy.Type.COMMUNIST);
    }

    /**
     * @return the number of anarchist policies still left to be
     *         drawn/enacted (0 if the anarchist is not included).
     */
    public int getRemainingAnarchistPolicies() {
        return getRemainingPolicies(Policy.Type.ANARCHIST);
    }

    /**
     * @return the number of anti-fascist policies still left to be
     *         drawn/enacted (0 if anti-policies are not included).
     */
    public int getRemainingAntiFascistPolicies() {
        return getRemainingPolicies(Policy.Type.ANTI_FASCIST);
    }

    /**
     * @return the number of anti-communist policies still left to be
     *         drawn/enacted (0 if anti-policies are not included).
     */
    public int getRemainingAntiCommunistPolicies() {
        return getRemainingPolicies(Policy.Type.ANTI_COMMUNIST);
    }

    /**
     * @return the number of Social Democratic policies still left to be
     *         drawn/enacted (0 if the Social Democratic policy is not
     *         included).
     */
    public int getRemainingSocialDemocraticPolicies() {
        return getRemainingPolicies(Policy.Type.SOCIAL_DEMOCRATIC);
    }

    /**
     * @return the number of Article 48 emergency power policies still left
     *         to be drawn/enacted (0 if emergency powers are not included).
     */
    public int getRemainingArticle48Policies() {
        return getRemainingPolicies(Policy.Type.ARTICLE_48);
    }

    /**
     * @return the number of Enabling Act emergency power policies still left
     *         to be drawn/enacted (0 if emergency powers are not included).
     */
    public int getRemainingEnablingActPolicies() {
        return getRemainingPolicies(Policy.Type.ENABLING_ACT);
    }

    /**
     * @param identity the Identity to count.
     * @return the number of players (alive or dead) assigned that identity
     *         over the course of the game. Safe to reveal to everyone: like
     *         the base game's role breakdown, the composition (how many of
     *         each role) isn't secret, only who holds which role is.
     */
    public int getNumPlayersWithIdentity(Identity identity) {
        int count = 0;
        for (Player player : playerList) {
            if (player.getIdentity() == identity) {
                count++;
            }
        }
        return count;
    }

    public boolean didElectionTrackerAdvance() {
        return didElectionTrackerAdvance;
    }

    public boolean didVetoOccurThisTurn() {
        return didVetoOccurThisTurn;
    }

    public boolean isCommunistsIncluded() {
        return communistsIncluded;
    }

    public boolean isAnarchistIncluded() {
        return anarchistIncluded;
    }

    public boolean isCapitalistIncluded() {
        return capitalistIncluded;
    }

    public boolean isMonarchistIncluded() {
        return monarchistIncluded;
    }

    public boolean isAntiFascistPolicyIncluded() {
        return antiFascistPolicyIncluded;
    }

    public boolean isAntiCommunistPolicyIncluded() {
        return antiCommunistPolicyIncluded;
    }

    public boolean isSocialDemocraticPolicyIncluded() {
        return socialDemocraticPolicyIncluded;
    }

    public boolean isLongerLiberalTrackIncluded() {
        return longerLiberalTrackIncluded;
    }

    public int getEmergencyPowersIncluded() {
        return emergencyPowersIncluded;
    }

    /**
     * @return the username of the player currently marked for execution by the
     *         Marked For Execution emergency power, or null if nobody is marked.
     */
    public String getMarkedForExecutionUsername() {
        return markedForExecutionUsername;
    }

    // </editor-fold>

    /////////////////// Constructor
    // <editor-fold desc="Constructor">

    /**
     * Constructs a new game of Secret Hitler with the given players.
     * 
     * @param players the names of the players to add to the game.
     * @requires there can be no repeat names in {@code players}. The number of
     *           players must be between MIN_PLAYERS and
     *           MAX_PLAYERS, inclusive.
     * @modifies this
     * @effects initializes the game, setting all player identities and card decks.
     *          The first player in the provided
     *          player list is the first president, and the game begins the
     *          chancellor nomination process.
     */
    public SecretHitlerGame(Collection<String> players) {
        this(players, false, false, false, false, false, false, false, false, 0);
    }

    /**
     * Computes the minimum number of players required to start a game with
     * the given mod options, mirroring the validation performed by the
     * constructor below.
     *
     * @param includeCommunist  whether Communists are included.
     * @param includeAnarchist  whether the Anarchist is included.
     * @param includeMonarchist whether the Monarchist is included.
     * @return the minimum number of players required.
     */
    public static int getMinPlayersForOptions(
            boolean includeCommunist, boolean includeAnarchist, boolean includeMonarchist) {
        int min = MIN_PLAYERS;
        if (includeCommunist) {
            min = Math.max(min, MIN_PLAYERS_WITH_COMMUNIST);
        }
        if (includeCommunist && includeAnarchist) {
            min = Math.max(min, MIN_PLAYERS_WITH_COMMUNIST_AND_ANARCHIST);
        }
        if (includeMonarchist) {
            min = Math.max(min,
                    includeCommunist ? MIN_PLAYERS_WITH_COMMUNIST_AND_MONARCHIST : MIN_PLAYERS_WITH_MONARCHIST);
        }
        return min;
    }

    public SecretHitlerGame(
        Collection<String> players, boolean includeCommunist,
        boolean includeAnarchist, boolean includeCapitalist,
        boolean includeMonarchist, boolean includeAntiFascistPolicy,
        boolean includeAntiCommunistPolicy, boolean includeSocialDemocraticPolicy,
        boolean longerLiberalTrack, int includeEmergencyPowers
        ) {
        if (players.size() < MIN_PLAYERS) {
            throw new IllegalArgumentException("There must be at least " + MIN_PLAYERS + " to start the game (only "
                    + players.size() + " provided).");
        } else if (includeCommunist && players.size() < MIN_PLAYERS_WITH_COMMUNIST) {
            throw new IllegalArgumentException("There must be at least " + MIN_PLAYERS_WITH_COMMUNIST + " to include communists (only "
                    + players.size() + " provided).");
        } else if (includeAnarchist && includeCommunist && players.size() < MIN_PLAYERS_WITH_COMMUNIST_AND_ANARCHIST) {
            throw new IllegalArgumentException("There must be at least " + MIN_PLAYERS_WITH_COMMUNIST_AND_ANARCHIST + " to include anarchists (only "
                    + players.size() + " provided).");
        } else if ((includeMonarchist && !includeCommunist && players.size() < MIN_PLAYERS_WITH_MONARCHIST) ||
                (includeMonarchist && includeCommunist && players.size() < MIN_PLAYERS_WITH_COMMUNIST_AND_MONARCHIST)) {
            throw new IllegalArgumentException("There must be at least " + (includeCommunist ? MIN_PLAYERS_WITH_COMMUNIST_AND_MONARCHIST : MIN_PLAYERS_WITH_MONARCHIST) + " to include monarchists (only "
                    + players.size() + " provided).");
        } else if (includeAntiFascistPolicy && !includeCommunist) {
            // The Anti-Fascist policy is placed on the Communist tracker and
            // replaces a Communist policy in the deck, so it's meaningless
            // (and has nowhere to go) without Communists in the game.
            throw new IllegalArgumentException("The Anti-Fascist policy requires Communists to be included.");
        } else if (players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "There can be a max of " + MAX_PLAYERS + " in a game (" + players.size() + " provided).");
        } if (includeEmergencyPowers < 0 || includeEmergencyPowers > MAX_EMERGENCY_POWER_POLICIES) {
            throw new IllegalArgumentException("includeEmergencyPowers must be between 0 and 3 (inclusive).");
        } else if (new HashSet<>(players).size() != players.size()) {
            throw new IllegalArgumentException("There cannot be duplicate player names.");
        }

        // Set up the list of players.
        playerList = new ArrayList<>();
        for (String name : players) {
            playerList.add(new Player(name));
        }

        random = new Random();
        electionTracker = 0;
        voteMap = new HashMap<>();

        communistsIncluded = includeCommunist;
        anarchistIncluded = includeAnarchist;
        capitalistIncluded = includeCapitalist;
        monarchistIncluded = includeMonarchist;
        antiFascistPolicyIncluded = includeAntiFascistPolicy;
        antiCommunistPolicyIncluded = includeAntiCommunistPolicy;
        socialDemocraticPolicyIncluded = includeSocialDemocraticPolicy;
        longerLiberalTrackIncluded = longerLiberalTrack;
        emergencyPowersIncluded = includeEmergencyPowers;

        resetDeck();
        assignRoles();
        electionTracker = 0;

        // Assign a new board based on the number of players.
        if (includeCommunist) {
            switch (players.size()) {
                case 6:
                    board = new SixPlayerBoardCommunist();
                    break;
                case 7:
                    board = new SevenPlayerBoardCommunist();
                    break;
                case 8:
                    board = new EightPlayerBoardCommunist();
                    break;
                case 9:
                    board = new NinePlayerBoardCommunist();
                    break;
                default: // 10+ players
                    board = new TenPlayerBoardCommunist();
                    break;
            }
        } else {
            if (playerList.size() <= 6) {
                board = new FiveToSixPlayerBoard();
            } else if (playerList.size() <= 8) {
                board = new SevenToEightPlayerBoard();
            } else {
                board = new NineToTenPlayerBoard();
            }
        }

        currentPresident = playerList.get(0).getUsername();
        currentChancellor = null;
        lastChancellor = null;
        lastPresident = null;

        state = GameState.CHANCELLOR_NOMINATION;
        round = 1;
    }

    // </editor-fold>

    /////////////////// Player Management
    // <editor-fold desc="Player Management">

    /**
     * Checks if the game has a player with the given username.
     * 
     * @param username the String username of the player to search for.
     * @return true iff the player is in the game.
     */
    public boolean hasPlayer(String username) {
        if (username == null) {
            return false;
        }
        for (Player p : playerList) {
            if (p.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the list of active players.
     * 
     * @return a copy of the list of Players.
     */
    public List<Player> getPlayerList() {
        return new ArrayList<>(playerList);
    }

    /**
     * Gets the index of a player in the list.
     * 
     * @param username the username to search for.
     * @return the index of the given player in the list. Returns -1 if the player
     *         was not found.
     */
    private int indexOfPlayer(String username) {
        for (int i = 0; i < playerList.size(); i++) {
            if (playerList.get(i).getUsername().equals(username)) {
                return i;
            }
        }
        return -1;
    }

    public Player getPlayer(String username) {
        return playerList.get(indexOfPlayer(username));
    }

    // </editor-fold>

    /////////////////// Game Setup
    // <editor-fold desc="Game Setup">

    /**
     * Resets the Draw and Discard decks.
     * 
     * @effects empties the discard deck, fills the draw deck with a standard card
     *          count, and shuffles.
     */
    private void resetDeck() {
        draw = new Deck();
        discard = new Deck();
        int f = 0, l = 0, c = 0, a = 0;
        if (communistsIncluded) {
            if (playerList.size() <= 7) {
                f = NUM_FASCIST_POLICIES_WITH_COMMUNIST_6TO7;
                l = NUM_LIBERAL_POLICIES_WITH_COMMUNIST_6TO7;
                c = NUM_COMMUNIST_POLICIES_6TO7;
            } else { // 8+ players
                f = NUM_FASCIST_POLICIES_WITH_COMMUNIST_8UP;
                l = NUM_LIBERAL_POLICIES_WITH_COMMUNIST_8UP;
                if (anarchistIncluded) {
                    c = NUM_COMMUNIST_POLICIES_WITH_ANARCHIST;
                    a = NUM_ANARCHIST_POLICIES_WITH_COMMUNIST;
                } else {
                    c = NUM_COMMUNIST_POLICIES_8UP;
                }
            }
        } else {
            f = NUM_FASCIST_POLICIES;
            l = NUM_LIBERAL_POLICIES;
            if (anarchistIncluded) {
                a = NUM_ANARCHIST_POLICIES;
            }
        }
        if (antiFascistPolicyIncluded) {
            // Swaps in for a Communist policy - it's placed on the Communist
            // tracker when enacted (see Board.enactPolicy()).
            c -= 1;
            draw.add(new Policy(Policy.Type.ANTI_FASCIST));
        }
        if (antiCommunistPolicyIncluded) {
            // Swaps in for a Fascist policy - it's placed on the Fascist
            // tracker when enacted.
            f -= 1;
            draw.add(new Policy(Policy.Type.ANTI_COMMUNIST));
        }
        if (socialDemocraticPolicyIncluded) {
            l -= 1;
            draw.add(new Policy(Policy.Type.SOCIAL_DEMOCRATIC));
        }
        
        for (int i = 0; i < f; i++) {
            draw.add(new Policy(Policy.Type.FASCIST));
        }
        for (int i = 0; i < l; i++) {
            draw.add(new Policy(Policy.Type.LIBERAL));
        }
        for (int i = 0; i < c; i++) {
            draw.add(new Policy(Policy.Type.COMMUNIST));
        }
        for (int i = 0; i < a; i++) {
            draw.add(new Policy(Policy.Type.ANARCHIST));
        }
        for (int i = 0; i < emergencyPowersIncluded; i++) {
            draw.add(new Policy(Policy.Type.ARTICLE_48));
            draw.add(new Policy(Policy.Type.ENABLING_ACT));
        }

        draw.shuffle();
    }

    /**
     * Randomly assigns the player roles.
     * 
     * @requires the number of players is between 5 and 10, inclusive.
     * @modifies this
     * @effects all Players in playerList are assigned either LIBERAL, FASCIST, or
     *          HITLER.
     *          The number of each is given by the following table:
     *          # players: 5 6 7 8 9 10
     *          # liberals: 3 4 4 5 5 6
     *          # fascists: 1 1 2 2 3 3
     *          # hitler: 1 1 1 1 1 1
     */
    private void assignRoles() {
        int players = playerList.size();
        if (players < MIN_PLAYERS) {
            throw new IllegalStateException("Cannot assign roles with insufficient players.");
        } else if (communistsIncluded && players < MIN_PLAYERS_WITH_COMMUNIST) {
            throw new IllegalStateException("Cannot assign roles with insufficient players.");
        } else if (anarchistIncluded && communistsIncluded && players < MIN_PLAYERS_WITH_COMMUNIST_AND_ANARCHIST) {
            throw new IllegalStateException("Cannot assign roles with insufficient players.");
        } else if ((monarchistIncluded && !communistsIncluded && players < MIN_PLAYERS_WITH_MONARCHIST) ||
                (monarchistIncluded && communistsIncluded && players < MIN_PLAYERS_WITH_COMMUNIST_AND_MONARCHIST)) {
            throw new IllegalStateException("Cannot assign roles with insufficient players.");
        } else if (players > MAX_PLAYERS) {
            throw new IllegalStateException("Cannot assign roles with too many players.");
        }

        int f = 0, c = 0;
        if (communistsIncluded) {
            f = NUM_FASCISTS_FOR_PLAYERS_WITH_COMMUNISTS[players];
            c = NUM_COMMUNISTS_FOR_PLAYERS[players];
        } else {
            f = NUM_FASCISTS_FOR_PLAYERS[players];
        }

        // Set all players to default state
        for (Player player : playerList) {
            player.setIdentity(Identity.LIBERAL);
        }

        // Randomly set one player to be hitler
        int indexOfHitler = random.nextInt(players);
        playerList.get(indexOfHitler).setIdentity(Identity.HITLER);

        if (anarchistIncluded) {
            if (communistsIncluded) {
                c--;
            }
            int randomIndex = random.nextInt(players);
            while (!playerList.get(randomIndex).isLiberal()) { // If player has already been set
                randomIndex = random.nextInt(players);
            }
            playerList.get(randomIndex).setIdentity(Identity.ANARCHIST);
        }
        if (monarchistIncluded) {
            f--;
            int randomIndex = random.nextInt(players);
            while (!playerList.get(randomIndex).isLiberal()) { // If player has already been set
                randomIndex = random.nextInt(players);
            }
            playerList.get(randomIndex).setIdentity(Identity.MONARCHIST);
        }
        while (f > 0) {
            int randomIndex = random.nextInt(players);
            if (playerList.get(randomIndex).isLiberal()) { // If player has not already been set
                playerList.get(randomIndex).setIdentity(Identity.FASCIST);
                f--;
            }
        }
        while (c > 0) {
            int randomIndex = random.nextInt(players);
            if (playerList.get(randomIndex).isLiberal()) { // If player has not already been set
                playerList.get(randomIndex).setIdentity(Identity.COMMUNIST);
                c--;
            }
        }
        if(capitalistIncluded) {
            int randomIndex = random.nextInt(players);
            while (!playerList.get(randomIndex).isLiberal()) { // If player has already been set
                randomIndex = random.nextInt(players);
            }
            playerList.get(randomIndex).setIdentity(Identity.CAPITALIST);
        }

        // Role assignment is done - fix each player's final dealt identity
        // forever, so later changes (e.g. Radicalization) don't erase what
        // their original teammates already know about them.
        for (Player player : playerList) {
            player.lockInOriginalIdentity();
        }
    }

    // </editor-fold>

    /////////////////// State Management
    // <editor-fold desc="State Management">

    /**
     * Gets the current state of the game.SecretHitlerGame.
     * 
     * @return a game.GameState representing the current state of the game.
     */
    public GameState getState() {
        if (board != null) {
            checkIfGameOver();
        }
        return state;
    }

    /**
     * Gets the current round number. The round is incremented whenever it's a
     * new president's turn, starting at 1 at the start of the game.
     */
    public int getRound() {
        return round;
    }

    /**
     * Updates the game state if the game ended by policies.
     * 
     * @modifies this
     * @effects sets this.state to the FASCIST_VICTORY_POLICY or
     *          LIBERAL_VICTORY_POLICY states if the
     *          win conditions for policies are met.
     */
    private void checkIfGameOver() {
        if (board.isFascistVictory()) {
            this.lastState = this.state;
            // Reaching fascist policy victory implies Hitler was never elected
            // chancellor (that would have already ended the game via election),
            // so the Monarchist's win condition is satisfied here.
            if (monarchistIncluded && capitalistIncluded) {
                state = GameState.MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_POLICY;
            } else if (monarchistIncluded) {
                state = GameState.MONARCHIST_VICTORY_WITH_FASCIST_POLICY;
            } else if (capitalistIncluded) {
                state = GameState.CAPITALIST_VICTORY_WITH_FASCIST_POLICY;
            } else {
                state = GameState.FASCIST_VICTORY_POLICY;
            }
        } else if (board.isLiberalVictory(longerLiberalTrackIncluded)) {
            this.lastState = this.state;
            if (capitalistIncluded){
                state = GameState.CAPITALIST_VICTORY_WITH_LIBERAL_POLICY;
            } else {
                state = GameState.LIBERAL_VICTORY_POLICY;
            }
        } else if (board.isCommunistVictory(playerList.size())) {
            this.lastState = this.state;
            if (anarchistIncluded && board.isAnarchistVonWithCommunists(playerList.size())) {
                state = GameState.ANARCHIST_WITH_COMMUNIST_VICTORY_POLICY;
            } else {
                state = GameState.COMMUNIST_VICTORY_POLICY;
            }
        }
    }

    /**
     * Returns whether the game has reached an ending victory state.
     */
    public boolean hasGameFinished() {
        return state == GameState.FASCIST_VICTORY_ELECTION
                || state == GameState.FASCIST_VICTORY_POLICY
                || state == GameState.LIBERAL_VICTORY_EXECUTION
                || state == GameState.LIBERAL_VICTORY_POLICY
                || state == GameState.COMMUNIST_VICTORY_POLICY
                || state == GameState.COMMUNIST_WITH_LIBERAL_VICTORY_EXECUTION
                || state == GameState.ANARCHIST_WITH_COMMUNIST_VICTORY_POLICY
                || state == GameState.ANARCHIST_VICTORY_POLICY
                || state == GameState.CAPITALIST_VICTORY_WITH_LIBERAL_POLICY
                || state == GameState.CAPITALIST_VICTORY_WITH_LIBERAL_EXECUTION
                || state == GameState.CAPITALIST_VICTORY_WITH_FASCIST_POLICY
                || state == GameState.CAPITALIST_VICTORY_WITH_FASCIST_ELECTION
                || state == GameState.MONARCHIST_VICTORY_WITH_FASCIST_ELECTION
                || state == GameState.MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_ELECTION
                || state == GameState.MONARCHIST_VICTORY_WITH_FASCIST_POLICY
                || state == GameState.MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_POLICY;
    }

    /**
     * Adds the discard deck to the draw deck and shuffles.
     * 
     * @modifies this
     * @effects Empties the discard pile into the draw pile and shuffles.
     */
    private void shuffleDiscardIntoDraw() {
        while (!discard.isEmpty()) {
            draw.add(discard.remove());
        }
        draw.shuffle();
    }

    // </editor-fold>

    /////////////////// Nomination and Voting
    // <editor-fold desc="Nomination and Voting">

    public int getLivingPlayerCount() {
        int numLivingPlayers = 0;
        for (Player player : playerList) {
            if (player.isAlive())
                numLivingPlayers++;
        }
        return numLivingPlayers;
    }

    /**
     * Selects the chancellor for the current legislation.
     * 
     * @param username username of the chancellor to elect.
     * @throws IllegalStateException    if the state is not
     *                                  {@code CHANCELLOR_SELECTION}.
     * @throws IllegalArgumentException if the player named {@code username} was the
     *                                  last elected chancellor, if
     *                                  there are >5 players and they are the last
     *                                  elected president, if the player is dead,
     *                                  or if the player does not exist.
     * @modifies this
     * @effects the current chancellor is set to the player named {@code username}.
     *          The game state is set to be in
     *          CHANCELLOR_VOTING.
     */
    public void nominateChancellor(String username) {
        int numLivingPlayers = getLivingPlayerCount();

        if (getState() != GameState.CHANCELLOR_NOMINATION) {
            throw new IllegalStateException("Cannot elect a chancellor now (invalid state).");
        } else if (username.equals(lastChancellor) || (username.equals(lastPresident) && numLivingPlayers > 5)) {
            throw new IllegalArgumentException("Cannot elect chancellor that was previously in office.");
        } else if (!hasPlayer(username)) {
            throw new IllegalArgumentException("Player " + username + " does not exist.");
        } else if (!getPlayer(username).isAlive()) {
            throw new IllegalArgumentException("Player " + username + " is dead and cannot be elected for chancellor.");
        } else if (username.equals(currentPresident)) {
            throw new IllegalArgumentException("President cannot choose themselves as chancellor.");
        }

        didElectionTrackerAdvance = false; // reset the election tracker
        currentChancellor = username;
        this.lastState = this.state;
        state = GameState.CHANCELLOR_VOTING; // exits the previous state.
        voteMap = new HashMap<>(); // initializes a new map for voting.
    }

    /**
     * Returns whether the given player has voted yet during the currently
     * open voting round.
     */
    public boolean hasPlayerVoted(String username) {
        return voteMap.containsKey(username);
    }

    /**
     * Registers a vote for chancellor from a given player.
     * 
     * @param username the name of the player giving the vote.
     * @param vote     the vote of the player (true = ja, false = nein).
     * @throws IllegalArgumentException if the Player with name {@code username} is
     *                                  not in the game.
     * @throws IllegalStateException    if the Player with name {@code username} has
     *                                  already voted, or if the game is not
     *                                  in the voting ({@code CHANCELLOR_VOTING})
     *                                  state.
     * @modifies this
     * @effects registers the given vote. If all players have voted, determines
     *          whether the vote passed.
     *          If the vote passed, the state advances to
     *          {@code LEGISLATIVE_PRESIDENT}. (unless the chancellor is Hitler
     *          and three fascist policies have been passed, in which case the state
     *          advances to {@code FASCIST_VICTORY_ELECTION.}
     *          If the vote did not pass, advances the election tracker by 1. If the
     *          election tracker reaches its max,
     *          immediately enacts the top policy on the pile and handles state
     *          progression.
     */
    public void registerVote(String username, boolean vote) {
        if (!hasPlayer(username)) {
            throw new IllegalArgumentException("Player " + username + " is not in the game and cannot vote.");
        } else if (voteMap.containsKey(username)) {
            throw new IllegalStateException("Player " + username + " cannot vote twice.");
        } else if (state != GameState.CHANCELLOR_VOTING) {
            throw new IllegalStateException("Player " + username + " cannot vote when a vote is not taking place.");
        }

        voteMap.put(username, vote);

        // Count up votes and check if all votes have been submitted.
        boolean allPlayersHaveVoted = true;
        int totalVotes = 0;
        int totalYesVotes = 0;

        for (Player player : playerList) {
            String playerName = player.getUsername();

            if (player.isAlive()) { // only account for
                if (voteMap.containsKey(playerName)) {
                    totalVotes += 1;
                    if (voteMap.get(playerName)) { // player voted yes
                        totalYesVotes += 1;
                    }
                } else {
                    allPlayersHaveVoted = false; // Player vote not accounted for
                }
            }
        }

        if (allPlayersHaveVoted) {
            if (((float) totalYesVotes / (float) totalVotes) > VOTING_CUTOFF) { // vote passed successfully
                lastChancellor = currentChancellor;
                lastPresident = currentPresident;
                if (getPlayer(currentChancellor).isHitler() && board.fascistsCanWinByElection()) {
                    this.lastState = this.state;
                    // Fascists won by electing Hitler chancellor: game ends.
                    // The Monarchist's plot to keep Hitler out of office failed,
                    // so they do not share in this particular victory.
                    if (monarchistIncluded && capitalistIncluded) {
                        state = GameState.MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_ELECTION;
                    } else if (monarchistIncluded) {
                        state = GameState.MONARCHIST_VICTORY_WITH_FASCIST_ELECTION;
                    } else if (capitalistIncluded) {
                        state = GameState.CAPITALIST_VICTORY_WITH_FASCIST_ELECTION;
                    } else {
                        state = GameState.FASCIST_VICTORY_ELECTION;
                    }
                } else {
                    // The chancellor survived an election that would have won
                    // the game for the Fascists had they been Hitler, so
                    // they're now publicly confirmed to not be Hitler.
                    if (board.fascistsCanWinByElection()) {
                        getPlayer(currentChancellor).confirmNotHitler();
                    }
                    startLegislativeSession();
                }
            } else { // vote failed
                advanceElectionTracker();
            }
        }
    }

    /**
     * Returns a map representing what each player voted.
     * 
     * @return a map, where the keys are the usernames and the booleans are the vote
     *         (yes/no) of the player.
     */
    public Map<String, Boolean> getVotes() {
        return new HashMap<>(voteMap); // making a copy of the votes so that they cannot be externally modified
    }

    /**
     * Advances the election tracker and enacts a policy if needed.
     * 
     * @modifies this
     * @effects If the tracker < 2, advances the tracker by 1.
     *          If the tracker == 2, rests the tracker to 0 and enacts the first
     *          policy on the top of the draw pile.
     */
    private void advanceElectionTracker() {
        didElectionTrackerAdvance = true;
        electionTracker += 1;
        if (electionTracker == MAX_FAILED_ELECTIONS) {
            electionTracker = 0; // Reset
            forceDrawAndEnactTopPolicy();
        } else {
            concludePresidentialActions();
        }
    }

    /**
     * Forces the enactment of the top policy in the draw deck, exactly as
     * when the election tracker reaches its maximum: no presidential power
     * is granted (this is also used by the Anarchist's cascading mechanic,
     * which mimics the election tracker reaching its maximum).
     *
     * @modifies this
     * @effects removes the top card of the draw deck, enacts it, and updates
     *          the game state accordingly (bypassing presidential powers).
     *          Note that the drawn policy is NOT added back to the discard
     *          pile.
     */
    private void forceDrawAndEnactTopPolicy() {
        if (draw.getSize() < MIN_DRAW_DECK_SIZE) {
            shuffleDiscardIntoDraw();
        }
        Policy newPolicy = draw.remove();
        board.enactPolicy(newPolicy);

        // This round never reached a presidential discard, so any leftover
        // reference from an earlier round cannot be used for Vote Of No
        // Confidence.
        lastPresidentDiscardedPolicy = null;

        boolean previouslyAdvanced = didElectionTrackerAdvance;
        didElectionTrackerAdvance = true; // Ensures onEnactPolicy skips presidential powers.
        onEnactPolicy(newPolicy.getType());
        didElectionTrackerAdvance = previouslyAdvanced;
    }

    // </editor-fold>

    /////////////////// President Management
    // <editor-fold desc="President Management">
    /**
     * Called when the president's actions are over.
     * 
     * @modifies this
     * @effects advances the state to {@code POST_LEGISLATIVE}.
     */
    private void concludePresidentialActions() {
        this.lastState = this.state;
        this.state = GameState.POST_LEGISLATIVE;
        emergencyPowerActingOfficer = null;
    }

    /**
     * @return the username of the officer (President for Article 48, or
     *         Chancellor for the Enabling Act) currently authorized to act
     *         while an emergency power is being resolved. Returns null if no
     *         emergency power is currently being resolved (including when
     *         Execution/Peek were reached normally via the fascist tracker).
     */
    public String getEmergencyPowerActingOfficer() {
        return emergencyPowerActingOfficer;
    }

    /**
     * Called to end the current's president term.
     * 
     * @throws IllegalStateException if the state is not {@code POST_LEGISLATIVE}.
     * @modifies this
     * @effects advances the state to {@code CHANCELLOR_NOMINATION} and updates the
     *          current president.
     *          If the PRESIDENTIAL_POWER_ELECTION power was activated, sets the
     *          next president to the elected.
     *          If the election round finished, returns to the next player in the
     *          normal round ordering.
     *          Otherwise, chooses the next eligible (alive) player in the ordering
     *          to become president.
     */
    public void endPresidentialTerm() {
        if (this.state != GameState.POST_LEGISLATIVE) {
            throw new IllegalStateException();
        }

        if (electedPresident != null) { // If the PRESIDENTIAL_POWER_ELECTION was active, chooses the elected president.
            currentPresident = electedPresident;
            electedPresident = null;
        } else if (nextPresident != null) { // Once the PRESIDENTIAL_POWER_ELECTION round concludes, returns to the
                                            // normal order.
            currentPresident = nextPresident;
            while (!getPlayer(currentPresident).isAlive()) {
                // Advance to the next president
                int presIndex = indexOfPlayer(currentPresident);
                List<Player> playerList = getPlayerList();
                int nextPresIndex = (presIndex + 1) % playerList.size(); // Loops to 0 if last president killed
                currentPresident = playerList.get(nextPresIndex).getUsername();
            }
            nextPresident = null;
        } else { // advance the presidency
            // Advance presidency:
            currentPresident = getNextActivePlayer(currentPresident);
        }
        currentChancellor = null;
        this.lastState = this.state;
        this.state = GameState.CHANCELLOR_NOMINATION;
        this.round++;
    }

    /**
     * Finds the next active, living player in order.
     * 
     * @param player the player to find the next active player from.
     * @return the username of the next player in order. Returns null if no player
     *         was found.
     */
    private String getNextActivePlayer(String player) {
        for (int i = 1; i < playerList.size(); i++) {
            int index = (i + indexOfPlayer(player)) % playerList.size();
            if (playerList.get(index).isAlive()) {
                return playerList.get(index).getUsername();
            }
        }
        return null;
    }

    // </editor-fold>

    /////////////////// Legislative Session
    // <editor-fold desc="Legislative Session">

    /**
     * Starts the legislative session.
     * Sets the available policies and the game state.
     */
    private void startLegislativeSession() {
        // Action may be triggered more than once in failure states,
        // so only update lastState once
        if (this.state != this.lastState && this.state != GameState.LEGISLATIVE_PRESIDENT) {
            this.lastState = this.state;
            state = GameState.LEGISLATIVE_PRESIDENT; // Legislative session begins.
        }

        if (draw.getSize() < MIN_DRAW_DECK_SIZE) {
            shuffleDiscardIntoDraw();
        }

        legislativePolicies = new ArrayList<>();
        for (int i = 0; i < PRESIDENT_DRAW_SIZE; i++) {
            legislativePolicies.add(draw.remove());
        }
        // A new round means any previous Enabling Act "Vote Of No Confidence"
        // reference is no longer relevant.
        lastPresidentDiscardedPolicy = null;
    }

    /**
     * Gets the legislative choices available for the President.
     * 
     * @throws IllegalStateException if called when the state is not
     *                               {@code LEGISLATIVE_PRESIDENT} or if an
     *                               incorrect
     *                               number of policies is available.
     * @return a list of policies (size of {@code this.PRESIDENT_DRAW_SIZE})
     *         representing the available choices.
     */
    public List<Policy> getPresidentLegislativeChoices() {
        if (state != GameState.LEGISLATIVE_PRESIDENT) {
            throw new IllegalStateException(
                    "Cannot get President legislative choices when not in legislative session.");
        }
        return new ArrayList<>(legislativePolicies); // makes a copy of the legislative policies
    }

    /**
     * Discards the Policy in the list of presidential policy options.
     * 
     * @param index the index of the policy in the list of legislative choices to
     *              enact.
     * @throws IllegalStateException     if called when the state is not
     *                                   {@code LEGISLATIVE_PRESIDENT}.
     * @throws IndexOutOfBoundsException if index is outside of range [0, 2]
     *                                   (inclusive).
     * @modifies this
     * @effects adds the card in the policy list at index {@code index} to the
     *          discard deck.
     *          Advances state to the {@code LEGISLATIVE_CHANCELLOR} state.
     */
    public void presidentDiscardPolicy(int index) {
        if (state != GameState.LEGISLATIVE_PRESIDENT) {
            throw new IllegalStateException("Cannot discard a policy from the president's hand in this state.");
        } else if (index < 0 || index >= PRESIDENT_DRAW_SIZE) {
            throw new IndexOutOfBoundsException("Cannot discard policy at the index " + index + "");
        }
        Policy discardedPolicy = legislativePolicies.remove(index);
        lastPresidentDiscardedPolicy = discardedPolicy;
        discard.add(discardedPolicy);
        this.lastState = this.state;
        state = GameState.LEGISLATIVE_CHANCELLOR;
    }

    /**
     * Gets the legislative choices available for the Chancellor.
     * 
     * @throws IllegalStateException if called when the state is not
     *                               {@code LEGISLATIVE_CHANCELLOR} or
     *                               {@code LEGISLATIVE_PRESIDENT_VETO}.
     * @return a list of policies (size of {@code this.CHANCELLOR_DRAW_SIZE})
     *         representing the available choices.
     */
    public List<Policy> getChancellorLegislativeChoices() {
        if (getState() != GameState.LEGISLATIVE_CHANCELLOR && getState() != GameState.LEGISLATIVE_PRESIDENT_VETO) {
            throw new IllegalStateException(
                    "Cannot get Chancellor legislative choices when not in legislative session.");
        }
        if (legislativePolicies.size() != CHANCELLOR_DRAW_SIZE) {
            throw new IllegalStateException(
                    "An incorrect number of legislative policies are available for the chancellor ("
                            + legislativePolicies.size() + " instead of " + CHANCELLOR_DRAW_SIZE);
        }
        return new ArrayList<>(legislativePolicies);
    }

    /**
     * Enacts the Policy in the list of chancellor policy options.
     * 
     * @param index the index of the policy in the list of legislative choices to
     *              enact.
     * @throws IllegalStateException     if called when the state is not
     *                                   {@code LEGISLATIVE_CHANCELLOR}.
     * @throws IndexOutOfBoundsException if index is outside of range [0, 2]
     *                                   (inclusive).
     * @modifies this
     * @effects enacts the policy at index {@code index} and discards the remaining
     *          card.
     *          Advances state to any relevant presidential powers, otherwise,
     *          advances to state {@code POST_LEGISLATIVE}.
     */
    public void chancellorEnactPolicy(int index) {
        if (getState() != GameState.LEGISLATIVE_CHANCELLOR) {
            throw new IllegalStateException("Cannot discard a policy from the chancellor's hand in this state.");
        } else if (index < 0 || index >= CHANCELLOR_DRAW_SIZE) {
            throw new IndexOutOfBoundsException("Cannot discard policy at the index " + index + "");
        }

        Policy newPolicy = legislativePolicies.remove(index);
        board.enactPolicy(newPolicy);
        discard.add(legislativePolicies.remove(0)); // Discard last remaining Policy
        didVetoOccurThisTurn = false; // Reset because we have moved past chancellor stage
        onEnactPolicy(newPolicy.getType());
    }

    /**
     * Marks the chancellor as having vetoed the current policy agenda.
     *
     * @throws IllegalStateException if called when the state is not
     *                               {@code LEGISLATIVE_CHANCELLOR}, or if
     *                               veto isn't available yet (see
     *                               {@link #isVetoAvailable()}).
     * @modifies this
     * @effects advances the state to {@code LEGISLATIVE_PRESIDENT_VETO} and awaits
     *          the president's approval.
     */
    public void chancellorVeto() {
        if (getState() != GameState.LEGISLATIVE_CHANCELLOR) {
            throw new IllegalStateException("Cannot veto in state " + getState().toString());
        } else if (!isVetoAvailable()) {
            throw new IllegalStateException(
                    "Veto is only available once the fascist or communist tracker is one policy away from "
                            + "winning (fascist: " + board.getNumFascistPolicies() + ", communist: "
                            + board.getNumCommunistPolicies() + ")");
        } else if (didVetoOccurThisTurn()) {
            throw new IllegalStateException("Cannot veto again once veto is denied.");
        }
        didVetoOccurThisTurn = true;
        state = GameState.LEGISLATIVE_PRESIDENT_VETO;
    }

    /**
     * @return true if the fascist or communist tracker is exactly one
     *         policy away from winning by policy count - the condition
     *         under which veto power becomes available to the Chancellor.
     */
    public boolean isVetoAvailable() {
        boolean fascistVetoSlot = board.getNumFascistPolicies() == board.getFascistPoliciesToWin() - 1;
        boolean communistVetoSlot = communistsIncluded
                && board.getNumCommunistPolicies() == board.getCommunistPoliciesToWin(playerList.size()) - 1;
        return fascistVetoSlot || communistVetoSlot;
    }

    /**
     * Handles the president's response to an initiated veto.
     * 
     * @param response is the president's vote (true = veto accepted, false = veto
     *                 denied)
     * @throws IllegalStateException if called when the state is not
     *                               {@code LEGISLATIVE_PRESIDENT_VETO}
     * @modifies this
     * @effects If the veto is denied ({@code response} = false), then the game
     *          returns to the chancellor's decision
     *          (state = LEGISLATIVE_CHANCELLOR).
     *          If the veto is approved ({@code response} = true), then the game
     *          advances the election tracker. If the
     *          tracker is at MAX_FAILED_ELECTIONS, enacts the first policy on top
     *          of the draw deck and carries out
     *          any consequences of the policy.
     */
    public void presidentialVeto(boolean response) {
        if (state != GameState.LEGISLATIVE_PRESIDENT_VETO) {
            throw new IllegalStateException(
                    "Cannot get president veto input during state " + getState().toString() + "");
        }
        if (response) { // veto was approved, advance election tracker
            // Empty legislative back into discard pile
            while (!legislativePolicies.isEmpty()) {
                discard.add(legislativePolicies.remove(0));
            }
            advanceElectionTracker();
            didVetoOccurThisTurn = false;
        } else { // veto was denied, return to chancellor selection
            this.lastState = this.state;
            state = GameState.LEGISLATIVE_CHANCELLOR;
        }
    }

    /**
     * Called AFTER a policy has been enacted, and sets the state according to any
     * consequences.
     * 
     * @modifies this
     * @effects sets the state to handle any presidential powers that arise.
     *          Otherwise, state is set to {@code CHANCELLOR_SELECTION}.
     *          Also handles reshuffling the discard into the draw deck when there
     *          are insufficient cards for a hand,
     *          and resets the election tracker to 0.
     */
    private void onEnactPolicy(Policy.Type policyType) {
        electionTracker = 0;
        lastEnactedPolicy = policyType;
        anarchistCascadeForcedPolicy = null;

        if (draw.getSize() < MIN_DRAW_DECK_SIZE) {
            shuffleDiscardIntoDraw();
        }

        // Applies regardless of how the policy was enacted: a "Marked For
        // Execution" target is killed once 3 fascist policies have passed.
        if (resolveMarkedForExecution()) {
            return; // The marked player was Hitler: the game has already ended.
        }

        if (didElectionTrackerAdvance()) {
            // A policy forced through (either by the election tracker
            // reaching its maximum, or by the Anarchist's own cascading
            // mechanic below, which mimics it) never grants a presidential
            // power. In the standalone Anarchist-without-Communists variant,
            // the Anarchist wins immediately if this forced policy is itself
            // an Anarchist policy.
            if (policyType == Policy.Type.ANARCHIST && anarchistIncluded && !communistsIncluded) {
                this.lastState = this.state;
                state = GameState.ANARCHIST_VICTORY_POLICY;
                return;
            }
            // A forced Article 48/Enabling Act still fills its printed slot
            // on the Emergency Powers board (so it's visible there), but -
            // same as any other forced policy - its power never resolves.
            // There's also no sitting chancellor to act on a forced Enabling
            // Act (a failed election clears currentChancellor), so there's
            // no one to assign as the acting officer anyway.
            if (policyType == Policy.Type.ARTICLE_48 || policyType == Policy.Type.ENABLING_ACT) {
                emergencyPowersEnacted++;
                emergencyPowerTrackerHistory.add(policyType);
            }
            state = GameState.POST_LEGISLATIVE;
            // Reset the term limits (last chancellor and president)
            lastChancellor = null;
            lastPresident = null;
            return;
        }

        this.lastState = this.state;

        // Emergency power cards never advance a tracker or grant a tracker
        // power; instead they fill the next slot on the printed Emergency
        // Powers board, whose power then applies automatically - no choice
        // is involved (see getEmergencyPowerForSlot()).
        if (policyType == Policy.Type.ARTICLE_48) {
            currentEmergencyPowerIsArticle48 = true;
            emergencyPowerActingOfficer = currentPresident;
            emergencyPowersEnacted++;
            emergencyPowerTrackerHistory.add(policyType);
            resolveEmergencyPower(getEmergencyPowerForSlot(emergencyPowersEnacted, true));
            return;
        } else if (policyType == Policy.Type.ENABLING_ACT) {
            currentEmergencyPowerIsArticle48 = false;
            emergencyPowerActingOfficer = currentChancellor;
            emergencyPowersEnacted++;
            emergencyPowerTrackerHistory.add(policyType);
            resolveEmergencyPower(getEmergencyPowerForSlot(emergencyPowersEnacted, false));
            return;
        }

        // Anti-policies pull a policy off the opposing tracker and suppress that
        // tracker's power the next time it re-fills the vacated slot.
        if (policyType == Policy.Type.ANTI_COMMUNIST) {
            board.removeCommunistPolicy();
            skipNextCommunistPower = true;
        } else if (policyType == Policy.Type.ANTI_FASCIST) {
            board.removeFascistPolicy();
            skipNextFascistPower = true;
        }

        // In the standalone Anarchist-without-Communists variant, a normally
        // enacted Anarchist policy doesn't win outright, but shuffles a new
        // Anarchist policy into the draw deck and immediately forces the
        // next policy through, exactly as if the election tracker had
        // reached its maximum (which may itself trigger the Anarchist's win
        // condition, handled above).
        if (policyType == Policy.Type.ANARCHIST && anarchistIncluded && !communistsIncluded) {
            draw.add(new Policy(Policy.Type.ANARCHIST));
            discard.add(new Policy(Policy.Type.ANARCHIST));
            draw.shuffle();
            forceDrawAndEnactTopPolicy();
            // forceDrawAndEnactTopPolicy() just ran a second, nested
            // onEnactPolicy() call for the forced card, which overwrote
            // lastEnactedPolicy with ITS type (and cleared this field again).
            // Surface both results distinctly: the Anarchist card as the
            // "normal" enacted policy, and what it forced through alongside it.
            anarchistCascadeForcedPolicy = lastEnactedPolicy;
            lastEnactedPolicy = Policy.Type.ANARCHIST;
            return;
        }

        // Social Democratic policy: the President chooses which tracker to
        // remove a policy from; it never grants a tracker power itself.
        if (policyType == Policy.Type.SOCIAL_DEMOCRATIC) {
            state = GameState.PRESIDENTIAL_POWER_SOCIAL_DEMOCRATIC;
            return;
        }

        PresidentialPower activatedPower = board.getActivatedPower();

        boolean onFascistTrack = policyType == Policy.Type.FASCIST || policyType == Policy.Type.ANTI_COMMUNIST;
        boolean onCommunistTrack = policyType == Policy.Type.COMMUNIST || policyType == Policy.Type.ANTI_FASCIST
                || policyType == Policy.Type.ANARCHIST;

        if (policyType == Policy.Type.ANARCHIST && communistsIncluded) {
            // With Communists in the game, an Anarchist policy still fills the
            // communist tracker slot but never grants the communist power.
            activatedPower = PresidentialPower.NONE;
        } else if (onFascistTrack && skipNextFascistPower) {
            activatedPower = PresidentialPower.NONE;
            skipNextFascistPower = false;
        } else if (onCommunistTrack && skipNextCommunistPower) {
            activatedPower = PresidentialPower.NONE;
            skipNextCommunistPower = false;
        }

        // Clear any leftover votes from a previous Radicalization/Communist
        // Investigation power before (possibly) entering a new one.
        communistVotes.clear();

        switch (activatedPower) {
            case PEEK:
                state = GameState.PRESIDENTIAL_POWER_PEEK;
                break;
            case EXECUTION:
                state = GameState.PRESIDENTIAL_POWER_EXECUTION;
                break;
            case ELECTION:
                state = GameState.PRESIDENTIAL_POWER_ELECTION;
                break;
            case INVESTIGATE:
                state = GameState.PRESIDENTIAL_POWER_INVESTIGATE;
                break;
            case COMMUNISTS_INVESTIGATION:
                state = GameState.PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION;
                break;
            case CONFESSION:
                state = GameState.PRESIDENTIAL_POWER_CONFESSION;
                break;
            case CONGRESS:
                state = GameState.PRESIDENTIAL_POWER_CONGRESS;
                break;
            case FIVE_YEAR_PLAN:
                state = GameState.PRESIDENTIAL_POWER_FIVE_YEAR_PLAN;
                break;
            case RADICALIZATION:
                state = GameState.PRESIDENTIAL_POWER_RADICALIZATION;
                break;
            case NONE:
                state = GameState.POST_LEGISLATIVE;
                break;
        }
    }

    /**
     * Executes the player marked by the "Marked For Execution" emergency
     * power once 3 fascist policies have been enacted in total.
     *
     * @modifies this
     * @effects if the marked player is executed and is Hitler, ends the game
     *          with the appropriate victory state.
     * @return true if the game ended as a result of this execution.
     */
    private boolean resolveMarkedForExecution() {
        if (markedForExecutionUsername == null
                || board.getNumFascistPolicies() < 3) {
            return false;
        }

        Player marked = getPlayer(markedForExecutionUsername);
        markedForExecutionUsername = null;
        if (!marked.isAlive()) {
            return false;
        }

        marked.kill();
        if (marked.isHitler()) {
            this.lastState = this.state;
            state = determineHitlerExecutionVictoryState();
            return true;
        }
        return false;
    }

    /**
     * Determines the victory state that results from Hitler being executed,
     * taking into account which optional parties are included.
     */
    private GameState determineHitlerExecutionVictoryState() {
        if (communistsIncluded) {
            return GameState.COMMUNIST_WITH_LIBERAL_VICTORY_EXECUTION;
        } else if (capitalistIncluded) {
            return GameState.CAPITALIST_VICTORY_WITH_LIBERAL_EXECUTION;
        } else {
            return GameState.LIBERAL_VICTORY_EXECUTION;
        }
    }

    // </editor-fold>

    /////////////////// Presidential Powers
    // <editor-fold desc="Presidential Powers">

    /**
     * Gets the preview of the top 3 cards of the deck during the
     * {@code PRESIDENTIAL_POWER_PEEK} state.
     * 
     * @throws IllegalStateException if called when state is not
     *                               {@code PRESIDENTIAL_POWER_PEEK}.
     * @return a size-3 list of Policies, where index 0 is the top of the stack.
     */
    public List<Policy> getPeek() {
        if (state != GameState.PRESIDENTIAL_POWER_PEEK) {
            throw new IllegalStateException("Cannot peek when the power is not active.");
        } else if (draw.getSize() < 3) {
            throw new IllegalStateException("Insufficient cards in the draw deck.");
        }
        List<Policy> policies = new ArrayList<>();
        policies.add(draw.peek(0));
        policies.add(draw.peek(1));
        policies.add(draw.peek(2));
        return policies;
    }

    /**
     * Ends the {@code PRESIDENTIAL_POWER_PEEK} state.
     * 
     * @throws IllegalStateException if called when state is not
     *                               {@code PRESIDENTIAL_POWER_PEEK}.
     * @effects Advances the state to {@code POST_LEGISLATIVE}.
     */
    public void endPeek() {
        concludePresidentialActions();
    }

    /**
     * Ends the {@code PRESIDENTIAL_POWER_CONGRESS} state, once the newly
     * radicalised communists have had a chance to recognise the original ones.
     *
     * @throws IllegalStateException if called when state is not
     *                               {@code PRESIDENTIAL_POWER_CONGRESS}.
     * @effects Advances the state to {@code POST_LEGISLATIVE}.
     */
    public void endCongress() {
        if (state != GameState.PRESIDENTIAL_POWER_CONGRESS) {
            throw new IllegalStateException("Cannot end congress when the power is not active.");
        }
        congressHappened = true;
        concludePresidentialActions();
    }

    /**
     * @return whether the Congress power has resolved yet. In 8+ player
     *         games, Communists only learn each other's identities once
     *         this is true.
     */
    public boolean hasCongressHappened() {
        return congressHappened;
    }

    /**
     * Investigates the party identity of a given player.
     * 
     * @param username the username of the player to investigate.
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_INVESTIGATE}.
     * @throws IllegalArgumentException if the player is not alive, is not in the
     *                                  game, or has been investigated previously.
     * @return the party membership of the player. If the player is fascist or
     *         Hitler, returns Identity.FASCIST.
     *         if the player is liberal, returns Identity.LIBERAL.
     *         if the player is communist, returns Identity.COMMUNIST.
     *         if the player is anarchist, returns Identity.ANARCHIST.
     *         if the player is capitalist, returns Identity.CAPITALIST.
     *         Once called, advances the state of the game to POST_LEGISLATIVE.
     */
    public Identity investigatePlayer(String username) {
        if (state != GameState.PRESIDENTIAL_POWER_INVESTIGATE) {
            throw new IllegalStateException("Cannot investigate a player when the power is not active.");
        } else if (!hasPlayer(username)) {
            throw new IllegalArgumentException("Player " + username + " does not exist.");
        } else if (!getPlayer(username).isAlive()) {
            throw new IllegalArgumentException("Cannot investigate a dead player (" + username + ").");
        } else if (getPlayer(username).hasBeenInvestigated()) {
            throw new IllegalArgumentException("Cannot investigate a player twice (" + username + ").");
        }

        target = username;
        getPlayer(username).investigate(); // sets a flag that this player has been investigated.
        concludePresidentialActions();

        if (getPlayer(username).isFascist()) {
            return Identity.FASCIST;
        } else if (getPlayer(username).isCommunist()) {
            return Identity.COMMUNIST;
        } else if (getPlayer(username).isAnarchist()) {
            return Identity.ANARCHIST;
        } else if (getPlayer(username).isCapitalist()) {
            return Identity.CAPITALIST;
        } else {
            return Identity.LIBERAL;
        }
    }

    /**
     * Executes a given player.
     * 
     * @param username the username of the player to execute.
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_EXECUTION}.
     * @throws IllegalArgumentException if the player is already dead or is not in
     *                                  the game.
     * @modifies this
     * @effects The specified player is marked as not alive.
     *          If they were Hitler, advances the state of the game to
     *          LIBERAL_VICTORY_EXECUTION.
     *          Otherwise, once called, advances the state of the game to
     *          POST_LEGISLATIVE.
     */
    public void executePlayer(String username) {
        if (state != GameState.PRESIDENTIAL_POWER_EXECUTION) {
            throw new IllegalStateException("Cannot execute a player when the power is not active.");
        } else if (!hasPlayer(username)) {
            throw new IllegalArgumentException("Player " + username + " does not exist.");
        }

        Player playerToKill = getPlayer(username);
        target = username;
        if (!playerToKill.isAlive()) {
            throw new IllegalArgumentException("Cannot execute " + username + " because they are not alive.");
        }

        playerToKill.kill();
        if (playerToKill.isHitler()) { // game ends.
            this.lastState = this.state;
            state = determineHitlerExecutionVictoryState();
        } else {
            concludePresidentialActions();
        }
    }

    /**
     * Sets the next president through the Election power.
     * 
     * @param username the username of the player to become president next.
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_ELECTION}.
     * @throws IllegalArgumentException if the player is dead or is not in the game.
     * @modifies this
     * @effects The specified player becomes the next president.
     *          After the completion of their term, the presidency returns to the
     *          next president
     *          in the normal rotation order.
     *          Once called, advances the state to POST_LEGISLATIVE.
     */
    public void electNextPresident(String username) {
        if (state != GameState.PRESIDENTIAL_POWER_ELECTION) {
            throw new IllegalStateException("Cannot elect a player president when the power is not active.");
        } else if (!hasPlayer(username)) {
            throw new IllegalArgumentException("Player " + username + " does not exist.");
        } else if (!getPlayer(username).isAlive()) {
            throw new IllegalArgumentException("Cannot elect " + username + " because they are not alive.");
        }

        target = username;
        nextPresident = getNextActivePlayer(currentPresident);

        if (this.currentPresident.equals(target)) {
            throw new IllegalArgumentException("President cannot elect themselves during a special election.");
        }

        electedPresident = username;
        concludePresidentialActions();
    }

    /**
     * Investigates a player identity to all communists.
     * 
     * @param targetUsername the username of the player to investigate.
     * @param investigatorUsername the username of the investigator (for validation).
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_INVESTIGATE}.
     * @throws IllegalArgumentException if one of players is not alive, not in the
     *                                  game or not communist.
     * @return the party membership of the player. If the player is fascist or
     *         Hitler, returns Identity.FASCIST.
     *         if the player is liberal, returns Identity.LIBERAL.
     *         if the player is anarchist, returns Identity.ANARCHIST.
     *         if the player is capitalist, returns Identity.CAPITALIST.
     *         Once called, advances the state of the game to POST_LEGISLATIVE.
     */
    public Identity communistsInvestigatePlayer(String targetUsername, String investigatorUsername) {
        if (state != GameState.PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION) {
            throw new IllegalStateException("Cannot investigate a player when the power is not active.");
        } else if (!hasPlayer(targetUsername)) {
            throw new IllegalArgumentException("Player " + targetUsername + " does not exist.");
        } else if (!getPlayer(investigatorUsername).isAlive()) {
            throw new IllegalArgumentException("Player " + investigatorUsername + " does not exist.");
        } else if (!getPlayer(targetUsername).isAlive()) {
            throw new IllegalArgumentException("Cannot investigate a dead player (" + targetUsername + ").");
        } else if (!getPlayer(investigatorUsername).isAlive()) {
            throw new IllegalArgumentException("Cannot investigate a dead player (" + investigatorUsername + ").");
        } else if (!getPlayer(investigatorUsername).isCommunist()) {
            throw new IllegalArgumentException("Only communist players can investigate during this power.");
        }

        target = targetUsername;
        concludePresidentialActions();

        if (getPlayer(targetUsername).isFascist()) {
            return Identity.FASCIST;
        } else if (getPlayer(targetUsername).isCommunist()) {
            return Identity.COMMUNIST;
        } else if (getPlayer(targetUsername).isAnarchist()) {
            return Identity.ANARCHIST;
        } else if (getPlayer(targetUsername).isCapitalist()) {
            return Identity.CAPITALIST;
        } else {
            return Identity.LIBERAL;
        }
    }

    /**
     * Forces the sitting president to publicly declare a party (Fascist or
     * Communist) that they are NOT a member of. Everyone plays along as if
     * they might be Liberal, so nobody - Liberal included - can ever deny
     * being Liberal; see {@link #isValidDeniedParty} for exactly which of
     * the remaining two options each identity is allowed to pick. The claim
     * is public and persists on their portrait for the rest of the game (see
     * {@link #getConfessionUsername()}/{@link #getConfessionDeniedParty()}).
     *
     * @param presidentUsername the username of the sitting president (for
     *                          validation).
     * @param deniedParty the party being denied - must be a party
     *                    {@link #isValidDeniedParty} allows for this player.
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_CONFESSION}.
     * @throws IllegalArgumentException if the player is not alive, is not in
     *                                  the game, is not the sitting
     *                                  president, or if {@code deniedParty}
     *                                  isn't valid for them.
     * @modifies this
     * @effects records the public claim and advances the state to
     *          {@code POST_LEGISLATIVE}.
     */
    public void confession(String presidentUsername, Identity deniedParty) {
        if (state != GameState.PRESIDENTIAL_POWER_CONFESSION) {
            throw new IllegalStateException("Cannot confess when the power is not active.");
        } else if (!hasPlayer(presidentUsername)) {
            throw new IllegalArgumentException("Player " + presidentUsername + " does not exist.");
        } else if (!presidentUsername.equals(currentPresident)) {
            throw new IllegalArgumentException("Only the sitting president must confess.");
        } else if (!getPlayer(presidentUsername).isAlive()) {
            throw new IllegalArgumentException("Cannot confess when dead (" + presidentUsername + ").");
        } else if (!isValidDeniedParty(presidentUsername, deniedParty)) {
            throw new IllegalArgumentException("Cannot deny membership in that party.");
        }

        confessionUsername = presidentUsername;
        confessionDeniedParty = deniedParty;
        target = presidentUsername;
        concludePresidentialActions();
    }

    /**
     * @return true if {@code deniedParty} is a party {@code username} may
     *         legitimately claim NOT to be a member of during Confession.
     *         Everyone plays along as if they might be Liberal, so LIBERAL
     *         is never a valid choice for anyone. Among the remaining two:
     *         Fascist-aligned players (Fascist, Hitler, Monarchist, the
     *         Capitalist) can only deny being Communist; Communist-aligned
     *         players (Communist, the Anarchist) can only deny being
     *         Fascist; Liberals may freely pick either (both are
     *         truthfully deniable for them). A Monarchist who has since
     *         been Radicalized follows the Communist-aligned rule instead,
     *         same as any other current Communist.
     */
    public boolean isValidDeniedParty(String username, Identity deniedParty) {
        if (deniedParty != Identity.FASCIST && deniedParty != Identity.COMMUNIST) {
            return false;
        }
        Player player = getPlayer(username);
        if (player.isFascist() || player.isCapitalist()) {
            return deniedParty == Identity.COMMUNIST;
        } else if (player.isCommunist() || player.isAnarchist()) {
            return deniedParty == Identity.FASCIST;
        } else {
            return true; // Liberal: either is truthfully deniable.
        }
    }

    /**
     * @return the username of whoever most recently confessed (see
     *         {@link #confession(String, Identity)}), or null if nobody has yet.
     */
    public String getConfessionUsername() {
        return confessionUsername;
    }

    /**
     * @return the party that {@link #getConfessionUsername()} publicly
     *         denied being a member of, or null if nobody has confessed yet.
     */
    public Identity getConfessionDeniedParty() {
        return confessionDeniedParty;
    }

    /**
     * Adds two communist policies and one liberal policy to the draw deck and
     * shuffles it.
     * 
     * @throws IllegalStateException if called when state is not
     *                               {@code PRESIDENTIAL_POWER_FIVE_YEAR_PLAN}.
     * @modifies this
     * @effects adds the policies to the draw deck, shuffles the draw deck, and
     *          advances the state to {@code POST_LEGISLATIVE}.
     */
    public void fiveYearPlan() {
        if (state != GameState.PRESIDENTIAL_POWER_FIVE_YEAR_PLAN) {
            throw new IllegalStateException("Cannot perform five year plan when the power is not active.");
        }
        
        draw.add(new Policy(Policy.Type.COMMUNIST));
        draw.add(new Policy(Policy.Type.COMMUNIST));
        draw.add(new Policy(Policy.Type.LIBERAL));
        shuffleDiscardIntoDraw();
        concludePresidentialActions();
    }

    /**
     * Casts (or changes) a communist's vote for who to target during the
     * Radicalization or Communist Investigation power. Once every alive
     * communist's vote points at the same player, that player is chosen
     * automatically (see {@link #getUnanimousCommunistVoteTarget()}).
     *
     * @param voterUsername  the communist casting the vote.
     * @param targetUsername the player being voted for.
     * @throws IllegalStateException    if called when the state is not
     *                                  {@code PRESIDENTIAL_POWER_RADICALIZATION}
     *                                  or {@code PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION}.
     * @throws IllegalArgumentException if the voter is not an alive communist,
     *                                  or the target does not exist, is dead,
     *                                  or is a fellow communist.
     * @modifies this
     * @effects records the voter's current vote, replacing any previous one.
     */
    public void castCommunistVote(String voterUsername, String targetUsername) {
        validateCommunistVoteState();
        if (!hasPlayer(voterUsername) || !getPlayer(voterUsername).isAlive()
                || !getPlayer(voterUsername).isCommunist()) {
            throw new IllegalArgumentException("Only alive communist players can vote.");
        } else if (!hasPlayer(targetUsername) || !getPlayer(targetUsername).isAlive()) {
            throw new IllegalArgumentException("Cannot vote for a dead or nonexistent player.");
        } else if (getPlayer(targetUsername).isCommunist()) {
            throw new IllegalArgumentException("Cannot vote for a fellow communist.");
        }
        communistVotes.put(voterUsername, targetUsername);
    }

    /**
     * Retracts a communist's current vote, if they have one.
     *
     * @param voterUsername the communist retracting their vote.
     * @throws IllegalStateException    if called when the state is not
     *                                  {@code PRESIDENTIAL_POWER_RADICALIZATION}
     *                                  or {@code PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION}.
     * @throws IllegalArgumentException if the voter is not a communist.
     * @modifies this
     */
    public void retractCommunistVote(String voterUsername) {
        validateCommunistVoteState();
        if (!hasPlayer(voterUsername) || !getPlayer(voterUsername).isCommunist()) {
            throw new IllegalArgumentException("Only communist players can retract a vote.");
        }
        communistVotes.remove(voterUsername);
    }

    private void validateCommunistVoteState() {
        if (state != GameState.PRESIDENTIAL_POWER_RADICALIZATION
                && state != GameState.PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION) {
            throw new IllegalStateException(
                    "Communists can only vote during Radicalization or Communist Investigation.");
        }
    }

    /**
     * @return an unmodifiable view of every communist's current vote (voter
     *         username -> target username). Only meaningful to communist
     *         viewers - this is exposed to the frontend gated on the
     *         viewer being a communist themselves.
     */
    public Map<String, String> getCommunistVotes() {
        return Collections.unmodifiableMap(communistVotes);
    }

    /**
     * @return the target every alive communist has unanimously voted for,
     *         or null if the vote hasn't reached unanimity yet (including
     *         if nobody has voted).
     */
    public String getUnanimousCommunistVoteTarget() {
        int aliveCommunistCount = 0;
        for (Player p : playerList) {
            if (p.isAlive() && p.isCommunist()) {
                aliveCommunistCount++;
            }
        }
        if (communistVotes.isEmpty() || communistVotes.size() < aliveCommunistCount) {
            return null;
        }
        Set<String> distinctTargets = new HashSet<>(communistVotes.values());
        return distinctTargets.size() == 1 ? distinctTargets.iterator().next() : null;
    }

    /**
     * Clears all communist votes. Called once a unanimous vote has been
     * resolved into an actual Radicalization/Communist Investigation.
     * @modifies this
     */
    public void clearCommunistVotes() {
        communistVotes.clear();
    }

    /**
     * Radicalizes a player into a communist.
     *
     * @param targetUsername the username of the player to radicalize.
     * @param commUsername the username of the player that pressed button (for validation).
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_RADICALIZATION}.
     * @throws IllegalArgumentException if one of players is not alive, not in
     *                                  the game or not communist.
     * @modifies this
     * @effects If the target is Hitler or the Capitalist, nothing happens to
     *          their identity (they secretly swap their card back) but the
     *          power is still consumed. Otherwise, the specified player's
     *          identity is changed to COMMUNIST.
     *
     */
    public void radicalization(String targetUsername, String commUsername) {
        if (state != GameState.PRESIDENTIAL_POWER_RADICALIZATION) {
            throw new IllegalStateException("Cannot radicalize a player when the power is not active.");
        } else if (!hasPlayer(targetUsername)) {
            throw new IllegalArgumentException("Player " + targetUsername + " does not exist.");
        } else if (!hasPlayer(commUsername)) {
            throw new IllegalArgumentException("Player " + commUsername + " does not exist.");
        } else if (!getPlayer(targetUsername).isAlive()) {
            throw new IllegalArgumentException("Cannot radicalize a dead player (" + targetUsername + ").");
        } else if (!getPlayer(commUsername).isAlive()) {
            throw new IllegalArgumentException("Dead player (" + commUsername + ") cannot radicalize.");
        } else if (!getPlayer(commUsername).isCommunist()) {
            throw new IllegalArgumentException("Only communist players can radicalize during this power.");
        }

        target = targetUsername;
        // Hitler and the Capitalist secretly swap their card back, so their
        // identity never actually changes - but the Communists don't learn
        // that it failed, and the power is still spent.
        if (!getPlayer(targetUsername).isHitler() && !getPlayer(targetUsername).isCapitalist()) {
            getPlayer(targetUsername).setIdentity(Identity.COMMUNIST);
        }
        concludePresidentialActions();
    }

    /**
     * Removes a policy from either the fascist or communist tracker, as chosen
     * by the President after a Social Democratic policy is enacted.
     *
     * @param trackerType which tracker to remove a policy from; must be
     *                    {@code Policy.Type.FASCIST} or
     *                    {@code Policy.Type.COMMUNIST}.
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_SOCIAL_DEMOCRATIC}.
     * @throws IllegalArgumentException if {@code trackerType} is neither
     *                                  FASCIST nor COMMUNIST.
     * @modifies this
     * @effects removes a policy from the chosen tracker and suppresses that
     *          tracker's power the next time it re-fills the vacated slot.
     *          Advances the state to {@code POST_LEGISLATIVE}.
     */
    public void socialDemocraticRemovePolicy(Policy.Type trackerType) {
        if (state != GameState.PRESIDENTIAL_POWER_SOCIAL_DEMOCRATIC) {
            throw new IllegalStateException("Cannot use the social democratic power when it is not active.");
        }

        if (trackerType == Policy.Type.FASCIST) {
            board.removeFascistPolicy();
            skipNextFascistPower = true;
        } else if (trackerType == Policy.Type.COMMUNIST) {
            board.removeCommunistPolicy();
            skipNextCommunistPower = true;
        } else {
            throw new IllegalArgumentException("Must remove a policy from either the fascist or communist tracker.");
        }
        concludePresidentialActions();
    }

    /**
     * @return how many Article 48/Enabling Act cards have been enacted so
     *         far this game (shared count across both types).
     */
    public int getEmergencyPowersEnacted() {
        return emergencyPowersEnacted;
    }

    /**
     * @return the Article 48/Enabling Act policy type that filled each of
     *         the printed board's 6 slots, in left-to-right order - lets
     *         the board render which card type occupies each slot.
     */
    public List<Policy.Type> getEmergencyPowerTrackerHistory() {
        return Collections.unmodifiableList(emergencyPowerTrackerHistory);
    }

    /**
     * Debug-only: exposes the true, ordered contents of the draw deck.
     * @return an unmodifiable list of Policy.Type, where index 0 is the next card to be drawn.
     */
    public List<Policy.Type> getDrawDeckOrder() {
        return draw.getOrderedTypes();
    }

    /**
     * Debug-only: exposes the true, ordered contents of the discard pile.
     * @return an unmodifiable list of Policy.Type, where index 0 is the most recently discarded card.
     */
    public List<Policy.Type> getDiscardPileOrder() {
        return discard.getOrderedTypes();
    }

    /**
     * @param slot       the 1-indexed slot number on the printed Emergency
     *                   Powers board being filled (i.e. how many emergency
     *                   power cards, of either type, have been enacted so
     *                   far including this one).
     * @param isArticle48 whether the card filling this slot is Article 48
     *                    (true) or the Enabling Act (false). Only matters
     *                    for the 6th slot, which branches by card type.
     * @return the power printed on that slot - see
     *         {@link #EMERGENCY_POWER_SLOT_ORDER}. There is no player
     *         choice: the slot number alone determines the power.
     */
    private EmergencyPower getEmergencyPowerForSlot(int slot, boolean isArticle48) {
        if (slot <= EMERGENCY_POWER_SLOT_ORDER.length) {
            return EMERGENCY_POWER_SLOT_ORDER[slot - 1];
        }
        return isArticle48 ? EmergencyPower.PRESIDENTIAL_PARDON : EmergencyPower.VOTE_OF_NO_CONFIDENCE;
    }

    /**
     * Resolves the given emergency power, automatically determined by
     * {@link #getEmergencyPowerForSlot}: advances the state to resolve it,
     * or resolves it immediately if it needs no further input.
     * @modifies this
     */
    private void resolveEmergencyPower(EmergencyPower power) {
        switch (power) {
            case PROPAGANDA:
                state = GameState.PRESIDENTIAL_POWER_PROPAGANDA;
                break;
            case POLICY_PEEK:
                state = GameState.PRESIDENTIAL_POWER_PEEK;
                break;
            case IMPEACHMENT:
                state = GameState.PRESIDENTIAL_POWER_IMPEACHMENT;
                break;
            case MARKED_FOR_EXECUTION:
                state = GameState.PRESIDENTIAL_POWER_MARK_FOR_EXECUTION;
                break;
            case EXECUTION:
                state = GameState.PRESIDENTIAL_POWER_EXECUTION;
                break;
            case PRESIDENTIAL_PARDON:
                markedForExecutionUsername = null;
                concludePresidentialActions();
                break;
            case VOTE_OF_NO_CONFIDENCE:
                voteOfNoConfidence();
                break;
        }
    }

    /**
     * Secretly peeks at the top card of the draw deck during the Propaganda
     * emergency power.
     *
     * @throws IllegalStateException if called when state is not
     *                               {@code PRESIDENTIAL_POWER_PROPAGANDA}.
     * @return the Policy currently on top of the draw deck.
     */
    public Policy getPropagandaPeek() {
        if (state != GameState.PRESIDENTIAL_POWER_PROPAGANDA) {
            throw new IllegalStateException("Cannot peek the top policy when the power is not active.");
        }
        if (draw.getSize() < 1) {
            shuffleDiscardIntoDraw();
        }
        return draw.peek(0);
    }

    /**
     * Resolves the Propaganda emergency power.
     *
     * @param discardTopCard whether to discard the top card of the draw deck
     *                       ({@code true}), or leave it in place ({@code false}).
     * @throws IllegalStateException if called when state is not
     *                               {@code PRESIDENTIAL_POWER_PROPAGANDA}.
     * @modifies this
     * @effects if {@code discardTopCard}, moves the top card of the draw deck
     *          to the discard pile. Advances the state to
     *          {@code POST_LEGISLATIVE}.
     */
    public void propagandaDiscard(boolean discardTopCard) {
        if (state != GameState.PRESIDENTIAL_POWER_PROPAGANDA) {
            throw new IllegalStateException("Cannot resolve propaganda when the power is not active.");
        }
        if (discardTopCard) {
            discard.add(draw.remove());
        }
        concludePresidentialActions();
    }

    /**
     * Resolves the Impeachment emergency power: the officer who did not choose
     * this power must reveal their party membership to a player chosen by the
     * officer who did.
     *
     * @param viewerUsername the username of the player chosen to view the
     *                       revealed card.
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_IMPEACHMENT}.
     * @throws IllegalArgumentException if the viewer is not alive, is not in
     *                                  the game, or is the officer who chose
     *                                  this power.
     * @return the party membership of the impeached officer.
     */
    public Identity impeach(String viewerUsername) {
        if (state != GameState.PRESIDENTIAL_POWER_IMPEACHMENT) {
            throw new IllegalStateException("Cannot impeach when the power is not active.");
        } else if (!hasPlayer(viewerUsername)) {
            throw new IllegalArgumentException("Player " + viewerUsername + " does not exist.");
        } else if (!getPlayer(viewerUsername).isAlive()) {
            throw new IllegalArgumentException("Cannot show the card to a dead player (" + viewerUsername + ").");
        }

        // Article 48: the President chooses who the Chancellor reveals to.
        // Enabling Act: the Chancellor chooses who the President reveals to.
        String officerBeingImpeached = currentEmergencyPowerIsArticle48 ? currentChancellor : currentPresident;
        String chooser = currentEmergencyPowerIsArticle48 ? currentPresident : currentChancellor;
        if (viewerUsername.equals(chooser)) {
            throw new IllegalArgumentException("The chooser cannot choose themselves to view the card.");
        }

        target = officerBeingImpeached;
        concludePresidentialActions();

        Player officer = getPlayer(officerBeingImpeached);
        if (officer.isFascist()) {
            return Identity.FASCIST;
        } else if (officer.isCommunist()) {
            return Identity.COMMUNIST;
        } else if (officer.isAnarchist()) {
            return Identity.ANARCHIST;
        } else if (officer.isCapitalist()) {
            return Identity.CAPITALIST;
        } else {
            return Identity.LIBERAL;
        }
    }

    /**
     * Marks a player to be executed once 3 fascist policies have been
     * enacted in total, as part of the Marked For Execution emergency power.
     *
     * @param username the username of the player to mark.
     * @throws IllegalStateException    if called when state is not
     *                                  {@code PRESIDENTIAL_POWER_MARK_FOR_EXECUTION}.
     * @throws IllegalArgumentException if the player is not alive or is not in
     *                                  the game.
     * @modifies this
     * @effects marks the player for execution. Advances the state to
     *          {@code POST_LEGISLATIVE}.
     */
    public void markForExecution(String username) {
        if (state != GameState.PRESIDENTIAL_POWER_MARK_FOR_EXECUTION) {
            throw new IllegalStateException("Cannot mark a player for execution when the power is not active.");
        } else if (!hasPlayer(username)) {
            throw new IllegalArgumentException("Player " + username + " does not exist.");
        } else if (!getPlayer(username).isAlive()) {
            throw new IllegalArgumentException("Cannot mark a dead player (" + username + ") for execution.");
        }

        markedForExecutionUsername = username;
        target = username;
        concludePresidentialActions();
    }

    /**
     * Resolves the Vote Of No Confidence emergency power: enacts whatever
     * policy the President discarded this legislative round instead.
     *
     * @throws IllegalStateException if no policy was discarded by the
     *                                President this round.
     * @modifies this
     * @effects enacts the previously-discarded policy and handles any
     *          resulting consequences.
     */
    private void voteOfNoConfidence() {
        if (lastPresidentDiscardedPolicy == null) {
            throw new IllegalStateException("No policy was discarded by the President this round.");
        }

        Policy policyToEnact = lastPresidentDiscardedPolicy;
        lastPresidentDiscardedPolicy = null;
        if (!discard.removeIfPresent(policyToEnact)) {
            draw.removeIfPresent(policyToEnact);
        }
        board.enactPolicy(policyToEnact);
        onEnactPolicy(policyToEnact.getType());
    }

    // </editor-fold>

}
