package com.atex.plugins.sitemap.protocol;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jdom.Comment;
import org.jdom.Element;
import org.jdom.Namespace;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * A builder used to build a generic node for the sitemap protocol (see https://www.sitemaps.org/protocol.html).
 *
 * @author mnova
 */
public class ElementNodeBuilder<T extends ElementNodeBuilder> {

    private List<Namespaces> namespaces = new ArrayList<>();
    private String rootName;
    private String comment;

    public T namespaces(List<Namespaces> namespaces) {
        this.namespaces = namespaces;
        return (T) this;
    }

    public T namespaces(Namespaces...namespaces) {
        for (final Namespaces ns : namespaces) {
            if (!this.namespaces.contains(ns)) {
                this.namespaces.add(ns);
            }
        }
        return (T) this;
    }

    public T rootName(final String rootName) {
        this.rootName = rootName;
        return (T) this;
    }

    public T comment(final String comment) {
        this.comment = comment;
        return (T) this;
    }

    protected ElementBuilder createElementBuilder() {
        if (Strings.isNullOrEmpty(rootName)) {
            throw new RuntimeException("root name cannot be null, cannot create node");
        }

        final Element element;
        if (namespaces != null && namespaces.size() > 0) {
            final Namespaces mainNs = namespaces.get(0);
            element = new Element(rootName, mainNs.ns());
            for (int idx = 1; idx < namespaces.size(); idx++) {
                element.addNamespaceDeclaration(namespaces.get(idx).ns());
            }
        } else {
            element = new Element(rootName);
        }
        if (comment != null) {
            element.addContent(new Comment(comment));
        }
        return new ElementBuilder(element);
    }

    public Element build() {
        return createElementBuilder().build();
    }

    protected class ElementBuilder {
        final Element root;

        public ElementBuilder(final Element root) {
            this.root = root;
        }

        public ElementBuilder(final Namespace ns, final String name) {
            this(new Element(name, ns));
        }

        public ElementBuilder(final String name) {
            this(Namespaces.SITEMAP.ns(), name);
        }

        public ElementBuilder addContent(final Optional<Element> content) {
            content.ifPresent(root::addContent);
            return this;
        }

        public Element build() {
            return root;
        }

    }


}
