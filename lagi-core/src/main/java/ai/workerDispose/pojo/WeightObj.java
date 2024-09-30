package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WeightObj{
    private Integer nid;
    private Integer did;
    private Double weight;
}
