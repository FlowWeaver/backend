package site.icebang.domain.workflow.mapper;

import org.apache.ibatis.annotations.Mapper;

import site.icebang.domain.workflow.model.TaskRun;

import java.util.Optional;

@Mapper
public interface TaskRunMapper {
  void insert(TaskRun taskRun);

  void update(TaskRun taskRun);

  Optional<TaskRun> findLatestSuccessRunInJob(Long jobRunId, String taskName);
}
