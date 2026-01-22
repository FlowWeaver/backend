package site.icebang.domain.workflow.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import site.icebang.domain.workflow.model.JobRun;

@Mapper
public interface JobRunMapper {
  void insert(JobRun jobRun);

  void update(JobRun jobRun);

  JobRun findSuccessfulJobByWorkflowRunId(
      @Param("workflowRunId") Long workflowRunId,
      @Param("jobId") Long jobId
  );
}
