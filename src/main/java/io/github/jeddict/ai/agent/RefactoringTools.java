/**
 * Copyright 2025 the original author or authors from the Jeddict project (https://jeddict.github.io/).
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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import dev.langchain4j.agent.tool.Tool;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.*;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RefactoringSession;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.openide.util.lookup.Lookups;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;
import java.io.IOException;

/**
 * Tools for code-level operations in NetBeans projects.
 *
 * <p>
 * Provides AI-friendly operations for formatting, renaming, moving classes,
 * updating method bodies, adding Javadoc, adding fields, and other code
 * modifications.</p>
 */
public class RefactoringTools extends AbstractCodeTool {

    public RefactoringTools(final String basedir) throws IOException {
        super(basedir);
    }

    /**
     * Format a Java file using NetBeans formatter.
     *
     * <p>
     * <b>Example:</b></p>
     * <pre>
     * formatFile("src/main/java/com/example/MyClass.java");
     * // -> "File formatted successfully"
     * </pre>
     *
     * @param path relative path to the Java file
     * @return status message
     */
    @Tool("""
    JAVA ONLY: Format a Java source file using the NetBeans code formatter.
    Use this after modifying a Java file to ensure proper indentation and code style.
    """)
    @ToolPolicy(READWRITE)
    public String formatFile(String path) throws Exception {
        progress("Formatting " + path);
        return withJavaSource(path, javaSource -> {
            javaSource.runModificationTask(cc -> cc.toPhase(JavaSource.Phase.UP_TO_DATE)).commit();
            return "File formatted successfully";
        }, true);
    }

