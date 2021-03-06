/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.logging.console.taskgrouping.rich

import org.fusesource.jansi.Ansi
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.integtests.fixtures.console.AbstractConsoleGroupedTaskFunctionalTest.StyledOutput
import org.gradle.internal.logging.console.taskgrouping.AbstractBasicGroupedTaskLoggingFunctionalTest
import spock.lang.Issue

@SuppressWarnings("IntegrationTestFixtures")
class RichConsoleBasicGroupedTaskLoggingFunctionalTest extends AbstractBasicGroupedTaskLoggingFunctionalTest {
    ConsoleOutput consoleType = ConsoleOutput.Rich

    private final StyledOutput failingTask = styled("> Task :failing", Ansi.Color.RED, Ansi.Attribute.INTENSITY_BOLD)
    private final StyledOutput succeedingTask = styled("> Task :succeeding", null, Ansi.Attribute.INTENSITY_BOLD)
    private final StyledOutput configuringProject = styled("> Configure project :", Ansi.Color.RED, Ansi.Attribute.INTENSITY_BOLD)

    @Issue("gradle/gradle#2038")
    def "tasks with no actions are not displayed"() {
        given:
        buildFile << "task log"

        when:
        succeeds('log')

        then:
        !result.groupedOutput.hasTask(':log')
    }

    def "group header is printed red if task failed"() {
        given:
        buildFile << """
            task failing { doFirst { 
                logger.quiet 'hello'
                throw new RuntimeException('Failure...')
            } }
        """

        when:
        fails('failing')

        then:
        result.groupedOutput.task(':failing').output == 'hello'
        result.assertRawOutputContains(failingTask.output)
    }

    def "group header is printed red if task failed and there is no output"() {
        given:
        buildFile << """
            task failing { doFirst { 
                throw new RuntimeException('Failure...')
            } }
        """

        when:
        fails('failing')

        then:
        result.assertRawOutputContains(failingTask.output)
    }

    def "group header is printed white if task succeeds"() {
        given:
        buildFile << """
            task succeeding { doFirst { 
                logger.quiet 'hello'
            } }
        """

        when:
        succeeds('succeeding')

        then:
        result.assertRawOutputContains(succeedingTask.output)
    }

    def "configure project group header is printed red if configuration fails with additional failures"() {
        given:
        buildFile << """
            afterEvaluate { 
                println "executing after evaluate..."
                throw new RuntimeException("After Evaluate Failure...") 
            }
            throw new RuntimeException('Config Failure...')
        """
        executer.withStacktraceDisabled()

        when:
        fails('failing')

        then:
        result.assertRawOutputContains(configuringProject.output)
    }
}
