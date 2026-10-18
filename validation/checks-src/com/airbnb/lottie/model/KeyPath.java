package com.airbnb.lottie.model;
public class KeyPath {public final Object element;public KeyPath(String... keys){element=null;}public KeyPath(Object element){this.element=element;}public Object getResolvedElement(){return element;}}
