package org.jsoup.parser;

import static org.jsoup.internal.StringUtil.inSorted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.helper.Validate;
import org.jsoup.internal.Normalizer;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;

public class TreeBuilderStack {
    private ParseSettings settings;

    private ArrayList<Element> stack;
    private Map<String, ArrayList<Integer>> stackIndexes;

    public TreeBuilderStack(ParseSettings settings) {
        this.settings = settings;
        stack = new ArrayList<>(32);
        stackIndexes = new HashMap<>();
    }

    private ArrayList<Integer> getIndexesList(Element el) {
        return stackIndexes.computeIfAbsent(Normalizer.normalize(el.tagName()), k -> new ArrayList<>(8));
    }

    public void push(Element element) {
        getIndexesList(element).add(stack.size());
        stack.add(element);
    }

    public void insertAt(int index, Element element) {
        // adjust all indexes greater than inserted index
        stackIndexes.forEach((normalName, idxs) -> {
            for (int i = 0; i < idxs.size(); i++) {
                if (idxs.get(i) >= index) {
                    idxs.set(i, idxs.get(i) + 1);
                }
            }
        });

        ArrayList<Integer> indexes = getIndexesList(element);
        indexes.add(index);
        indexes.sort(Integer::compareTo);

        stack.add(index, element);
    }

    @Nullable
    public Element pop() {
        int size = stack.size();
        if (size == 0)
            return null;

        Element popped = stack.remove(size - 1);
        if (popped == null)
            return null;

        String name = Normalizer.normalize(popped.tagName());
        ArrayList<Integer> indexes = stackIndexes.get(name);
        indexes.remove(indexes.size() - 1);
        if (indexes.isEmpty())
            stackIndexes.remove(name);


        return popped;
    }

    public boolean remove(Element element) {
        String name = Normalizer.normalize(element.tagName());
        ArrayList<Integer> indexes = stackIndexes.get(name);
        if (indexes == null)
            return false;

        int stackIndex = -1;
        for (int i = 0; i < indexes.size(); i++) {
            if (stack.get(indexes.get(i)) == element) {
                stackIndex = indexes.get(i);
                indexes.remove(i);
                if (indexes.isEmpty())
                    stackIndexes.remove(name);

                break;
            }
        }
        if (stackIndex == -1)
            return false;

        stack.remove(stackIndex);

        // adjust all indexes greater than removed index
        final int stackIndex2 = stackIndex;
        stackIndexes.forEach((normalName, idxs) -> {
            for (int i = 0; i < idxs.size(); i++) {
                if (idxs.get(i) > stackIndex2) {
                    idxs.set(i, idxs.get(i) - 1);
                }
            }
        });

        return true;
    }

    public void replace(Element out, Element in) {
        int index = lastIndexOf(out);
        Validate.isTrue(index != -1);
        stack.set(index, in);

        String outName = Normalizer.normalize(out.tagName());
        ArrayList<Integer> outIndexes = stackIndexes.get(outName);
        outIndexes.remove((Integer) index);
        if (outIndexes.isEmpty())
            stackIndexes.remove(outName);


        ArrayList<Integer> inIndexes = getIndexesList(in);
        inIndexes.add(index);
        inIndexes.sort(Integer::compareTo);
    }

    public int lastIndexOf(Element element) {
        String name = Normalizer.normalize(element.tagName());
        ArrayList<Integer> indexes = stackIndexes.get(name);
        if (indexes == null)
            return -1;

        for (int i = indexes.size() - 1; i >= 0; i--) {
            int index = indexes.get(i);
            if (stack.get(index) == element) {
                return index;
            }
        }
        return -1;
    }

    public int lastIndexOfType(String name, String namespace) {
        ArrayList<Integer> indexes = stackIndexes.get(Normalizer.normalize(name));
        if (indexes == null)
            return -1;

        for (int i = indexes.size() - 1; i >= 0; i--) {
            int index = indexes.get(i);
            if (stack.get(index).tag().namespace().equals(namespace)) {
                return index;
            }
        }
        return -1;
    }

    public int lastIndexOfType(String name) {
        ArrayList<Integer> indexes = stackIndexes.get(Normalizer.normalize(name));
        if (indexes == null)
            return -1;

        return indexes.get(indexes.size() - 1);
    }

    @Nullable
    public Element lastOfType(String name, String namespace) {
        int lastIndex = lastIndexOfType(name, namespace);
        return lastIndex == -1 ? null : stack.get(lastIndex);
    }

    @Nullable
    public Element lastOfType(String name) {
        int lastIndex = lastIndexOfType(name);
        return lastIndex == -1 ? null : stack.get(lastIndex);
    }

    @Nullable
    public boolean hasOfTypeNot(String[] normalNames) {
        return stackIndexes.keySet().stream().anyMatch(el -> !inSorted(el, normalNames));
    }

    @Nullable
    public Element last() {
        Integer size = stack.size();
        return size > 0 ? stack.get(size - 1) : null;
    }

    public Element get(int index) {
        return stack.get(index);
    }

    @Nullable
    public Element above(Element element) {
        int index = lastIndexOf(element);
        if (index == -1 || index == 0)
            return null;
        return stack.get(index - 1);
    }

    public int size() {
        return stack.size();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
