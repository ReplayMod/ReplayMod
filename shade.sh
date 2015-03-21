#!/bin/sh
JARPATH="$1"
LIBSPATH="$2"
FILENAME="$3"

TEMPDIR="./tempdir"
mkdir $TEMPDIR

unzip -o $LIBSPATH"/*.jar" -d $TEMPDIR
unzip -o $JARPATH -d $TEMPDIR

JARDIR=$(dirname "$JARPATH")

#Thanks to johni0702 for the following line of code <3
cd $TEMPDIR && ls | zip -r -@ "../$JARDIR/$FILENAME" && cd ..

rm -r $TEMPDIR