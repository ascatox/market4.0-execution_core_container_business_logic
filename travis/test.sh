#!/bin/bash
newman run ./travis/tests/tests-${NETWORK}-${NETWORK_E}.json --insecure
