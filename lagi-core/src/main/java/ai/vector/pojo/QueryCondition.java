package ai.vector.pojo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class QueryCondition {
    private String text;
    private Integer n;
    private Map<String, String> where = new HashMap<>();
    private String category;
}
