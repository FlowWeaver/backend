package site.icebang.domain.workflow.mapper;

import org.apache.ibatis.annotations.Mapper;
import site.icebang.domain.workflow.model.TaskIoData;
import java.util.Optional;

@Mapper
public interface TaskIoDataMapper {
    void insert(TaskIoData taskIoData);
    Optional<TaskIoData> findOutputByTaskRunId(Long taskRunId);
}