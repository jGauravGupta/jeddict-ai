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
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.lang.model.element.ElementKind;
import org.apache.commons.lang3.tuple.Pair;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netbeans.api.java.source.ElementHandle;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Tests for {@link SymbolHunter}.
 * This class verifies that the {@code SymbolHunter} can correctly identify and
 * report occurrences of Java symbols (classes, methods, and fields) within a source file.
 */
public class SymbolHunterTest {

    private Set<String> collector;
    private final Consumer<String> onClass = (name) -> collector.add(name);
    private final BiConsumer<String, String> onMethod = (qname, sname) -> collector.add("m/" + qname + "." + sname);
    private final BiConsumer<String, String> onField = (qname, sname) -> collector.add("f/" + qname + "." + sname);


    /**
     * Initializes the collector set before each test.
     */
    @BeforeEach
    public void before() {
        collector = new HashSet<>();
    }

    /**
     * Tests the {@code hunt} method's ability to identify classes, methods, and fields.
     * It verifies that the correct symbols are reported for different symbol names.
     */
    @Test
    public void hunt_class_method_field() {
        final FileObject fo = FileUtil.toFileObject(
            new File("src/test/java/io/github/jeddict/ai/test/DummyProject.java").getAbsoluteFile()
        );

        final SymbolHunter hunter = new SymbolHunter(Pair.of(
            fo,
            ElementHandle.createTypeElementHandle(ElementKind.CLASS, "io.jeddict.ai.test.DummyProject")
        )).onClass(onClass).onMethod(onMethod).onField(onField);

        hunter.hunt("DummyProject");
        then(collector).containsOnly("io.jeddict.ai.test.DummyProject", "m/io.jeddict.ai.test.DummyProject.DummyProject", "m/io.jeddict.ai.test.DummyProject.DummyProject");

        collector.clear();
        hunter.hunt("getProjectDirectory");
        then(collector).containsOnly("m/io.jeddict.ai.test.DummyProject.getProjectDirectory");

        collector.clear();
        hunter.hunt("projectDir");
        then(collector).containsOnly("f/io.jeddict.ai.test.DummyProject.projectDir");

        collector.clear();
        hunter.hunt("noSymbol");
        then(collector).isEmpty();
    }
}
