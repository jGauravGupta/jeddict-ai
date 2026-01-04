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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.tuple.Pair;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.openide.filesystems.FileObject;

/**
 * Utility class for searching and identifying Java symbols (classes, methods, fields)
 * within a specific {@link FileObject} using the NetBeans Java Source API.
 * It resolves {@link TypeElement}s and their enclosed {@link Element}s to find matches.
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
     * This method leverages the NetBeans Java Source API to resolve and inspect elements within the file.
     * It checks if the {@link TypeElement} itself matches the {@code symbolName}, and then
     * iterates through its enclosed elements (methods, constructors, fields) to find further matches.
     * When a match is found, the corresponding registered consumer (onClass, onMethod, onField) is invoked.
     *
     * @param symbolName The name of the symbol to hunt for (e.g., "MyClass", "myMethod", "myField").
     */
    public void hunt(final String symbolName) {

        JavaSource js = JavaSource.forFileObject(type.getLeft());
        if (js != null) {
            try {
                js.runUserActionTask(cc -> {
                    cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    TypeElement element = type.getRight().resolve(cc);
                    if (element == null) {
                        return;
                    }

                    if (element.getSimpleName().contentEquals(symbolName)) {
                        onClass.accept(element.getQualifiedName().toString());
                    }

                    for (Element e : element.getEnclosedElements()) {
                        if (!e.getSimpleName().contentEquals(symbolName)) {
                            continue;
                        }

                        if (e.getKind() == ElementKind.METHOD
                                || e.getKind() == ElementKind.CONSTRUCTOR) {
                            onMethod.accept(
                                element.getQualifiedName().toString(),
                                e.getSimpleName().toString()
                            );
                        } else if (e.getKind() == ElementKind.FIELD) {
                            onField.accept(
                                element.getQualifiedName().toString(),
                                e.getSimpleName().toString()
                            );
                        }
                    }
                }, true);
            } catch (IOException x) {
                // ignore the file
            }
        }
    }

}
