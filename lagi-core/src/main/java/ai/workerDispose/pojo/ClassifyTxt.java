package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ClassifyTxt {
    private Integer nid;
    private Integer node_id;
    private Integer id;
    private String desc;
    private String node_text;
    private Integer sub_node_table_index;
}
