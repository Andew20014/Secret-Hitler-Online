import React, {Component} from 'react';
import LiberalBoard from "../assets/board-liberal.png";
import LiberalBoardLong from "../assets/board-liberal-long.png";
import ElectionTracker from "../assets/board-tracker.png";
import PolicyLiberal from "../assets/board-policy-liberal.png";
import PolicySocialDemocratic from "../assets/board-policy-social-democratic.png";
import FascistBoard_5_6 from "../assets/board-fascist-5-6.png";
import FascistBoard_7_8 from "../assets/board-fascist-7-8.png";
import FascistBoard_9_10 from "../assets/board-fascist-9-10.png";
import PolicyFascist from "../assets/board-policy-fascist.png";
import PolicyAntiCommunist from "../assets/board-policy-anti-communist.png";
import CommunistBoard_6_7 from "../assets/board-communist-6-7.png";
import CommunistBoard_8_9 from "../assets/board-communist-8-9.png";
import CommunistBoard_10Plus from "../assets/board-communist-10-plus.png";
import PolicyCommunist from "../assets/board-policy-communist.png";
import PolicyAntiFascist from "../assets/board-policy-anti-fascist.png";
import PolicyAnarchist from "../assets/board-policy-anarchist.png";
import EmergencyPowersBoard from "../assets/board-emergency-powers.png";
import PolicyArticle48 from "../assets/board-policy-article48.png";
import PolicyEnablingAct from "../assets/board-policy-enablingact.png";

import "./Board.css";

// Maps a tracker slot's specific PolicyType to its board token icon, so
// e.g. an anti-communist policy shows its own icon in the fascist tracker
// slot it filled instead of a generic fascist icon.
const LIBERAL_TRACKER_ICONS = {
    LIBERAL: PolicyLiberal,
    SOCIAL_DEMOCRATIC: PolicySocialDemocratic,
};
const FASCIST_TRACKER_ICONS = {
    FASCIST: PolicyFascist,
    ANTI_COMMUNIST: PolicyAntiCommunist,
};
const COMMUNIST_TRACKER_ICONS = {
    COMMUNIST: PolicyCommunist,
    ANTI_FASCIST: PolicyAntiFascist,
    // Anarchist policies also advance the merged communist tracker (see
    // Board.java's getNumCommunistPolicies()).
    ANARCHIST: PolicyAnarchist,
};
const EMERGENCY_POWER_TRACKER_ICONS = {
    ARTICLE_48: PolicyArticle48,
    ENABLING_ACT: PolicyEnablingAct,
};

// The printed Emergency Powers board isn't evenly spaced like the other
// trackers: its first 5 slots are equal-width columns, but the 6th splits
// into two narrower sub-boxes (Article 48's "Pardon" and the Enabling
// Act's "Vote Of No Confidence"). Only one card type ever fills it, so
// rather than anchoring the token under whichever sub-box matches the
// enacted card's type, it's centered across both - the slot is a single
// pocket, not two. These positions (as % of the board image's width) were
// measured directly against board-emergency-powers.png.
const EMERGENCY_POWER_SLOT_POSITIONS = ["16.2%", "29.6%", "43.1%", "56.6%", "70.1%"];
const EMERGENCY_POWER_SLOT_6_POSITION = "83.95%";

class Board extends Component {


    /**
     * Returns the correct board image based on the number of players in the game.
     * @requires this.props.numPlayers must in range [5, 10], inclusive.
     * @return {image} The Fascist board corresponding to the number of players.
     *         >= 6: FascistBoard_5_6
     *         7-8: FascistBoard_7_8
     *         9-10: FascistBoard_9_10
     */
    getFascistBoard() {
        if(this.props.numPlayers <= 6) {
            return FascistBoard_5_6;
        } else if (this.props.numPlayers <= 8) {
            return FascistBoard_7_8;
        } else {
            return FascistBoard_9_10;
        }
    }

