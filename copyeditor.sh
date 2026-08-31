#!/usr/bin/env bash
echo "starting Claude to fix: [$1] "
PROMPT="Please fix the grammar and spelling errors in the file $1. Ensure that my AsciiDoc syntax is correct. I want this to read as though the *New York Times* grammar mavens scrutinized every character. Once you are finished, please provide a list of locations where the content flows poorly, the code doesn't line up with the content, the transitions are unclear, or critical information may be missing for the user. Identify and fix hanging prepositions. Tell me if I repeat myself somewhere. Finally, please identify any areas where a diagram might enhance clarity."



claude --permission-mode auto  "$PROMPT"

