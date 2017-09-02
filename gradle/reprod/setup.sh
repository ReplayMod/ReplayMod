#!/usr/bin/env bash
set -e
set -x

PROXY_PORT="25678"
PROXY_SETTINGS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=$PROXY_PORT -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=$PROXY_PORT"

pushd gradle/reprod

SHA256SUM="sha256sum"
command -v "$SHA256SUM" > /dev/null 2>&1 || SHA256SUM="shasum -a 256" # OSX
sha256val () {
    $SHA256SUM $1 | cut -d' ' -f1
}

setup_dep () {
    dep=$1
    url=$2
    commit=$3
    jar=$4
    jarhash=$5

    [ -f "deps/$dep.jar" ] && [ "$(sha256val "deps/$dep.jar")" == "$jarhash" ] && return
    rm -rf "deps/$dep.jar"

    if [ ! -d "deps/$dep" ] || [ "$(git -C "deps/$dep" rev-parse $commit^{commit})" != "$commit" ]; then
        rm -rf "deps/$dep"
        mkdir -p "deps/$dep"
        pushd "deps/$dep"
            git clone "$url" .
            git fetch origin $commit
            git checkout "$commit"
        popd
    fi

    pushd "deps/$dep"
        git reset --hard "$commit"
	git submodule update --init --recursive
        patches=$(find ../../patches/$dep/ -name *.patch | sort)
        [ "$patches" != "" ] && echo "$patches" | xargs git am
        git reset --soft "$commit" # Because forgegradle includes the commit hash in the jar

        rm -rf gradle/wrapper gradlew
        [ ! -d gradle ] && mkdir gradle
        cp -r ../../../wrapper gradle/
        cp ../../../wrapper/gradle-wrapper.properties gradle/wrapper/
        cp ../../../../gradlew .

        chmod +x gradlew
        ./gradlew $PROXY_SETTINGS -I ../../init.gradle build -x test -x javadoc

        actual_hash=$(sha256val "$jar")
        if [ "$actual_hash" != "$jarhash" ]; then
            echo "Failed to verify checksum of build artifact of dependency: $dep"
            echo "Expected: $jarhash"
            echo "But was:  $actual_hash"
            exit 1
        fi

        # Subshell to allow for expansion of *
        cp "$(echo $jar)" "../$dep.jar"
    popd
}

# Setup http(s) proxy
setup_dep "proxy-witness" "https://github.com/johni0702/proxy-witness" "17ebb2e22f812faed9a28bae6bf1d8b28f798d56" "build/libs/proxy-witness.jar" "f6846fda75a35a55a38db7c3b8215ef43377c33af0ac8118318716223da10ecc"
java -Dproxywitness.httpUris=http://export.mcpbot.bspk.rs/versions.json -jar deps/proxy-witness.jar "$PROXY_PORT" checksums.txt > proxy.log 2>&1 &
proxy_pid=$!
trap "kill $proxy_pid" EXIT

# Required for mixin
setup_dep "fernflower" "https://github.com/fesh0r/fernflower.git" "85f61bee8194ab69afa746b965973a18eda67608" "build/libs/fernflower.jar" "577c2c4e04f0026675cacb25ec525f2ccba5de2eecac525b28bed18f9ccfd790"

# Required for forgegradle
setup_dep "forgeflower" "https://github.com/MinecraftForge/FernFlowerLegacy.git" "114aebe82cf40075c5c4f916c409b8aebf8096d5" "build/libs/fernflower-2.0-SNAPSHOT.jar" "2f1b3fd389628e00cc39630627885e9e6bccde7119d3ad3c88394eceaf3fc878"
setup_dep "mcinjector" "https://github.com/ModCoderPack/MCInjector.git" "7258466461baf7dc4f313b06b0d589407e4e1fba" "build/libs/mcinjector-3.4-SNAPSHOT.jar" "98cd0c13030aa8103585621fdab5db6b74ea0db7b8b30bc74061e7b5d314aa01"
setup_dep "srg2source" "https://github.com/MinecraftForge/Srg2Source.git" "5f11e2933f722000dbf742ffdd0d9c5ee8044bbb" "build/libs/Srg2Source-3.3-SNAPSHOT.jar" "4336d8b61899ee2c24b69b5464117030a2444d155b647f529af51d7fa290dd74"

# Required for RM
setup_dep "mixingradle" "https://github.com/SpongePowered/MixinGradle.git" "52217aa8ca221dcd0b9fb657b037e663db808f38" "build/libs/mixingradle-0.4-SNAPSHOT.jar" "8b3508867128a5d564631635dff898a36f9aca8db54b7bb3af6f4924e3f4bead"
setup_dep "forgegradle" "https://github.com/MinecraftForge/ForgeGradle.git" "17806f45d20d0b55bff70a616eaeb939bd6a543c" "build/libs/ForgeGradle-2.1-SNAPSHOT.jar" "071dafb1ae078a9c50944ffa3519cd51c9cf876b09d200a9e9be314d92faf983"
setup_dep "mixin" "https://github.com/SpongePowered/Mixin.git" "b558323da3bd6ce94aeb442bfd7357f6c40d2fd4" "build/libs/mixin-0.6.11-SNAPSHOT.jar" "8ec6ce24b8192f043976344305ba6afbc35b052056f1559ca5914a77c5eae71d"

rm -rf tmp
mkdir tmp
pushd tmp
	cp ../../../build.gradle build.gradle
	git init
	git add build.gradle
	git commit -m "Add build.gradle"
	git am ../patches/replaymod/*.patch
popd

popd # Back to root

./gradlew -Preprod $PROXY_SETTINGS -I gradle/reprod/init.gradle "$@"
