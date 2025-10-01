package site.icebang.domain.log.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import site.icebang.domain.workflow.dto.ExecutionLogDto;
import site.icebang.domain.workflow.dto.log.WorkflowLogQueryCriteriaDto;

@Mapper
public interface ExecutionLogMapper {
  List<ExecutionLogDto> selectLogsByCriteria(WorkflowLogQueryCriteriaDto criteria);
}
