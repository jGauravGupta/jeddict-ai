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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.tuple.Pair;
import org.netbeans.api.java.source.ElementHandle;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * A simplified utility for scanning Java source files within a given root {@link Path}
 * for testing purposes.
 *
 * This class is intended as a testing replacement (a working mock) of the
 * corresponding source code, which can not be covered directly due to dependencies
 * on NB and javac compiler functionalities not easy to put under test.
 * 
 * This version directly reads file system paths and constructs
 * {@link ElementHandle}s without relying on the full NetBeans Java Source API.
 */
public class SourceScanner {

    public final Path root;

    /**
     * Constructs a new SourceScanner for a given root {@link FileObject}.
     * The root {@link FileObject} is converted to a {@link Path}.
     * @param root The root {@link FileObject} to start scanning from.
     */
    public SourceScanner(final FileObject root) {
        this.root = Paths.get(root.getPath());
    }

    /**
     * Scans the root {@link Path} and its subdirectories for Java source files
     * and returns a list of all declared {@link TypeElement}s found.
     * This implementation directly constructs {@link ElementHandle}s based on file paths.
     *
     * @return A {@link List} of {@link Pair}s, where each {@link Pair} contains
     *         the {@link FileObject} of the source file and an {@link ElementHandle}
     *         to the {@link TypeElement}.
     */
    public List<Pair<FileObject, ElementHandle<TypeElement>>> types() {
        final List<Pair<FileObject, ElementHandle<TypeElement>>> ret = new ArrayList();

        try {
            getJavaClasses(root).forEach((path) -> {
                String className = root.relativize(path).toString();
                className = className.replace(File.separator, ".").substring(0, className.length()-5);
                ret.add(
                    Pair.of(
                        FileUtil.toFileObject(path.toAbsolutePath()),
                        ElementHandle.createTypeElementHandle(ElementKind.CLASS, className)
                    )
                );
            });
        } catch (IOException x) {
            // ignore
        }

        return ret;
    }

    /**
     * Recursively walks the given root path to find all Java source files.
     * @param rootPath The starting {@link Path} to search for Java files.
     * @return A {@link List} of {@link Path}s, each pointing to a Java source file.
     * @throws IOException if an I/O error occurs during file system traversal.
     */
    protected List<Path> getJavaClasses(final Path rootPath) throws IOException {
        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java"))
            .collect(Collectors.toList());
        }
    }
}
