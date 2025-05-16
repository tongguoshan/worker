package ai.workerDispose.pojo;

import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(exclude = {"tableIndex", "subId"})
public class DictValue {
    private Integer did;
    private String plainText;
    private Integer tableIndex;
    private Integer subId;

    public DictValue(Integer did, String plainText) {
        this.did = did;
        this.plainText = plainText;
    }
}
