package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
public class DictValue {
    private Integer did;
    private String plainText;
}
