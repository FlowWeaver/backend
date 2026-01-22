package site.icebang.domain.workflow.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import site.icebang.domain.workflow.model.WorkflowRun;

@Mapper
public interface WorkflowRunMapper {
    void insert(WorkflowRun workflowRun);

    void update(WorkflowRun workflowRun);

    List<WorkflowRun> findByStatus(@Param("status") String status);
}