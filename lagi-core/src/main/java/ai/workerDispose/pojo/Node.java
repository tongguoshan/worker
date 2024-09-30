package ai.workerDispose.pojo;

import lombok.*;

import java.util.List;
import java.util.Objects;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Node {
    private Integer nid;
    private Double weight;
    private String node;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node dict = (Node) obj;
        return Objects.equals(nid, dict.nid) && Objects.equals(nid, dict.nid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nid, nid);
    }
}
