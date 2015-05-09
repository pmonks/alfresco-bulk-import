#!/bin/bash

# Validation
if [ -z $1 ]; then
  echo "Please specify the number of files to create as the first command line parameter." >&2
  exit 1
fi

if [ -z $2 ]; then
  echo "Please specify the target directory in which to write the files as the second command line parameter." >&2
  exit 1
fi

if [ $1 -ge 0 ]; then
  echo "" > /dev/null # do nothing - fall through
else
  echo "$1 is not a number - please provide a number." >&2
  exit 1
fi

if [ -d "$2" -a -w "$2" ]; then
  echo "" > /dev/null # do nothing - fall through
else
  echo "$2 is not a writable directory." >&2
  exit 1
fi

# Do it
filenamePrefix="file";
content="The quick brown fox jumped over the lazy dogs.";

# Counter
i=1;

# Main loop
while [ $i -le $1 ]; do
  echo "${content}" > $2/${filenamePrefix}_${i}.txt
  i=$[$i+1]
done
