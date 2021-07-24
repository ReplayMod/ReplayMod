package com.replaymod.core.utils;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Result<Ok, Err> {

    public static <Ok, Err> OkImpl<Ok, Err> ok(Ok value) {
        return new OkImpl<>(value);
    }

    public static <Ok, Err> ErrImpl<Ok, Err> err(Err value) {
        return new ErrImpl<>(value);
    }

    public abstract boolean isOk();
    public abstract boolean isErr();
    public abstract Ok okOrNull();
    public abstract Err errOrNull();
    public abstract void ifOk(Consumer<Ok> consumer);
    public abstract void ifErr(Consumer<Err> consumer);
    public abstract Ok okOrElse(Function<Err, Ok> orElse);
    public abstract Err errOrElse(Function<Ok, Err> orElse);
    public abstract <T> Result<T, Err> mapOk(Function<Ok, T> func);
    public abstract <T> Result<Ok, T> mapErr(Function<Err, T> func);

    private static class OkImpl<Ok, Err> extends Result<Ok, Err> {
        private final Ok value;

        public OkImpl(Ok value) {
            this.value = value;
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public boolean isErr() {
            return false;
        }

        @Override
        public Ok okOrNull() {
            return this.value;
        }

        @Override
        public Err errOrNull() {
            return null;
        }

        @Override
        public void ifOk(Consumer<Ok> consumer) {
            consumer.accept(this.value);
        }

        @Override
        public void ifErr(Consumer<Err> consumer) {
        }

        @Override
        public Ok okOrElse(Function<Err, Ok> orElse) {
            return this.value;
        }

        @Override
        public Err errOrElse(Function<Ok, Err> orElse) {
            return orElse.apply(this.value);
        }

        @Override
        public <V> Result<V, Err> mapOk(Function<Ok, V> func) {
            return ok(func.apply(this.value));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Result<Ok, T> mapErr(Function<Err, T> func) {
            return (Result<Ok, T>) this;
        }
    }

    private static class ErrImpl<Ok, Err> extends Result<Ok, Err> {
        private final Err value;

        public ErrImpl(Err value) {
            this.value = value;
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public boolean isErr() {
            return true;
        }

        @Override
        public Ok okOrNull() {
            return null;
        }

        @Override
        public Err errOrNull() {
            return this.value;
        }

        @Override
        public void ifOk(Consumer<Ok> consumer) {
        }

        @Override
        public void ifErr(Consumer<Err> consumer) {
            consumer.accept(this.value);
        }

        @Override
        public Ok okOrElse(Function<Err, Ok> orElse) {
            return orElse.apply(this.value);
        }

        @Override
        public Err errOrElse(Function<Ok, Err> orElse) {
            return this.value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <V> Result<V, Err> mapOk(Function<Ok, V> func) {
            return (Result<V, Err>) this;
        }

        @Override
        public <T> Result<Ok, T> mapErr(Function<Err, T> func) {
            return err(func.apply(this.value));
        }
    }
}
