package site.icebang.domain.workflow.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import site.icebang.domain.workflow.model.TaskIoData;

@Mapper
public interface TaskIoDataMapper {
  void insert(TaskIoData taskIoData);

  Optional<TaskIoData> findOutputByTaskRunId(Long taskRunId);
}
