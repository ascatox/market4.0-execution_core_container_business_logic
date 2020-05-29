#!/bin/bash
newman run ./travis/tests/tests-${NETWORK}.json --insecure
