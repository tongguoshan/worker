package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WriteClassifyCvs {
    private Integer nid;
    private Integer node_id;
    private Integer id;
    private String desc;
    private String node_text;
    private String type;
    private Integer sub_node_table_index;
}
