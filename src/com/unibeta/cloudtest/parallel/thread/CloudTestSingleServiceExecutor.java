package com.unibeta.cloudtest.parallel.thread;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.CloudCaseInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestOutput.ResultStatistics;
import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.parallel.mapred.Server;
import com.unibeta.cloudtest.parallel.mapred.Task;
import com.unibeta.cloudtest.parallel.util.LocalParallelJobUtil;
import com.unibeta.vrules.utils.CommonUtils;

public class CloudTestSingleServiceExecutor extends Thread implements Runnable {

    private static Logger log = Logger
            .getLogger(CloudTestSingleServiceExecutor.class);

    private CloudTestOutput cloudTestOutput = null;
    TestService testService = null;

    @SuppressWarnings("unused")
    private CloudTestSingleServiceExecutor() {

    }

    public CloudTestSingleServiceExecutor(TestService testService,
            String fileName) {

        this.fileName = fileName;
        this.testService = testService;

        this.task.setCaseUri(this.fileName);
        this.task.setId(task.getCaseUri());
    }

    public CloudTestSingleServiceExecutor(Server server, Task task) {

        this.server = server;
        this.task = task;
        task.setOwner(this.server.getId());

        // super.setName(task.getOwner());
        this.fileName = task.getCaseUri();
        this.testService = server.getTestService();

    }

    public CloudTestSingleServiceExecutor(TestService testService,
            String fileName, String[] caseId) {

        this.testService = testService;
        this.fileName = fileName;
        this.caseId = caseId;

        this.task.setCaseUri(this.fileName);
        this.task.setId(task.getCaseUri());
    }

    public CloudTestOutput getCloudTestOutput() {

        return cloudTestOutput;
    }

    private String fileName;
    private String[] caseId;
    private Task task = new Task();
    private Server server = new Server();

    public void run() {

        CloudCaseInput input = new CloudCaseInput();

        /* synchronized (task) */{
            task.setOwner(server.getId());
            task.setStatus(Task.STATUS_SUBMITTED);
            task.getHistoryOwners().add(task.getOwner());

            server.setIdle(false);
            server.setCurrentTask(task);

            input.setFileName(LocalParallelJobUtil.wrapWithToken(task.getCaseUri()));
            input.setCaseId(caseId);

            task.setStatus(Task.STATUS_INPROCESS);
        }

        log.info("Server [" + server.getId() + "] is processing ["
                + task.getCaseUri()+ "]. " + this.server.getTaskQueue().size()
                + " task-blocks remaining in queue.");

        try {
            cloudTestOutput = this.testService.doTest(input);
        } catch (Exception e) {
            log.error("Task [" + task.getCaseUri()
                    + "] was executed failure by " + task.getOwner()
                    + ", caused by: " + e.getMessage());

            if (cloudTestOutput == null) {
                task.setStatus(Task.STATUS_FAILED);
                task.setMessage(e.getMessage());
            }

            return;
        }

        /* synchronized (task) */{

            task.setResult(cloudTestOutput);
            server.setIdle(true);
            server.setCurrentTask(null);

            if (CommonUtils.isNullOrEmpty(cloudTestOutput
                            .getErrorMessage())) {
                task.setStatus(Task.STATUS_DONE);
                task.setOwner(server.getId());

                ResultStatistics resultStatistics = cloudTestOutput
                        .getResultStatistics() == null ? new ResultStatistics()
                        : cloudTestOutput.getResultStatistics();
                log.info("Task [" + task.getCaseUri() + "] was done by "
                        + task.getOwner() + " in "
                        + resultStatistics.getDurationTime() + "s.");
            } else {
                log.info("Task [" + task.getCaseUri()
                        + "] was executed failure by " + task.getOwner()
                        + ", caused by " + cloudTestOutput.getErrorMessage());
                task.setStatus(Task.STATUS_FAILED);
                task.setMessage(cloudTestOutput.getErrorMessage());
            }
        }

    }
}