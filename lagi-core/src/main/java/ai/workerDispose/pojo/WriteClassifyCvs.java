package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WriteClassifyCvs {
    private String id;
    private String name;
    private String desc;
    private String type;
}
