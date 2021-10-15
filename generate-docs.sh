#!/bin/bash

rm -rf documentation
rm -rf ./sdk/build
./gradlew dokkaHtml
mv ./sdk/build/dokka/html documentation