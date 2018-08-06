package com.unibeta.cloudtest.parallel.mapred;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * Divide tasks and combine the results processor.
 * 
 * @author jordan.xue
 */
public class JobMapReducer {

    static Logger log = Logger.getLogger(JobMapReducer.class);

    public static Map<String, Task> map(String filePath) {

        Map<String, Task> map = new HashMap<String, Task>();

        File file = new File(filePath);
        if (file.isFile()) {
            String id = getContextedURI(filePath);
            Task t = new Task();

            t.setId(id);
            t.setCaseUri(id);
            t.setBlockSize(calculateBlockSize(t.getCaseUri()));

            map.put(id, t);

            return map;
        }

        List<String> fileList = CloudTestUtils.getAllFilePathListInFolder(
                filePath, true);

        List<String> pathList = new ArrayList<String>();

        for (String str : fileList) {
            File f = new File(str);
            String parent = f.getParent();
            parent = resolveTaskBlockType(f);

            pathList.add(parent);
        }

        Collections.sort(pathList, new FilePathComparator());

        for (String str : pathList) {

            if (!checkContains(map, str)) {

                String id = getContextedURI(str);

                Task t = new Task();

                t.setId(id);
                t.setCaseUri(id);
                t.setBlockSize(calculateBlockSize(t.getCaseUri()));

                map.put(id, t);
            }
        }

        return map;
    }

