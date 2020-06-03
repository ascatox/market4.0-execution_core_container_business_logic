#!/bin/bash
export CONCURR=1
export REPS=20
newman run ./travis/tests/stress-tests/tests-${NET}-${NETE}.json --insecure
#bash ./travis/tests/stress-tests/tests-${NET}-${NETE}.sh