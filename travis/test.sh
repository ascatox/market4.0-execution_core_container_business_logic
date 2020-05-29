#!/bin/bash
newman run ./travis/tests/tests-${NET}-${NETE}.json --insecure
