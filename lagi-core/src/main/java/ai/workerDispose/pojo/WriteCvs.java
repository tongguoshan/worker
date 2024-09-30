package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WriteCvs {
    private String id;
    private String name;
    private String comments;
    private String dimension;

}
