#!/bin/sh

if [ -z "$1" ]; then
  echo "WARNING: this script will create 111,100 small files on your drive, and"
  echo "         it will take quite a while to do so."
  read -p "Press any key to continue or Ctrl+C to quit..."
fi

echo "Creating 100 files..."
mkdir -p 100files
../createFiles.sh 100 ./100files/

echo "Creating 1000 files..."
mkdir -p 1000files
../createFiles.sh 1000 ./1000files/

echo "Creating 10000 files..."
mkdir -p 10000files
../createFiles.sh 10000 ./10000files/

echo "Creating 100000 files..."
mkdir -p 100000files
../createFiles.sh 100000 ./100000files/
