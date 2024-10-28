package ai.workerDispose.pojo;

import ai.vector.pojo.IndexRecord;
import lombok.Data;

import java.util.List;

@Data
public class VectorQueryResponse {
    private String status;
    private List<IndexRecord> data;
}
