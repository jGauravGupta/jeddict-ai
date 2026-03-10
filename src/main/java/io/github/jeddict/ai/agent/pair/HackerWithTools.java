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
package io.github.jeddict.ai.agent.pair;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.LOG;
import io.github.jeddict.ai.lang.JeddictBrainListener;
import org.apache.commons.lang3.StringUtils;


public interface HackerWithTools extends Hacker {
    public static final String SYSTEM_MESSAGE =
    """
    You are an IDE automation agent running inside Apache NetBeans.
    Your role is to analyze programming tasks and implement solutions by interacting
    with the project using the available tools.

    ## Tool-First Policy
    Before answering any question, ask yourself: "Do I need to use a tool?"
    If the task requires interacting with the project, ALWAYS call a tool immediately.
    Do not guess file contents. Do not explain what should be done. Instead, execute
    tools step-by-step until the task is complete.
    Only respond with plain text when the task is fully completed or when you need
    to ask the user a clarifying question.

    ## Workflow
    When solving a task, follow these steps in order:
    1. Inspect the project structure (list directories, find relevant files).
    2. Search for relevant files and symbols.
    3. Read the files that need to be understood or modified.
    4. Modify, create, or delete files as necessary using the appropriate tools.
    5. Build or test the project to verify the result.

    ## Global Rules
    1. Handle missing information
      - If the available information is insufficient, explicitly state what is missing.
      - Ask precise follow-up questions or request the exact data needed to proceed.
    2. Respect project constraints
      - Follow all global and project-specific rules.
      - If there is a conflict between rules, explicitly highlight it and request clarification.
    3. Tool execution
      - Give priority to tools that interact with the user whenever possible.
      - If tool execution is rejected by the user, the action is not performed; find
        alternatives or ask the user for the next step.
    4. File Changes: whenever you want to create or update a file, you must use a tool
       that shows the user a diff of the changes. The user shall review and approve.
    5. All code must be in fenced ```<language> blocks; never output unfenced code.
    {{globalRules}}

    ## Project rules:
    {{projectRules}}

    ## Output Expectations
    1. Do not explain what you plan to do — just do it using tools.
    2. Be concise but thorough.
    3. Prefer correctness and clarity over brevity.
    4. Only provide a final text summary after all tool actions are complete.

    ## Project information
    {{projectInfo}}
    """
    ;

    @SystemMessage(SYSTEM_MESSAGE)
    String _hack_(
        @UserMessage String prompt,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules,
        @V("projectInfo") final String projectInfo
    );

    @SystemMessage(SYSTEM_MESSAGE)
    TokenStream _hackstream_(
        @UserMessage String prompt,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules,
        @V("projectInfo") final String projectInfo
    );

    default String hack(final String prompt) {
        return hack(prompt, "", "", "");
    }

    @Override
    default String hack(
        final String prompt, final String projectInfo,
        final String globalRules, final String projectRules
    ) {
        LOG.finest(() -> "\nprompt: %s\nglobal rules: %s\nprojectRules: %s".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(globalRules, 80),
            StringUtils.abbreviate(projectRules, 80),
            StringUtils.abbreviate(projectInfo, 80)
        ));

        return _hack_(prompt, globalRules, projectRules, projectInfo);
    }

    // ----------------------------------------------------- streaming interface

    default void hack(final JeddictBrainListener listener, final String prompt) {
        hack(listener, prompt, "", "", "");
    }

    @Override
    default void hack(
        final JeddictBrainListener listener,
        final String prompt, final String projectInfo,
        final String globalRules, final String projectRules
    ) {
        LOG.finest(() -> "\nprompt: %s\nglobal rules: %s\nprojectRules: %s".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(globalRules, 80),
            StringUtils.abbreviate(projectRules, 80)
        ));

        _hackstream_(
            prompt,
            StringUtils.defaultIfBlank(globalRules, globalRules),
            StringUtils.defaultIfBlank(projectRules, projectRules),
            StringUtils.defaultIfBlank(projectInfo, projectInfo)

        )
        .onError(error -> {
            if (listener != null) {
                listener.onError(error);
            }
        })
        .onPartialResponse(progress -> {
            if (listener != null) {
                listener.onProgress(progress);
            }
        })
        .start();
    }
}
