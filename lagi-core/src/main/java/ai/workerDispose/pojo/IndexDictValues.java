package ai.workerDispose.pojo;

import lombok.*;

import java.util.List;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class IndexDictValues {
    private Integer did;
    private String plainText;
    private List<Node> nodes;
}
