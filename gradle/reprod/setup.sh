#!/usr/bin/env bash
set -e
set -x

PROXY_PORT="25678"
export PROXY_SETTINGS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=$PROXY_PORT -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=$PROXY_PORT"

pushd gradle/reprod

SHA256SUM="sha256sum"
command -v "$SHA256SUM" > /dev/null 2>&1 || SHA256SUM="shasum -a 256" # OSX
sha256val () {
    $SHA256SUM $1 | cut -d' ' -f1
}

# Setup http(s) proxy
url="https://github.com/johni0702/proxy-witness"
commit="07e79a999991473489d443b8934ccca547ac7985"
if [ ! -d "deps/proxy-witness" ] || [ "$(git -C "deps/proxy-witness" rev-parse $commit^{commit})" != "$commit" ]; then
    rm -rf "deps/proxy-witness"
    mkdir -p "deps/proxy-witness"
    pushd "deps/proxy-witness"
        git clone "$url" .
        git fetch origin $commit
        git checkout "$commit"
    popd
fi

pushd "deps/proxy-witness"
    git reset --hard "$commit"
    ./gradlew $PROXY_SETTINGS build -x test -x javadoc
popd

java -Dproxywitness.httpUris=http://export.mcpbot.bspk.rs/versions.json \
     -Dproxywitness.useCache=false \
     -jar deps/proxy-witness/build/libs/proxy-witness.jar \
     "$PROXY_PORT" checksums.txt > proxy.log 2>&1 &
proxy_pid=$!
trap "kill $proxy_pid" EXIT

../../gradlew $PROXY_SETTINGS -I init.gradle build

popd # Back to root

./gradlew -Preprod $PROXY_SETTINGS -I gradle/reprod/init.gradle "$@"

if [ "$(git blame -p version.txt | head -n1 | cut -d' ' -f1)" == "$(git rev-parse HEAD)" ]; then
    echo "Trying to fetch signature for resulting jar files.."
    pushd versions
        for ver in */; do
            [ "$ver" == "core/" ] && continue
            [ -d "$ver/build/libs" ] || continue
            pushd "$ver"
                # Note: This requires there to be one and only one jar file (ignoring source artifacts)
                jar="build/libs/$(ls build/libs | grep -v sources)"
                jar_hash=$(sha256val "$jar")
                mkdir build/sign_tmp
                pushd build/sign_tmp
                    # Signatures are generated using:
                    # jarsigner -sigfile johni -signedjar signed.jar unsigned.jar replaymod
                    # jar xf signed.jar META-INF/MANIFEST.MF META-INF/JOHNI.SF META-INF/JOHNI.RSA
                    # tar cJf signature.tar.xz META-INF
                    wget -O - "https://www.johni0702.de/replaymod/signature/$jar_hash" | tar xJ
                    jar uMf "../../$jar" META-INF/
                popd
            popd
        done
    popd
fi
