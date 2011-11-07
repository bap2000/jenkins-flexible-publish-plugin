/*
 * The MIT License
 *
 * Copyright (C) 2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkins_ci.plugins.flexible_publish;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConditionalPublisher implements Describable<ConditionalPublisher> {

    private static String getDisplayName(final Describable describable) {
        return describable.getDescriptor().getDisplayName();
    }

    private final RunCondition condition;
    private final Publisher publisher;

    @DataBoundConstructor
    public ConditionalPublisher(final RunCondition condition, final Publisher publisher) {
        this.condition = condition;
        this.publisher = publisher;
    }

    public RunCondition getCondition() {
        return condition;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public ConditionalPublisherDescriptor getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(ConditionalPublisherDescriptor.class);
    }

    public Collection<? extends Action> getProjectActions(final AbstractProject<?, ?> project) {
        return publisher.getProjectActions(project);
    }

    public boolean prebuild(final AbstractBuild<?, ?> build, final BuildListener listener) {
        if (condition.runPrebuild(build, listener)) {
            logRunning(listener, Messages.stage_prebuild());
            return publisher.prebuild(build, listener);
        } else {
            logNotRunning(listener, Messages.stage_prebuild());
            return true;
        }
    }

    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
                                                                                                throws InterruptedException, IOException {
        if (condition.runPerform(build, listener)) {
            logRunning(listener, Messages.stage_perform());
            return publisher.perform(build, launcher, listener);
        } else {
            logNotRunning(listener, Messages.stage_perform());
            return true;
        }
    }

    private void logRunning(final BuildListener listener, final String stage) {
        listener.getLogger().println(Messages.condition_true(getDisplayName(condition), stage, getDisplayName(publisher)));
    }

    private void logNotRunning(final BuildListener listener, final String stage) {
        listener.getLogger().println(Messages.condition_false(getDisplayName(condition), stage, getDisplayName(publisher)));
    }

    @Extension
    public static class ConditionalPublisherDescriptor extends Descriptor<ConditionalPublisher> {

        @Override
        public String getDisplayName() {
            return "Never seen - one hopes :-)";
        }

        public List<? extends Descriptor<? extends RunCondition>> getRunConditions() {
            return RunCondition.all();
        }

        public List<? extends Descriptor<? extends Publisher>> getAllowedPublishers() {
            final List<BuildStepDescriptor<? extends Publisher>> publishers = new ArrayList<BuildStepDescriptor<? extends Publisher>>();
            AbstractProject project = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);
            for (Descriptor descriptor : Publisher.all()) {
                if (descriptor instanceof FlexiblePublisher.FlexiblePublisherDescriptor) continue;
                if (!(descriptor instanceof BuildStepDescriptor)) continue;
                BuildStepDescriptor<? extends Publisher> buildStepDescriptor = (BuildStepDescriptor) descriptor;
                // would be nice to refuse if needsToRunAfterFinalized - but that's on the publisher which does not yet exist!
                if ((project != null) && buildStepDescriptor.isApplicable(project.getClass())) {
                    if (hasDbc(buildStepDescriptor.clazz))
                        publishers.add(buildStepDescriptor);
                }
            }
            return publishers;
        }

        private boolean hasDbc(final Class<?> clazz) {
            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (constructor.isAnnotationPresent(DataBoundConstructor.class))
                    return true;
            }
            return false;
        }

    }

}
