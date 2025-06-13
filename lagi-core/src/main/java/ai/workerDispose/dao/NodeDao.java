package ai.workerDispose.dao;

import ai.database.impl.MysqlAdapter;
import ai.workerDispose.pojo.UniNode;

import java.util.ArrayList;
import java.util.List;

public class NodeDao extends MysqlAdapter {
    public List<UniNode> getNodeNotInZIndex(int offset, int limit) {
        String sql = " SELECT a.nid, a.comments FROM ai_unindex_node a " +
                "LEFT JOIN ai_zindex_user b ON a.nid = b.nid WHERE b.nid IS NULL LIMIT ?,?;";
        List<UniNode> list = select(UniNode.class, sql, offset, limit);
        return list != null && !list.isEmpty() ? list : new ArrayList<>();
    }
}
