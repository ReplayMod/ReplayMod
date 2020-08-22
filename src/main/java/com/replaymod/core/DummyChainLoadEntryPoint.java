//#if FABRIC>=1
package com.replaymod.core;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

// Required for ReplayModMMLauncher.
//
// Chain-loading mixin configurations only works when the first class that is loaded doesn't have any mixins which
// target it. The first class would ordinarily be MC's Main but there are valid use cases for targeting it, see
// e.g. https://github.com/ReplayMod/ReplayMod/issues/327
// So, instead of relying on the bad assumption that Main doesn't have any mixins, we'll instead register this
// dummy pre-launch entry point which is practically guaranteed to not have any mixins and gets called before Main.
public class DummyChainLoadEntryPoint implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
    }
}
//#endif
