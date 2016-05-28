#!/usr/bin/env bash

java -classpath ../build/libs/*:../libs/*:../libs/*.jar \
  -Dconfig=../conf/server.properties \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
  -Dorg.slf4j.simpleLogger.showShortLogName=true \
  io.bekti.anubis.server.AnubisServerMain