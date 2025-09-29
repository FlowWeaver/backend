package site.icebang.domain.workflow.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
public class TaskIoData {
    private Long id;
    private Long taskRunId;
    private String ioType;
    private String name;
    private String dataType;
    private String dataValue; // JSON을 문자열로 저장
    private Long dataSize;
    private Instant createdAt;

    public TaskIoData(Long taskRunId, String ioType, String name, String dataType, String dataValue, Long dataSize) {
        this.taskRunId = taskRunId;
        this.ioType = ioType;
        this.name = name;
        this.dataType = dataType;
        this.dataValue = dataValue;
        this.dataSize = dataSize;
        this.createdAt = Instant.now();
    }
}
