# Collects build artifacts that aren't present in the online cache
# into a subfolder that can be merged with the online cache
# This script isn't expected to function on OSX.

set -e
set -x

# Step one: copy all artifacts into subfolder under new name
rm -rf artifactCache
mkdir artifactCache
cd artifactCache
for f in $(find ../deps/repo -type f); do
    cp $f $(sha256sum $f | cut -d' ' -f1)
done

# Step two: remove all files that are already in the cache
for f in *; do
    if wget -q --spider "https://www.johni0702.de/replaymod/artifact/$f"; then
        rm $f
    fi
done
