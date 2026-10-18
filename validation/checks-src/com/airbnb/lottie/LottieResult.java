package com.airbnb.lottie;
public final class LottieResult {private final Object value;private final Throwable error;public LottieResult(Object value,Throwable error){this.value=value;this.error=error;}public Object getValue(){return value;}public Throwable getException(){return error;}}
