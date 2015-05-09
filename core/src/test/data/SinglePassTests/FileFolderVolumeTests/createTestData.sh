#!/bin/sh

if [ -z "$1" ]; then
  echo "WARNING: this script will create approximately 20,000 small files on your"
  echo "         drive, and it will take quite a while to do so."
  read -p "Press any key to continue or Ctrl+C to quit..."
fi

echo "Creating templates..."
mkdir -p 100files
../createFiles.sh 100 ./100files/
mkdir -p 1000files
../createFiles.sh 1000 ./1000files/
mkdir -p 10000files
../createFiles.sh 10000 ./10000files/

echo "Creating a deep directory structure..."
mkdir -p sub1/sub1.1/
mkdir -p sub1/sub1.2/
mkdir -p sub1/sub1.3/
mkdir -p sub1/sub1.4/
mkdir -p sub1/sub1.5/
mkdir -p sub1/sub1.6/
mkdir -p sub1/sub1.7/
mkdir -p sub1/sub1.8/
mkdir -p sub1/sub1.9/
mkdir -p sub1/sub1.10/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.1/sub2.1.1.1.1.1/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.2/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.3/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.1/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.2/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.3/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.4/
mkdir -p sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.5/

echo "Copying templates across the deep directory structure..."
cp 1000files/* sub1/
cp 100files/*  sub1/sub1.1/
cp 100files/*  sub1/sub1.2/
cp 100files/*  sub1/sub1.3/
cp 100files/*  sub1/sub1.4/
cp 100files/*  sub1/sub1.5/
cp 100files/*  sub1/sub1.6/
cp 100files/*  sub1/sub1.7/
cp 100files/*  sub1/sub1.8/
cp 100files/*  sub1/sub1.9/
cp 100files/*  sub1/sub1.10/
cp 1000files/* sub2/
cp 1000files/* sub2/sub2.1/
cp 1000files/* sub2/sub2.1/sub2.1.1/
cp 1000files/* sub2/sub2.1/sub2.1.1/sub2.1.1.1/
cp 1000files/* sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.1/
cp 1000files/* sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.1/sub2.1.1.1.1.1/
mv 1000files/  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.1/sub2.1.1.1.1.1/sub2.1.1.1.1.1.1/
mv 10000files/ sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.1/sub2.1.1.1.1.1/sub2.1.1.1.1.1.1/sub2.1.1.1.1.1.1.1/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.2/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.3/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.1/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.2/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.3/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.4/sub2.1.1.1.4.4/
cp 100files/*  sub2/sub2.1/sub2.1.1/sub2.1.1.1/sub2.1.1.1.5/
mv 100files/   sub3/
