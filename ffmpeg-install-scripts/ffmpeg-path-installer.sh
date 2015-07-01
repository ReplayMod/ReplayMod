#!/bin/bash
if [ ! -e "ffmpeg" ]
then
	echo "ffmpeg couldn't be found. Make sure you are executing this script from the same folder."
	exit
fi
mkdir -p /usr/local/bin
cp ffmpeg /usr/local/bin/
export PATH=$PATH:/usr/local/bin

echo "Successfully copied ffmpeg to /usr/local/bin and set the PATH variable."