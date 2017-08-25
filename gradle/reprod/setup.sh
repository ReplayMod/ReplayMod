#!/usr/bin/env bash
set -e
set -x

cd gradle/reprod

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
        patches=$(find ../../patches/$dep/ -name *.patch | sort)
        [ "$patches" != "" ] && echo "$patches" | xargs git am
        git reset --soft "$commit" # Because forgegradle includes the commit hash in the jar

        rm -rf gradle/wrapper gradlew
        [ ! -d gradle ] && mkdir gradle
        cp -r ../../../wrapper gradle/
        cp ../../../wrapper/gradle-wrapper.properties gradle/wrapper/
        cp ../../../../gradlew .

        chmod +x gradlew
        ./gradlew build -x test

        actual_hash=$(sha256val "build/libs/$jar")
        if [ "$actual_hash" != "$jarhash" ]; then
            echo "Failed to verify checksum of build artifact of dependency: $dep"
            echo "Expected: $jarhash"
            echo "But was:  $actual_hash"
            exit 1
        fi

        cp "build/libs/$jar" "../$dep.jar"
    popd
}

# Required for all
setup_dep "gradle-witness" "https://github.com/ReplayMod/gradle-witness.git" "c162a15841c2eba54b182fa81733c0aa9227f023" "gradle-witness.jar" "5e9ce687248029bf6364010168a65a4ad66fcb712dbd5ba69c59697ef564964b"

# Required for mixin
setup_dep "shadow" "https://github.com/johnrengelman/shadow.git" "60d0f28103be076dc991a624bf79ca7a13835973" "shadow.jar" "eec0417a8cd44457551c80335cd384a187647b6429ed7b5ae3ce1b26f78a2c1f"
setup_dep "fernflower" "https://github.com/fesh0r/fernflower.git" "85f61bee8194ab69afa746b965973a18eda67608" "fernflower.jar" "577c2c4e04f0026675cacb25ec525f2ccba5de2eecac525b28bed18f9ccfd790"

# Required for RM
setup_dep "mixingradle" "https://github.com/SpongePowered/MixinGradle.git" "3d81c8e202ec435056fb2068fdc34cfefa99be2d" "mixingradle-0.4-SNAPSHOT.jar" "8b3508867128a5d564631635dff898a36f9aca8db54b7bb3af6f4924e3f4bead"
setup_dep "forgegradle" "https://github.com/MinecraftForge/ForgeGradle.git" "a228a836a2dc5ce546d2d53c48760f52f082d7ad" "ForgeGradle-2.3-SNAPSHOT.jar" "aaa19067cd51397756be850bd3e621667b535c0e897fc77e1bb1eb72550e4a63"
setup_dep "mixin" "https://github.com/SpongePowered/Mixin.git" "b558323da3bd6ce94aeb442bfd7357f6c40d2fd4" "mixin-0.6.11-SNAPSHOT.jar" "8ec6ce24b8192f043976344305ba6afbc35b052056f1559ca5914a77c5eae71d"

rm -rf tmp
mkdir tmp
pushd tmp
	cp ../../../build.gradle build.gradle
	git init
	git add build.gradle
	git commit -m "Add build.gradle"
	git am ../patches/replaymod/*.patch
popd
