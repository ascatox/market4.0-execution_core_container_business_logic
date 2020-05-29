#!/bin/bash
newman run ./travis/tests/tests-${NET}-${NET-E}.json --insecure
