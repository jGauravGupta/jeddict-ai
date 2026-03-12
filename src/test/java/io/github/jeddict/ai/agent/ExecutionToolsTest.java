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

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.test.TestBase;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.BDDAssertions.then;

@CacioTest
public class ExecutionToolsTest extends TestBase {

    private static final String MAIN_CLASS = "com.example.Main";

    private ExecutionTools tools(String dir) throws Exception {
        return new ExecutionTools(dir, "test-project", "mvn install", "mvn test");
    }

    // -------------------------------------------------------------------------
    // Maven
    // -------------------------------------------------------------------------

    @Test
    public void resolveRunCommand_maven_without_wrapper_returns_mvn_exec()
    throws Exception {
        // pom.xml is already present in the minimal test project (no mvnw)
        new File(projectDir, "mvnw").delete();
        new File(projectDir, "mvnw.cmd").delete();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isEqualTo("mvn exec:java -Dexec.mainClass=" + MAIN_CLASS);
        });
    }

    @Test
    public void resolveRunCommand_maven_with_unix_wrapper_returns_mvnw()
    throws Exception {
        new File(projectDir, "mvnw").createNewFile();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isEqualTo("./mvnw exec:java -Dexec.mainClass=" + MAIN_CLASS);
        });
    }

    @Test
    public void resolveRunCommand_maven_with_windows_wrapper_returns_mvnw_cmd()
    throws Exception {
        new File(projectDir, "mvnw.cmd").createNewFile();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Windows 10");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isEqualTo("mvnw.cmd exec:java -Dexec.mainClass=" + MAIN_CLASS);
        });
    }

    // -------------------------------------------------------------------------
    // Gradle
    // -------------------------------------------------------------------------

    @Test
    public void resolveRunCommand_gradle_without_wrapper_returns_gradle_run()
    throws Exception {
        // Remove pom.xml so Gradle is detected instead
        new File(projectDir, "pom.xml").delete();
        new File(projectDir, "build.gradle").createNewFile();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isEqualTo("gradle run --main-class=" + MAIN_CLASS);
        });
    }

    @Test
    public void resolveRunCommand_gradle_with_unix_wrapper_returns_gradlew()
    throws Exception {
        new File(projectDir, "pom.xml").delete();
        new File(projectDir, "build.gradle.kts").createNewFile();
        new File(projectDir, "gradlew").createNewFile();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isEqualTo("./gradlew run --main-class=" + MAIN_CLASS);
        });
    }

    @Test
    public void resolveRunCommand_gradle_with_windows_wrapper_returns_gradlew_bat()
    throws Exception {
        new File(projectDir, "pom.xml").delete();
        new File(projectDir, "build.gradle").createNewFile();
        new File(projectDir, "gradlew.bat").createNewFile();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Windows 10");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isEqualTo("gradlew.bat run --main-class=" + MAIN_CLASS);
        });
    }

    // -------------------------------------------------------------------------
    // Ant
    // -------------------------------------------------------------------------

    @Test
    public void resolveRunCommand_ant_returns_ant_run()
    throws Exception {
        new File(projectDir, "pom.xml").delete();
        new File(projectDir, "build.xml").createNewFile();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isEqualTo("ant run -Dmain.class=" + MAIN_CLASS);
        });
    }

    // -------------------------------------------------------------------------
    // Fallback: direct java -cp
    // -------------------------------------------------------------------------

    @Test
    public void resolveRunCommand_no_build_system_with_jar_returns_java_cp()
    throws Exception {
        new File(projectDir, "pom.xml").delete();
        Path targetDir = Files.createDirectories(Path.of(projectDir, "target"));
        File jar = targetDir.resolve("myapp-1.0.jar").toFile();
        jar.createNewFile();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).startsWith("java -cp \"").endsWith("\" " + MAIN_CLASS);
            then(cmd).contains("myapp-1.0.jar");
        });
    }

    @Test
    public void resolveRunCommand_no_build_system_no_jar_returns_null()
    throws Exception {
        new File(projectDir, "pom.xml").delete();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String cmd = tools(projectDir).resolveRunCommand(MAIN_CLASS);
            then(cmd).isNull();
        });
    }

    // -------------------------------------------------------------------------
    // runJavaClass null-command path
    // -------------------------------------------------------------------------

    @Test
    public void runJavaClass_with_no_build_system_returns_helpful_message()
    throws Exception {
        new File(projectDir, "pom.xml").delete();

        restoreSystemProperties(() -> {
            System.setProperty("os.name", "Linux");
            String result = tools(projectDir).runJavaClass(MAIN_CLASS);
            then(result).contains("Cannot run").contains(MAIN_CLASS);
        });
    }
}
