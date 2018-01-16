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
    pomhash=$6

    depjar="deps/repo/reprod/$dep/0/$dep-0.jar"
    deppom="deps/repo/reprod/$dep/0/$dep-0.pom"

    [ -f "$depjar" ] && [ "$(sha256val "$depjar")" == "$jarhash" ] && [ -f "$deppom" ] && [ "$(sha256val "$deppom")" == "$pomhash" ] && return
    [ -f "$depjar" ] && [ "*" == "$jarhash" ] && return
    rm -rf "$depjar" "$deppom"

    mkdir -p "deps/repo/reprod/$dep/0"

    if [ "*" != "$jarhash" ] && [ "$OFFLINE" != "1" ]; then
        # Try to fetch the pre-built artifact
        if wget -O "deps/dl.jar" "https://www.johni0702.de/replaymod/artifact/$jarhash" && wget -O "deps/dl.pom" "https://www.johni0702.de/replaymod/artifact/$pomhash"; then
            # Verify downloaded jar/pom files
            if [ "$(sha256val "deps/dl.jar")" == "$jarhash" ] && [ "$(sha256val "deps/dl.pom")" == "$pomhash" ]; then
                # Got valid pre-built artifact, use it
                mv "deps/dl.jar" "$depjar"
                mv "deps/dl.pom" "$deppom"
                return
            fi
        fi
    fi


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
        if [ "$dep" == "proxy-witness" ]; then
            ./gradlew $PROXY_SETTINGS build -x test -x javadoc
        else
            ./gradlew $PROXY_SETTINGS -I ../../init.gradle build -x test -x javadoc
        fi

        actual_hash=$(sha256val "$jar")
        if [ "*" != "$jarhash" ] && [ "$actual_hash" != "$jarhash" ]; then
            echo "Failed to verify checksum of build artifact of dependency: $dep"
            echo "Expected: $jarhash"
            echo "But was:  $actual_hash"
            exit 1
        fi

        actual_hash=$(sha256val "pom.xml")
        if [ "*" != "$jarhash" ] && [ "$actual_hash" != "$pomhash" ]; then
            echo "Failed to verify checksum of build artifact pom of dependency: $dep"
            echo "Expected: $pomhash"
            echo "But was:  $actual_hash"
            exit 1
        fi

        # Subshell to allow for expansion of *
        cp "$(echo $jar)" "../../$depjar"
        [ "$dep" == "proxy-witness" ] || cp "pom.xml" "../../$deppom"
    popd
}

# Setup http(s) proxy
setup_dep "proxy-witness" "https://github.com/johni0702/proxy-witness" "17ebb2e22f812faed9a28bae6bf1d8b28f798d56" "build/libs/proxy-witness.jar" "*" "*"
java -Dproxywitness.httpUris=http://export.mcpbot.bspk.rs/versions.json -jar deps/repo/reprod/proxy-witness/0/proxy-witness-0.jar "$PROXY_PORT" checksums.txt > proxy.log 2>&1 &
proxy_pid=$!
trap "kill $proxy_pid" EXIT

# Required for mixin
setup_dep "fernflower" "https://github.com/fesh0r/fernflower.git" "adbf29f8ce902cd164fc0c41491030268ecc8265" "build/libs/fernflower.jar" "d2042d3121118a7175fc902d0e49f4ad37d00467e7ea535574974142eff64184" "901b8a76ceb15db97d4dd19c5e8fcb0fc226ae602ef38ea906eb5258be9b929a"

# Required for forgegradle
setup_dep "forgeflower" "https://github.com/MinecraftForge/FernFlowerLegacy.git" "114aebe82cf40075c5c4f916c409b8aebf8096d5" "build/libs/fernflower-2.0-SNAPSHOT.jar" "c8b4a4a13a1158008aed46da2c805184078324718697f14a67f1778168579956" "fbda3f30d54624fe4c59a69e145b644d8642afc0dd4eb0d732ccdaa0ebd5d8a3"
setup_dep "mcinjector" "https://github.com/ModCoderPack/MCInjector.git" "7258466461baf7dc4f313b06b0d589407e4e1fba" "build/libs/mcinjector-3.4-SNAPSHOT.jar" "98b685ea0d3ae9fad8b8a7e7b885512f7fe0b61becf27d9c74b91a626d107efc" "3c08655c11e6509bfc62ff87d0770cefa0ecbd8dccece9e083c9b7b89779a8c5"
setup_dep "srg2source" "https://github.com/MinecraftForge/Srg2Source.git" "ea4ea624672e8b75fb2a78f0fb9105db13d5a4a6" "build/libs/Srg2Source-4.0-SNAPSHOT.jar" "727445a7b946535217be658726c537acf1921f0a9e2ac0ea717dff9b36dc529f" "4ebd7b738f368ee5c25a1028a167257e7012cef7c72c8b5b06149286992c631e"

# Required for RM
setup_dep "mixingradle" "https://github.com/SpongePowered/MixinGradle.git" "52217aa8ca221dcd0b9fb657b037e663db808f38" "build/libs/mixingradle-0.4-SNAPSHOT.jar" "8b3508867128a5d564631635dff898a36f9aca8db54b7bb3af6f4924e3f4bead" "42cbf81136d4e26c06d3234427665b50d18768cc1f210512204ca1d5e3a42b5c"
setup_dep "forgegradle" "https://github.com/MinecraftForge/ForgeGradle.git" "d13a6c892f51937567b3d01d85bea4f3d128e71d" "build/libs/ForgeGradle-2.2-SNAPSHOT.jar" "355a8bd6f49f3d11c40d58da9bb388e05b634ddfac43ab222bf2326aa40f0be4" "45966ee785c42e34c36be3e73046aca1a581a919fb02a9028970d19d35488b18"
setup_dep "mixin" "https://github.com/SpongePowered/Mixin.git" "404f5da0b6b18980b4e44c7f1cfeddcd47fe4aa6" "build/libs/mixin-0.7.5-SNAPSHOT.jar" "7d181a5e7c317c00c07da14e7770e4356b0f201414cd2a836e987be9fa133d43" "11000383a5051f25a70c2bff3a3167b6859ae4f9838a0ab45560e9f9c9d9def7"

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

if [ "$SIGNED_JAR" == "1" ]; then
    echo "Trying fetch signature for resulting jar file.."
    # Note: This requires there to be one and only one jar file (ignoring source artifacts)
    jar="build/libs/$(ls build/libs | grep -v sources)"
    jar_hash=$(sha256val "$jar")
    pushd gradle/reprod/tmp
    mkdir sign_tmp
    pushd sign_tmp
        # Signatures are generated using:
        # jarsigner -sigfile johni -signedjar signed.jar unsigned.jar replaymod
        # jar xf signed.jar META-INF/MANIFEST.MF META-INF/JOHNI.SF META-INF/JOHNI.RSA
        # tar cJf signature.tar.xz META-INF
        wget -O - "https://www.johni0702.de/replaymod/signature/$jar_hash" | tar xJ
        jar uMf "../../../../$jar" META-INF/
    popd
fi