    /**
     * Returns the correct Communist board image based on the number of players in the game.
     * @requires this.props.numPlayers must in range [6, 10+], inclusive.
     * @return {image} The Communist board corresponding to the number of players.
     *         6-7: CommunistBoard_6_7 (5 policies to win)
     *         8-9: CommunistBoard_8_9 (6 policies to win)
     *         10+: CommunistBoard_10Plus (6 policies to win)
     */
    getCommunistBoard() {
        if(this.props.numPlayers <= 7) {
            return CommunistBoard_6_7;
        } else if (this.props.numPlayers <= 9) {
            return CommunistBoard_8_9;
        } else {
            return CommunistBoard_10Plus;
        }
    }

    /**
     * Places a series of repeating images.
     * @param count {number} the count of currently visible images.
     * @param totalCount {number} the total number of images to place.
     * @param src {Image} the image source to use for the tiles.
     * @param id {String} the HTML id to apply.
     * @param offset {String} the offset from the left of the first tile (given as a %)
     * @param spacing {String} the horizontal offset between each image (given as %)
     * @returns {<img>[]} an array of {@literal <img>} tags of length {@code totalCount}. Each image has the given
     *          {@code id} identity, {@code src} as an image source. There will be {@code spacing} between each image, and
     *          all images will be offset by {@code offset}.
     *          The first {@code count} images from the left will be given the class-name "show", the remaining will be given
     *          the class-name "hide".
     */
    placeRepeating(count, totalCount, src, id, offset, spacing) {
        let images = [];
        let index = 0;
        for(index; index < totalCount; index++) {
            let className = "hide";
            if (index < count) {
                className = "show";
            }
            images[index] = (
                <img src={src}
                     id={id}
                     style={{position: "absolute", left:"calc(" + offset + " + " + index.toString() + "*" + spacing +")"}}
                     alt={""}
                     className={className}
                     key={index}
                />
            );
        }
        return images;
    }

    /**
     * Like placeRepeating, but picks each tile's image individually based on
     * the specific PolicyType enacted into that slot, so e.g. an
     * anti-communist policy shows its own icon rather than a generic
     * fascist one.
     * @param history {string[]} the ordered PolicyTypes enacted into this tracker's slots so far.
     * @param totalCount {number} the total number of slots on this tracker.
     * @param iconMap {Object} maps a PolicyType string to its token image.
     * @param fallbackSrc {Image} used if a slot's type isn't in iconMap.
     * @param id {String} the HTML id to apply.
     * @param offset {String} the offset from the left of the first tile (given as a %)
     * @param spacing {String} the horizontal offset between each image (given as %)
     * @returns {<img>[]} an array of {@literal <img>} tags of length {@code totalCount}.
     */
    placeRepeatingWithHistory(history, totalCount, iconMap, fallbackSrc, id, offset, spacing) {
        let images = [];
        for (let index = 0; index < totalCount; index++) {
            const type = history[index];
            const src = (type && iconMap[type]) || fallbackSrc;
            const className = index < history.length ? "show" : "hide";
            images.push(
                <img src={src}
                     id={id}
                     style={{position: "absolute", left:"calc(" + offset + " + " + index.toString() + "*" + spacing +")"}}
                     alt={""}
                     className={className}
                     key={index}
                />
            );
        }
        return images;
    }

    /**
     * Places a token for each Article 48/Enabling Act card enacted so far
     * onto its exact slot on the printed Emergency Powers board. Unlike
     * placeRepeatingWithHistory, slots aren't evenly spaced (the 6th is a
     * single double-wide pocket shared by both card types, always centered
     * across it regardless of which type landed there - only one card ever
     * fills it), so each slot's position is looked up individually instead
     * of computed from a fixed offset/spacing.
     * @param history {string[]} the ordered PolicyTypes (ARTICLE_48/ENABLING_ACT)
     *        enacted into each of the board's 6 slots so far.
     * @returns {<img>[]} one token per enacted card (no placeholders for empty slots).
     */
    placeEmergencyPowerTokens(history) {
        return history.map((type, index) => {
            const isSlot6 = index >= EMERGENCY_POWER_SLOT_POSITIONS.length;
            const left = isSlot6 ? EMERGENCY_POWER_SLOT_6_POSITION : EMERGENCY_POWER_SLOT_POSITIONS[index];
            return (
                <img src={EMERGENCY_POWER_TRACKER_ICONS[type]}
                     id="policy-emergency-power"
                     style={{position: "absolute", left: left}}
                     alt={type + " filled slot " + (index + 1)}
                     className={isSlot6 ? "show slot-6" : "show"}
                     key={index}
                />
            );
        });
    }

