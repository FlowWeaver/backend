package site.icebang.domain.workflow.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import site.icebang.domain.workflow.model.TaskIoData;

@Mapper
public interface TaskIoDataMapper {
  void insert(TaskIoData taskIoData);

  Optional<TaskIoData> findOutputByTaskRunId(Long taskRunId);

  List<TaskIoData> findByTaskRunIds(
      @Param("taskRunIds") List<Long> taskRunIds,
      @Param("ioType") String ioType,
      @Param("limit") Integer limit);
}
