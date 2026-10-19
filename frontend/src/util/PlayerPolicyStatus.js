import React, {Component} from "react";
import PropTypes from "prop-types";
import IconFascist from "../assets/player-icon-fascist.png";
import IconHitler from "../assets/player-icon-hitler.png";
import IconLiberal from "../assets/player-icon-liberal.png";
import IconCommunist from "../assets/player-icon-communist.png";
import IconAnarchist from "../assets/player-icon-anarchist.png";
import IconCapitalist from "../assets/player-icon-capitalist.png";
import IconMonarchist from "../assets/player-icon-monarchist.png";
import IconAntiFascist from "../assets/icon-anti-fascist.png";
import IconAntiCommunist from "../assets/icon-anti-communist.png";
import IconSocialDemocratic from "../assets/icon-social-democratic.png";
import IconArticle48 from "../assets/icon-social-article-48.png";
import IconEnablingAct from "../assets/icon-social-enabling-act.png";

import './PlayerPolicyStatus.css';

class PlayerPolicyStatus extends  Component {

    /**
     * Renders a single icon + count pair. Used for both roles (icons
     * matching Player.js's identity reveal art) and policy types (icons
     * matching the small tokens used on the game board).
     * @param src {Image} the icon image.
     * @param count {number} the count to display next to the icon.
     * @param colorClass {string} a "highlight-*" class controlling the count's text color.
     * @param title {string} the accessible/tooltip name for the icon.
     */
    renderIcon(src, count, colorClass, title) {
        return (
            <React.Fragment key={title}>
                <img id="pps-icon" src={src} alt={title}/>
                <p id={"pps-icon-number"} className={colorClass}>{count}</p>
            </React.Fragment>
        );
    }

    render() {
        let props = this.props;
        let roleCounts = props.roleCounts;

        return (
            <div id={"pps-container"}>
                <p id={"pps-text"}>
                    Players:
                </p>
                <div id={"pps-icon-container"}>
                    <img id="pps-icon" src={IconLiberal} alt={"Liberal"}/>
                    <p id={"pps-icon-number"} className={"highlight-blue"}>{roleCounts.LIBERAL || 0}</p>
                    <img id="pps-icon" src={IconFascist} alt={"Fascist"}/>
                    <p id={"pps-icon-number"} className={"highlight-fascist-policy"}>{roleCounts.FASCIST || 0}</p>
                    <img id="pps-icon" src={IconHitler} alt={"Hitler"}/>
                    <p id={"pps-icon-number"} className={"highlight-fascist-policy"}>{roleCounts.HITLER || 0}</p>
                    {props.communistsIncluded &&
                        this.renderIcon(IconCommunist, roleCounts.COMMUNIST || 0, "highlight-communist", "Communist")}
                    {props.anarchistIncluded &&
                        this.renderIcon(IconAnarchist, roleCounts.ANARCHIST || 0, "highlight-anarchist", "Anarchist")}
                    {props.capitalistIncluded &&
                        this.renderIcon(IconCapitalist, roleCounts.CAPITALIST || 0, "highlight-capitalist", "Capitalist")}
                    {props.monarchistIncluded &&
                        this.renderIcon(IconMonarchist, roleCounts.MONARCHIST || 0, "highlight-fascist-policy", "Monarchist")}
                </div>

                <p id={"pps-text"}>
                    Unenacted Policies:
                </p>
                <div id={"pps-icon-container"}>
                    {this.renderIcon(IconLiberal, props.remainingLiberalPolicies, "highlight-blue", "Liberal")}
                    {this.renderIcon(IconFascist, props.remainingFascistPolicies, "highlight-fascist-policy", "Fascist")}
                    {props.communistsIncluded &&
                        this.renderIcon(IconCommunist, props.remainingCommunistPolicies, "highlight-communist", "Communist")}
                    {props.anarchistIncluded &&
                        this.renderIcon(IconAnarchist, props.remainingAnarchistPolicies, "highlight-anarchist", "Anarchist")}
                    {props.antiFascistPolicyIncluded &&
                        this.renderIcon(IconAntiFascist, props.remainingAntiFascistPolicies, "highlight-communist", "Anti-Fascist")}
                    {props.antiCommunistPolicyIncluded &&
                        this.renderIcon(IconAntiCommunist, props.remainingAntiCommunistPolicies, "highlight-fascist-policy", "Anti-Communist")}
                    {props.socialDemocraticPolicyIncluded &&
                        this.renderIcon(IconSocialDemocratic, props.remainingSocialDemocraticPolicies, "highlight-blue", "Social Democratic")}
                    {props.emergencyPowersIncluded &&
                        this.renderIcon(IconArticle48, props.remainingArticle48Policies, "highlight-emergency-power", "Article 48")}
                    {props.emergencyPowersIncluded &&
                        this.renderIcon(IconEnablingAct, props.remainingEnablingActPolicies, "highlight-emergency-power", "Enabling Act")}
                </div>
            </div>
        )
    }
}

PlayerPolicyStatus.propTypes = {
    remainingFascistPolicies: PropTypes.number.isRequired,
    remainingLiberalPolicies: PropTypes.number.isRequired,
    remainingCommunistPolicies: PropTypes.number,
    remainingAnarchistPolicies: PropTypes.number,
    remainingAntiFascistPolicies: PropTypes.number,
    remainingAntiCommunistPolicies: PropTypes.number,
    remainingSocialDemocraticPolicies: PropTypes.number,
    remainingArticle48Policies: PropTypes.number,
    remainingEnablingActPolicies: PropTypes.number,
    roleCounts: PropTypes.object,
    communistsIncluded: PropTypes.bool,
    anarchistIncluded: PropTypes.bool,
    capitalistIncluded: PropTypes.bool,
    monarchistIncluded: PropTypes.bool,
    antiFascistPolicyIncluded: PropTypes.bool,
    antiCommunistPolicyIncluded: PropTypes.bool,
    socialDemocraticPolicyIncluded: PropTypes.bool,
    emergencyPowersIncluded: PropTypes.bool,
};

PlayerPolicyStatus.defaultProps = {
    remainingCommunistPolicies: 0,
    remainingAnarchistPolicies: 0,
    remainingAntiFascistPolicies: 0,
    remainingAntiCommunistPolicies: 0,
    remainingSocialDemocraticPolicies: 0,
    remainingArticle48Policies: 0,
    remainingEnablingActPolicies: 0,
    roleCounts: {},
    communistsIncluded: false,
    anarchistIncluded: false,
    capitalistIncluded: false,
    monarchistIncluded: false,
    antiFascistPolicyIncluded: false,
    antiCommunistPolicyIncluded: false,
    socialDemocraticPolicyIncluded: false,
    emergencyPowersIncluded: false,
};

export default PlayerPolicyStatus;
