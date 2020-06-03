#!/bin/bash
export CONCURR=1
export REPS=20
newman run ./travis/tests/tests-${NET}-${NETE}.json --insecure
bash ./travis/tests/tests-${NET}-${NETE}.sh