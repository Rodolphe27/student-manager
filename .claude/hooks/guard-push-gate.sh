#!/usr/bin/env bash
# PreToolUse guard on Bash: git push is a hard confirmation gate.
# Matches push-shaped git commands anywhere in the command string, immune to
# argument order, and always downgrades to an explicit ask - never a silent
# pass, never a hard block. The permission prompt IS the gate.

set -u

input="$(cat 2>/dev/null || true)"
[ -n "$input" ] || exit 0

# Key-scoped, non-greedy capture of the "command" value: stops at its own
# closing quote (treating a backslash-escaped quote as interior text) so a
# later JSON field (e.g. "description") can't bleed into the captured string.
cmd="$(printf '%s' "$input" | sed -nE 's/.*"command"[[:space:]]*:[[:space:]]*"((\\.|[^"\\])*)".*/\1/p' | head -n 1)"

ask() {
  cat <<JSON
{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask","permissionDecisionReason":"$1"}}
JSON
  exit 0
}

# Input present but the command value could not be isolated: fail toward the
# ask, never a silent pass.
[ -n "$cmd" ] || ask "Push gate: the command could not be isolated from the tool input. Asking to be safe."

# git ... push within one pipeline segment: "git" (or git.exe) as a word,
# then "push" as a word, with no segment separator (; & |) between them.
if printf '%s' "$cmd" | grep -Eq '(^|[[:space:];&|(])git(\.exe)?[[:space:]]([^;&|]*[[:space:]])?push([[:space:]]|$)'; then
  ask "Push gate: this command looks push-shaped. Confirm before pushing."
fi

exit 0
