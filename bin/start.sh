#!/usr/bin/env bash

cd "$(dirname "$0")"

java -classpath ../build/libs/*:../libs/*:../conf io.bekti.anubis.server.AnubisServerMain