    @Tool("""
    JAVA ONLY: Rename a class in a Java source file and update all its usages across the project.
    Use this when the task requires renaming a class as part of a refactoring.
    """)
    @ToolPolicy(READWRITE)
    public String renameClass(String path, String oldName, String newName) throws Exception {
        progress("Renaming class " + oldName + " -> " + newName);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (type.getSimpleName().toString().equals(oldName)) {
                        ElementHandle<TypeElement> handle = ElementHandle.create(type);
                        RenameRefactoring refactor = new RenameRefactoring(Lookups.singleton(handle));
                        refactor.setNewName(newName);

                        RefactoringSession session = RefactoringSession.create("Rename Class");
                        refactor.prepare(session);
                        session.doRefactoring(true);

                        result.append("Class renamed from ")
                                .append(oldName)
                                .append(" to ")
                                .append(newName);
                    }
                }
            }).commit();

            return result.length() == 0
                    ? "No class named " + oldName + " found."
                    : result.toString();
        }, true);
    }

    @Tool("""
    JAVA ONLY: Rename a method in a Java class and update all its usages across the project.
    Use this when the task requires renaming a method as part of a refactoring.
    """)
    @ToolPolicy(READWRITE)
    public String renameMethod(String path, String className, String oldMethod, String newMethod)
            throws Exception {
        progress("Renaming method " + oldMethod + " -> " + newMethod + " in class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (type.getSimpleName().toString().equals(className)) {
                        for (Element member : type.getEnclosedElements()) {
                            if ((member.getKind() == javax.lang.model.element.ElementKind.METHOD || member.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
                                    && member.getSimpleName().toString().equals(oldMethod)) {
                                ElementHandle<Element> handle = ElementHandle.create(member);
                                RenameRefactoring ref = new RenameRefactoring(Lookups.singleton(handle));
                                ref.setNewName(newMethod);

                                RefactoringSession session = RefactoringSession.create("Rename Method");
                                ref.prepare(session);
                                session.doRefactoring(true);

                                result.append("Method renamed: ").append(oldMethod).append(" -> ").append(newMethod).append("\n");
                            }
                        }
                    }
                }
            }).commit();
            return result.length() == 0 ? "No method " + oldMethod + " found in " + className : result.toString();
        }, true);
    }

    @Tool("""
    JAVA ONLY: Move a Java class to a different package and update all its import statements across the project.
    Use this when the task requires moving a class to reorganize the package structure.
    """)
    @ToolPolicy(READWRITE)
    public String moveClass(String path, String className, String newPackage) throws Exception {
        progress("Moving class " + className + " to package " + newPackage);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (type.getSimpleName().toString().equals(className)) {
                        ElementHandle<TypeElement> handle = ElementHandle.create(type);
                        org.netbeans.modules.refactoring.api.MoveRefactoring ref
                                = new org.netbeans.modules.refactoring.api.MoveRefactoring(Lookups.singleton(handle));
                        ref.setTarget(Lookups.singleton(newPackage));

                        RefactoringSession session = RefactoringSession.create("Move Class");
                        ref.prepare(session);
                        session.doRefactoring(true);

                        result.append("Moved class ").append(className)
                                .append(" to package ").append(newPackage);
                    }
                }
            }).commit();
            return result.length() == 0 ? "No class " + className + " found." : result.toString();
        }, true);
    }

    @Tool("""
    JAVA ONLY: List all methods declared in a specific class within a Java source file.
    Use this to discover what methods exist in a class before modifying or replacing one.
    """)
    @ToolPolicy(READONLY)
    public String listMethods(String path, String className) throws Exception {
        progress("📋 Listing methods of class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runUserActionTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (!type.getSimpleName().toString().equals(className)) {
                        continue;
                    }

                    result.append("Methods in ").append(className).append(":\n");
                    for (Element member : type.getEnclosedElements()) {
                        if (member.getKind() == javax.lang.model.element.ElementKind.METHOD) {
                            result.append(" - ").append(member.getSimpleName()).append("\n");
                        }
                    }
                }
            }, true);
            return result.length() == 0 ? "No methods found in " + className : result.toString();
        }, true);
    }

    @Tool("""
    JAVA ONLY: Update the body of an existing method in a Java class.
    Use this to replace only the statements inside a method without touching the signature.
    Only provide the statements inside the method body, without the signature or surrounding braces.
    """)
    @ToolPolicy(READWRITE)
    public String updateMethodBody(String path, String className, String methodName, String newBody) throws Exception {
        progress("☕ Updating body of method " + methodName + " in class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                javax.swing.text.Document doc = cc.getDocument();
                if (doc == null) {
                    return;
                }

                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (!type.getSimpleName().toString().equals(className)) {
                        continue;
                    }

                    for (Element member : type.getEnclosedElements()) {
                        if ((member.getKind() == javax.lang.model.element.ElementKind.METHOD
                                || member.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
                                && member.getSimpleName().toString().equals(methodName)) {

                            TreePath memberPath = cc.getTrees().getPath(member);
                            if (memberPath == null) {
                                continue;
                            }

                            long start = cc.getTrees().getSourcePositions()
                                    .getStartPosition(cc.getCompilationUnit(), memberPath.getLeaf());
                            long end = cc.getTrees().getSourcePositions()
                                    .getEndPosition(cc.getCompilationUnit(), memberPath.getLeaf());

                            try {
                                // Replace the method body by finding the braces
                                String fullText = doc.getText((int) start, (int) (end - start));
                                int bodyStart = fullText.indexOf('{');
                                int bodyEnd = fullText.lastIndexOf('}');
                                if (bodyStart >= 0 && bodyEnd > bodyStart) {
                                    String updated = fullText.substring(0, bodyStart + 1)
                                            + "\n" + newBody + "\n"
                                            + fullText.substring(bodyEnd);
                                    doc.remove((int) start, (int) (end - start));
                                    doc.insertString((int) start, updated, null);
                                    result.append("Method body updated for ").append(methodName);
                                }
                            } catch (javax.swing.text.BadLocationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).commit();
            formatFile(path);
            return result.length() == 0 ? "No method " + methodName + " found in " + className : result.toString();
        }, true);
    }

    @Tool("""
    JAVA ONLY: Replace an existing method in a Java class with completely new code.
    Use this when the entire method signature and/or body needs to be replaced.
    Provide the full method definition including the signature, annotations, and body with braces.
    Prefer updateMethodBody when only the method body needs to change.
    """)
    @ToolPolicy(READWRITE)
    public String replaceMethod(String path, String className, String methodName, String newMethodCode) throws Exception {
        progress("☕ Replacing method " + methodName + " in class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                javax.swing.text.Document doc = cc.getDocument();
                if (doc == null) {
                    return;
                }

                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (!type.getSimpleName().toString().equals(className)) {
                        continue;
                    }

                    for (Element member : type.getEnclosedElements()) {
                        if ((member.getKind() == javax.lang.model.element.ElementKind.METHOD
                                || member.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
                                && member.getSimpleName().toString().equals(methodName)) {

                            TreePath memberPath = cc.getTrees().getPath(member);
                            if (memberPath == null) {
                                continue;
                            }

                            long start = cc.getTrees().getSourcePositions()
                                    .getStartPosition(cc.getCompilationUnit(), memberPath.getLeaf());
                            long end = cc.getTrees().getSourcePositions()
                                    .getEndPosition(cc.getCompilationUnit(), memberPath.getLeaf());

                            try {
                                // Replace the entire method text
                                doc.remove((int) start, (int) (end - start));
                                doc.insertString((int) start, newMethodCode, null);
                                result.append("Method ").append(methodName).append(" replaced in ").append(className);
                            } catch (javax.swing.text.BadLocationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).commit();
            formatFile(path);
            return result.length() == 0
                    ? "No method " + methodName + " found in " + className
                    : result.toString();
        }, true);
    }

    @Tool("Delete a method from a class by its name")
    @ToolPolicy(READWRITE)
    public String deleteMethod(String path, String className, String methodName) throws Exception {
        progress("🗑 Deleting method " + methodName + " from class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                javax.swing.text.Document doc = cc.getDocument();
                if (doc == null) {
                    return;
                }

                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (!type.getSimpleName().toString().equals(className)) {
                        continue;
                    }

                    for (Element member : type.getEnclosedElements()) {
                        if ((member.getKind() == javax.lang.model.element.ElementKind.METHOD
                                || member.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
                                && member.getSimpleName().toString().equals(methodName)) {

                            TreePath memberPath = cc.getTrees().getPath(member);
                            if (memberPath == null) {
                                continue;
                            }

                            long start = cc.getTrees().getSourcePositions()
                                    .getStartPosition(cc.getCompilationUnit(), memberPath.getLeaf());
                            long end = cc.getTrees().getSourcePositions()
                                    .getEndPosition(cc.getCompilationUnit(), memberPath.getLeaf());

                            try {
                                doc.remove((int) start, (int) (end - start));
                                result.append("Method ").append(methodName).append(" deleted from ").append(className);
                            } catch (javax.swing.text.BadLocationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).commit();
            formatFile(path);
            return result.length() == 0 ? "No method " + methodName + " found in " + className : result.toString();
        }, true);
    }

    @Tool("Add a Javadoc comment to a method")
    @ToolPolicy(READWRITE)
    public String addMethodJavadoc(String path, String className, String methodName, String javadoc) throws Exception {
        progress("☕ Adding Javadoc to method " + methodName + " in class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                javax.swing.text.Document doc = cc.getDocument();
                if (doc == null) {
                    return;
                }

                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (!type.getSimpleName().toString().equals(className)) {
                        continue;
                    }

                    for (Element member : type.getEnclosedElements()) {
                        if ((member.getKind() == javax.lang.model.element.ElementKind.METHOD
                                || member.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
                                && member.getSimpleName().toString().equals(methodName)) {

                            TreePath memberPath = cc.getTrees().getPath(member);
                            if (memberPath == null) {
                                continue;
                            }

                            long pos = cc.getTrees().getSourcePositions()
                                    .getStartPosition(cc.getCompilationUnit(), memberPath.getLeaf());

                            try {
                                // Insert the Javadoc
                                String comment = "/** " + javadoc + " */\n";
                                doc.insertString((int) pos, comment, null);
                                result.append("Javadoc added to ").append(methodName);
                            } catch (javax.swing.text.BadLocationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).commit();
            formatFile(path);

            return result.length() == 0
                    ? "No method " + methodName + " found in " + className
                    : result.toString();
        }, true);
    }

    @Tool("Add a new field to a class")
    @ToolPolicy(READWRITE)
    public String addField(String path, String className, String fieldName, String fieldType, Set<String> modifiers) throws Exception {
        progress("☕ Adding Field " + fieldType + " " + fieldName + " in class " + className);
        Set<Modifier> mods = modifiers.stream()
                .map(String::toUpperCase)
                .map(Modifier::valueOf)
                .collect(Collectors.toSet());

        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                TreeMaker make = cc.getTreeMaker();
                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (type.getSimpleName().toString().equals(className)) {
                        ClassTree clazzTree = cc.getTrees().getTree(type);
                        VariableTree field = make.Variable(
                                make.Modifiers(mods),
                                fieldName,
                                make.QualIdent(cc.getElements().getTypeElement(fieldType)),
                                null
                        );
                        ClassTree newClazz = make.addClassMember(clazzTree, field);
                        cc.rewrite(clazzTree, newClazz);
                        result.append("Field ").append(fieldName).append(" added to ").append(className);
                    }
                }
            }).commit();
            return result.length() == 0 ? "No class " + className + " found." : result.toString();
        }, true);
    }

    @Tool("Rename a field in a Java file")
    @ToolPolicy(READWRITE)
    public String renameField(String path, String className, String oldFieldName, String newFieldName) throws Exception {
        progress("✏ Renaming field " + oldFieldName + " -> " + newFieldName + " in class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (!type.getSimpleName().toString().equals(className)) {
                        continue;
                    }

                    for (Element member : type.getEnclosedElements()) {
                        if (member.getKind() == ElementKind.FIELD
                                && member.getSimpleName().toString().equals(oldFieldName)) {

                            TreePathHandle tph = TreePathHandle.create(member, cc);
                            RenameRefactoring ref = new RenameRefactoring(Lookups.singleton(tph));
                            ref.setNewName(newFieldName);

                            RefactoringSession session = RefactoringSession.create("Rename Field");
                            Problem problem = ref.checkParameters();
                            if (problem != null) {
                                result.append("Rename problem: ").append(problem.getMessage());
                                return;
                            }

                            session.doRefactoring(true);

                            result.append("Field renamed: ")
                                    .append(oldFieldName)
                                    .append(" -> ")
                                    .append(newFieldName);
                        }
                    }

                }
            }).commit();
            formatFile(path);
            return result.length() == 0
                    ? "No field " + oldFieldName + " found in " + className
                    : result.toString();
        }, true);
    }

    @Tool("Delete a field from a class by its name")
    @ToolPolicy(READWRITE)
    public String deleteField(String path, String className, String fieldName) throws Exception {
        progress("🗑 Deleting field " + fieldName + " from class " + className);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                TreeMaker make = cc.getTreeMaker();
                for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                    if (!type.getSimpleName().toString().equals(className)) {
                        continue;
                    }

                    ClassTree clazzTree = cc.getTrees().getTree(type);
                    for (Tree member : clazzTree.getMembers()) {
                        if (member instanceof VariableTree var && var.getName().toString().equals(fieldName)) {
                            ClassTree newClazz = make.removeClassMember(clazzTree, member);
                            cc.rewrite(clazzTree, newClazz);
                            result.append("Field ").append(fieldName).append(" deleted from ").append(className);
                            break;
                        }
                    }
                }
            }).commit();
            formatFile(path);
            return result.length() == 0 ? "No field " + fieldName + " found in " + className : result.toString();
        }, true);
    }

    @Tool("Add an import statement to a Java file")
    @ToolPolicy(READWRITE)
    public String addImport(String path, String importName) throws Exception {
        progress("➕ Adding import " + importName + " to " + path);
        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runModificationTask(cc -> {
                cc.toPhase(JavaSource.Phase.PARSED);
                CompilationUnitTree cut = cc.getCompilationUnit();
                TreeMaker make = cc.getTreeMaker();
                ImportTree newImport = make.Import(make.QualIdent(cc.getElements().getTypeElement(importName)), false);

                CompilationUnitTree newCut = make.addCompUnitImport(cut, newImport);
                cc.rewrite(cut, newCut);
                result.append("Import added: ").append(importName);
            }).commit();
            return result.length() == 0 ? "Failed to add import " + importName : result.toString();
        }, true);
    }

}
