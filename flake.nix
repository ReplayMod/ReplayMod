{
  description = "ReplayMod 1.19.4 dev environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-23.11";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk8
            adoptopenjdk-hotspot-bin-16
            jdk17
            jdk21
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
            export JAVA_HOME=${pkgs.jdk21}
            export PATH=$JAVA_HOME/bin:$PATH
            export GRADLE_OPTS="-Dorg.gradle.java.installations.paths=${pkgs.jdk8},${pkgs.adoptopenjdk-hotspot-bin-16},${pkgs.jdk17},${pkgs.jdk21} ''${GRADLE_OPTS:-}"
            echo "ReplayMod dev shell - Java: $(java -version 2>&1 | head -1)"
            echo "CPUs: $(nproc)  |  DRI: $(ls /dev/dri/ 2>/dev/null | tr '\n' ' ')"
          '';
        };
      });
}
