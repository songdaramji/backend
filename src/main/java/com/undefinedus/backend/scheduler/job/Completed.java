package com.undefinedus.backend.scheduler.job;

import com.undefinedus.backend.domain.entity.Discussion;
import com.undefinedus.backend.domain.enums.DiscussionStatus;
import com.undefinedus.backend.exception.discussion.DiscussionNotFoundException;
import com.undefinedus.backend.repository.DiscussionRepository;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Completed implements Job {

    private final DiscussionRepository discussionRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Completed");
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String discussionIdStr = dataMap.getString("discussionId");

        Long discussionId = Long.parseLong(discussionIdStr);

        Discussion discussion = discussionRepository.findById(discussionId).orElseThrow(
            () -> new DiscussionNotFoundException("해당 토론을 찾지 못했습니다. : " + discussionId));


        discussion.changeStatus(DiscussionStatus.COMPLETED);

        discussionRepository.save(discussion);

    }
}