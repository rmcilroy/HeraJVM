/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Common Public License (CPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/cpl1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.compilers.opt;

/**
 * Reverse the order of an enumeration.
 */

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public final class OPT_ReverseEnumerator<T> implements Enumeration<T> {

  private final ArrayList<T> vec;
  private int index;

  public boolean hasMoreElements() {
    return index > 0;
  }

  public T nextElement() {
    index--;
    if (index >= 0) {
      return vec.get(index);
    } else {
      throw new NoSuchElementException();
    }
  }

  public OPT_ReverseEnumerator(Enumeration<T> e) {
    vec = new ArrayList<T>();
    while (e.hasMoreElements()) {
      vec.add(e.nextElement());
    }
    index = vec.size();
  }
}



