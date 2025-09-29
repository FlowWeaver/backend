package site.icebang.domain.workflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.icebang.domain.workflow.mapper.TaskIoDataMapper;
import site.icebang.domain.workflow.mapper.TaskRunMapper;
import site.icebang.domain.workflow.model.JobRun;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowContextService {

    private final TaskRunMapper taskRunMapper;
    private final TaskIoDataMapper taskIoDataMapper;
    private final ObjectMapper objectMapper;

    /**
     * 특정 Job 실행 내에서, 이전에 성공한 Task의 이름으로 결과(Output)를 조회합니다.
     * @param jobRun 현재 실행중인 JobRun
     * @param sourceTaskName 결과를 조회할 이전 Task의 이름
     * @return 조회된 결과 데이터 (JsonNode)
     */
    public Optional<JsonNode> getPreviousTaskOutput(JobRun jobRun, String sourceTaskName) {
        try {
            return taskRunMapper.findLatestSuccessRunInJob(jobRun.getId(), sourceTaskName)
                    .flatMap(taskRun -> taskIoDataMapper.findOutputByTaskRunId(taskRun.getId()))
                    .map(ioData -> {
                        try {
                            return objectMapper.readTree(ioData.getDataValue());
                        } catch (Exception e) {
                            log.error("TaskIoData JSON 파싱 실패: TaskIoDataId={}", ioData.getId(), e);
                            return null;
                        }
                    });
        } catch (Exception e) {
            log.error("이전 Task 결과 조회 중 오류 발생: JobRunId={}, TaskName={}", jobRun.getId(), sourceTaskName, e);
            return Optional.empty();
        }
    }
}