package jenkins.plugins.mttr;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import jenkins.plugins.model.AggregateBuildMetric;
import jenkins.plugins.model.BuildMessage;
import jenkins.plugins.model.MTTRMetric;
import jenkins.plugins.util.ReadUtil;
import jenkins.plugins.util.StoreUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class MTTRAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(MTTRAction.class.getName());

    public static final String MTTR_LAST_7_DAYS = "last7days";
    public static final String MTTR_LAST_30_DAYS = "last30days";
    public static final String MTTR_ALL_BUILDS = "allBuilds";
    public static final String ALL_BUILDS_FILE_NAME = "all_builds.mr";

    private AbstractProject project;

    public MTTRAction(AbstractProject project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    public List<String> getShowResult() throws IOException {
        Properties properties = ReadUtil.getJobProperties(project);
        if (properties == null) {
            LOGGER.info("property file can't find");
            return Lists.newArrayList(Messages.canNotGetResult());
        }

        List<String> result = Lists.newArrayList();
        long last7days = Long.valueOf(properties.get(MTTR_LAST_7_DAYS).toString());
        result.add(Messages.last7DaysBuildsResult(Util.getPastTimeString(last7days)));
        long last30days = Long.valueOf(properties.get(MTTR_LAST_30_DAYS).toString());
        result.add(Messages.last30DaysBuildsResult(Util.getPastTimeString(last30days)));
        long allBuilds = Long.valueOf(properties.get(MTTR_ALL_BUILDS).toString());
        result.add(Messages.allBuildsResult(Util.getPastTimeString(allBuilds)));
        return result;
    }

    @Extension
    public static final class ProjectActionFactory extends TransientProjectActionFactory {

        @Override
        public Collection<? extends Action> createFor(AbstractProject target) {
            return Collections.singleton(new MTTRAction(target));
        }
    }

    @Extension
    public static class RunListenerImpl extends RunListener<Run> {

        public RunListenerImpl() {
        }

        public void onCompleted(Run run, TaskListener listener) {
            File storeFile = new File(run.getParent().getRootDir().getAbsolutePath()
                    + File.separator + ALL_BUILDS_FILE_NAME);

            StoreUtil.storeBuildMessages(storeFile, run);

            List<BuildMessage> buildMessages = ReadUtil.getBuildMessageFrom(storeFile);

            AggregateBuildMetric last7DayInfo = new MTTRMetric(MTTR_LAST_7_DAYS, cutListByAgoDays(buildMessages, -7));

            AggregateBuildMetric last30DayInfo = new MTTRMetric(MTTR_LAST_30_DAYS, cutListByAgoDays(buildMessages, -30));

            AggregateBuildMetric allFailedInfo = new MTTRMetric(MTTR_ALL_BUILDS, buildMessages);

            StoreUtil.storeBuildMetric(run, last7DayInfo, last30DayInfo, allFailedInfo);
        }

        private List<BuildMessage> cutListByAgoDays(List<BuildMessage> builds, int daysAgo) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, daysAgo);

            List<BuildMessage> subList = Lists.newArrayList();
            for (BuildMessage build : builds) {
                if (build.getStartTime() > calendar.getTimeInMillis()) {
                    subList.add(build);
                }
            }
            return subList;
        }

    }
}
