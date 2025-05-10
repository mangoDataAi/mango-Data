package com.mango.test.database.event;

import com.mango.test.database.entity.MaterializeTask;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskCreateEvent extends ApplicationEvent {
    private final MaterializeTask task;

    public TaskCreateEvent(Object source, MaterializeTask task) {
        super(source);
        this.task = task;
    }
} 