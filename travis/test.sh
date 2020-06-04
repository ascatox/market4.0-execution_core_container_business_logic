#!/bin/bash
export CONCURR=1
export REPS=20
newman run ./travis/tests/tests-${NET}-${NETE}.json --insecure --timeout-request 120000