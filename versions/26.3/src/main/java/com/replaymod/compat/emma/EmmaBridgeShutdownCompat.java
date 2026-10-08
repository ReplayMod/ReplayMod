package com.replaymod.compat.emma;

import com.replaymod.compat.ReplayModCompat;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Compatibility with the Emmatone workers bundled in Emma Bridge 0.2.0. */
public final class EmmaBridgeShutdownCompat {
    private EmmaBridgeShutdownCompat() {}

    public static void shutdown() {
        boolean interrupted = false;
        try {
            Class<?> emmatone = Class.forName("emmatone.Emmatone");
            ExecutorService executor = (ExecutorService) emmatone.getMethod("getExecutor").invoke(null);
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    ReplayModCompat.LOGGER.warn("Emma Bridge workers did not finish within five seconds");
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }

            // WorldProvider keeps disconnected worlds in a private cache. Flush them after stopping writers.
            Field field = Class.forName("emmatone.cache.WorldProvider").getDeclaredField("worldCache");
            field.setAccessible(true);
            Map<?, ?> cache = (Map<?, ?>) field.get(null);
            List<?> worlds;
            synchronized (cache) {
                worlds = new ArrayList<>(cache.values());
            }
            for (Object world : worlds) {
                Object chunks = world.getClass().getMethod("getCachedWorld").invoke(world);
                chunks.getClass().getMethod("save").invoke(chunks);
            }
            ReplayModCompat.LOGGER.info("Emma Bridge workers stopped and cached worlds saved");
        } catch (ReflectiveOperationException | RuntimeException e) {
            ReplayModCompat.LOGGER.error("Could not stop Emma Bridge workers cleanly", e);
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
