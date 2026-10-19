import React, { Component } from "react";
import PropTypes from "prop-types";
import "./PolicyDisplay.css";
import { LIBERAL, FASCIST } from "../constants";
import { PolicyType } from "../types";
import LiberalPolicy from "../assets/policy-liberal.png";
import FascistPolicy from "../assets/policy-fascist.png";
import AnarchistPolicy from "../assets/policy-anarchist.png";
import CommunistPolicy from "../assets/policy-communist.png";
import AntiFascistPolicy from "../assets/policy-anti-fascist.png";
import AntiCommunistPolicy from "../assets/policy-anti-communist.png";
import SocialDemocraticPolicy from "../assets/policy-social-democratic.png";
import Article48Policy from "../assets/policy-article48.png";
import EnablingActPolicy from "../assets/policy-enablingact.png";

// Maps policy types with dedicated artwork to their image. Any type not
// listed here falls back to the fascist card image with a text label so
// it isn't mistaken for an actual fascist policy.
const POLICY_IMAGES = {
	[LIBERAL]: LiberalPolicy,
	[FASCIST]: FascistPolicy,
	[PolicyType.ANARCHIST]: AnarchistPolicy,
	[PolicyType.COMMUNIST]: CommunistPolicy,
	[PolicyType.ANTI_FASCIST]: AntiFascistPolicy,
	[PolicyType.ANTI_COMMUNIST]: AntiCommunistPolicy,
	[PolicyType.SOCIAL_DEMOCRATIC]: SocialDemocraticPolicy,
	[PolicyType.ARTICLE_48]: Article48Policy,
	[PolicyType.ENABLING_ACT]: EnablingActPolicy,
};

class PolicyDisplay extends Component {
	render() {
		return (
			<div id={"legislative-policy-container"}>
				{this.props.policies.map((value, index) => {
					const isLiberal = value === LIBERAL;
					const isFascist = value === FASCIST;
					const hasDedicatedArt = POLICY_IMAGES.hasOwnProperty(value);
					let policyName = isLiberal
						? "liberal"
						: isFascist
						? "fascist"
						: value.toLowerCase();
					return (
						<div className={"legislative-policy-wrapper"} key={index}>
							<img
								id={"legislative-policy"}
								className={
									this.props.allowSelection
										? "selectable " +
										  (index === this.props.selection ? " selected" : "")
										: ""
								}
								onClick={() => this.props.onClick(index)}
								disabled={!this.props.allowSelection}
								src={hasDedicatedArt ? POLICY_IMAGES[value] : FascistPolicy}
								alt={
									"A " +
									policyName +
									" policy." +
									(this.props.allowSelection ? " Click to select." : "")
								}
							/>
							{!hasDedicatedArt && (
								<span className={"legislative-policy-label"}>{value}</span>
							)}
						</div>
					);
				})}
			</div>
		);
	}
}

PolicyDisplay.propTypes = {
	policies: PropTypes.array.isRequired,
	onClick: PropTypes.func, // If undefined, the policies cannot be selected.
	selection: PropTypes.number,
	allowSelection: PropTypes.bool,
};

export default PolicyDisplay;
