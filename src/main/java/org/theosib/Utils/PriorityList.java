package org.theosib.Utils;

import java.util.*;
import java.util.function.Consumer;

public class PriorityList<T extends Priority> implements Iterable<T> {
    private List<T> list = new ArrayList<>();

    public List<T> getList() {
        return list;
    }

    public void add(T p) {
        list.add(p);
        list.sort(Comparator.comparing(Priority::priority).reversed());
    }

    public void remove(T p) {
        list.remove(p);
    }

    public void clear() {
        list.clear();
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return list.spliterator();
    }
}
