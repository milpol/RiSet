package com.jarbytes.collections;

import java.nio.ByteBuffer;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class RiSet implements Set<Integer> {

    private final int size;
    private final int max;
    private final ByteBuffer ri;
    private final ByteBuffer delta;

    public RiSet(Collection<Integer> input) {
        final List<Integer> inputSorted = input.stream()
                .distinct()
                .sorted()
                .collect(toList());
        this.size = inputSorted.size();
        if (size > 0) {
            this.max = inputSorted.get(size - 1);
            final List<Integer> out = new ArrayList<>();
            final List<Integer> deltaPosition = new ArrayList<>();
            final List<Integer> delta = new ArrayList<>();
            int currentDeltaPosition = 0;
            int currentDelta = 0;
            for (int i = 0; i < size; ++i) {
                if (i != 0 && inputSorted.get(i) - inputSorted.get(i - 1) == 1) {
                    ++currentDelta;
                    if (currentDeltaPosition == 0) {
                        currentDeltaPosition = out.size() - 1;
                    }
                } else {
                    out.add(inputSorted.get(i));
                    if (currentDelta > 0) {
                        deltaPosition.add(currentDeltaPosition);
                        delta.add(currentDelta);
                    }
                    currentDelta = 0;
                    currentDeltaPosition = 0;
                }
            }
            if (currentDelta > 0) {
                deltaPosition.add(currentDeltaPosition);
                delta.add(currentDelta);
            }
            this.ri = ByteBuffer.allocateDirect(4 * out.size());
            this.delta = ByteBuffer.allocateDirect(2 * 2 * delta.size());
            out.forEach(this.ri::putInt);
            for (int i = 0; i < delta.size(); ++i) {
                this.delta.putShort(deltaPosition.get(i).shortValue());
                this.delta.putShort(delta.get(i).shortValue());
            }
            this.ri.rewind();
            this.delta.rewind();
        } else {
            max = 0;
            ri = null;
            delta = null;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size > 0;
    }

    @Override
    public synchronized boolean contains(Object o) {
        if (!(o instanceof Integer)) {
            return false;
        } else {
            boolean found = false;
            int min = ri.getInt();
            int check = (Integer) o;
            if (check >= min && check <= max) {
                if (check == min || check == max) {
                    found = true;
                } else {
                    ri.rewind();
                    int currentDeltaPosition = delta.hasRemaining() ? delta.getShort() : -1;
                    int currentDelta = delta.hasRemaining() ? delta.getShort() : -1;
                    int currentPosition = 0;
                    while (ri.hasRemaining()) {
                        int current = ri.getInt();
                        if (currentPosition == currentDeltaPosition) {
                            if (check >= current && check <= current + currentDelta) {
                                found = true;
                                break;
                            }
                            if (delta.hasRemaining()) {
                                currentDeltaPosition = delta.getShort();
                                currentDelta = delta.getShort();
                            }
                        } else {
                            if (check == current) {
                                found = true;
                                break;
                            }
                        }
                        ++currentPosition;
                    }
                }
            }
            ri.rewind();
            delta.rewind();
            return found;
        }
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Maybe some day.");
    }

    @Override
    public Iterator<Integer> iterator() {
        final ByteBuffer localRi;
        final ByteBuffer localDelta;
        synchronized (this) {
            localRi = ByteBuffer.allocate(ri.capacity());
            ri.rewind();
            localRi.put(ri);
            ri.rewind();
            localRi.flip();

            localDelta = ByteBuffer.allocate(delta.capacity());
            delta.rewind();
            localDelta.put(delta);
            delta.rewind();
            localDelta.flip();
        }
        return new Iterator<>() {
            int currentDeltaPosition = delta.hasRemaining() ? delta.getShort() : -1;
            int remainingDelta = delta.hasRemaining() ? delta.getShort() : -1;
            Integer current = null;
            int position = 0;

            @Override
            public boolean hasNext() {
                return localRi.hasRemaining() || remainingDelta > 0;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                } else {
                    if (current == null) {
                        current = localRi.getInt();
                        ++position;
                        return current;
                    }
                    if (remainingDelta > 0) {
                        ++current;
                        --remainingDelta;
                        if (remainingDelta == 0) {
                            currentDeltaPosition = delta.hasRemaining() ? delta.getShort() : -1;
                        }
                        return current;
                    }
                    current = localRi.getInt();
                    if (position == currentDeltaPosition) {
                        remainingDelta = delta.getShort();
                    }
                    ++position;
                    return current;
                }
            }
        };
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }

    @Override
    public boolean add(Integer integer) {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Unmodifiable collection.");
    }
}
