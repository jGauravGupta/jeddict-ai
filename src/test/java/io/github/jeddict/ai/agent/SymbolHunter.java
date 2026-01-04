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

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.tuple.Pair;
import org.netbeans.api.java.source.ElementHandle;
import org.openide.filesystems.FileObject;
import static ste.lloop.Loop.on;


/**
 * Utility class for searching and identifying Java symbols (classes, methods,
 * fields) within a given {@link FileObject}.
 *
 * This class is intended as a testing replacement (a working mock) of the
 * corresponding source code, which can not be covered directly due to dependencies
 * on NB and javac compiler functionalities not easy to put under test.
 *
 * It uses simple regex patterns to locate symbol occurrences and invokes
 * registered consumers for each type of match.
 */
public class SymbolHunter {

    public final Pair<FileObject, ElementHandle<TypeElement>> type;

    private Consumer<String> onClass;
    private BiConsumer<String, String> onMethod;
    private BiConsumer<String, String> onField;


    /**
     * Constructs a new SymbolHunter for a given Java type.
     * @param type A {@link Pair} containing the {@link FileObject} of the source file
     *             and an {@link ElementHandle} to the {@link TypeElement} to be hunted.
     */
    public SymbolHunter(Pair<FileObject, ElementHandle<TypeElement>> type) {
        this.type = type;

        onClass = null;
        onMethod = onField = null;
    }

    /**
     * Sets a consumer to be invoked when a class symbol is found.
     * @param onClass A {@link Consumer} that accepts the fully qualified name of the class.
     * @return This SymbolHunter instance for method chaining.
     */
    public SymbolHunter onClass(final Consumer<String> onClass) {
        this.onClass  = onClass; return this;
    }

    /**
     * Sets a consumer to be invoked when a method symbol is found.
     * @param onMethod A {@link BiConsumer} that accepts the fully qualified class name and the method name.
     * @return This SymbolHunter instance for method chaining.
     */
    public SymbolHunter onMethod(final BiConsumer<String, String> onMethod) {
        this.onMethod  = onMethod; return this;
    }

    /**
     * Sets a consumer to be invoked when a field symbol is found.
     * @param onField A {@link BiConsumer} that accepts the fully qualified class name and the field name.
     * @return This SymbolHunter instance for method chaining.
     */
    public SymbolHunter onField(final BiConsumer<String, String> onField) {
        this.onField  = onField; return this;
    }

    /**
     * Searches the source file associated with the {@code type} for occurrences of {@code symbolName}.
     * It uses simple regex patterns to identify classes, methods, and fields.
     * When a match is found, the corresponding registered consumer (onClass, onMethod, onField) is invoked.
     * <p>
     * <b>Assumptions for Java source code formatting:</b>
     * <ul>
     *     <li>Class names are preceded by the "class" keyword and followed by a space and an opening curly brace.</li>
     *     <li>Method names are followed by an opening parenthesis.</li>
     *     <li>Field names are followed by a semicolon and not part of an assignment operation.</li>
     * </ul>
     * These assumptions simplify the regex patterns and might not cover all possible Java syntax variations.
     *
     * @param symbolName The name of the symbol to hunt for (e.g., "MyClass", "myMethod", "myField").
     */
    public void hunt(final String symbolName) {
        try {
            Pattern classPattern = Pattern.compile("class\\s+" + symbolName + "\\s+[a-z,A-z, ]*\\{");
            Pattern methodPattern = Pattern.compile("\\b" + symbolName + "\\s*\\(");
            // exlude assignments with lookahead operators
            Pattern fieldPattern = Pattern.compile("(?<![+\\-*/!=]\\s*)\\b" + symbolName + "\\s*(?!=)\\s*;");

            on(type.getLeft().asLines()).loop( line -> {
                // Check for class match
                Matcher classMatcher = classPattern.matcher(line);
                if (classMatcher.find()) {
                    onClass.accept(type.getRight().getBinaryName());
                } else {
                    // Check for method match
                    Matcher methodMatcher = methodPattern.matcher(line);
                    if (methodMatcher.find()) {
                        onMethod.accept(type.getRight().getBinaryName(), symbolName);
                    } else {
                        // Check for field match
                        Matcher fieldMatcher = fieldPattern.matcher(line);
                        if (fieldMatcher.find()) {
                            onField.accept(type.getRight().getBinaryName(), symbolName);
                        }
                    }
                }
            });
        } catch (IOException x) {
            x.printStackTrace();
        }
    }

}
