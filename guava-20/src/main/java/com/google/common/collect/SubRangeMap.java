package com.google.common.collect;

import java.util.*;

abstract class SubRangeMap<K extends Comparable, V> implements RangeMap<K , V> {

  /*private final Range<K> subRange;

  SubRangeMap(Range<K> subRange) {
    this.subRange = subRange;
  }

  @Override
  public V get(K key) {
    return subRange.contains(key)
      ? TreeConnectedRangeMap.this.get(key)
      : null;
  }

  @Override
  public Map.Entry<Range<K>, V> getEntry(K key) {
    if (subRange.contains(key)) {
      Map.Entry<Range<K>, V> entry = TreeConnectedRangeMap.this.getEntry(key);
      if (entry != null) {
        return Maps.immutableEntry(entry.getKey().intersection(subRange), entry.getValue());
      }
    }
    return null;
  }

  @Override
  public Range<K> span() {
    return Range.create(getLowerBound(), getUpperBound());
  }

  private Cut<K> getUpperBound() {
    Cut<K> upperBound = getUpperBound();
    Map.Entry<Cut<K>, RangeMapEntry<K, V>> upperEntry =
      entriesByLowerBound.lowerEntry(subRange.upperBound);
    if (upperEntry == null) {
      throw new NoSuchElementException();
    } else if (upperEntry.getValue().getUpperBound().compareTo(subRange.upperBound) >= 0) {
      upperBound = subRange.upperBound;
    } else {
      upperBound = upperEntry.getValue().getUpperBound();
    }
    return upperBound;
  }

  private Cut<K> getLowerBound() {
    Cut<K> lowerBound;
    Map.Entry<Cut<K>, RangeMapEntry<K, V>> lowerEntry =
      entriesByLowerBound.floorEntry(subRange.lowerBound);
    if (lowerEntry != null &&
      lowerEntry.getValue().getUpperBound().compareTo(subRange.lowerBound) > 0) {
      lowerBound = subRange.lowerBound;
    } else {
      lowerBound = entriesByLowerBound.ceilingKey(subRange.lowerBound);
      if (lowerBound == null || lowerBound.compareTo(subRange.upperBound) >= 0) {
        throw new NoSuchElementException();
      }
    }
    return lowerBound;
  }

  @Override
  public void put(Range<K> range, V value) {
    checkArgument(subRange.encloses(range),
      "Cannot put range %s into a subRangeMap(%s)", range, subRange);
    TreeConnectedRangeMap.this.put(range, value);
  }

  @Override
  public void putAll(RangeMap<K, V> rangeMap) {
    if (rangeMap.asMapOfRanges().isEmpty()) {
      return;
    }
    Range<K> span = rangeMap.span();
    checkArgument(subRange.encloses(span),
      "Cannot putAll rangeMap with span %s into a subRangeMap(%s)", span, subRange);
    TreeConnectedRangeMap.this.putAll(rangeMap);
  }

  @Override
  public void clear() {
    TreeConnectedRangeMap.this.remove(subRange);
  }

  @Override
  public void remove(Range<K> range) {
    if (range.isConnected(subRange)) {
      TreeConnectedRangeMap.this.remove(range.intersection(subRange));
    }
  }

  @Override
  public RangeMap<K, V> subRangeMap(Range<K> range) {
    if (!range.isConnected(subRange)) {
      return emptySubRangeMap();
    } else {
      return TreeConnectedRangeMap.this.subRangeMap(range.intersection(subRange));
    }
  }

  @Override
  public Map<Range<K>, V> asMapOfRanges() {
    return new SubRangeMapAsMap();
  }

  @Override
  public Map<Range<K>, V> asDescendingMapOfRanges() {
    return new SubRangeMapAsMap() {

      @Override
      Iterator<Map.Entry<Range<K>, V>> entryIterator() {
        if (subRange.isEmpty()) {
          return Iterators.emptyIterator();
        }
        final Iterator<RangeMapEntry<K, V>> backingItr =
          entriesByLowerBound
            .headMap(subRange.upperBound, false)
            .descendingMap()
            .values()
            .iterator();
        return new AbstractIterator<Map.Entry<Range<K>, V>>() {

          @Override
          protected Map.Entry<Range<K>, V> computeNext() {
            if (backingItr.hasNext()) {
              RangeMapEntry<K, V> entry = backingItr.next();
              if (entry.getUpperBound().compareTo(subRange.lowerBound) <= 0) {
                return endOfData();
              }
              return Maps.immutableEntry(entry.getKey().intersection(subRange), entry.getValue());
            }
            return endOfData();
          }
        };
      }
    };
  }

  @Override
  public boolean equals( Object o) {
    if (o instanceof RangeMap) {
      RangeMap<?, ?> rangeMap = (RangeMap<?, ?>) o;
      return asMapOfRanges().equals(rangeMap.asMapOfRanges());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return asMapOfRanges().hashCode();
  }

  @Override
  public String toString() {
    return asMapOfRanges().toString();
  }

  class SubRangeMapAsMap extends AbstractMap<Range<K>, V> {

    @Override
    public boolean containsKey(Object key) {
      return get(key) != null;
    }

    @Override
    public V get(Object key) {
      try {
        if (key instanceof Range) {
          @SuppressWarnings("unchecked") // we catch ClassCastExceptions
            Range<K> r = (Range<K>) key;
          if (!subRange.encloses(r) || r.isEmpty()) {
            return null;
          }
          RangeMapEntry<K, V> candidate = null;
          if (r.lowerBound.compareTo(subRange.lowerBound) == 0) {
            // r could be truncated on the left
            Entry<Cut<K>, RangeMapEntry<K, V>> entry =
              entriesByLowerBound.floorEntry(r.lowerBound);
            if (entry != null) {
              candidate = entry.getValue();
            }
          } else {
            candidate = entriesByLowerBound.get(r.lowerBound);
          }

          if (candidate != null && candidate.getKey().isConnected(subRange)
            && candidate.getKey().intersection(subRange).equals(r)) {
            return candidate.getValue();
          }
        }
      } catch (ClassCastException e) {
        return null;
      }
      return null;
    }

    @Override
    public V remove(Object key) {
      V value = get(key);
      if (value != null) {
        @SuppressWarnings("unchecked") // it's definitely in the map, so safe
          Range<K> range = (Range<K>) key;
        TreeConnectedRangeMap.this.remove(range);
        return value;
      }
      return null;
    }

    @Override
    public void clear() {
      SubRangeMap.this.clear();
    }

    private boolean removeIf(Predicate<? super Entry<Range<K>, V>> predicate) {
      List<Range<K>> toRemove = Lists.newArrayList();
      for (Entry<Range<K>, V> entry : entrySet()) {
        if (predicate.apply(entry)) {
          toRemove.add(entry.getKey());
        }
      }
      for (Range<K> range : toRemove) {
        this.remove(range);
      }
      return !toRemove.isEmpty();
    }

    @Override
    public Set<Range<K>> keySet() {
      return new Maps.KeySet<Range<K>, V>(this) {
        @Override
        Map<Range<K>, V> map() {
          return SubRangeMapAsMap.this;
        }

        @Override
        public boolean remove( Object o) {
          return SubRangeMapAsMap.this.remove(o) != null;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          final boolean b = removeIf((java.util.function.Predicate<? super Range<K>>) compose(not(in(c)), Maps.<Range<K>>keyFunction()));
          return b;
        }
      };
    }

    @Override
    public Set<Entry<Range<K>, V>> entrySet() {
      return new Maps.EntrySet<Range<K>, V>() {
        @Override
        Map<Range<K>, V> map() {
          return SubRangeMapAsMap.this;
        }

        @Override
        public Iterator<Entry<Range<K>, V>> iterator() {
          if (subRange.isEmpty()) {
            return Iterators.emptyIterator();
          }
          Cut<K> cutToStart = MoreObjects.firstNonNull(
            entriesByLowerBound.floorKey(subRange.lowerBound),
            subRange.lowerBound);
          final Iterator<RangeMapEntry<K, V>> backingItr =
            entriesByLowerBound.tailMap(cutToStart, true).values().iterator();
          return new AbstractIterator<Entry<Range<K>, V>>() {

            @Override
            protected Entry<Range<K>, V> computeNext() {
              while (backingItr.hasNext()) {
                RangeMapEntry<K, V> entry = backingItr.next();
                if (entry.getLowerBound().compareTo(subRange.upperBound) >= 0) {
                  break;
                } else if (entry.getUpperBound().compareTo(subRange.lowerBound) > 0) {
                  // this might not be true e.g. at the start of the iteration
                  return Maps.immutableEntry(
                    entry.getKey().intersection(subRange), entry.getValue());
                }
              }
              return endOfData();
            }
          };
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeIf((java.util.function.Predicate<? super java.util.Map.Entry<Range<K>, V>>) not(in(c)));
        }

        @Override
        public int size() {
          return Iterators.size(iterator());
        }

        @Override
        public boolean isEmpty() {
          return !iterator().hasNext();
        }
      };
    }

    @Override
    public Collection<V> values() {
      return new Maps.Values<Range<K>, V>(SubRangeMapAsMap.this) {
        @Override
        public boolean removeAll(Collection<?> c) {
          return removeIf((java.util.function.Predicate<? super V>) compose(in(c), Maps.<V>valueFunction()));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeIf((java.util.function.Predicate<? super V>) compose(not(in(c)), Maps.<V>valueFunction()));
        }
      };
    }
  }*/

}
