package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Generator {
    private String parentId;
    private String childId;
    private String parent;
    private String child;
}
