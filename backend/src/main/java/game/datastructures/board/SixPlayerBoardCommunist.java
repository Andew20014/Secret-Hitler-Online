package game.datastructures.board;

import game.datastructures.Policy;

/**
 * A Board meant for 5-6 players.
 */
public class SixPlayerBoardCommunist extends Board {


    @Override
    public PresidentialPower getActivatedPower() {
        if (getLastEnactedType() == Policy.Type.FASCIST || getLastEnactedType() == Policy.Type.ANTI_COMMUNIST) {
            switch (getNumFascistPolicies()) {
                case 3:
                    return PresidentialPower.PEEK;
                case 4:
                case 5:
                    return PresidentialPower.EXECUTION;
                default:
                    //nothing happens, return no power.
            }
        } else if (getLastEnactedType() == Policy.Type.COMMUNIST || getLastEnactedType() == Policy.Type.ANTI_FASCIST || getLastEnactedType() == Policy.Type.ANARCHIST) {
            switch (getNumCommunistPolicies()) {
                case 1:
                    return PresidentialPower.COMMUNISTS_INVESTIGATION;
                case 2:
                    return PresidentialPower.RADICALIZATION;
                case 3:
                    return PresidentialPower.FIVE_YEAR_PLAN;
                case 4:
                    return PresidentialPower.CONGRESS;
            }
        }
        return PresidentialPower.NONE;
    }

}
