/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.testfixtures

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.tasks.TaskAction
import org.gradle.model.Model
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelType
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.Resources
import org.junit.Rule
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean

class ProjectBuilderTest extends Specification {
    @Rule public final TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider()
    @Rule public final Resources resources = new Resources()

    def canCreateARootProject() {

        when:
        def project = ProjectBuilder.builder().build()

        then:
        project instanceof DefaultProject
        project.name == 'test'
        project.path == ':'
        project.projectDir.parentFile != null
        project.gradle != null
        project.gradle.rootProject == project
        project.gradle.gradleHomeDir == project.file('gradleHome')
        project.gradle.gradleUserHomeDir == project.file('userHome')
    }

    private Project buildProject() {
        ProjectBuilder.builder().withProjectDir(temporaryFolder.testDirectory).build()
    }

    def canCreateARootProjectWithAGivenProjectDir() {
        when:
        def project = buildProject()

        then:
        project.projectDir == temporaryFolder.testDirectory
        project.gradle.gradleHomeDir == project.file('gradleHome')
        project.gradle.gradleUserHomeDir == project.file('userHome')
    }

    def canApplyACustomPluginUsingClass() {
        when:
        def project = buildProject()
        project.apply plugin: CustomPlugin

        then:
        project.tasks.hello instanceof DefaultTask
    }

    def canApplyACustomPluginById() {
        when:
        def project = buildProject()
        project.apply plugin: 'custom-plugin'

        then:
        project.tasks.hello instanceof DefaultTask
    }

    def canApplyACustomPluginByType() {
        when:
        def project = buildProject()
        project.apply type: CustomPlugin

        then:
        project.tasks.hello instanceof DefaultTask
    }

    def canApplyARuleSourceById() {
        when:
        def project = buildProject()
        project.apply plugin: 'custom-rule-source'

        then:
        project.modelRegistry.get(ModelPath.path("foo"), ModelType.of(String)) == "bar"
    }

    def canApplyARuleSourceByType() {
        when:
        def project = buildProject()
        project.apply type: CustomRuleSource

        then:
        project.modelRegistry.get(ModelPath.path("foo"), ModelType.of(String)) == "bar"
    }

    def cannotApplyATypeThatIsNeitherAPluginNorARuleSource() {
        when:
        def project = buildProject()
        project.apply type: String

        then:
        PluginApplicationException e = thrown()
        e.cause instanceof IllegalArgumentException
        e.cause.message == "${String.name} is neither a plugin or a rule source and cannot be applied."
    }

    def cannotApplyARuleSourceToANonModelRuleScopeElement() {
        when:
        def project = buildProject()
        project.gradle.apply plugin: "custom-rule-source"

        then:
        PluginApplicationException e = thrown()
        e.cause instanceof IllegalArgumentException
        e.cause.message == "'${CustomRuleSource.name}' does not implement the Plugin interface and only classes that implement it can be applied to 'build 'test''"
    }

    def usefulMessageIsPresentedWhenApplyingRuleSourceOnlyTypeAsAPlugin() {
        when:
        def project = buildProject()
        project.apply plugin: CustomRuleSource

        then:
        IllegalArgumentException e = thrown()
        e.message == "'${CustomRuleSource.name}' is a rule source only type, use 'type' key instead of 'plugin' key to apply it via PluginAware.apply()"
    }

    def canCreateAndExecuteACustomTask() {
        when:
        def project = buildProject()
        def task = project.task('custom', type: CustomTask)
        task.doStuff()

        then:
        task.property == 'some value'
    }

    def canApplyABuildScript() {
        when:
        def project = buildProject()
        project.apply from: resources.getResource('ProjectBuilderTest.gradle')

        then:
        project.tasks.hello instanceof DefaultTask
    }

    def "Can trigger afterEvaluate programmatically"() {
        setup:
        def latch = new AtomicBoolean(false)

        when:
        def project = buildProject()

        project.afterEvaluate {
            latch.getAndSet(true)
        }

        project.evaluate()

        then:
        noExceptionThrown()
        latch.get()
    }

    @Ignore
    @Issue("GRADLE-3136")
    def "Can trigger afterEvaluate programmatically after calling getTasksByName"() {
        setup:
        def latch = new AtomicBoolean(false)

        when:
        def project = buildProject()

        project.getTasksByName('myTask', true)

        project.afterEvaluate {
            latch.getAndSet(true)
        }

        project.evaluate()

        then:
        noExceptionThrown()
        latch.get()
    }
}

public class CustomTask extends DefaultTask {
    def String property

    @TaskAction
    def doStuff() {
        property = 'some value'
    }
}

public class CustomPlugin implements Plugin<Project> {
    void apply(Project target) {
        target.task('hello');
    }
}

@RuleSource
public class CustomRuleSource {

    @Model
    String foo() {
        "bar"
    }
}