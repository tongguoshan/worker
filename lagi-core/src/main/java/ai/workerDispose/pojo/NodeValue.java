package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NodeValue {
    private Integer nid;
    private String node;
    private Double weight;
    private Integer did;
    private String plainText;

}
