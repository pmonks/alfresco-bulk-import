#!/bin/sh
echo "Creating pathological folder structure..."
mkdir -p sub1/1.1
mkdir -p sub1/1.2
mkdir -p sub1/1.3
mkdir -p sub1/1.4
mkdir -p sub1/1.5
mkdir -p sub1/1.6
mkdir -p sub1/1.7
mkdir -p sub1/1.8
mkdir -p sub1/1.9
mkdir -p sub1/1.10
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.1/sub2.1.1.1.1.1/sub2.1.1.1.1.1.1/sub2.1.1.1.1.1.1.1
mkdir -p sub3
mkdir -p deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep
touch deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deepFile.txt
echo "This file is located in a very deep folder structure." >>deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deepFile.txt

# Note: this will only work if lein-exec (https://github.com/kumarshantanu/lein-exec) is installed
./make-large-folder-structure.clj
