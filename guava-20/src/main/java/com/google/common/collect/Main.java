package com.google.common.collect;

import java.util.Collection;

import static com.google.common.base.Predicates.*;

public class Main {

  public static void main(String[] args) {

  }

  private static<K extends Comparable> void retainsAll(Collection<?> c) {
    java.util.function.Predicate<? super Range<K>> p = (java.util.function.Predicate<? super Range<K>>) compose(not(in(c)), Maps.<Range<K>>keyFunction());
  }
}
