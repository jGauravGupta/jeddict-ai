/**
 * Copyright 2025-26 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jeddict.ai.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.tuple.Pair;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
import org.openide.filesystems.FileObject;

/**
 * Scans Java source files within a given root {@link FileObject} to identify and collect
 * {@link TypeElement}s. It leverages NetBeans Java Source API to resolve types.
 */
public class SourceScanner {

    public final FileObject root;

    /**
     * Constructs a new SourceScanner for a given root {@link FileObject}.
     * @param root The root {@link FileObject} to start scanning from.
     */
    public SourceScanner(final FileObject root) {
        this.root = root;
    }

    /**
     * Scans the root {@link FileObject} and its subdirectories for Java source files
     * and returns a list of all declared {@link TypeElement}s found.
     *
     * @return A {@link List} of {@link Pair}s, where each {@link Pair} contains
     *         the {@link FileObject} of the source file and an {@link ElementHandle}
     *         to the {@link TypeElement}.
     */
    public List<Pair<FileObject, ElementHandle<TypeElement>>> types() {
        final ClasspathInfo cpInfo = ClasspathInfo.create(root);
        final ClassIndex index = cpInfo.getClassIndex();

        final List<Pair<FileObject, ElementHandle<TypeElement>>> ret = new ArrayList();

        final Set<ElementHandle<TypeElement>> handles = index.getDeclaredTypes(
            "",
            ClassIndex.NameKind.PREFIX,
            Set.of(ClassIndex.SearchScope.SOURCE)
        );

        for (ElementHandle<TypeElement> h : handles) {
            final FileObject fo = SourceUtils.getFile(h, cpInfo);
            if (fo != null) {
                ret.add(Pair.of(fo, h));
            }
        }
        return ret;
    }
}
