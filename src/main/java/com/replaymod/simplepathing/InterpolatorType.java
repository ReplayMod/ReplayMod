package com.replaymod.simplepathing;

import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@AllArgsConstructor
public enum InterpolatorType {
    DEFAULT("default", null, null),
    CUBIC("cubic", CubicSplineInterpolator.class, CubicSplineInterpolator::new),
    LINEAR("linear", LinearInterpolator.class, LinearInterpolator::new);

    private String localizationKey;

    @Getter
    private Class<? extends Interpolator> interpolatorClass;

    private Supplier<Interpolator> interpolatorConstructor;

    public String getI18nName() {
        return String.format("replaymod.gui.editkeyframe.interpolator.%1$s.name", localizationKey);
    }

    public String getI18nDescription() {
        return String.format("replaymod.gui.editkeyframe.interpolator.%1$s.desc", localizationKey);
    }

    public static InterpolatorType fromString(String string) {
        for (InterpolatorType t : values()) {
            if (t.getI18nName().equals(string)) return t;
        }
        return CUBIC; //the default
    }

    public static InterpolatorType fromClass(Class<? extends Interpolator> cls) {
        for (InterpolatorType type : values()) {
            if (cls.equals(type.getInterpolatorClass())) {
                return type;
            }
        }
        return DEFAULT;
    }

    public Interpolator newInstance() {
        return interpolatorConstructor.get();
    }
}
