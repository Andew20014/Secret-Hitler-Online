import React, { ReactElement, useEffect, useRef, useState } from "react";
import ButtonPrompt from "./ButtonPrompt";
import "./XLPrompts.css";
import { SERVER_TIMEOUT } from "../constants";
import { PolicyType, Role, SendWSCommand, WSCommandType } from "../types";

type ConfessionPromptProps = {
  role: Role;
  sendWSCommand: SendWSCommand;
};

// Everyone plays along as if they might be Liberal, so nobody can ever deny
// being Liberal. Among the remaining two options, Fascist-aligned
// identities can only deny being Communist, and Communist-aligned
// identities can only deny being Fascist - only Liberals get a real choice.
// A Radicalized Monarchist is now actually Communist, so their role here
// will already read COMMUNIST by the time this shows up.
// Mirrors SecretHitlerGame.isValidDeniedParty() - keep in sync if that changes.
const FASCIST_ALIGNED_ROLES = [
  Role.FASCIST,
  Role.HITLER,
  Role.MONARCHIST,
  Role.CAPITALIST,
];
const COMMUNIST_ALIGNED_ROLES = [Role.COMMUNIST, Role.ANARCHIST];

function canDeny(
  role: Role,
  party: PolicyType.FASCIST | PolicyType.COMMUNIST
): boolean {
  if (FASCIST_ALIGNED_ROLES.includes(role)) {
    return party === PolicyType.COMMUNIST;
  } else if (COMMUNIST_ALIGNED_ROLES.includes(role)) {
    return party === PolicyType.FASCIST;
  }
  return true; // Liberal: either is truthfully deniable.
}

/**
 * Lets the sitting President publicly declare a party (Fascist or
 * Communist) they are NOT a member of, as the Communist "Confession"
 * power. Only the option(s) that are actually true for their own identity
 * are shown - nobody can deny being Liberal, since everyone plays along as
 * if they might be one.
 */
export default function ConfessionPrompt(
  props: ConfessionPromptProps
): ReactElement {
  const timeoutID = useRef<NodeJS.Timeout | undefined>(undefined);
  const [isWaitingForServer, setIsWaitingForServer] = useState(false);

  useEffect(() => {
    return () => clearTimeout(timeoutID.current);
  }, []);

  const respond = (deniedParty: PolicyType.FASCIST | PolicyType.COMMUNIST) => {
    setIsWaitingForServer(true);
    timeoutID.current = setTimeout(() => {
      setIsWaitingForServer(false);
    }, SERVER_TIMEOUT);
    props.sendWSCommand({
      command: WSCommandType.REGISTER_CONFESSION,
      deniedParty,
    });
  };

  return (
    <ButtonPrompt
      label={"CONFESSION"}
      headerText={
        "You must publicly declare a party you are NOT a member of. Everyone plays along as if they might be Liberal, so that option isn't available to anyone."
      }
      renderButton={() => (
        <div id={"confession-button-container"}>
          {canDeny(props.role, PolicyType.FASCIST) && (
            <button
              disabled={isWaitingForServer}
              onClick={() => respond(PolicyType.FASCIST)}
            >
              I AM NOT A FASCIST
            </button>
          )}
          {canDeny(props.role, PolicyType.COMMUNIST) && (
            <button
              disabled={isWaitingForServer}
              onClick={() => respond(PolicyType.COMMUNIST)}
            >
              I AM NOT A COMMUNIST
            </button>
          )}
        </div>
      )}
    />
  );
}
