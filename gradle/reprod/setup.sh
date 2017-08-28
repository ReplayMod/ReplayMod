#!/usr/bin/env bash
set -e
set -x

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
        ./gradlew -I ../../init.gradle build -x test

        actual_hash=$(sha256val "$jar")
        if [ "$actual_hash" != "$jarhash" ]; then
            echo "Failed to verify checksum of build artifact of dependency: $dep"
            echo "Expected: $jarhash"
            echo "But was:  $actual_hash"
            exit 1
        fi

        cp "$jar" "../$dep.jar"
    popd
}

# Required for all
setup_dep "gradle-witness" "https://github.com/ReplayMod/gradle-witness.git" "c162a15841c2eba54b182fa81733c0aa9227f023" "build/libs/gradle-witness.jar" "5e9ce687248029bf6364010168a65a4ad66fcb712dbd5ba69c59697ef564964b"

# Required for mixin
setup_dep "shadow" "https://github.com/johnrengelman/shadow.git" "60d0f28103be076dc991a624bf79ca7a13835973" "build/libs/shadow.jar" "eec0417a8cd44457551c80335cd384a187647b6429ed7b5ae3ce1b26f78a2c1f"
setup_dep "fernflower" "https://github.com/fesh0r/fernflower.git" "85f61bee8194ab69afa746b965973a18eda67608" "build/libs/fernflower.jar" "577c2c4e04f0026675cacb25ec525f2ccba5de2eecac525b28bed18f9ccfd790"

# Required for forgeflower
setup_dep "gitpatcher" "https://github.com/Minecrell/gitpatcher.git" "e815821d516bac0da01d579575f895b2a8abefe7" "build/libs/gitpatcher-0.9.0.jar" "95cc738b92b5f37466593880177e236519c9b2f10e3d38f97e36fd3ad3bd4448"

# Required for forgegradle
setup_dep "forgeflower" "https://github.com/MinecraftForge/ForgeFlower.git" "32a04b9840fb6b10a8e2c178af86e20bdb85e366" "ForgeFlower/build/libs/forgeflower-1.0.342-SNAPSHOT.jar" "4b32e3140decbe39eaf77399f0390fbece6b3a1f3ff3424e4754adefe1c7c932"
setup_dep "mcinjector" "https://github.com/ModCoderPack/MCInjector.git" "7258466461baf7dc4f313b06b0d589407e4e1fba" "build/libs/mcinjector-3.4-SNAPSHOT.jar" "98cd0c13030aa8103585621fdab5db6b74ea0db7b8b30bc74061e7b5d314aa01"
setup_dep "srg2source" "https://github.com/MinecraftForge/Srg2Source.git" "ea4ea624672e8b75fb2a78f0fb9105db13d5a4a6" "build/libs/Srg2Source-4.0-SNAPSHOT.jar" "575e5fa1d6834edb34e0bf1d56ae7237b02b2d87a6e2d30c8605d5287fd2ef53"

# Required for RM
setup_dep "mixingradle" "https://github.com/SpongePowered/MixinGradle.git" "3d81c8e202ec435056fb2068fdc34cfefa99be2d" "build/libs/mixingradle-0.4-SNAPSHOT.jar" "8b3508867128a5d564631635dff898a36f9aca8db54b7bb3af6f4924e3f4bead"
setup_dep "forgegradle" "https://github.com/MinecraftForge/ForgeGradle.git" "a228a836a2dc5ce546d2d53c48760f52f082d7ad" "build/libs/ForgeGradle-2.3-SNAPSHOT.jar" "94bf0d8be7e68a3d9828c8208024c21f2c171663dd7d56b95b3459bfc67273c0"
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

./gradlew -Preprod -I gradle/reprod/init.gradle "$@"
