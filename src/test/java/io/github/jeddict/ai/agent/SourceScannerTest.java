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

import java.nio.file.Paths;
import java.util.List;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.tuple.Pair;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.netbeans.api.java.source.ElementHandle;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Tests for {@link SourceScanner}.
 * This class verifies the functionality of {@code SourceScanner} in identifying
 * Java {@link TypeElement}s within a given {@link FileObject} root.
 */
public class SourceScannerTest {

    /**
     * Tests that the {@code types()} method correctly scans a test directory
     * and returns the expected {@link TypeElement}s.
     * @throws Exception if an error occurs during the test.
     */
    @Test
    public void scan_test_dir_returns_types() throws Exception {
        final FileObject root =
            FileUtil.toFileObject(Paths.get("src/test/java").toAbsolutePath().normalize());

        final SourceScanner s = new SourceScanner(root);

        List<Pair<FileObject, ElementHandle<TypeElement>>> types = s.types();

        //
        // Let's make sure that expected types are there
        //
        FileObject fo = FileUtil.toFileObject(Paths.get("src/test/java/io/github/jeddict/ai/test/SayHello.java").toAbsolutePath().normalize());
        then(handle(types, fo).getBinaryName()).isEqualTo("io.github.jeddict.ai.test.SayHello");

        fo = FileUtil.toFileObject(Paths.get("src/test/java/io/github/jeddict/ai/agent/SourceScanner.java").toAbsolutePath().normalize());
        then(handle(types, fo).getBinaryName()).isEqualTo("io.github.jeddict.ai.agent.SourceScanner");
    }

    // --------------------------------------------------------- private methods

    /**
     * Helper method to find an {@link ElementHandle<TypeElement>} associated with a given {@link FileObject}.
     * @param types The list of {@link Pair}s containing {@link FileObject} and {@link ElementHandle<TypeElement>}.
     * @param fo The {@link FileObject} to match.
     * @return The matching {@link ElementHandle<TypeElement>}, or {@code null} if not found.
     */
    private ElementHandle<TypeElement> handle(
        final List<Pair<FileObject, ElementHandle<TypeElement>>> types,
        final FileObject fo
    ) {
        for (Pair<FileObject, ElementHandle<TypeElement>> t: types) {
            if (t.getLeft().toURI().equals(fo.toURI())) {
                return t.getRight();
            }
        }

        return null;
    }

}
