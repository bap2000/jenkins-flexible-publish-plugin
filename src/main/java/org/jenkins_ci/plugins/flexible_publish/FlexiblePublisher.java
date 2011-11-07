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
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FlexiblePublisher extends Recorder {

    public static final String PROMOTION_JOB_TYPE = "hudson.plugins.promoted_builds.PromotionProcess";

    private ArrayList<ConditionalPublisher> publishers;

    @DataBoundConstructor
    public FlexiblePublisher(final ArrayList<ConditionalPublisher> publishers) {
        this.publishers = publishers;
    }

    public ArrayList<ConditionalPublisher> getPublishers() {
        return publishers;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public Collection getProjectActions(final AbstractProject<?, ?> project) {
        final List actions = new ArrayList();
        for (ConditionalPublisher publisher : publishers)
            actions.addAll(publisher.getProjectActions(project));
        return actions;
    }

    @Override
    public boolean prebuild(final AbstractBuild<?, ?> build, final BuildListener listener) {
        for (ConditionalPublisher publisher : publishers)
            if (!publisher.prebuild(build, listener))
                return false;
        return true;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
                                                                                                throws InterruptedException, IOException {
        for (ConditionalPublisher publisher : publishers)
            if (!publisher.perform(build, launcher, listener))
                return false;
        return true;
    }

    @Extension(ordinal = Integer.MAX_VALUE - 500)
    public static class FlexiblePublisherDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return Messages.publisher_displayName();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            //@TODO enable for matrix builds - requires aggregation
//            return FreeStyleProject.class.equals(aClass);
            return !MatrixProject.class.equals(aClass) && !PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }

    }

}
