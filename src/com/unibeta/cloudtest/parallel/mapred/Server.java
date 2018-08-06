package com.unibeta.cloudtest.parallel.mapred;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.unibeta.cloudtest.TestService;

public class Server {

    private String id = "localhost";
    private boolean idle = true;
    private Task currentTask;
    private TestService testService;
    private String desc;
    private Queue<Task> taskQueue = new LinkedBlockingQueue<Task>();
    private long tasksBlockSize;

    public long getTasksBlockSize() {

        return tasksBlockSize;
    }

    public void setTasksBlockSize(long tasksBlockSize) {

        this.tasksBlockSize = tasksBlockSize;
    }

    public Queue<Task> getTaskQueue() {

        return taskQueue;
    }

    public void setTaskQueue(Queue<Task> taskQueue) {

        this.taskQueue = taskQueue;
    }

    public String getDesc() {

        return desc;
    }

    public void setDesc(String desc) {

        this.desc = desc;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public boolean isIdle() {

        return idle;
    }

    public void setIdle(boolean idle) {

        this.idle = idle;
    }

    public Task getCurrentTask() {

        return currentTask;
    }

    public void setCurrentTask(Task currentTask) {

        this.currentTask = currentTask;
    }

    public TestService getTestService() {

        return testService;
    }

    public void setTestService(TestService testService) {

        this.testService = testService;
    }

}
