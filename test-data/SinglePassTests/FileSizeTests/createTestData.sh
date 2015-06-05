#!/bin/sh

if [ -z "$1" ]; then
  echo "WARNING: this script will create around 4.2GB of random data on your drive, and"
  echo "         it will take quite a while to do so."
  read -p "Press any key to continue or Ctrl+C to quit..."
fi

echo "Creating small (KB) files..."
touch zeroByte.bin
dd if=/dev/urandom of=oneKilobyte.bin bs=1024 count=1 > /dev/null 2>&1
dd if=/dev/urandom of=tenKilobytes.bin bs=1024 count=10 > /dev/null 2>&1
dd if=/dev/urandom of=oneHundredKilobytes.bin bs=1024 count=100 > /dev/null 2>&1

echo "Creating medium (MB) files..."
dd if=/dev/urandom of=oneMegabyte.bin bs=$((1024 * 1024)) count=1 > /dev/null 2>&1
dd if=/dev/urandom of=tenMegabytes.bin bs=$((1024 * 1024)) count=10 > /dev/null 2>&1
dd if=/dev/urandom of=oneHundredMegabytes.bin bs=$((1024 * 1024)) count=100 > /dev/null 2>&1

echo "Creating large (GB) files..."
dd if=/dev/urandom of=oneGigabyte.bin bs=$((1024 * 1024 * 1024)) count=1 > /dev/null 2>&1
dd if=/dev/urandom of=threeGigabytes.bin bs=$((1024 * 1024 * 1024)) count=3 > /dev/null 2>&1