    render() {
        const isLongLiberalBoard = this.props.longerLiberalTrackIncluded;
        const liberalBoardSrc = isLongLiberalBoard ? LiberalBoardLong : LiberalBoard;
        const totalLiberalPolicies = isLongLiberalBoard ? 6 : 5;
        const liberalOffset = isLongLiberalBoard ? "11.2%" : "18.2%";
        const liberalSpacing = isLongLiberalBoard ? "13.6%" : "13.54%";

        const totalCommunistPolicies = this.props.numPlayers <= 7 ? 5 : 6;
        const communistOffset = this.props.numPlayers <= 7 ? "14.8%" : "11.9%";
        const communistSpacing = this.props.numPlayers <= 7 ? "15.5%" : "13.45%";

        return (
            <div id="board-container" style={{display:"flex", flexDirection:"column"}}>
                <div id="board-group" style ={{margin:"4px 10px", position:"relative"}}>
                    <img id="board"
                         src={liberalBoardSrc}
                         alt={this.props.numLiberalPolicies + " liberal policies have been passed."}
                    />
                    <img id="election-tracker"
                         src={ElectionTracker}
                         style={{position:"absolute",
                                 top:"74%", left:"calc(34.2% + " + this.props.electionTracker+"*9.16%)",
                                 width:"3.2%"}}
                         alt={"Election tracker at position " + this.props.electionTracker + " out of 3."}
                    />
                    {this.placeRepeatingWithHistory(this.props.liberalTrackerHistory, totalLiberalPolicies, LIBERAL_TRACKER_ICONS, PolicyLiberal, "policy", liberalOffset, liberalSpacing)}
                </div>

                <div id="board-group" style={{margin:"4px 10px", position:"relative"}}>
                    <img
                      id="board"
                      src={this.getFascistBoard()}
                      alt={this.props.numFascistPolicies + " fascist policies have been passed."}
                    />
                    {this.placeRepeatingWithHistory(this.props.fascistTrackerHistory, 6, FASCIST_TRACKER_ICONS, PolicyFascist, "policy", "11%", "13.6%")}
                </div>

                {this.props.communistsIncluded &&
                    <div id="board-group" style={{margin:"4px 10px", position:"relative"}}>
                        <img
                          id="board"
                          src={this.getCommunistBoard()}
                          alt={this.props.numCommunistPolicies + " communist policies have been passed."}
                        />
                        {this.placeRepeatingWithHistory(this.props.communistTrackerHistory, totalCommunistPolicies, COMMUNIST_TRACKER_ICONS, PolicyCommunist, "policy-communist", communistOffset, communistSpacing)}
                    </div>
                }

                {this.props.emergencyPowersIncluded > 0 &&
                    <div id="board-group" style={{margin:"4px 10px", position:"relative"}}>
                        <img
                          id="board"
                          src={EmergencyPowersBoard}
                          alt={this.props.emergencyPowerTrackerHistory.length + " of 6 emergency power cards enacted."}
                        />
                        {this.placeEmergencyPowerTokens(this.props.emergencyPowerTrackerHistory)}
                    </div>
                }
            </div>
        );
    }

}

Board.defaultProps = {
    numFascistPolicies: 5,
    numLiberalPolicies: 6,
    numCommunistPolicies: 0,
    liberalTrackerHistory: [],
    fascistTrackerHistory: [],
    communistTrackerHistory: [],
    communistsIncluded: false,
    longerLiberalTrackIncluded: false,
    emergencyPowersIncluded: 0,
    emergencyPowerTrackerHistory: [],
    electionTracker: 0,
    numPlayers: 5
};

export default Board;