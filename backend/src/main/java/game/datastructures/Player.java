package game.datastructures;

import java.io.Serializable;

/**
 * Holds the data for an individual player.
 * A player has a username and identity associated with them.
 */
public class Player implements Serializable {

    final private String username;
    private Identity id;
    // The identity this player was dealt at game setup, fixed once via
    // lockInOriginalIdentity() and kept forever after, even if setIdentity()
    // is later called again (e.g. Radicalization converting a Fascist to a
    // Communist). Teammates who already knew this player under their
    // original identity (see GameToJSONConverter) keep seeing that original
    // identity - that knowledge, once gained, is never silently revoked or
    // overwritten just because the player's current identity changed later.
    private Identity originalIdentity;
    private boolean isAlive;
    private boolean investigated;
    private boolean confirmedNotHitler;

    private boolean isCPU = false;

    /**
     * Constructs a new Player with the given username.
     * @param username The username of the player.
     * @modifies this
     * @effects this is a new Player that is alive, has an unassigned Identity,
     *          and has not been investigated.
     */
    public Player(String username) {
        this.username = username;
        id = Identity.UNASSIGNED;
        originalIdentity = Identity.UNASSIGNED;
        isAlive = true;
        investigated = false;
        confirmedNotHitler = false;
    }

    public String getUsername() {
        return this.username;
    }

    /**
     * Sets the player's identity.
     * @param id the Identity of the player.
     * @throws IllegalArgumentException if {@code id} is null
     * @modifies this
     * @effects sets the player's identity to {@code id}.
     */
    public void setIdentity(Identity id) {
        this.id = id;
    }

    public Identity getIdentity() {
      return id;
    }

    /**
     * Fixes {@link #getOriginalIdentity()} to this player's current identity,
     * permanently. Must be called once role assignment is fully finished at
     * game setup (role assignment itself calls {@code setIdentity()}
     * multiple times per player along the way, e.g. defaulting everyone to
     * Liberal first), and never again afterward.
     * @modifies this
     */
    public void lockInOriginalIdentity() {
        this.originalIdentity = this.id;
    }

    /**
     * @return the identity this player was originally dealt at game setup,
     *         regardless of any later change (e.g. Radicalization). Only
     *         meaningful after {@link #lockInOriginalIdentity()} has been
     *         called.
     */
    public Identity getOriginalIdentity() {
        return originalIdentity;
    }

    public void markAsCpu() {
      isCPU = true;
    }

    public boolean isCpu() {
      return isCPU;
    }

    public boolean isHitler() {
        return this.id.equals(Identity.HITLER);
    }

    public void kill() {
        isAlive = false;
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public void investigate() {
        investigated = true;
    }

    public boolean hasBeenInvestigated() {
        return this.investigated;
    }

    /**
     * Marks this player as confirmed to not be Hitler - e.g. because they
     * survived a Chancellorship that would have won the game for the
     * Fascists had they been Hitler.
     * @modifies this
     */
    public void confirmNotHitler() {
        confirmedNotHitler = true;
    }

    public boolean isConfirmedNotHitler() {
        return this.confirmedNotHitler;
    }

    /**
     * @return true if the player is fascist or hitler //or monarchist.
     */
    public boolean isFascist() {
        return this.id.equals(Identity.HITLER) || this.id.equals(Identity.FASCIST) || this.id.equals(Identity.MONARCHIST);
    }

    /**
     * @return true if the player was dealt fascist, hitler, or monarchist at
     *         game setup, regardless of their current identity (e.g. even
     *         after being converted to Communist by Radicalization).
     */
    public boolean wasFascist() {
        return this.originalIdentity.equals(Identity.HITLER) || this.originalIdentity.equals(Identity.FASCIST)
                || this.originalIdentity.equals(Identity.MONARCHIST);
    }

    /**
     * @return true if the player is liberal.
     */
    public boolean isLiberal() {
        return this.id.equals(Identity.LIBERAL);
    }

    /**
     * @return true if the player is communnist.
     */
    public boolean isCommunist() {
        return this.id.equals(Identity.COMMUNIST);
    }

    /**
     * @return true if the player was dealt Communist at game setup (an
     *         "original" Communist), as opposed to converted later by
     *         Radicalization. Original Communists recognize each other (and
     *         any later Radicalization targets) immediately; a Radicalization
     *         target doesn't recognize the original team until Congress.
     */
    public boolean wasCommunist() {
        return this.originalIdentity.equals(Identity.COMMUNIST);
    }

    /**
     * @return true if the player is anarchist.
     */
    public boolean isAnarchist() {
        return this.id.equals(Identity.ANARCHIST);
    }

    /**
     * @return true if the player is capitalist.
     */
    public boolean isCapitalist() {
        return this.id.equals(Identity.CAPITALIST);
    }
}
