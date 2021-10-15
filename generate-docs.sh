#!/bin/bash

./gradlew dokkaHtml
mv ./sdk/build/dokka/html generated_docs