/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.Namer;
import org.gradle.api.internal.collections.CollectionFilter;
import org.gradle.api.internal.collections.ElementSource;
import org.gradle.api.internal.collections.FilteredList;
import org.gradle.api.internal.collections.IndexedElementSource;
import org.gradle.api.internal.collections.ListElementSource;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.internal.reflect.Instantiator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class DefaultNamedDomainObjectList<T> extends DefaultNamedDomainObjectCollection<T> implements NamedDomainObjectList<T> {
    public DefaultNamedDomainObjectList(DefaultNamedDomainObjectList<? super T> objects, CollectionFilter<T> filter, Instantiator instantiator, Namer<? super T> namer) {
        super(objects, filter, instantiator, namer);
    }

    public DefaultNamedDomainObjectList(Class<T> type, Instantiator instantiator, Namer<? super T> namer) {
        super(type, new ListElementSource<T>(), instantiator, namer);
    }

    public void add(int index, T element) {
        assertMutable();
        assertCanAdd(element);
        getStore().add(index, element);
        didAdd(element);
        getEventRegister().getAddAction().execute(element);
    }

    public boolean addAll(int index, Collection<? extends T> c) {
        assertMutable();
        boolean changed = false;
        int current = index;
        for (T t : c) {
            if (!hasWithName(getNamer().determineName(t))) {
                getStore().add(current, t);
                didAdd(t);
                getEventRegister().getAddAction().execute(t);
                changed = true;
                current++;
            }
        }
        return changed;
    }

    @Override
    protected IndexedElementSource<T> getStore() {
        return (IndexedElementSource<T>) super.getStore();
    }

    public T get(int index) {
        return getStore().get(index);
    }

    public T set(int index, T element) {
        assertMutable();
        assertCanAdd(element);
        T oldElement = getStore().set(index, element);
        if (oldElement != null) {
            didRemove(oldElement);
        }
        getEventRegister().getRemoveAction().execute(oldElement);
        didAdd(element);
        getEventRegister().getAddAction().execute(element);
        return oldElement;
    }

    public T remove(int index) {
        assertMutable();
        T element = getStore().remove(index);
        if (element != null) {
            didRemove(element);
        }
        getEventRegister().getRemoveAction().execute(element);
        return element;
    }

    public int indexOf(Object o) {
        return getStore().indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return getStore().lastIndexOf(o);
    }

    public ListIterator<T> listIterator() {
        return new ListIteratorImpl(getStore().listIterator());
    }

    public ListIterator<T> listIterator(int index) {
        return new ListIteratorImpl(getStore().listIterator(index));
    }

    public List<T> subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(getStore().subList(fromIndex, toIndex));
    }

    @Override
    protected <S extends T> IndexedElementSource<S> filteredStore(CollectionFilter<S> filter, ElementSource<T> elementSource) {
        return new FilteredList<T, S>(elementSource, filter);
    }

    @Override
    public NamedDomainObjectList<T> matching(Closure spec) {
        return matching(Specs.<T>convertClosureToSpec(spec));
    }

    @Override
    public NamedDomainObjectList<T> matching(Spec<? super T> spec) {
        return new DefaultNamedDomainObjectList<T>(this, createFilter(spec), getInstantiator(), getNamer());
    }

    @Override
    public <S extends T> NamedDomainObjectList<S> withType(Class<S> type) {
        return new DefaultNamedDomainObjectList<S>(this, createFilter(type), getInstantiator(), getNamer());
    }

    @Override
    public List<T> findAll(Closure cl) {
        return findAll(cl, new ArrayList<T>());
    }

    private class ListIteratorImpl implements ListIterator<T> {
        private final ListIterator<T> iterator;
        private T lastElement;

        public ListIteratorImpl(ListIterator<T> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        public T next() {
            lastElement = iterator.next();
            return lastElement;
        }

        public T previous() {
            lastElement = iterator.previous();
            return lastElement;
        }

        public int nextIndex() {
            return iterator.nextIndex();
        }

        public int previousIndex() {
            return iterator.previousIndex();
        }

        public void add(T t) {
            assertMutable();
            assertCanAdd(t);
            iterator.add(t);
            didAdd(t);
            getEventRegister().getAddAction().execute(t);
        }

        public void remove() {
            assertMutable();
            iterator.remove();
            didRemove(lastElement);
            getEventRegister().getRemoveAction().execute(lastElement);
            lastElement = null;
        }

        public void set(T t) {
            assertMutable();
            assertCanAdd(t);
            iterator.set(t);
            didRemove(lastElement);
            getEventRegister().getRemoveAction().execute(lastElement);
            didAdd(t);
            getEventRegister().getAddAction().execute(t);
            lastElement = null;
        }
    }

}
