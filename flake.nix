{
  description = "ReplayMod 1.19.4 dev environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-23.11";
  inputs.nixpkgs-unstable.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, nixpkgs-unstable, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        unstablePkgs = nixpkgs-unstable.legacyPackages.${system};
      in {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk8
            adoptopenjdk-hotspot-bin-16
            jdk17
            jdk21
            unstablePkgs.jdk25
            gradle
            ffmpeg-full
            vulkan-tools
            mesa
            libGL
            libGLU
            pkg-config
            git
          ];

          shellHook = ''
            export JAVA_HOME=${pkgs.jdk21}/lib/openjdk
            export PATH=$JAVA_HOME/bin:$PATH
            export JDK8_HOME=${pkgs.jdk8}/lib/openjdk
            export JDK16_HOME=${pkgs.adoptopenjdk-hotspot-bin-16}
            export JDK17_HOME=${pkgs.jdk17}/lib/openjdk
            export JDK21_HOME=${pkgs.jdk21}/lib/openjdk
            export JDK25_HOME=${unstablePkgs.jdk25}/lib/openjdk
            export REPLAYMOD_GRADLE_TOOLCHAINS="-Dorg.gradle.java.installations.fromEnv=JDK8_HOME,JDK16_HOME,JDK17_HOME,JDK21_HOME,JDK25_HOME -Dorg.gradle.java.installations.paths=$JDK8_HOME,$JDK16_HOME,$JDK17_HOME,$JDK21_HOME,$JDK25_HOME"
            export GRADLE_OPTS="$REPLAYMOD_GRADLE_TOOLCHAINS ''${GRADLE_OPTS:-}"
            export JAVA_OPTS="$REPLAYMOD_GRADLE_TOOLCHAINS ''${JAVA_OPTS:-}"
            echo "ReplayMod dev shell - Java: $(java -version 2>&1 | head -1)"
            echo "CPUs: $(nproc)  |  DRI: $(ls /dev/dri/ 2>/dev/null | tr '\n' ' ')"
          '';
        };
      });
}
