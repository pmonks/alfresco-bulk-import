#!/bin/sh
echo "WARNING: this script will create over 150,000 files on your hard drive"
echo "         totaling around 4.5GB, and will take quite a while to do so."
echo "         It also requires a filesystem that is Unicode capable."
read -p "Press any key to continue or Ctrl+C to quit..."

cd FileFolderVolumeTests/
./createTestData.sh SKIPWARNINGS

cd ../FileNameTests/
./createTestData.sh SKIPWARNINGS

cd ../FileSizeTests/
./createTestData.sh SKIPWARNINGS

cd ../FileVolumeTests/
./createTestData.sh SKIPWARNINGS

cd ../FolderStructureTests/
./createTestData.sh SKIPWARNINGS

cd ../PermissionTests/
./createTestData.sh SKIPWARNINGS

echo "Done."
