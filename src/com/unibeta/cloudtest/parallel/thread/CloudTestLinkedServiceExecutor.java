package com.unibeta.cloudtest.parallel.thread;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.CloudCaseInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestOutput.ResultStatistics;
import com.unibeta.cloudtest.parallel.mapred.Server;
import com.unibeta.cloudtest.parallel.mapred.Task;
import com.unibeta.cloudtest.parallel.util.LocalParallelJobUtil;
import com.unibeta.vrules.utils.CommonUtils;

public class CloudTestLinkedServiceExecutor implements Runnable {

    Server server = null;
    private boolean isShutdown = false;

    private CloudTestOutput cloudTestOutput;
    private Logger log = Logger.getLogger(this.getClass());

    public CloudTestLinkedServiceExecutor(Server server) {

        this.server = server;
    }

    public void shutdown() {

        isShutdown = true;
    }

    public void run() {

        while (!this.server.getTaskQueue().isEmpty() || !isShutdown) {

            Task task = null;

            task = this.server.getTaskQueue().poll();
            if (null == task) {
                continue;
            }

            CloudCaseInput input = new CloudCaseInput();

            /* synchronized (task) */{
                task.setOwner(server.getId());
                task.setStatus(Task.STATUS_SUBMITTED);
                task.getHistoryOwners().add(task.getOwner());

                server.setIdle(false);
                server.setCurrentTask(task);

                input.setFileName(LocalParallelJobUtil.wrapWithToken(task.getCaseUri()));
                input.setCaseId(task.getCaseId());
                task.setStatus(Task.STATUS_INPROCESS);
            }

            log.info("Server [" + server.getId() + "] is processing ["
                    + task.getCaseUri()+ "]. " + this.server.getTaskQueue().size()
                    + " task-blocks remaining in queue.");

            try {
                cloudTestOutput = this.server.getTestService().doTest(input);
            } catch (Exception e) {
                log.error("Task [" + task.getCaseUri()
                        + "] was executed failure by " + task.getOwner()
                        + ", caused by " + e.getMessage());

                if (cloudTestOutput == null) {
                    task.setStatus(Task.STATUS_FAILED);
                    task.setMessage(e.getMessage());
                }

                continue;
            }

            /* synchronized (task) */{

                task.setResult(cloudTestOutput);
                server.setIdle(true);
                server.setCurrentTask(null);

                if (CommonUtils
                        .isNullOrEmpty(cloudTestOutput.getErrorMessage())) {
                    task.setStatus(Task.STATUS_DONE);
                    task.setOwner(server.getId());

                    ResultStatistics resultStatistics = cloudTestOutput
                            .getResultStatistics() == null ? new ResultStatistics()
                            : cloudTestOutput.getResultStatistics();
                    log.info("Task [" + task.getCaseUri() + "] was done by "
                            + task.getOwner() + " in "
                            + resultStatistics.getDurationTime()+ "s.");
                } else {
                    log.info("Task [" + task.getCaseUri()
                            + "] was executed failure by " + task.getOwner()
                            + ", caused by "
                            + cloudTestOutput.getErrorMessage());
                    task.setStatus(Task.STATUS_FAILED);
                    task.setMessage(cloudTestOutput.getErrorMessage());
                }
            }
        }

    }

}
