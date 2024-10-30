package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DictValue {
    private Integer did;
    private String plainText;
}
