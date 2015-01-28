/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.performance

import spock.lang.Unroll

import static org.gradle.performance.measure.Duration.millis

class FirstBuildPerformanceTest extends AbstractCrossVersionPerformanceTest {
    @Unroll("Project '#testProject' first use")
    def "build"() {
        // This is just an approximation of first use. We simply recompile the scripts
        given:
        runner.testId = "first use $testProject"
        runner.testProject = testProject
        runner.tasksToRun = ['help']
        runner.args = ['--recompile-scripts']
        runner.maxExecutionTimeRegression = maxExecutionTimeRegression
        runner.targetVersions = ['1.0', '2.0', '2.1', '2.2', '2.2.1', 'last']

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject    | maxExecutionTimeRegression
        "manyProjects" | millis(500)
    }
}