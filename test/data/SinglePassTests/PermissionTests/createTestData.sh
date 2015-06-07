#!/bin/sh

echo "Creating permissioned and non-permissioned files..."

echo "The quick brown fox jumps over the lazy dogs." > readable.txt
mkdir -p readableDirectory
echo "The quick brown fox jumps over the lazy dogs." > unreadable.txt
mkdir -p unreadableDirectory
chmod ugo-rwx unreadable*