    private static String resolveTaskBlockType(File f) {

        String parent;
        try {
            String type = PluginConfigProxy
                    .getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_MAPRED_TASK_BLOCK_TYPE);
            if (!CommonUtils.isNullOrEmpty(type)
                    && CloudTestConstants.CLOUDTEST_PARALLEL_MAPRED_TASK_BLOCK_TYPE_FILE
                            .equalsIgnoreCase(type)) {
                parent = f.getPath();
            } else {
                parent = f.getParent();
            }
        } catch (Exception e) {
            parent = f.getParent();
            log.warn("set cloudtest.parallel.mapred.task.block.type as 'folder' by default, caused by "
                    + e.getMessage());
        }
        return parent;
    }

    private static String getContextedURI(String str) {

        int i = str.replace("\\", "/").indexOf(
                ConfigurationProxy.getCloudTestRootPath().replace("\\", "/"));
        if (i < 0) {
            i = 0;
        }

        String id = str.substring(
                i
                        + ConfigurationProxy.getCloudTestRootPath()
                                .replace("\\", "/").length())
                .replace("\\", "/");
        return id;
    }

    private static long calculateBlockSize(String caseUri) {

        List<String> fileList = CloudTestUtils
                .getAllFilePathListInFolder(
                        ConfigurationProxy.getCloudTestRootPath() + "/"
                                + caseUri, true);
        long size = 0;

        for (String f : fileList) {
            size += new File(f).length();
        }

        return size;
    }

    public static CloudTestOutput reduce(Map<String, Task> map) {

        CloudTestOutput output = new CloudTestOutput();

        for (Task t : map.values()) {

            signTaskOwnerToResult(t);
            if (output.getTestCaseResults() == null) {
                output.setTestCaseResults(new ArrayList<CloudTestOutput>());
            }

            if (t.getStatus() == Task.STATUS_REJECTED
                    && !TestService.CLOUDTEST_ERROR_MESSAGE_NO_TEST_CASE_WAS_FOUND
                            .equalsIgnoreCase(t.getMessage())) {
                output.setErrorMessage((output.getErrorMessage() == null ? ""
                        : output.getErrorMessage() + "\n")
                        + t.getId()
                        + " was rejected.\nCaused by:"
                        + t.getMessage()
                        + "\n\n");
            } else if (null != t.getResult()) {
                if (CommonUtils.isNullOrEmpty(t.getResult().getErrorMessage())) {
                    output.getTestCaseResults().addAll(
                            t.getResult().getTestCaseResults());
                } else {
                    output.setErrorMessage((output.getErrorMessage() == null ? ""
                            : output.getErrorMessage() + ", ")
                            + "Error from server node["
                            + t.getOwner()
                            + "]:"
                            + t.getResult().getErrorMessage());
                }
            } else {
                output.setErrorMessage((output.getErrorMessage() == null ? ""
                        : output.getErrorMessage() + ", ")
                        + (t.getId() + " executed with unknow problem."));
            }

        }

        return output;

    }

    private static void signTaskOwnerToResult(Task t) {

        if (t != null && t.getResult() != null
                && null != t.getResult().getTestCaseResults()) {
            for (CloudTestOutput o : t.getResult().getTestCaseResults()) {
                o.setCasePath(t.getOwner() + CloudTestConstants.SLAVE_SERVER_NAME_SEPARATOR + o.getCasePath());
            }
        }
    }

    public static void optimize(Map<String, Task> tasks,
            Map<String, Server> servers) {

        for (Task task : tasks.values()) {
            if (isPendingTask(task.getStatus())) {

                Server ser = optimizeTaskAndServerWorkload(task, servers);

                if (null != ser) {
                    log.info("Task Optimization: Adding task [" + task.getId()
                            + "] to server [" + ser.getId() + "]");
                    ser.getTaskQueue().add(task);
                }
            }
        }
    }

    public static boolean optimize(Map<String, Server> servers) {

        Server s1 = pickLowloadServer(servers.values());
        Server s2 = pickHeavyLoadServer(servers.values());

        if (s1 != null
                && s2 != null
                && (s1.getTaskQueue().isEmpty() || s1.getTaskQueue().size() + 1 <= s2
                        .getTaskQueue().size() - 1)) {

            synchronized (s2) {
                Task t2 = s2.getTaskQueue().peek();
                if (null != t2 && !t2.getHistoryOwners().contains(s1.getId())
                        && isPendingTask(t2.getStatus())) {
                    s1.getTaskQueue().add(s2.getTaskQueue().poll());

                    log.info("Optimizing task[" + t2.getId() + "] from "
                            + s2.getId() + " to " + s1.getId());
                }
            }
        }

        return s2.getTaskQueue().isEmpty() && s1.getTaskQueue().isEmpty();
    }

    private static boolean isPendingTask(int status) {

        return status == Task.STATUS_FAILED || status == Task.STATUS_PENDING;
    }

    private static Server pickHeavyLoadServer(Collection<Server> values) {

        int i = -1;
        Server s = null;

        for (Server ser : values) {
            int size = ser.getTaskQueue().size();

            if (i <= 0) {

                i = size;
                s = ser;
            }

            if (size > i) {
                i = size;
                s = ser;
            }
        }

        return s;
    }

    private static Server pickLowloadServer(Collection<Server> values) {

        int i = -1;
        Server s = null;

        for (Server ser : values) {
            int size = ser.getTaskQueue().size();

            if (i <= 0) {

                i = size;
                s = ser;
            }

            if (size <= i) {
                i = size;
                s = ser;
            }
        }

        return s;
    }

    private static Server optimizeTaskAndServerWorkload(Task task,
            Map<String, Server> servers) {

        Server server = null;

        if (isAllTried(task, servers)) {

            task.setStatus(Task.STATUS_REJECTED);
            log.warn("Task id = ["
                    + task.getId()
                    + "] has been rejected, caused by none server node can handle it.");

            return null;
        }

        server = optimizeWorkloadFirst(task, servers);

        if (server == null) {
            server = optimizeOnlyHit(task, servers);
        }

        return server;
    }

    private static boolean isAllTried(Task task, Map<String, Server> servers) {

        int minCount = -1;

        for (Server s : servers.values()) {

            if (minCount < 0) {
                minCount = s.getTaskQueue().size();
            }

            if (!task.getHistoryOwners().contains(s.getId())) {
                return false;
            }

        }
        return true;
    }

    private static Server optimizeWorkloadFirst(Task task,
            Map<String, Server> servers) {

        long minCount = -1;

        Server server = null;
        for (Server s : servers.values()) {

            if (minCount < 0) {
                minCount = s.getTasksBlockSize();
            }

            long size = s.getTasksBlockSize();

            if (size <= minCount
                    && !task.getHistoryOwners().contains(s.getId())) {

                minCount = size;

                server = s;
            }
        }

        if (null != server) {
            server.setTasksBlockSize(server.getTasksBlockSize()
                    + task.getBlockSize());
        }

        return server;
    }

    private static Server optimizeOnlyHit(Task task, Map<String, Server> servers) {

        Server server = null;
        for (Server s : servers.values()) {

            if (!task.getHistoryOwners().contains(s.getId())) {

                server = s;

                return server;
            }
        }
        return server;
    }

    static Server optimize0(Task task, Map<String, Server> servers) {

        if (task.getStatus() == Task.STATUS_PENDING
                || task.getStatus() == Task.STATUS_FAILED) {
            boolean isAllTried = true;

            for (Server s : servers.values()) {

                if (!task.getHistoryOwners().contains(s.getId())) {
                    isAllTried = false;
                }

                if (s.isIdle() && !task.getHistoryOwners().contains(s.getId())) {
                    return s;
                }
            }

            if (isAllTried) {
                task.setStatus(Task.STATUS_REJECTED);
                log.warn("Task id = ["
                        + task.getId()
                        + "] has been rejected, caused by none server node can handle it.");

                return null;
            }
        }

        return null;
    }

    private static boolean checkContains(Map<String, Task> map, String str) {

        if (str == null) {
            return false;
        }

        str = str.replace("\\", "/").replaceAll("/+", "/");
        for (String s : map.keySet()) {
            s = s.replace("\\", "/").replaceAll("/+", "/");

            if (str.equals(s) || str.contains(s)) {
                return true;
            }

        }

        return false;
    }

    static class FilePathComparator implements Comparator<String> {

        public int compare(String o1, String o2) {

            if (o1 == null || o2 == null) {
                return 0;
            }
            return (o1.length() < o2.length()) ? -1 : 1;
        }

    }

}
