#!/bin/bash

# generate goland zip package
bash ./gradlew goland

# generate terminal cli fat jar
# uncomment  // terminal("org.slf4j:slf4j-api:1.7.21") first
bash ./gradlew terminal

tree build/plugins